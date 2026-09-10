package com.example

import com.example.data.Profile

data class SessionArchiveWithScore(
    val profile: Profile,
    val compatibilityScore: Int,
    val lastMessageTimestamp: Long
)
