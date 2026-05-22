package com.example.logic

import java.util.Random

data class SudokuPuzzle(
    val originalGrid: IntArray,
    val solutionGrid: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SudokuPuzzle
        if (!originalGrid.contentEquals(other.originalGrid)) return false
        if (!solutionGrid.contentEquals(other.solutionGrid)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = originalGrid.contentHashCode()
        result = 31 * result + solutionGrid.contentHashCode()
        return result
    }
}

object SudokuGenerator {
    fun generate(difficulty: String, seed: Long? = null): SudokuPuzzle {
        val random = if (seed != null) Random(seed) else Random()
        
        // Pre-configured valid completed 9x9 Sudoku Board
        val base = arrayOf(
            intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            intArrayOf(4, 5, 6, 7, 8, 9, 1, 2, 3),
            intArrayOf(7, 8, 9, 1, 2, 3, 4, 5, 6),
            intArrayOf(2, 3, 1, 5, 6, 4, 8, 9, 7),
            intArrayOf(5, 6, 4, 8, 9, 7, 2, 3, 1),
            intArrayOf(8, 9, 7, 2, 3, 1, 5, 6, 4),
            intArrayOf(3, 1, 2, 6, 4, 5, 9, 7, 8),
            intArrayOf(6, 4, 5, 9, 7, 8, 3, 1, 2),
            intArrayOf(9, 7, 8, 3, 1, 2, 6, 4, 5)
        )
        
        // Copy base array to editable board
        val grid = Array(9) { r -> IntArray(9) { c -> base[r][c] } }
        
        // 1. Shuffle single digits (1..9 mapping)
        val digits = (1..9).toList().shuffled(random)
        for (r in 0..8) {
            for (c in 0..8) {
                grid[r][c] = digits[grid[r][c] - 1]
            }
        }
        
        // 2. Shuffle rows within three horizontal bands
        for (band in 0..2) {
            val rows = (0..2).toList().shuffled(random)
            val tempBand = Array(3) { r -> IntArray(9) { c -> grid[band * 3 + r][c] } }
            for (i in 0..2) {
                grid[band * 3 + i] = tempBand[rows[i]]
            }
        }
        
        // 3. Shuffle columns within three vertical stacks
        for (stack in 0..2) {
            val cols = (0..2).toList().shuffled(random)
            val tempStack = Array(9) { r -> IntArray(3) { c -> grid[r][stack * 3 + c] } }
            for (r in 0..8) {
                for (i in 0..2) {
                    grid[r][stack * 3 + i] = tempStack[r][cols[i]]
                }
            }
        }
        
        // 4. Shuffle the horizontal bands themselves
        val bands = (0..2).toList().shuffled(random)
        val tempGridRows = Array(9) { r -> IntArray(9) { c -> grid[r][c] } }
        for (b in 0..2) {
            val sourceBand = bands[b]
            for (r in 0..2) {
                grid[b * 3 + r] = tempGridRows[sourceBand * 3 + r]
            }
        }
        
        // 5. Shuffle the vertical stacks themselves
        val stacks = (0..2).toList().shuffled(random)
        val tempGridCols = Array(9) { r -> IntArray(9) { c -> grid[r][c] } }
        for (r in 0..8) {
            for (s in 0..2) {
                val sourceStack = stacks[s]
                for (c in 0..2) {
                    grid[r][s * 3 + c] = tempGridCols[r][sourceStack * 3 + c]
                }
            }
        }
        
        // Flatten grid into an 81-element array
        val solution = IntArray(81)
        for (r in 0..8) {
            for (c in 0..8) {
                solution[r * 9 + c] = grid[r][c]
            }
        }
        
        // 6. Remove cells to create the puzzle based on difficulty
        val puzzle = solution.clone()
        val removeCount = when (difficulty.lowercase()) {
            "easy" -> random.nextInt(4) + 36      // 36 to 39 removed, leaving 42-45
            "medium" -> random.nextInt(4) + 44    // 44 to 47 removed, leaving 34-37
            "hard" -> random.nextInt(4) + 52      // 52 to 55 removed, leaving 26-29
            "expert" -> random.nextInt(5) + 58    // 58 to 62 removed, leaving 19-23
            else -> random.nextInt(4) + 40
        }
        
        val listIndices = (0..80).toMutableList()
        listIndices.shuffle(random)
        for (i in 0 until removeCount) {
            puzzle[listIndices[i]] = 0
        }
        
        return SudokuPuzzle(puzzle, solution)
    }
}
