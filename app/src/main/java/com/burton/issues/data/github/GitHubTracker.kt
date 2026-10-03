package com.burton.issues.data.github

import android.util.Base64
import com.burton.issues.data.backend.IssueTracker
import com.burton.issues.data.parse.AndroidCatalog
import com.burton.issues.data.parse.GitHubCodec
import com.burton.issues.data.parse.GradleIds
import com.burton.issues.data.parse.TinyJson.objList
import com.burton.issues.data.parse.TinyJson.str
import com.burton.issues.domain.Account
import com.burton.issues.domain.IssueComment
import com.burton.issues.domain.IssueDetail
import com.burton.issues.domain.IssueLabel
import com.burton.issues.domain.IssueMilestone
import com.burton.issues.domain.IssuePatch
import com.burton.issues.domain.IssueQuery
import com.burton.issues.domain.IssueSummary
import com.burton.issues.domain.IssueUser
import com.burton.issues.domain.NewIssueDraft
import com.burton.issues.domain.TrackedApp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubTracker @Inject constructor(
    private val api: GitHubApi,
) : IssueTracker {
    override val id: String = "github"
    override val displayName: String = "GitHub"
    override val org: String = ORG

    override suspend fun currentUser(): Account = GitHubCodec.account(api.getObject("user"))

    override suspend fun discoverAndroidApps(): List<TrackedApp> {
        val repos = mutableListOf<Map<String, Any?>>()
        var page = 1
        while (page <= 5) {
            val batch = api.getList(
                "orgs/$ORG/repos",
                mapOf("type" to "public", "per_page" to "100", "page" to page.toString(), "sort" to "full_name"),
            )
            if (batch.isEmpty()) break
            repos += batch
            if (batch.size < 100) break
            page++
        }
        return repos.mapNotNull { row ->
            val name = row.str("name")
            val language = row.str("language")
            val description = row.str("description")
            if (!AndroidCatalog.looksLikeAndroidRepo(name, language, description)) return@mapNotNull null
            val full = row.str("full_name")
            val applicationId = runCatching { readApplicationId(full) }.getOrDefault("")
            if (applicationId.isBlank() && language.lowercase() !in setOf("kotlin", "java") && !name.contains("android", true)) {
                return@mapNotNull null
            }
            GitHubCodec.repoApp(row, applicationId)
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun listIssues(repo: String, query: IssueQuery): List<IssueSummary> {
        if (query.search.isNotBlank()) {
            val parts = mutableListOf("repo:$repo", "is:issue", query.search)
            when (query.state.apiValue) {
                "open" -> parts += "is:open"
                "closed" -> parts += "is:closed"
            }
            return searchIssues(parts.joinToString(" "))
        }
        val params = mutableMapOf(
            "state" to query.state.apiValue,
            "per_page" to "50",
            "sort" to "updated",
        )
        if (query.labels.isNotEmpty()) params["labels"] = query.labels.joinToString(",")
        if (query.assignee.isNotBlank()) params["assignee"] = query.assignee
        if (query.creator.isNotBlank()) params["creator"] = query.creator
        if (query.milestone.isNotBlank()) params["milestone"] = query.milestone
        return api.getList("repos/$repo/issues", params).mapNotNull { GitHubCodec.issue(it, repo) }
    }

    override suspend fun getIssue(repo: String, number: Int): IssueDetail {
        val issue = GitHubCodec.issue(api.getObject("repos/$repo/issues/$number"), repo)
            ?: error("GitHub returned a pull request for $repo#$number")
        return IssueDetail(issue = issue, comments = listComments(repo, number))
    }

    override suspend fun listComments(repo: String, number: Int): List<IssueComment> =
        api.getList("repos/$repo/issues/$number/comments", mapOf("per_page" to "100"))
            .map(GitHubCodec::comment)

    override suspend fun createIssue(repo: String, draft: NewIssueDraft): IssueSummary {
        val body = mutableMapOf<String, Any?>(
            "title" to draft.title,
            "body" to draft.body,
        )
        if (draft.labels.isNotEmpty()) body["labels"] = draft.labels
        if (draft.assignees.isNotEmpty()) body["assignees"] = draft.assignees
        if (draft.milestone != null) body["milestone"] = draft.milestone
        return GitHubCodec.issue(api.post("repos/$repo/issues", body), repo)
            ?: error("GitHub did not return an issue")
    }

    override suspend fun updateIssue(repo: String, number: Int, patch: IssuePatch): IssueSummary {
        val body = mutableMapOf<String, Any?>()
        patch.title?.let { body["title"] = it }
        patch.body?.let { body["body"] = it }
        patch.state?.let { body["state"] = it }
        patch.labels?.let { body["labels"] = it }
        patch.assignees?.let { body["assignees"] = it }
        when {
            patch.clearMilestone -> body["milestone"] = null
            patch.milestone != null -> body["milestone"] = patch.milestone
        }
        return GitHubCodec.issue(api.patch("repos/$repo/issues/$number", body), repo)
            ?: error("GitHub did not return an issue")
    }

    override suspend fun addComment(repo: String, number: Int, body: String): IssueComment =
        GitHubCodec.comment(api.post("repos/$repo/issues/$number/comments", mapOf("body" to body)))

    override suspend fun updateComment(repo: String, commentId: Long, body: String): IssueComment =
        GitHubCodec.comment(api.patch("repos/$repo/issues/comments/$commentId", mapOf("body" to body)))

    override suspend fun deleteComment(repo: String, commentId: Long) {
        api.delete("repos/$repo/issues/comments/$commentId")
    }

    override suspend fun listLabels(repo: String): List<IssueLabel> =
        api.getList("repos/$repo/labels", mapOf("per_page" to "100")).map(GitHubCodec::label)

    override suspend fun listAssignees(repo: String): List<IssueUser> =
        api.getList("repos/$repo/assignees", mapOf("per_page" to "100")).map(GitHubCodec::user)

    override suspend fun listMilestones(repo: String): List<IssueMilestone> =
        api.getList("repos/$repo/milestones", mapOf("state" to "open", "per_page" to "100"))
            .map(GitHubCodec::milestone)

    override suspend fun searchIssues(query: String): List<IssueSummary> {
        val result = api.getObject("search/issues", mapOf("q" to query, "per_page" to "50"))
        return result.objList("items").mapNotNull { GitHubCodec.issue(it) }
    }

    private suspend fun readApplicationId(repo: String): String {
        val gradle = readFile(repo, "app/build.gradle.kts").ifBlank { readFile(repo, "app/build.gradle") }
        val fromGradle = GradleIds.applicationId(gradle)
        if (fromGradle.isNotBlank()) return fromGradle
        val manifest = readFile(repo, "app/src/main/AndroidManifest.xml")
        return GradleIds.manifestPackage(manifest)
    }

    private suspend fun readFile(repo: String, path: String): String {
        val row = runCatching { api.getObject("repos/$repo/contents/$path") }.getOrNull() ?: return ""
        if (row.str("encoding") != "base64") return row.str("content")
        val raw = row.str("content").replace("\n", "")
        return String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
    }

    companion object {
        const val ORG = "Burton-Workspaces"
    }
}
