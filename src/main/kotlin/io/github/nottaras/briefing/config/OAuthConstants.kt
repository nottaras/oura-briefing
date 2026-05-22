package io.github.nottaras.briefing.config

object OAuthConstants {
    const val CALLBACK_PORT = 8080
    const val REDIRECT_URI = "http://localhost:$CALLBACK_PORT/callback"
    const val AUTHORIZE_URL = "https://cloud.ouraring.com/oauth/authorize"
    const val TOKEN_URL = "https://api.ouraring.com/oauth/token"
    const val SCOPES = "daily"
}
