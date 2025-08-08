package com.jooandriano.googleoauth

data class GoogleOAuthConfig(
    val clientId: String,
    val clientSecret: String? = null,
    val redirectPort: Int = 11223,
    val scopes: List<String> = listOf("openid", "email", "profile"),
    val promptConsent: Boolean = true
)