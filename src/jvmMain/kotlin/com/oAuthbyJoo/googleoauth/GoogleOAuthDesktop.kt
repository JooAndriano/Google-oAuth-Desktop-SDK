package com.yourorg.googleoauth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

data class GoogleTokens(val accessToken: String?, val idToken: String?)

object GoogleOAuthDesktop {
    private lateinit var lastPkce: PkceUtil.PkcePair

    fun startLogin(
        config: GoogleOAuthConfig,
        coroutineScope: CoroutineScope,
        onResult: (GoogleTokens) -> Unit,
        onError: (String) -> Unit
    ) {
        val pkce = PkceUtil.generate().also { lastPkce = it }
        val state = UUID.randomUUID().toString()
        val redirectUri = "http://localhost:${config.redirectPort}"
        val authUri = "https://accounts.google.com/o/oauth2/v2/auth"
        val scope = URLEncoder.encode(config.scopes.joinToString(" "), "UTF-8")

        val authUrl = buildString {
            append("$authUri?")
            append("client_id=${config.clientId}")
            append("&redirect_uri=$redirectUri")
            append("&response_type=code")
            append("&scope=$scope")
            append("&state=$state")
            append("&code_challenge=${pkce.codeChallenge}")
            append("&code_challenge_method=S256")
            append("&access_type=offline")
            if (config.promptConsent) append("&prompt=consent")
        }

        var server: ApplicationEngine? = null
        server = embeddedServer(Netty, port = config.redirectPort) {
            routing {
                get("/") {
                    val code = call.request.queryParameters["code"]
                    val receivedState = call.request.queryParameters["state"]

                    if (code != null && receivedState == state) {
                        call.respondText(
                            "<html><body><h2>Login berhasil!</h2><p>Kamu bisa tutup tab ini.</p></body></html>"
                        )

                        coroutineScope.launch {
                            val tokens = exchangeCodeForTokens(
                                code = code,
                                redirectUri = redirectUri,
                                clientId = config.clientId,
                                clientSecret = config.clientSecret
                            )

                            if (tokens == null) {
                                onError("Gagal menukar authorization code menjadi token.")
                            } else {
                                onResult(tokens)
                            }

                            delay(800)
                            server?.stop(500, 500)
                        }
                    } else {
                        call.respondText("<html><body><h3>Code/state tidak valid.</h3></body></html>")
                    }
                }
            }
        }.start(wait = false)

        Desktop.getDesktop().browse(URI(authUrl))
    }

    private suspend fun exchangeCodeForTokens(
        code: String,
        redirectUri: String,
        clientId: String,
        clientSecret: String?
    ): GoogleTokens? {
        val jsonCfg = Json { ignoreUnknownKeys = true }
        val client = HttpClient(CIO) { install(ContentNegotiation) { json(jsonCfg) } }

        return try {
            val params = Parameters.build {
                append("code", code)
                append("client_id", clientId)
                append("code_verifier", lastPkce.codeVerifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri)
                if (!clientSecret.isNullOrBlank()) append("client_secret", clientSecret)
            }

            val bodyStr = client.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = params
            ).body<String>()

            val obj = jsonCfg.parseToJsonElement(bodyStr).jsonObject
            val access = obj["access_token"]?.jsonPrimitive?.content
            val idTok = obj["id_token"]?.jsonPrimitive?.content
            GoogleTokens(accessToken = access, idToken = idTok)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            client.close()
        }
    }
}

@Composable
fun GoogleLoginButton(
    modifier: Modifier = Modifier,
    config: GoogleOAuthConfig,
    text: String = "Login with Google",
    onResult: (GoogleTokens) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            GoogleOAuthDesktop.startLogin(
                config = config,
                coroutineScope = scope,
                onResult = onResult,
                onError = onError
            )
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.White,
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(24.dp)
    ) {
        Image(
            painter = painterResource("icons/google_logo.png"),
            contentDescription = "Google Logo",
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}