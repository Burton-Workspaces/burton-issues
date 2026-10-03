package com.burton.issues.data.github

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenHolder @Inject constructor() {
    @Volatile
    var token: String = ""
}
