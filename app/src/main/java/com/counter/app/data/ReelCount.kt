package com.counter.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reel_counts",
    primaryKeys = ["date", "packageName"],
    indices = [Index(value = ["date"])]
)
data class ReelCount(
    val date: String,
    val packageName: String,
    val count: Int = 0
)
