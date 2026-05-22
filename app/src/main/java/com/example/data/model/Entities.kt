package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSession(
    @PrimaryKey val id: Int, // 1 for Standard, 2 for Daily
    val difficulty: String,
    val originalGrid: String, // 81 chars "045001..."
    val currentGrid: String,  // 81 chars
    val solutionGrid: String, // 81 chars
    val notes: String,        // 81 parts joined by '|' (e.g., "135|7||24|....")
    val elapsedSeconds: Long,
    val errorsCount: Int,
    val isCompleted: Boolean,
    val dateString: String,   // "normal" or "YYYY-MM-DD"
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_challenges")
data class DailyChallengeStatus(
    @PrimaryKey val dateString: String, // YYYY-MM-DD
    val isCompleted: Boolean,
    val completedTimeSeconds: Long,
    val scoreAwarded: Int,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val difficulty: String, // Easy, Medium, Hard, Expert
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0,
    val averageTimeSeconds: Long = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "SudokuPlayer",
    val totalXp: Int = 0,
    val dailyCrowns: Int = 0,
    val level: Int = 1
)
