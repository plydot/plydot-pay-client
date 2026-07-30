package com.plydot.pay.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AccessTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long? = null,
    @JsonProperty("token_type")
    val tokenType: String? = null,
)
