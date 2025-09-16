package com.ioi.myssue.data.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class AddUserResponse(
    val userId: Long
)