package com.burton.issues.data.github

class GitHubApiException(
    val path: String,
    val code: String,
    override val message: String = code,
) : RuntimeException(message)
