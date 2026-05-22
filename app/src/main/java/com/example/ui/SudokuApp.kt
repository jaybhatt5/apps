package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyChallengeStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SudokuApp(viewModel: GameViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = CardDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Play Game") },
                    label = { Text("Game") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = EditableColor,
                        unselectedTextColor = EditableColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Daily Challenges") },
                    label = { Text("Daily") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = EditableColor,
                        unselectedTextColor = EditableColor
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Leaderboard & Stars") },
                    label = { Text("Ranks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = PrimaryAccent,
                        unselectedIconColor = EditableColor,
                        unselectedTextColor = EditableColor
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkSlateBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> GamePlayScreen(viewModel = viewModel, state = state)
                1 -> DailyChallengeScreen(viewModel = viewModel)
                2 -> StatsAndLeaderboardScreen(viewModel = viewModel, userProfile = userProfile)
            }
        }
    }
}

// ============================================
// SCREEN 1: GAMEPLAY VIEW
// ============================================

@Composable
fun GamePlayScreen(viewModel: GameViewModel, state: SudokuState) {
    var showNewGameMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- HEADER ROW (Difficulty, Timer, Mistakes) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.difficulty.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = if (state.isDailyChallenge) "DAILY CHALLENGE" else "STANDARD GAME",
                    fontSize = 11.sp,
                    color = EditableColor
                )
            }

            // High-Contrast Timer Box
            Box(
                modifier = Modifier
                    .background(Color(0xFF1B202F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formatElapsedTime(state.elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Mistakes Heart styled Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Mistakes: ",
                    fontSize = 12.sp,
                    color = EditableColor
                )
                Text(
                    text = "${state.errorsCount}/${state.maxErrorsAllowed}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.errorsCount > 0) ErrorColor else Color.Green
                )
            }
        }

        // --- THE MAIN SUDOKU BOARD ---
        SudokuBoard(state = state, onCellSelected = { idx -> viewModel.selectCell(idx) })

        // --- GAME CONTROLS (Undo, Erase, Pencil, Hint) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Activity
            ControlButtonItem(
                iconEmoji = "↩️",
                label = "Undo",
                testTag = "undo_button",
                onClick = { viewModel.performUndo() }
            )

            // Erase Activity
            ControlButtonItem(
                iconEmoji = "🧹",
                label = "Erase",
                testTag = "erase_button",
                onClick = { viewModel.deleteCell() }
            )

            // Pencil / Notes Toggle Action
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (state.isNotesMode) AccentTeal else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .background(if (state.isNotesMode) CellSelected else CardDark)
                    .clickable { viewModel.toggleNotesMode() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✏️", fontSize = 20.sp)
                    Text(
                        text = if (state.isNotesMode) "Pencil ON" else "Pencil OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isNotesMode) AccentTeal else EditableColor
                    )
                }
            }

            // Hint Action
            ControlButtonItem(
                iconEmoji = "💡",
                label = "Hint (${state.hintsLeft})",
                testTag = "hint_button",
                onClick = { viewModel.triggerHint() },
                enabled = state.hintsLeft > 0
            )
        }

        // --- THE NUMBERS KEYPAD (1-9) ---
        KeypadPanel(onDigitClick = { num -> viewModel.enterDigit(num) })

        // --- NEW GAME BUTTON ---
        Button(
            onClick = { showNewGameMenu = true },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_game_trigger")
        ) {
            Text("NEW DIFFICULTY", fontWeight = FontWeight.Bold)
        }
    }

    // New Game difficulty menu dropdown list dialog
    if (showNewGameMenu) {
        AlertDialog(
            onDismissRequest = { showNewGameMenu = false },
            containerColor = CardDark,
            title = {
                Text(
                    "Select Game Level",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Easy", "Medium", "Hard", "Expert").forEach { level ->
                        Button(
                            onClick = {
                                viewModel.startNewGame(level)
                                showNewGameMenu = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (level) {
                                    "Easy" -> Color(0xFF4CAF50)
                                    "Medium" -> PrimaryAccent
                                    "Hard" -> Color(0xFFFF9800)
                                    "Expert" -> ErrorColor
                                    else -> PrimaryAccent
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("difficulty_${level.lowercase()}")
                        ) {
                            Text(level, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewGameMenu = false }) {
                    Text("CANCEL", color = EditableColor)
                }
            }
        )
    }

    // Game Win Modals
    if (state.isGameFinished) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CardDark,
            title = {
                Text(
                    "🏆 PUZZLE SOLVED!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HintBtnColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Amazing job! You solved the puzzle.", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Difficulty: ${state.difficulty}", color = EditableColor)
                    Text("Time Elapsed: ${formatElapsedTime(state.elapsedSeconds)}", color = EditableColor)
                    Text("Mistakes Committed: ${state.errorsCount}", color = EditableColor)
                    
                    if (state.earnedXp > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(PrimaryAccent, AccentTeal)),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "+${state.earnedXp} XP Points",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (state.isDailyChallenge) {
                            viewModel.selectTab(1) // Return to daily challenge menu
                            viewModel.startNewGame("Medium") // Prep next game
                        } else {
                            viewModel.startNewGame(state.difficulty)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("win_continue_button")
                ) {
                    Text("CONTINUE PLAYING", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Game Over Defeat Modal
    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CardDark,
            title = {
                Text(
                    "🚨 DEFEAT",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You have committed 3 mistakes and depleted your grid life.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (state.isDailyChallenge) {
                                viewModel.startDailyChallenge(state.currentDateString)
                            } else {
                                viewModel.startNewGame(state.difficulty)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("try_again_button")
                    ) {
                        Text("TRY AGAIN SAME SEED", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            viewModel.startNewGame("Easy")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("START AN EASY GAME", color = AccentTeal, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        )
    }
}

@Composable
fun ControlButtonItem(
    iconEmoji: String,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) CardDark else CardDark.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(iconEmoji, fontSize = 20.sp, color = if (enabled) Color.Unspecified else Color.Gray)
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (enabled) EditableColor else GrayTextTheme()
            )
        }
    }
}

@Composable
fun GrayTextTheme(): Color = Color(0xFF536075)

@Composable
fun SudokuBoard(state: SudokuState, onCellSelected: (Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(BoardBorderThick, shape = RoundedCornerShape(12.dp))
            .padding(3.dp) // Gap buffer edges
    ) {
        val boardWidth = maxWidth
        
        // Use block system for visual thick divides: 3x3 blocks row layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp) // Visual separation between 3x3 blocks
        ) {
            for (blockRow in 0..2) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp) // Visual separation between 3x3 blocks
                ) {
                    for (blockCol in 0..2) {
                        // Render a single 3x3 Block
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(1.dp) // Thin separation within block
                        ) {
                            for (cellRowInBlock in 0..2) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp) // Thin separation within block
                                ) {
                                    for (cellColInBlock in 0..2) {
                                        val actualRow = blockRow * 3 + cellRowInBlock
                                        val actualCol = blockCol * 3 + cellColInBlock
                                        val cellIndex = actualRow * 9 + actualCol
                                        
                                        val isOriginal = state.originalGrid[cellIndex] != 0
                                        val value = state.currentGrid[cellIndex]
                                        val isSelected = state.selectedCell == cellIndex
                                        val cellNotes = state.notes[cellIndex] ?: emptySet()
                                        
                                        // Highlights intersections
                                        val isHighlightedRowOrCol = state.selectedCell != -1 && (
                                                state.selectedCell / 9 == actualRow || state.selectedCell % 9 == actualCol
                                                )
                                        val isSameValHighlight = state.selectedCell != -1 && (
                                                state.currentGrid[state.selectedCell] != 0 && state.currentGrid[state.selectedCell] == value
                                                )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    when {
                                                        isSelected -> CellSelected
                                                        isSameValHighlight -> CellHighlight
                                                        isHighlightedRowOrCol -> CellHighlight.copy(alpha = 0.55f)
                                                        else -> GridBackDefault
                                                    }
                                                )
                                                .clickable { onCellSelected(cellIndex) }
                                                .testTag("cell_$cellIndex"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (value != 0) {
                                                val isError = value != state.solutionGrid[cellIndex]
                                                Text(
                                                    text = value.toString(),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when {
                                                            isError -> ErrorColor
                                                            isOriginal -> ClueColor
                                                            else -> EditableColor
                                                        }
                                                    )
                                                )
                                            } else if (cellNotes.isNotEmpty()) {
                                                PencilNotesGrid(cellNotes)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PencilNotesGrid(notes: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (r in 0..2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (c in 0..2) {
                    val digit = r * 3 + c + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (notes.contains(digit)) {
                            Text(
                                text = digit.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadPanel(onDigitClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (digit in 1..9) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(CardDark, RoundedCornerShape(12.dp))
                    .clickable { onDigitClick(digit) }
                    .testTag("keypad_$digit"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }
}

// ============================================
// SCREEN 2: DAILY CHALLENGE MENU
// ============================================

@Composable
fun DailyChallengeScreen(viewModel: GameViewModel) {
    val dailyList by viewModel.dailyChallenges.collectAsStateWithLifecycle()
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("E", Locale.getDefault())
    
    val weekDays = remember {
        val list = mutableListOf<Pair<String, String>>()
        // Get past 7 calendar days
        for (i in 0..6) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(sdf.format(cal.time) to sdfDay.format(cal.time))
        }
        list.reverse()
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Daily Challenge Display Card Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(CardDark, Color(0xFF1B202F))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "📅 DAILY QUESTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = HintBtnColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete your daily puzzle to earn premium gold crowns and XP bonuses to level up!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Text(
                "This Week's Challenges",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        // Horizontal Row representing the Past Week of challenge options
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { (dateStr, dayLabel) ->
                    val status = dailyList.find { it.dateString == dateStr }
                    val isSolved = status?.isCompleted == true
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSolved) CellSelected else CardDark,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSolved) HintBtnColor else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.startDailyChallenge(dateStr) }
                            .padding(vertical = 12.dp)
                            .testTag("day_$dateStr"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                dayLabel.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EditableColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (isSolved) {
                                Text("👑", fontSize = 20.sp)
                            } else {
                                Text("🎮", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val shortDate = dateStr.takeLast(2)
                            Text(
                                shortDate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSolved) HintBtnColor else Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "History & Crowns Log",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        if (dailyList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👑", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "No daily crowns won yet. Solve today's quest above!",
                            color = EditableColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(dailyList) { challenge ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val formattedDate = convertSdfDate(challenge.dateString)
                            Text(formattedDate, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Completion Speed: ${formatElapsedTime(challenge.completedTimeSeconds)}", fontSize = 12.sp, color = EditableColor)
                        }
                    }
                    Text("+${challenge.scoreAwarded} XP", fontWeight = FontWeight.Bold, color = AccentTeal)
                }
            }
        }
    }
}

// ============================================
// SCREEN 3: LEADERS & STATISTICS VIEW
// ============================================

@Composable
fun StatsAndLeaderboardScreen(viewModel: GameViewModel, userProfile: UserProfile) {
    val leaderboardPlayers by viewModel.leaderboard.collectAsStateWithLifecycle()
    val allStats by viewModel.allStats.collectAsStateWithLifecycle()
    
    var statSubTab by remember { mutableStateOf(0) } // 0 = Leaderboard, 1 = My Stats
    var difficultySelectStat by remember { mutableStateOf("Medium") }
    var renameUsernameText by remember { mutableStateOf(userProfile.username) }
    var editingNameMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- PROFILE HEADER BADGE (Level, Username, XP Progress Bar) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Level Badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(PrimaryAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(userProfile.level.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (editingNameMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = renameUsernameText,
                            onValueChange = { renameUsernameText = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("username_input_field"),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        IconButton(
                            onClick = {
                                if (renameUsernameText.isNotBlank()) {
                                    viewModel.setUsername(renameUsernameText)
                                }
                                editingNameMode = false
                            },
                            modifier = Modifier.testTag("save_username_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Green)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userProfile.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.testTag("username_label")
                        )
                        IconButton(onClick = { editingNameMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Username", tint = AccentTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // XP Progress Slider Indicator
                val base = (userProfile.totalXp / 1000) * 1000
                val progress = (userProfile.totalXp - base) / 1000f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = AccentTeal,
                    trackColor = Color.DarkGray
                )
                Text(
                    text = "${userProfile.totalXp} XP (Next Lvl: ${1000 - (userProfile.totalXp - base)} XP left)",
                    color = EditableColor,
                    fontSize = 11.sp
                )
            }

            // Crown logs count
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                Text("👑", fontSize = 24.sp)
                Text("${userProfile.dailyCrowns}", color = HintBtnColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SUB TABS SWAPPER (Leaderboard vs. User Stats) ---
        TabRow(
            selectedTabIndex = statSubTab,
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            Tab(
                selected = statSubTab == 0,
                onClick = { statSubTab = 0 },
                text = { Text("LEADERBOARDS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            )
            Tab(
                selected = statSubTab == 1,
                onClick = { statSubTab = 1 },
                text = { Text("STATISTICS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- CONTENT SWAP BLOCK ---
        if (statSubTab == 0) {
            // LEADERBOARDS ROW ITERATION
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(leaderboardPlayers) { player ->
                    val isSelf = player.isUser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelf) Brush.horizontalGradient(listOf(CellSelected, PrimaryAccent.copy(alpha = 0.4f))) else Brush.linearGradient(listOf(CardDark, CardDark)),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelf) AccentTeal else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("leaderboard_player_${player.rank}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank number icon container
                            Text(
                                text = when (player.rank) {
                                    1 -> "🥇"
                                    2 -> "🥈"
                                    3 -> "🥉"
                                    else -> "  #${player.rank}"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isSelf) Color.White else EditableColor,
                                modifier = Modifier.width(42.dp)
                            )

                            // Flag country
                            Text(player.flagEmoji, modifier = Modifier.padding(end = 12.dp))

                            Text(
                                text = if (isSelf) "${player.name} (YOU)" else player.name,
                                fontWeight = if (isSelf) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSelf) Color.White else ClueColor,
                                fontSize = 15.sp
                            )
                        }

                        // Score XP count
                        Text(
                            "${player.xp} XP",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelf) HintBtnColor else AccentTeal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            // USER PROGRESS STATISTICS BY DIFFICULTY SELECT
            Column(modifier = Modifier.fillMaxSize()) {
                // Dropdown Diff Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Easy", "Medium", "Hard", "Expert").forEach { diff ->
                        val isSel = difficultySelectStat == diff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) PrimaryAccent else CardDark)
                                .clickable { difficultySelectStat = diff }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff,
                                color = if (isSel) Color.White else EditableColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val curStat = allStats.find { it.difficulty.lowercase() == difficultySelectStat.lowercase() }
                    ?: UserStats(difficulty = difficultySelectStat)

                // Layout statistics cards list
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatDisplayCard(
                            title = "Games Played",
                            valStr = curStat.gamesPlayed.toString(),
                            emoji = "🎮",
                            modifier = Modifier.weight(1f)
                        )
                        StatDisplayCard(
                            title = "Games Won",
                            valStr = curStat.gamesWon.toString(),
                            emoji = "🏆",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatDisplayCard(
                            title = "Best Time",
                            valStr = if (curStat.bestTimeSeconds == 0L) "-" else formatElapsedTime(curStat.bestTimeSeconds),
                            emoji = "⚡",
                            modifier = Modifier.weight(1f)
                        )
                        StatDisplayCard(
                            title = "Average Time",
                            valStr = if (curStat.averageTimeSeconds == 0L) "-" else formatElapsedTime(curStat.averageTimeSeconds),
                            emoji = "⏱️",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatDisplayCard(
                            title = "Current Streak",
                            valStr = curStat.currentStreak.toString(),
                            emoji = "🔥",
                            modifier = Modifier.weight(1f)
                        )
                        StatDisplayCard(
                            title = "Max Streak",
                            valStr = curStat.maxStreak.toString(),
                            emoji = "🎓",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatDisplayCard(title: String, valStr: String, emoji: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = EditableColor, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(valStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

// --- SHARED TIME FORMATTING UTILS ---
fun formatElapsedTime(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

fun convertSdfDate(dateStr: String): String {
    return try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = originalFormat.parse(dateStr)
        val targetFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        if (date != null) targetFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
