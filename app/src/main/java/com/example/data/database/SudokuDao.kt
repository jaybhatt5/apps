package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyChallengeStatus
import com.example.data.model.GameSession
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface SudokuDao {
    // --- Game Sessions ---
    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getGameSession(id: Int): GameSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameSession(session: GameSession)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteGameSession(id: Int)

    // --- Daily Challenges ---
    @Query("SELECT * FROM daily_challenges ORDER BY dateString DESC")
    fun getAllDailyChallengesFlow(): Flow<List<DailyChallengeStatus>>

    @Query("SELECT * FROM daily_challenges WHERE dateString = :dateString")
    suspend fun getDailyChallenge(dateString: String): DailyChallengeStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyChallengeStatus(status: DailyChallengeStatus)

    // --- User Statistics ---
    @Query("SELECT * FROM user_stats")
    fun getAllUserStatsFlow(): Flow<List<UserStats>>

    @Query("SELECT * FROM user_stats WHERE difficulty = :difficulty")
    suspend fun getUserStats(difficulty: String): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserStats(stats: UserStats)

    // --- User Profile ---
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)
}
