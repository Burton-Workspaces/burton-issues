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

    fun packageFromReferrer(scheme: String?, host: String?): String {
        if (scheme == "android-app" && !host.isNullOrBlank()) return host
        return ""
    }

    fun packageFromReferrer(uri: Uri?): String {
        if (uri == null) return ""
        return packageFromReferrer(uri.scheme, uri.host)
    }

    fun fromIntent(intent: Intent?, referringPackage: String = ""): NewIssueRequest? {
        if (intent == null) return null
        val uri = intent.data
        val request = when {
            isNewIssue(uri) && uri != null -> fromUri(uri)
            intent.action == ACTION_CREATE || intent.action == Intent.ACTION_SEND -> fromExtras(intent)
            else -> null
        } ?: return null
        return withCaller(request, referringPackage)
    }

    fun fromUri(uri: Uri): NewIssueRequest = NewIssueRequest(
        packageName = uri.getQueryParameter("package").orEmpty()
            .ifBlank { uri.getQueryParameter("applicationId").orEmpty() },
        repo = uri.getQueryParameter("repo").orEmpty(),
        app = uri.getQueryParameter("app").orEmpty(),
        title = uri.getQueryParameter("title").orEmpty(),
        body = uri.getQueryParameter("body").orEmpty(),
    )

    fun withCaller(request: NewIssueRequest, packageName: String): NewIssueRequest {
        if (request.hasTarget || packageName.isBlank()) return request
        return request.copy(packageName = packageName)
    }

    private fun fromExtras(intent: Intent): NewIssueRequest = NewIssueRequest(
        packageName = extra(intent, "package")
            .ifBlank { extra(intent, "applicationId") }
            .ifBlank { extra(intent, Intent.EXTRA_PACKAGE_NAME) },
        repo = extra(intent, "repo"),
        app = extra(intent, "app"),
        title = extra(intent, "title").ifBlank { extra(intent, Intent.EXTRA_SUBJECT) },
        body = extra(intent, "body").ifBlank { extra(intent, Intent.EXTRA_TEXT) },
    )

    private fun extra(intent: Intent, key: String): String =
        intent.getStringExtra(key).orEmpty().trim()
}
