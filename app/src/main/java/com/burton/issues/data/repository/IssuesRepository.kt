package com.burton.issues.data.repository

import com.burton.issues.BuildConfig
import com.burton.issues.data.backend.IssueTracker
import com.burton.issues.data.github.GitHubApi
import com.burton.issues.data.github.GitHubApiException
import com.burton.issues.data.github.GitHubAuth
import com.burton.issues.data.github.GitHubOAuth
import com.burton.issues.data.github.TokenHolder
import com.burton.issues.data.parse.TinyJson.int
import com.burton.issues.data.parse.TinyJson.str
import com.burton.issues.domain.IssueComment
import com.burton.issues.domain.IssueDetail
import com.burton.issues.domain.IssueLabel
import com.burton.issues.domain.IssueMilestone
import com.burton.issues.domain.IssuePatch
import com.burton.issues.domain.IssueQuery
import com.burton.issues.domain.IssueSummary
import com.burton.issues.domain.IssueUser
import com.burton.issues.domain.IssuesSnapshot
import com.burton.issues.domain.NewIssueDraft
import com.burton.issues.domain.NewIssueRequest
import com.burton.issues.domain.TrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IssuesRepository @Inject constructor(
    private val tracker: IssueTracker,
    private val api: GitHubApi,
    private val prefs: LocalPrefs,
    private val tokenHolder: TokenHolder,
    private val installedApps: InstalledApps,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(IssuesSnapshot())
    val state = _state.asStateFlow()
    private val lock = Mutex()
    private var started = false
    private var devicePoll: Job? = null

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val token = prefs.auth.first().accessToken
            if (token.isNotBlank()) {
                signIn(token, refreshCatalog = true)
            } else {
                _state.update { it.copy(tokenPresent = false, scanning = false) }
            }
        }
    }

    suspend fun signIn(token: String, refreshCatalog: Boolean = true) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) error("empty_token")
        tokenHolder.token = trimmed
        _state.update { it.copy(scanning = true, error = null, tokenPresent = true, deviceUserCode = "", deviceVerificationUri = "") }
        try {
            val account = tracker.currentUser()
            prefs.setAuth(GitHubAuth(trimmed))
            _state.update { it.copy(account = account, tokenPresent = true) }
            if (refreshCatalog) refreshCatalogAndInbox()
        } catch (error: Throwable) {
            tokenHolder.token = ""
            _state.update {
                it.copy(
                    tokenPresent = false,
                    scanning = false,
                    account = null,
                    error = publicError(error),
                )
            }
            throw error
        }
    }

    fun beginOAuth(): String {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val pkce = GitHubOAuth.pkce()
        scope.launch { prefs.setPendingOauth(pkce.state, pkce.verifier) }
        return GitHubOAuth.authorizeUrl(clientId, pkce.state, pkce.challenge, BuildConfig.GITHUB_REDIRECT_URI)
    }

    suspend fun completeOAuth(code: String, state: String) {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val pending = prefs.pendingOauth.first()
        if (pending == null || pending.state.isBlank() || pending.state != state) {
            error("oauth_state")
        }
        val body = api.formPost(
            GitHubOAuth.TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "code" to code,
                "redirect_uri" to BuildConfig.GITHUB_REDIRECT_URI,
                "code_verifier" to pending.verifier,
            ),
        )
        val token = body.str("access_token")
        if (token.isBlank()) {
            throw GitHubApiException("oauth", body.str("error").ifBlank { "no_token" }, body.str("error_description"))
        }
        prefs.clearPendingOauth()
        signIn(token)
    }

    suspend fun beginDeviceLogin(): GitHubOAuth.DeviceStart {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val body = api.formPost(
            GitHubOAuth.DEVICE_CODE_URL,
            mapOf("client_id" to clientId, "scope" to GitHubOAuth.SCOPE),
        )
        val start = GitHubOAuth.DeviceStart(
            deviceCode = body.str("device_code"),
            userCode = body.str("user_code"),
            verificationUri = body.str("verification_uri").ifBlank { "https://github.com/login/device" },
            intervalSeconds = body.int("interval", 5).coerceAtLeast(5),
            expiresInSeconds = body.int("expires_in", 900),
        )
        if (start.deviceCode.isBlank() || start.userCode.isBlank()) {
            throw GitHubApiException("device", body.str("error").ifBlank { "device_failed" }, body.str("error_description"))
        }
        _state.update {
            it.copy(
                deviceUserCode = start.userCode,
                deviceVerificationUri = start.verificationUri,
                error = null,
            )
        }
        devicePoll?.cancel()
        devicePoll = scope.launch { pollDevice(start) }
        return start
    }

    private suspend fun pollDevice(start: GitHubOAuth.DeviceStart) {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        var wait = start.intervalSeconds * 1000L
        val deadline = System.currentTimeMillis() + start.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(wait)
            val result = runCatching {
                api.formPost(
                    GitHubOAuth.TOKEN_URL,
                    mapOf(
                        "client_id" to clientId,
                        "device_code" to start.deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    ),
                )
            }
            val body = result.getOrNull()
            val errorCode = (result.exceptionOrNull() as? GitHubApiException)?.code
                ?: body?.str("error").orEmpty()
            val token = body?.str("access_token").orEmpty()
            if (token.isNotBlank()) {
                signIn(token)
                return
            }
            when (errorCode) {
                "authorization_pending", "" -> Unit
                "slow_down" -> wait += 5000
                "expired_token", "access_denied" -> {
                    _state.update {
                        it.copy(
                            deviceUserCode = "",
                            deviceVerificationUri = "",
                            error = if (errorCode == "access_denied") "GitHub login was cancelled." else "Device login expired. Try again.",
                        )
                    }
                    return
                }
                else -> {
                    val message = result.exceptionOrNull()?.let(::publicError)
                        ?: body?.str("error_description")?.ifBlank { body.str("error") }
                    _state.update { it.copy(error = message, deviceUserCode = "", deviceVerificationUri = "") }
                    return
                }
            }
        }
        _state.update { it.copy(deviceUserCode = "", deviceVerificationUri = "", error = "Device login expired. Try again.") }
    }

    fun failOauth(message: String) {
        _state.update { it.copy(error = message, scanning = false) }
        scope.launch { prefs.clearPendingOauth() }
    }

    suspend fun signOut() {
        devicePoll?.cancel()
        tokenHolder.token = ""
        prefs.clearToken()
        _state.value = IssuesSnapshot()
    }

    suspend fun refreshCatalogAndInbox() = lock.withLock {
        if (tokenHolder.token.isBlank()) return
        _state.update { it.copy(scanning = true, error = null) }
        try {
            val catalog = tracker.discoverAndroidApps()
            val installed = installedApps.applicationIds()
            var subscribed = prefs.subscribedRepos.first()
            if (subscribed.isEmpty()) {
                subscribed = catalog.map { it.repo }.toSet()
                prefs.setSubscribed(subscribed)
            }
            val marked = catalog.map { app ->
                app.copy(
                    installed = app.applicationId.isNotBlank() && app.applicationId in installed,
                    subscribed = app.repo in subscribed,
                )
            }
            _state.update { it.copy(catalog = marked) }
            refreshInboxLocked(marked.filter { it.subscribed }.map { it.repo })
        } catch (error: Throwable) {
            _state.update { it.copy(scanning = false, error = publicError(error)) }
        }
    }

    suspend fun setSubscribed(repo: String, on: Boolean) {
        val current = prefs.subscribedRepos.first().toMutableSet()
        if (on) current += repo else current -= repo
        prefs.setSubscribed(current)
        _state.update { snap ->
            snap.copy(catalog = snap.catalog.map { if (it.repo == repo) it.copy(subscribed = on) else it })
        }
        refreshInbox()
    }

    suspend fun refreshInbox() = lock.withLock {
        refreshInboxLocked(_state.value.catalog.filter { it.subscribed }.map { it.repo })
    }

    private suspend fun refreshInboxLocked(repos: List<String>) {
        if (tokenHolder.token.isBlank()) {
            _state.update { it.copy(scanning = false) }
            return
        }
        _state.update { it.copy(scanning = true) }
        try {
            val issues = if (repos.isEmpty()) {
                emptyList()
            } else {
                val query = repos.joinToString(" ") { "repo:$it" } + " is:issue is:open"
                tracker.searchIssues(query)
            }
            _state.update { it.copy(inbox = issues, scanning = false, error = null) }
        } catch (error: Throwable) {
            _state.update { it.copy(scanning = false, error = publicError(error)) }
        }
    }

    fun queueNewIssue(request: NewIssueRequest) {
        _state.update { it.copy(pendingNewIssue = request) }
    }

    fun consumeNewIssue(): NewIssueRequest? {
        val pending = _state.value.pendingNewIssue
        _state.update { it.copy(pendingNewIssue = null) }
        return pending
    }

    fun resolveApp(request: NewIssueRequest): TrackedApp? =
        _state.value.catalog.firstOrNull { request.matches(it) }

    suspend fun listIssues(repo: String, query: IssueQuery): List<IssueSummary> =
        tracker.listIssues(repo, query)

    suspend fun getIssue(repo: String, number: Int): IssueDetail =
        tracker.getIssue(repo, number)

    suspend fun createIssue(repo: String, draft: NewIssueDraft): IssueSummary {
        val created = tracker.createIssue(repo, draft)
        scope.launch { refreshInbox() }
        return created
    }

    suspend fun updateIssue(repo: String, number: Int, patch: IssuePatch): IssueSummary {
        val updated = tracker.updateIssue(repo, number, patch)
        scope.launch { refreshInbox() }
        return updated
    }

    suspend fun addComment(repo: String, number: Int, body: String): IssueComment =
        tracker.addComment(repo, number, body)

    suspend fun updateComment(repo: String, commentId: Long, body: String): IssueComment =
        tracker.updateComment(repo, commentId, body)

    suspend fun deleteComment(repo: String, commentId: Long) =
        tracker.deleteComment(repo, commentId)

    suspend fun listLabels(repo: String): List<IssueLabel> = tracker.listLabels(repo)

    suspend fun listAssignees(repo: String): List<IssueUser> = tracker.listAssignees(repo)

    suspend fun listMilestones(repo: String): List<IssueMilestone> = tracker.listMilestones(repo)

    suspend fun search(text: String): List<IssueSummary> {
        val repos = _state.value.catalog.filter { it.subscribed }.map { it.repo }
        if (repos.isEmpty() || text.isBlank()) return emptyList()
        val query = repos.joinToString(" ") { "repo:$it" } + " is:issue " + text.trim()
        return tracker.searchIssues(query)
    }

    private fun publicError(error: Throwable): String {
        val code = (error as? GitHubApiException)?.code.orEmpty()
        val message = error.message.orEmpty()
        return when {
            code.contains("401") || code.contains("unauthorized", true) || message.contains("Bad credentials", true) ->
                "GitHub rejected that token."
            code == "missing_client_id" ->
                "This build has no GitHub Client ID. Fill github/client-id.txt after creating the OAuth app."
            code == "oauth_state" -> "GitHub login did not match this phone. Try Connect with GitHub again."
            message.isNotBlank() -> message
            else -> "Could not reach GitHub."
        }
    }
}
