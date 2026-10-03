package com.burton.issues.domain

data class Account(
    val login: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val htmlUrl: String = "",
) {
    val display: String get() = name.ifBlank { login }
}

data class TrackedApp(
    val repo: String,
    val name: String,
    val description: String = "",
    val applicationId: String = "",
    val htmlUrl: String = "",
    val language: String = "",
    val installed: Boolean = false,
    val subscribed: Boolean = false,
    val openCount: Int? = null,
) {
    val shortName: String
        get() = name.removePrefix("Burton ").ifBlank { repo.substringAfterLast('/') }
}

data class IssueLabel(
    val name: String,
    val color: String = "",
    val description: String = "",
)

data class IssueUser(
    val login: String,
    val avatarUrl: String = "",
)

data class IssueMilestone(
    val number: Int,
    val title: String,
    val state: String = "open",
    val description: String = "",
)

data class IssueSummary(
    val repo: String,
    val number: Int,
    val title: String,
    val state: String,
    val body: String = "",
    val htmlUrl: String = "",
    val user: IssueUser = IssueUser(""),
    val labels: List<IssueLabel> = emptyList(),
    val assignees: List<IssueUser> = emptyList(),
    val milestone: IssueMilestone? = null,
    val comments: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val closedAt: String = "",
    val locked: Boolean = false,
) {
    val open: Boolean get() = state.equals("open", ignoreCase = true)
    val key: String get() = "$repo#$number"
}

data class IssueComment(
    val id: Long,
    val body: String,
    val user: IssueUser = IssueUser(""),
    val htmlUrl: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val authorAssociation: String = "",
)

data class IssueDetail(
    val issue: IssueSummary,
    val comments: List<IssueComment> = emptyList(),
)

enum class IssueStateFilter {
    OPEN, CLOSED, ALL;

    val apiValue: String
        get() = when (this) {
            OPEN -> "open"
            CLOSED -> "closed"
            ALL -> "all"
        }
}

data class IssueQuery(
    val state: IssueStateFilter = IssueStateFilter.OPEN,
    val labels: List<String> = emptyList(),
    val assignee: String = "",
    val creator: String = "",
    val milestone: String = "",
    val search: String = "",
)

data class NewIssueDraft(
    val title: String,
    val body: String = "",
    val labels: List<String> = emptyList(),
    val assignees: List<String> = emptyList(),
    val milestone: Int? = null,
)

data class IssuePatch(
    val title: String? = null,
    val body: String? = null,
    val state: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null,
    val milestone: Int? = null,
    val clearMilestone: Boolean = false,
)

data class NewIssueRequest(
    val packageName: String = "",
    val repo: String = "",
    val app: String = "",
    val title: String = "",
    val body: String = "",
) {
    val hasTarget: Boolean
        get() = packageName.isNotBlank() || repo.isNotBlank() || app.isNotBlank()

    fun matches(tracked: TrackedApp): Boolean {
        if (packageName.isNotBlank() && catalogIdsMatch(packageName, tracked.applicationId)) {
            return true
        }
        if (repo.isNotBlank() && tracked.repo.equals(repo.removePrefix("https://github.com/"), ignoreCase = true)) {
            return true
        }
        if (app.isNotBlank()) {
            val needle = app.trim()
            if (tracked.repo.substringAfterLast('/').equals(needle, ignoreCase = true)) return true
            if (catalogIdsMatch(tracked.applicationId, needle)) return true
            if (tracked.name.equals(needle, ignoreCase = true)) return true
        }
        return false
    }

    private fun catalogIdsMatch(left: String, right: String): Boolean {
        val a = left.removeSuffix(".debug")
        val b = right.removeSuffix(".debug")
        return a.equals(b, ignoreCase = true)
    }
}

data class IssuesSnapshot(
    val tokenPresent: Boolean = false,
    val scanning: Boolean = false,
    val error: String? = null,
    val account: Account? = null,
    val backendId: String = "github",
    val catalog: List<TrackedApp> = emptyList(),
    val inbox: List<IssueSummary> = emptyList(),
    val pendingNewIssue: NewIssueRequest? = null,
    val deviceUserCode: String = "",
    val deviceVerificationUri: String = "",
) {
    val subscribed: List<TrackedApp> get() = catalog.filter { it.subscribed }
}

object TimeText {
    fun short(iso: String): String {
        if (iso.isBlank()) return ""
        val date = iso.take(10)
        if (date.length < 10) return iso
        val year = date.substring(0, 4)
        val month = date.substring(5, 7).toIntOrNull() ?: return date
        val day = date.substring(8, 10).trimStart('0').ifBlank { date.substring(8, 10) }
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val label = months.getOrNull(month - 1) ?: date.substring(5, 7)
        return "$label $day, $year"
    }
}
