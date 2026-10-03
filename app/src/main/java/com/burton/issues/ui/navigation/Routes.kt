package com.burton.issues.ui.navigation

object Routes {
    const val INBOX = "inbox"
    const val APPS = "apps"
    const val SEARCH = "search"
    const val ISSUES = "issues/{repo}"
    const val ISSUE = "issue/{repo}/{number}"
    const val COMPOSE = "compose?repo={repo}"

    fun issues(repo: String) = "issues/${encode(repo)}"
    fun issue(repo: String, number: Int) = "issue/${encode(repo)}/$number"
    fun compose(repo: String = "") = "compose?repo=${encode(repo)}"

    fun encode(value: String): String = android.net.Uri.encode(value)
    fun decode(value: String?): String = android.net.Uri.decode(value.orEmpty())
}
