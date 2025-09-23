package com.ioi.myssue.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class AddUserRequest(
    val deviceUuid: String,
    val fcmToken: String
)