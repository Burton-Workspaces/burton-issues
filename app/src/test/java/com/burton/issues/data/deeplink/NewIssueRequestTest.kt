package com.burton.issues.data.deeplink

import com.burton.issues.domain.NewIssueRequest
import com.burton.issues.domain.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewIssueRequestTest {
    private val pod = TrackedApp(
        repo = "Burton-Workspaces/burton-pod",
        name = "Burton Pod",
        applicationId = "com.burton.pod",
    )

    @Test
    fun matchesPackageRepoAndAppSlug() {
        assertTrue(NewIssueRequest(packageName = "com.burton.pod").matches(pod))
        assertTrue(NewIssueRequest(repo = "Burton-Workspaces/burton-pod").matches(pod))
        assertTrue(NewIssueRequest(app = "burton-pod").matches(pod))
        assertTrue(NewIssueRequest(app = "Burton Pod").matches(pod))
        assertFalse(NewIssueRequest(packageName = "com.burton.slack").matches(pod))
    }

    @Test
    fun newIssueCallbackMatchesCustomSchemeAndPagesHop() {
        assertTrue(NewIssueIntents.isNewIssue("burtonissues", "new", null))
        assertTrue(NewIssueIntents.isNewIssue("https", "burton-workspaces.github.io", "/burton-issues/new"))
        assertTrue(NewIssueIntents.isNewIssue("https", "burton-workspaces.github.io", "/burton-issues/new/"))
        assertFalse(NewIssueIntents.isNewIssue("https", "burton-workspaces.github.io", "/burton-issues/oauth/"))
        assertFalse(NewIssueIntents.isNewIssue("burtonissues", "oauth", null))
    }

    @Test
    fun hasTargetWhenAnySelectorPresent() {
        assertTrue(NewIssueRequest(packageName = "com.burton.pod").hasTarget)
        assertEquals(false, NewIssueRequest().hasTarget)
    }

    @Test
    fun withCallerFillsPackageWhenRequestHasNoTarget() {
        val filled = NewIssueIntents.withCaller(NewIssueRequest(), "com.burton.pod")
        assertEquals("com.burton.pod", filled.packageName)
        assertTrue(filled.matches(pod))
    }

    @Test
    fun withCallerKeepsExplicitTarget() {
        val kept = NewIssueIntents.withCaller(
            NewIssueRequest(packageName = "com.burton.pod"),
            "com.burton.slack",
        )
        assertEquals("com.burton.pod", kept.packageName)
    }

    @Test
    fun packageFromAndroidAppReferrer() {
        assertEquals("com.burton.pod", NewIssueIntents.packageFromReferrer("android-app", "com.burton.pod"))
        assertEquals("", NewIssueIntents.packageFromReferrer("https", "burton-workspaces.github.io"))
        assertEquals("", NewIssueIntents.packageFromReferrer("android-app", null))
    }
}
