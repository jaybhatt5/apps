package com.example.data.repository

import com.example.data.database.SudokuDao
import com.example.data.model.DailyChallengeStatus
import com.example.data.model.GameSession
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import kotlinx.coroutines.flow.Flow

class SudokuRepository(private val sudokuDao: SudokuDao) {

    val dailyChallenges: Flow<List<DailyChallengeStatus>> = sudokuDao.getAllDailyChallengesFlow()
    val allStats: Flow<List<UserStats>> = sudokuDao.getAllUserStatsFlow()
    val userProfile: Flow<UserProfile?> = sudokuDao.getUserProfileFlow()

    suspend fun getGameSession(id: Int): GameSession? = sudokuDao.getGameSession(id)

    suspend fun saveGameSession(session: GameSession) = sudokuDao.saveGameSession(session)

    suspend fun deleteGameSession(id: Int) = sudokuDao.deleteGameSession(id)

    suspend fun getDailyChallenge(dateString: String): DailyChallengeStatus? = sudokuDao.getDailyChallenge(dateString)

    suspend fun getOrCreateUserProfile(): UserProfile {
        val existing = sudokuDao.getUserProfile()
        if (existing != null) return existing
        val defaultProfile = UserProfile()
        sudokuDao.saveUserProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun updateUsername(username: String) {
        val profile = getOrCreateUserProfile()
        sudokuDao.saveUserProfile(profile.copy(username = username))
    }

    suspend fun recordGameWin(
        difficulty: String,
        elapsedSeconds: Long,
        isDaily: Boolean,
        dateString: String = "normal"
    ): Int { // Returns XP earned
        // Calculate dynamic XP Based on difficulty & solving speed
        val baseXP = when (difficulty.lowercase()) {
            "easy" -> 100
            "medium" -> 250
            "hard" -> 500
            "expert" -> 1000
            else -> 100
        }
        
        // Time bonus rewards speed
        val speedBonus = if (elapsedSeconds < 240) 120 else if (elapsedSeconds < 480) 60 else if (elapsedSeconds < 900) 30 else 0
        val earnedXP = baseXP + speedBonus

        // Update User Profile
        val profile = getOrCreateUserProfile()
        val newXP = profile.totalXp + earnedXP
        val newLevel = (newXP / 1000) + 1
        val newCrowns = if (isDaily) profile.dailyCrowns + 1 else profile.dailyCrowns
        
        sudokuDao.saveUserProfile(
            profile.copy(
                totalXp = newXP,
                level = newLevel,
                dailyCrowns = newCrowns
            )
        )

        // Update User Difficulty Stats
        val stats = sudokuDao.getUserStats(difficulty) ?: UserStats(difficulty = difficulty)
        val newPlayed = stats.gamesPlayed + 1
        val newWon = stats.gamesWon + 1
        val newBest = if (stats.bestTimeSeconds == 0L) elapsedSeconds else minOf(stats.bestTimeSeconds, elapsedSeconds)
        val newAverage = if (stats.gamesWon == 0) elapsedSeconds else ((stats.averageTimeSeconds * stats.gamesWon) + elapsedSeconds) / newWon
        val newStreak = stats.currentStreak + 1
        val newMaxStreak = maxOf(stats.maxStreak, newStreak)

        sudokuDao.saveUserStats(
            stats.copy(
                gamesPlayed = newPlayed,
                gamesWon = newWon,
                bestTimeSeconds = newBest,
                averageTimeSeconds = newAverage,
                currentStreak = newStreak,
                maxStreak = newMaxStreak
            )
        )

        // Record Completed Day if this is a Daily Challenge
        if (isDaily) {
            sudokuDao.saveDailyChallengeStatus(
                DailyChallengeStatus(
                    dateString = dateString,
                    isCompleted = true,
                    completedTimeSeconds = elapsedSeconds,
                    scoreAwarded = earnedXP
                )
            )
        }

        return earnedXP
    }

    suspend fun recordGameLoss(difficulty: String) {
        val stats = sudokuDao.getUserStats(difficulty) ?: UserStats(difficulty = difficulty)
        val newPlayed = stats.gamesPlayed + 1
        sudokuDao.saveUserStats(
            stats.copy(
                gamesPlayed = newPlayed,
                currentStreak = 0 // Streak broken on a loss
            )
        )
    }
}
