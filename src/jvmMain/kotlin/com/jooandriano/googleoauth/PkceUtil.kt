package com.jooandriano.googleoauth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

object PkceUtil {
    data class PkcePair(val codeVerifier: String, val codeChallenge: String)

    fun generate(): PkcePair {
        val codeVerifier = UUID.randomUUID().toString().replace("-", "")
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return PkcePair(codeVerifier, codeChallenge)
    }
}