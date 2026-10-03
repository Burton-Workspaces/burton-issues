package com.burton.issues.data.deeplink

import android.content.Intent
import android.net.Uri
import com.burton.issues.domain.NewIssueRequest

object NewIssueIntents {
    const val ACTION_CREATE = "com.burton.issues.action.CREATE_ISSUE"
    const val SCHEME = "burtonissues"
    const val HOST_NEW = "new"
    const val HTTPS_HOST = "burton-workspaces.github.io"
    const val HTTPS_NEW_PREFIX = "/burton-issues/new"

    fun isNewIssue(scheme: String?, host: String?, path: String?): Boolean {
        if (scheme == SCHEME && host == HOST_NEW) return true
        if (scheme == "https" && host == HTTPS_HOST) {
            val p = path.orEmpty()
            return p == HTTPS_NEW_PREFIX || p.startsWith("$HTTPS_NEW_PREFIX/")
        }
        return false
    }

    fun isNewIssue(uri: Uri?): Boolean {
        if (uri == null) return false
        return isNewIssue(uri.scheme, uri.host, uri.path)
    }

    fun fromIntent(intent: Intent?): NewIssueRequest? {
        if (intent == null) return null
        val uri = intent.data
        if (isNewIssue(uri) && uri != null) return fromUri(uri)
        if (intent.action == ACTION_CREATE) {
            return NewIssueRequest(
                packageName = extra(intent, "package"),
                repo = extra(intent, "repo"),
                app = extra(intent, "app"),
                title = extra(intent, "title"),
                body = extra(intent, "body").ifBlank { extra(intent, Intent.EXTRA_TEXT) },
            )
        }
        return null
    }

    fun fromUri(uri: Uri): NewIssueRequest = NewIssueRequest(
        packageName = uri.getQueryParameter("package").orEmpty(),
        repo = uri.getQueryParameter("repo").orEmpty(),
        app = uri.getQueryParameter("app").orEmpty()
            .ifBlank { uri.getQueryParameter("applicationId").orEmpty() },
        title = uri.getQueryParameter("title").orEmpty(),
        body = uri.getQueryParameter("body").orEmpty(),
    )

    private fun extra(intent: Intent, key: String): String =
        intent.getStringExtra(key).orEmpty().trim()
}
