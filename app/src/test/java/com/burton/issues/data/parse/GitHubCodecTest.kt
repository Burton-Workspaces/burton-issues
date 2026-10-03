package com.burton.issues.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubCodecTest {
    @Test
    fun mapsIssueAndDropsPullRequests() {
        val issue = GitHubCodec.issue(
            TinyJson.parseObject(
                """
                {
                  "number": 12,
                  "title": "Crash on launch",
                  "state": "open",
                  "body": "steps",
                  "html_url": "https://github.com/Burton-Workspaces/burton-pod/issues/12",
                  "user": {"login": "ryco", "avatar_url": "https://x"},
                  "labels": [{"name": "bug", "color": "c45c4a"}],
                  "assignees": [],
                  "comments": 3,
                  "created_at": "2026-10-03T12:00:00Z",
                  "updated_at": "2026-10-03T12:00:00Z"
                }
                """.trimIndent(),
            ),
            repo = "Burton-Workspaces/burton-pod",
        )
        requireNotNull(issue)
        assertEquals(12, issue.number)
        assertEquals("Crash on launch", issue.title)
        assertTrue(issue.open)
        assertEquals("bug", issue.labels.single().name)

        val pr = GitHubCodec.issue(
            TinyJson.parseObject("""{"number":1,"title":"pr","state":"open","pull_request":{"url":"x"}}"""),
            repo = "Burton-Workspaces/burton-pod",
        )
        assertNull(pr)
    }

    @Test
    fun displayNameTitleCasesRepo() {
        assertEquals("Burton Pod", GitHubCodec.displayName("burton-pod", ""))
        assertEquals("Burton Photos", GitHubCodec.displayName("burton-photos-android", ""))
    }
}
