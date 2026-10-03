package com.burton.issues.data.parse

import com.burton.issues.data.parse.TinyJson.bool
import com.burton.issues.data.parse.TinyJson.int
import com.burton.issues.data.parse.TinyJson.long
import com.burton.issues.data.parse.TinyJson.obj
import com.burton.issues.data.parse.TinyJson.objList
import com.burton.issues.data.parse.TinyJson.str
import com.burton.issues.domain.Account
import com.burton.issues.domain.IssueComment
import com.burton.issues.domain.IssueLabel
import com.burton.issues.domain.IssueMilestone
import com.burton.issues.domain.IssueSummary
import com.burton.issues.domain.IssueUser
import com.burton.issues.domain.TrackedApp

object GitHubCodec {
    fun account(map: Map<String, Any?>): Account = Account(
        login = map.str("login"),
        name = map.str("name"),
        email = map.str("email"),
        avatarUrl = map.str("avatar_url"),
        htmlUrl = map.str("html_url"),
    )

    fun repoApp(map: Map<String, Any?>, applicationId: String = ""): TrackedApp {
        val full = map.str("full_name").ifBlank {
            val owner = map.obj("owner").str("login")
            val name = map.str("name")
            if (owner.isBlank()) name else "$owner/$name"
        }
        return TrackedApp(
            repo = full,
            name = displayName(map.str("name"), map.str("description")),
            description = map.str("description"),
            applicationId = applicationId,
            htmlUrl = map.str("html_url"),
            language = map.str("language"),
        )
    }

    fun issue(map: Map<String, Any?>, repo: String = ""): IssueSummary? {
        if (map.containsKey("pull_request")) return null
        val repository = map.obj("repository")
        val repoName = repo.ifBlank {
            map.str("repository_url")
                .removePrefix("https://api.github.com/repos/")
                .ifBlank { repository.str("full_name") }
        }
        if (repoName.isBlank()) return null
        return IssueSummary(
            repo = repoName,
            number = map.int("number"),
            title = map.str("title"),
            state = map.str("state"),
            body = map.str("body"),
            htmlUrl = map.str("html_url"),
            user = user(map.obj("user")),
            labels = map.objList("labels").map(::label),
            assignees = map.objList("assignees").map(::user),
            milestone = map["milestone"]?.let { it as? Map<*, *> }?.let {
                @Suppress("UNCHECKED_CAST")
                milestone(it as Map<String, Any?>)
            },
            comments = map.int("comments"),
            createdAt = map.str("created_at"),
            updatedAt = map.str("updated_at"),
            closedAt = map.str("closed_at"),
            locked = map.bool("locked"),
        )
    }

    fun comment(map: Map<String, Any?>): IssueComment = IssueComment(
        id = map.long("id"),
        body = map.str("body"),
        user = user(map.obj("user")),
        htmlUrl = map.str("html_url"),
        createdAt = map.str("created_at"),
        updatedAt = map.str("updated_at"),
        authorAssociation = map.str("author_association"),
    )

    fun label(map: Map<String, Any?>): IssueLabel = IssueLabel(
        name = map.str("name"),
        color = map.str("color"),
        description = map.str("description"),
    )

    fun user(map: Map<String, Any?>): IssueUser = IssueUser(
        login = map.str("login"),
        avatarUrl = map.str("avatar_url"),
    )

    fun milestone(map: Map<String, Any?>): IssueMilestone = IssueMilestone(
        number = map.int("number"),
        title = map.str("title"),
        state = map.str("state"),
        description = map.str("description"),
    )

    fun displayName(repoName: String, description: String): String {
        val slug = repoName.removeSuffix("-android")
        val words = slug.split('-').filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
        return words.ifBlank { repoName.ifBlank { description } }
    }
}
