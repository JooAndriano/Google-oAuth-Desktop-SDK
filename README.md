# Google OAuth Desktop SDK (Compose Desktop)

A lightweight SDK for implementing Google OAuth 2.0 (PKCE) in **Compose Desktop** apps:
- Opens browser for user consent
- Receives callback via localhost
- Exchanges authorization code for `access_token` / `id_token`
- Provides a ready-to-use `GoogleLoginButton`

## Google OAuth Setup

1. Create an OAuth Client in Google Cloud Console
    - **Recommended:** `Desktop App` (no client_secret required)
    - Or: `Web Application` (requires `client_secret` for token exchange)
2. Add the Redirect URI:
   ```
   http://localhost:11223
   ```
   (or another port, matching `GoogleOAuthConfig.redirectPort`)

## Install via JitPack

Add JitPack to your `settings.gradle` or `build.gradle`:

```kotlin
repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}
```

Add the dependency (replace USER/REPO & version):

```kotlin
dependencies {
    implementation("com.github.JooAndriano:google-oauth-desktop-sdk:0.1.0")
}
```

> Make sure to push a tag `v0.1.0` in GitHub.

## Usage (Compose Desktop)

```kotlin
val config = GoogleOAuthConfig(
    clientId = "YOUR_CLIENT_ID",
    clientSecret = "YOUR_CLIENT_SECRET_IF_WEB_CLIENT", // can be null for Desktop Client
    redirectPort = 11223
)

GoogleLoginButton(
    modifier = Modifier.fillMaxWidth(),
    config = config,
    onResult = { tokens ->
        println("Access: ${tokens.accessToken}")
        println("ID: ${tokens.idToken}")
        // TODO: send token to your backend
    },
    onError = { msg ->
        println("Login error: $msg")
    }
)
```

Or call without the button:

```kotlin
GoogleOAuthDesktop.startLogin(
    config = config,
    coroutineScope = rememberCoroutineScope(),
    onResult = { tokens -> /* handle */ },
    onError = { e -> /* show snackbar */ }
)
```

## Resources
Ensure the Google logo is available at:
```
src/jvmMain/resources/icons/google_logo.png
```

---
