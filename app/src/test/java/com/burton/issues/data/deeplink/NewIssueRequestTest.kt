package com.burton.issues.data.deeplink

import com.burton.issues.domain.NewIssueRequest
import com.burton.issues.domain.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewIssueRequestTest {
    @Test
    fun matchesPackageRepoAndAppSlug() {
        val app = TrackedApp(
            repo = "Burton-Workspaces/burton-pod",
            name = "Burton Pod",
            applicationId = "com.burton.pod",
        )
        assertTrue(NewIssueRequest(packageName = "com.burton.pod").matches(app))
        assertTrue(NewIssueRequest(repo = "Burton-Workspaces/burton-pod").matches(app))
        assertTrue(NewIssueRequest(app = "burton-pod").matches(app))
        assertTrue(NewIssueRequest(app = "Burton Pod").matches(app))
        assertFalse(NewIssueRequest(packageName = "com.burton.slack").matches(app))
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
}
