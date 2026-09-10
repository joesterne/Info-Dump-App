package com.example.data

import androidx.room.Embedded
import androidx.room.ColumnInfo

data class SessionArchive(
    @Embedded val profile: Profile,
    @ColumnInfo(name = "lastMessageTimestamp") val lastMessageTimestamp: Long
)
