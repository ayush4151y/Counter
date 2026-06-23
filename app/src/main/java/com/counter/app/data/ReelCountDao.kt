package com.counter.app.data

import androidx.room.*

@Dao
interface ReelCountDao {
    @Query("SELECT * FROM reel_counts WHERE date = :date")
    suspend fun getCountsForDate(date: String): List<ReelCount>

    @Query("SELECT * FROM reel_counts WHERE date = :date AND packageName = :packageName")
    suspend fun getCount(date: String, packageName: String): ReelCount?

    @Query("SELECT SUM(count) FROM reel_counts WHERE date = :date")
    suspend fun getTotalForDate(date: String): Int?

    @Upsert
    suspend fun upsert(count: ReelCount)

    @Query("SELECT * FROM reel_counts WHERE date = :date AND packageName = :packageName")
    fun observeCount(date: String, packageName: String): kotlinx.coroutines.flow.Flow<ReelCount?>

    @Query("SELECT SUM(count) FROM reel_counts WHERE date = :date")
    fun observeTotal(date: String): kotlinx.coroutines.flow.Flow<Int?>
}
