package com.flxProviders.superstream.api.settings

internal const val TOKEN_STATUS_KEY = "token_status"
internal const val TOKEN_KEY = "token"

enum class TokenStatus {
    Offline,
    Online;
}