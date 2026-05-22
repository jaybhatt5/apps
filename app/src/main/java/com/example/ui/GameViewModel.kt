package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyChallengeStatus
import com.example.data.model.GameSession
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import com.example.data.repository.SudokuRepository
import com.example.logic.SudokuGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SudokuState(
    val difficulty: String = "Easy",
    val originalGrid: IntArray = IntArray(81),
    val currentGrid: IntArray = IntArray(81),
    val solutionGrid: IntArray = IntArray(81),
    val selectedCell: Int = -1,
    val notes: Map<Int, Set<Int>> = emptyMap(),
    val isNotesMode: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val errorsCount: Int = 0,
    val maxErrorsAllowed: Int = 3,
    val isGameFinished: Boolean = false,
    val isGameOver: Boolean = false,
    val earnedXp: Int = -1,
    val isDailyChallenge: Boolean = false,
    val currentDateString: String = "normal",
    val isTimerActive: Boolean = false,
    val hintsLeft: Int = 3
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SudokuState
        if (difficulty != other.difficulty) return false
        if (!originalGrid.contentEquals(other.originalGrid)) return false
        if (!currentGrid.contentEquals(other.currentGrid)) return false
        if (!solutionGrid.contentEquals(other.solutionGrid)) return false
        if (selectedCell != other.selectedCell) return false
        if (notes != other.notes) return false
        if (isNotesMode != other.isNotesMode) return false
        if (elapsedSeconds != other.elapsedSeconds) return false
        if (errorsCount != other.errorsCount) return false
        if (maxErrorsAllowed != other.maxErrorsAllowed) return false
        if (isGameFinished != other.isGameFinished) return false
        if (isGameOver != other.isGameOver) return false
        if (earnedXp != other.earnedXp) return false
        if (isDailyChallenge != other.isDailyChallenge) return false
        if (currentDateString != other.currentDateString) return false
        if (isTimerActive != other.isTimerActive) return false
        if (hintsLeft != other.hintsLeft) return false
        return true
    }

    override fun hashCode(): Int {
        var result = difficulty.hashCode()
        result = 31 * result + originalGrid.contentHashCode()
        result = 31 * result + currentGrid.contentHashCode()
        result = 31 * result + solutionGrid.contentHashCode()
        result = 31 * result + selectedCell
        result = 31 * result + notes.hashCode()
        result = 31 * result + isNotesMode.hashCode()
        result = 31 * result + elapsedSeconds.hashCode()
        result = 31 * result + errorsCount
        result = 31 * result + maxErrorsAllowed
        result = 31 * result + isGameFinished.hashCode()
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + earnedXp
        result = 31 * result + isDailyChallenge.hashCode()
        result = 31 * result + currentDateString.hashCode()
        result = 31 * result + isTimerActive.hashCode()
        result = 31 * result + hintsLeft
        return result
    }
}

data class LeaderboardPlayer(
    val rank: Int,
    val name: String,
    val country: String,
    val flagEmoji: String,
    val xp: Int,
    val isUser: Boolean = false
)

data class UndoSnapshot(
    val currentGrid: IntArray,
    val notes: Map<Int, Set<Int>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UndoSnapshot
        if (!currentGrid.contentEquals(other.currentGrid)) return false
        if (notes != other.notes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = currentGrid.contentHashCode()
        result = 31 * result + notes.hashCode()
        return result
    }
}

class GameViewModel(private val repository: SudokuRepository) : ViewModel() {

    private val _state = MutableStateFlow(SudokuState())
    val state: StateFlow<SudokuState> = _state.asStateFlow()

    // Screen Tabs: 0 = Game, 1 = Daily Calendar, 2 = Leaderboard & Stats
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Undo Stack
    private val undoStack = mutableListOf<UndoSnapshot>()

    // Timer Job
    private var timerJob: Job? = null

    // Base roster of virtual computer-controlled players for leaderboard
    private val simulatedCompetitors = listOf(
        LeaderboardPlayer(0, "HiroshiT", "JP", "🇯🇵", 12450),
        LeaderboardPlayer(0, "SoniaGrid", "ES", "🇪🇸", 10200),
        LeaderboardPlayer(0, "Alex88", "US", "🇺🇸", 8700),
        LeaderboardPlayer(0, "SudokuMasterX", "DE", "🇩🇪", 7100),
        LeaderboardPlayer(0, "Mathlete", "IN", "🇮🇳", 5850),
        LeaderboardPlayer(0, "SolveFlash", "GB", "🇬🇧", 4900),
        LeaderboardPlayer(0, "EnigmaSolver", "FR", "🇫🇷", 3800),
        LeaderboardPlayer(0, "PuzzlerMax", "BR", "🇧🇷", 2800),
        LeaderboardPlayer(0, "ZenPencil", "CA", "🇨🇦", 1950),
        LeaderboardPlayer(0, "Flora99", "IT", "🇮🇹", 1250),
        LeaderboardPlayer(0, "Numbros", "AU", "🇦🇺", 780),
        LeaderboardPlayer(0, "GridLockPro", "NL", "🇳🇱", 420)
    )

    // User Profile
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Leaderboard flow (combining real User Profile with virtual players)
    val leaderboard: StateFlow<List<LeaderboardPlayer>> = repository.userProfile
        .map { profile ->
            val userXp = profile?.totalXp ?: 0
            val username = profile?.username ?: "SudokuPlayer"
            
            val userRow = LeaderboardPlayer(
                rank = 0,
                name = username,
                country = "US",
                flagEmoji = "🇺🇸",
                xp = userXp,
                isUser = true
            )
            
            val sortedList = (simulatedCompetitors + userRow)
                .sortedByDescending { it.xp }
            
            sortedList.mapIndexed { index, player ->
                player.copy(rank = index + 1)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStats: StateFlow<List<UserStats>> = repository.allStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyChallenges: StateFlow<List<DailyChallengeStatus>> = repository.dailyChallenges
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Pre-seed User Profile in DB
            repository.getOrCreateUserProfile()
            
            // Try loading standard resumed session (Standard ID = 1)
            loadActiveSession(1)
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        if (tabIndex == 0) {
            // Resume timer if game is playing
            if (!_state.value.isGameFinished && !_state.value.isGameOver && _state.value.originalGrid.any { it != 0 }) {
                startTimer()
            }
        } else {
            // Pause timer if user leaves gameplay tab
            pauseTimer()
        }
    }

    fun setUsername(name: String) {
        viewModelScope.launch {
            repository.updateUsername(name)
        }
    }

    // --- GAME ACTIONS ---

    fun startNewGame(difficulty: String) {
        pauseTimer()
        val puzzle = SudokuGenerator.generate(difficulty)
        
        undoStack.clear()
        _state.value = SudokuState(
            difficulty = difficulty,
            originalGrid = puzzle.originalGrid,
            currentGrid = puzzle.originalGrid.clone(),
            solutionGrid = puzzle.solutionGrid,
            isTimerActive = true,
            isDailyChallenge = false,
            currentDateString = "normal"
        )
        
        startTimer()
        saveSession(1)
    }

    fun startDailyChallenge(dateString: String) {
        pauseTimer()
        // Generate seed using hash code of date string to keep it perfectly identical for all users on this day
        val seed = dateString.hashCode().toLong()
        // Daily challenges are set to medium of difficulty
        val difficulty = "Medium"
        val puzzle = SudokuGenerator.generate(difficulty, seed)
        
        undoStack.clear()
        _state.value = SudokuState(
            difficulty = difficulty,
            originalGrid = puzzle.originalGrid,
            currentGrid = puzzle.originalGrid.clone(),
            solutionGrid = puzzle.solutionGrid,
            isTimerActive = true,
            isDailyChallenge = true,
            currentDateString = dateString
        )
        
        startTimer()
        saveSession(2) // ID = 2 for Daily Challenge active session
    }

    fun selectCell(index: Int) {
        if (_state.value.isGameFinished || _state.value.isGameOver) return
        _state.update { it.copy(selectedCell = index) }
    }

    fun enterDigit(digit: Int) {
        val selected = _state.value.selectedCell
        if (selected == -1 || _state.value.isGameFinished || _state.value.isGameOver) return
        
        // Locked if cell is part of original grid
        if (_state.value.originalGrid[selected] != 0) return

        // Push undo state before modifying
        pushToUndoStack()

        if (_state.value.isNotesMode) {
            // Notes Mode: Toggle Pencil Marking
            _state.update { currentState ->
                val currentCellNotes = currentState.notes[selected] ?: emptySet()
                val updatedNotes = if (currentCellNotes.contains(digit)) {
                    currentCellNotes - digit
                } else {
                    currentCellNotes + digit
                }
                
                // Keep pencil and remove digit from physical board values if any
                val gridCopy = currentState.currentGrid.clone()
                gridCopy[selected] = 0
                
                currentState.copy(
                    notes = currentState.notes + (selected to updatedNotes),
                    currentGrid = gridCopy
                )
            }
        } else {
            // Values Input Mode
            _state.update { currentState ->
                val gridCopy = currentState.currentGrid.clone()
                var currentSelectionErrorCount = currentState.errorsCount
                
                // Clear state notes for this cell since we filled an actual value
                val updatedNotes = currentState.notes - selected
                
                gridCopy[selected] = digit
                
                // Check if mistake is made
                if (digit != 0 && digit != currentState.solutionGrid[selected]) {
                    currentSelectionErrorCount++
                }

                currentState.copy(
                    currentGrid = gridCopy,
                    notes = updatedNotes,
                    errorsCount = currentSelectionErrorCount
                )
            }
            
            // Check defeat condition
            if (_state.value.errorsCount >= _state.value.maxErrorsAllowed) {
                pauseTimer()
                _state.update { it.copy(isGameOver = true, isTimerActive = false) }
                viewModelScope.launch {
                    repository.recordGameLoss(_state.value.difficulty)
                    repository.deleteGameSession(if (_state.value.isDailyChallenge) 2 else 1)
                }
                return
            }

            // Check victory condition
            checkVictory()
        }
        
        // Persist session
        saveSession(if (_state.value.isDailyChallenge) 2 else 1)
    }

    fun deleteCell() {
        val selected = _state.value.selectedCell
        if (selected == -1 || _state.value.isGameFinished || _state.value.isGameOver) return
        if (_state.value.originalGrid[selected] != 0) return

        pushToUndoStack()
        
        _state.update { currentState ->
            val gridCopy = currentState.currentGrid.clone()
            gridCopy[selected] = 0
            
            currentState.copy(
                currentGrid = gridCopy,
                notes = currentState.notes - selected
            )
        }
        
        saveSession(if (_state.value.isDailyChallenge) 2 else 1)
    }

    fun toggleNotesMode() {
        _state.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    fun triggerHint() {
        if (_state.value.selectedCell == -1 || _state.value.isGameFinished || _state.value.isGameOver) return
        val selected = _state.value.selectedCell
        
        // Cannot hint locked cells
        if (_state.value.originalGrid[selected] != 0) return
        if (_state.value.hintsLeft <= 0) return

        pushToUndoStack()

        _state.update { currentState ->
            val correctVal = currentState.solutionGrid[selected]
            val gridCopy = currentState.currentGrid.clone()
            gridCopy[selected] = correctVal
            
            currentState.copy(
                currentGrid = gridCopy,
                notes = currentState.notes - selected,
                hintsLeft = currentState.hintsLeft - 1
            )
        }

        checkVictory()
        saveSession(if (_state.value.isDailyChallenge) 2 else 1)
    }

    fun performUndo() {
        if (undoStack.isEmpty() || _state.value.isGameFinished || _state.value.isGameOver) return
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        _state.update { it.copy(
            currentGrid = previousState.currentGrid,
            notes = previousState.notes
        ) }
        saveSession(if (_state.value.isDailyChallenge) 2 else 1)
    }

    // --- UTILITY ENGINE ---

    private fun checkVictory() {
        val current = _state.value.currentGrid
        val solution = _state.value.solutionGrid
        
        // Check alignment
        if (current.contentEquals(solution)) {
            pauseTimer()
            _state.update { it.copy(isGameFinished = true, isTimerActive = false) }
            
            viewModelScope.launch {
                // Save records of wins and calculate points
                val xpEarned = repository.recordGameWin(
                    difficulty = _state.value.difficulty,
                    elapsedSeconds = _state.value.elapsedSeconds,
                    isDaily = _state.value.isDailyChallenge,
                    dateString = _state.value.currentDateString
                )
                
                _state.update { it.copy(earnedXp = xpEarned) }
                
                // Clear active resumed sessions since completed
                repository.deleteGameSession(if (_state.value.isDailyChallenge) 2 else 1)
            }
        }
    }

    private fun pushToUndoStack() {
        val gridCopy = _state.value.currentGrid.clone()
        val notesCopy = _state.value.notes.toMap()
        if (undoStack.size >= 25) {
            undoStack.removeAt(0)
        }
        undoStack.add(UndoSnapshot(gridCopy, notesCopy))
    }

    // Timer Implementation
    private fun startTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isTimerActive = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isTimerActive = false) }
    }

    // --- RESUME SESSION HANDLERS ---

    private fun loadActiveSession(id: Int) {
        viewModelScope.launch {
            val session = repository.getGameSession(id)
            if (session != null && !session.isCompleted) {
                // Reconstruct flat arrays
                val original = mapStringToGrid(session.originalGrid)
                val current = mapStringToGrid(session.currentGrid)
                val solution = mapStringToGrid(session.solutionGrid)
                val reconstructedNotes = mapStringToNotes(session.notes)
                
                _state.value = SudokuState(
                    difficulty = session.difficulty,
                    originalGrid = original,
                    currentGrid = current,
                    solutionGrid = solution,
                    notes = reconstructedNotes,
                    elapsedSeconds = session.elapsedSeconds,
                    errorsCount = session.errorsCount,
                    isDailyChallenge = session.dateString != "normal",
                    currentDateString = session.dateString,
                    isTimerActive = true
                )
                startTimer()
            } else {
                // Generate a default Medium puzzle on initial load
                startNewGame("Medium")
            }
        }
    }

    private fun saveSession(id: Int) {
        val s = _state.value
        // Only save active models
        if (s.originalGrid.all { it == 0 }) return
        
        viewModelScope.launch {
            val session = GameSession(
                id = id,
                difficulty = s.difficulty,
                originalGrid = mapGridToString(s.originalGrid),
                currentGrid = mapGridToString(s.currentGrid),
                solutionGrid = mapGridToString(s.solutionGrid),
                notes = mapNotesToString(s.notes),
                elapsedSeconds = s.elapsedSeconds,
                errorsCount = s.errorsCount,
                isCompleted = s.isGameFinished,
                dateString = s.currentDateString
            )
            repository.saveGameSession(session)
        }
    }

    // Note mappings utils (Format: "135|89|")
    private fun mapNotesToString(notes: Map<Int, Set<Int>>): String {
        val parts = Array(81) { "" }
        for (i in 0..80) {
            val s = notes[i]
            parts[i] = s?.sorted()?.joinToString("") ?: ""
        }
        return parts.joinToString("|")
    }

    private fun mapStringToNotes(str: String): Map<Int, Set<Int>> {
        val notes = mutableMapOf<Int, Set<Int>>()
        val tokens = str.split("|")
        for (i in 0 until minOf(81, tokens.size)) {
            val token = tokens[i]
            if (token.isNotEmpty()) {
                val setDigits = token.map { it.toString().toInt() }.toSet()
                notes[i] = setDigits
            }
        }
        return notes
    }

    private fun mapGridToString(grid: IntArray): String = grid.joinToString("")

    private fun mapStringToGrid(str: String): IntArray {
        val arr = IntArray(81)
        for (i in 0 until minOf(81, str.length)) {
            arr[i] = str[i].toString().toInt()
        }
        return arr
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class GameViewModelFactory(private val repository: SudokuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
