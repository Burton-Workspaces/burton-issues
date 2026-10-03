package com.burton.issues.data.backend

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

/**
 * One issue-tracking host. GitHub is the first implementation; GitLab and others
 * can plug in behind the same methods without changing the UI.
 */
interface IssueTracker {
    val id: String
    val displayName: String
    val org: String

    suspend fun currentUser(): Account

    suspend fun discoverAndroidApps(): List<TrackedApp>

    suspend fun listIssues(repo: String, query: IssueQuery): List<IssueSummary>

    suspend fun getIssue(repo: String, number: Int): IssueDetail

    suspend fun listComments(repo: String, number: Int): List<IssueComment>

    suspend fun createIssue(repo: String, draft: NewIssueDraft): IssueSummary

    suspend fun updateIssue(repo: String, number: Int, patch: IssuePatch): IssueSummary

    suspend fun addComment(repo: String, number: Int, body: String): IssueComment

    suspend fun updateComment(repo: String, commentId: Long, body: String): IssueComment

    suspend fun deleteComment(repo: String, commentId: Long)

    suspend fun listLabels(repo: String): List<IssueLabel>

    suspend fun listAssignees(repo: String): List<IssueUser>

    suspend fun listMilestones(repo: String): List<IssueMilestone>

    suspend fun searchIssues(query: String): List<IssueSummary>
}
