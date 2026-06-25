package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.Flashcard
import com.example.data.db.QuizResult
import com.example.ui.viewmodel.SciReadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(
    viewModel: SciReadViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Flashcards, 1: AI Quizzes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Main Screen Header
        Text(
            text = "Academic Study Hub",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Consolidate your knowledge with AI-generated quizzes and Spaced Repetition",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tab Selector
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Style, "Flashcards")
                        Spacer(Modifier.width(8.dp))
                        Text("Spaced Repetition Flashcards")
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QuestionAnswer, "Quizzes")
                        Spacer(Modifier.width(8.dp))
                        Text("Scientific AI Quizzes")
                    }
                }
            )
        }

        when (activeTab) {
            0 -> FlashcardSection(viewModel)
            1 -> QuizSection(viewModel)
        }
    }
}

// --- Spaced Repetition Flashcard Section ---
@Composable
fun FlashcardSection(viewModel: SciReadViewModel) {
    val dueCards by viewModel.dueFlashcards.collectAsState()
    val allCards by viewModel.flashcards.collectAsState()
    
    var isAddManualOpen by remember { mutableStateOf(false) }
    var manualFront by remember { mutableStateOf("") }
    var manualBack by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Text(
                "Due Today: ${dueCards.size} of ${allCards.size} total cards",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 13.sp
            )
        }

        Button(
            onClick = { isAddManualOpen = true },
            modifier = Modifier.testTag("add_flashcard_manual_button")
        ) {
            Icon(Icons.Default.Add, "Add card")
            Spacer(Modifier.width(4.dp))
            Text("Create Card")
        }
    }

    if (dueCards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = "Done",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF0F9B8E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Inbox Zero! Excellent Work.",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "You have reviewed all due scientific cards for today. Keep reading to create more!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        var cardIndex by remember { mutableStateOf(0) }
        if (cardIndex >= dueCards.size) {
            cardIndex = 0 // Reset bounds
        }
        val currentCard = dueCards.getOrNull(cardIndex)

        if (currentCard != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var isFlipped by remember(currentCard.id) { mutableStateOf(false) }
                
                // Flip Animation Factor
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    label = "Card Flip"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(240.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isFlipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // FRONT - Question
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.QuestionMark,
                                contentDescription = "Front",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentCard.front,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Tap Card to Reveal Answer",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // BACK - Answer (Rotated 180 so it's upright after rotationY)
                        Column(
                            modifier = Modifier
                                .graphicsLayer { rotationY = 180f }
                                .padding(24.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Back",
                                tint = Color(0xFF0F9B8E),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentCard.back,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Spaced Repetition Grading Controls
                Text(
                    "GRADE YOUR KNOWLEDGE COGNITIVELY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hard Button
                    OutlinedButton(
                        onClick = {
                            viewModel.rateFlashcard(currentCard, "hard")
                            isFlipped = false
                            if (cardIndex < dueCards.size - 1) cardIndex++
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("grade_hard"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hard", fontWeight = FontWeight.Bold)
                            Text("Repeat", fontSize = 10.sp)
                        }
                    }

                    // Good Button
                    OutlinedButton(
                        onClick = {
                            viewModel.rateFlashcard(currentCard, "good")
                            isFlipped = false
                            if (cardIndex < dueCards.size - 1) cardIndex++
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1.5f).testTag("grade_good"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Good", fontWeight = FontWeight.Bold)
                            Text("4 Days", fontSize = 10.sp)
                        }
                    }

                    // Easy Button
                    Button(
                        onClick = {
                            viewModel.rateFlashcard(currentCard, "easy")
                            isFlipped = false
                            if (cardIndex < dueCards.size - 1) cardIndex++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9B8E)),
                        modifier = Modifier.weight(1f).testTag("grade_easy")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Easy", fontWeight = FontWeight.Bold)
                            Text("6 Days", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (isAddManualOpen) {
        AlertDialog(
            onDismissRequest = { isAddManualOpen = false },
            title = { Text("Create Custom Study Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualFront,
                        onValueChange = { manualFront = it },
                        label = { Text("Front Question / Concept") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualBack,
                        onValueChange = { manualBack = it },
                        label = { Text("Back Answer / Explanation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualFront.isNotBlank() && manualBack.isNotBlank()) {
                            viewModel.addManualFlashcard(manualFront, manualBack)
                            isAddManualOpen = false
                            manualFront = ""
                            manualBack = ""
                        }
                    }
                ) {
                    Text("Save Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddManualOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Scientific AI Quiz Section ---
@Composable
fun QuizSection(viewModel: SciReadViewModel) {
    val results by viewModel.quizHistory.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Quiz Active Card Frame
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(end = 12.dp)
        ) {
            if (viewModel.isQuizLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "AI Generating academic exam questions...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (viewModel.quizQuestions.isEmpty()) {
                // Config Setup Screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "AI Quiz Generation Engine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Generate 5 high-yield multiple-choice exam questions dynamically parsed from reading topics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text("QUIZ TOPIC / CONCEPT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = viewModel.activeQuizTopic,
                            onValueChange = { viewModel.activeQuizTopic = it },
                            placeholder = { Text("E.g., DNA Methylation mechanism or CRISPR Off-targets") },
                            modifier = Modifier.fillMaxWidth().testTag("quiz_topic_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("DIFFICULTY SEGMENT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                                FilterChip(
                                    selected = viewModel.activeQuizDifficulty == level,
                                    onClick = { viewModel.activeQuizDifficulty = level },
                                    label = { Text(level) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { viewModel.startNewQuiz(viewModel.activeQuizTopic, viewModel.activeQuizDifficulty) },
                            modifier = Modifier.fillMaxWidth().testTag("generate_quiz_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, "Generate")
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Scientific Quiz")
                        }
                    }
                }
            } else if (viewModel.isQuizCompleted) {
                // Score Screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Success",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Academic Quiz Complete!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "YOUR SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${viewModel.correctAnswersCount} / ${viewModel.quizQuestions.size}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Excellent diagnostic evaluation! Result saved successfully in your local academic transcript.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Button(
                            onClick = { viewModel.quizQuestions = emptyList() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Take Another Quiz")
                        }
                    }
                }
            } else {
                // Active Quiz Questions Runner
                val question = viewModel.quizQuestions[viewModel.currentQuizQuestionIndex]

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Question ${viewModel.currentQuizQuestionIndex + 1} of ${viewModel.quizQuestions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    viewModel.activeQuizDifficulty,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Choices list
                        question.options.forEachIndexed { oIdx, opt ->
                            val isSelected = viewModel.selectedAnswerIndex == oIdx
                            val isCorrectAnswer = oIdx == question.correctIndex
                            val shouldHighlightCorrect = viewModel.isAnswerSubmitted && isCorrectAnswer
                            val shouldHighlightWrong = viewModel.isAnswerSubmitted && isSelected && !isCorrectAnswer

                            val containerColor = when {
                                shouldHighlightCorrect -> Color(0xFFC8E6C9) // Clean green
                                shouldHighlightWrong -> Color(0xFFFFCDD2)   // Soft red
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }

                            val borderStroke = when {
                                shouldHighlightCorrect -> BorderStroke(1.5.dp, Color(0xFF2E7D32))
                                shouldHighlightWrong -> BorderStroke(1.5.dp, Color(0xFFC62828))
                                isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerColor)
                                    .border(borderStroke, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !viewModel.isAnswerSubmitted) {
                                        viewModel.submitQuizAnswer(oIdx)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${('A'.code + oIdx).toChar()}.",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Answer Feedback & Explanation
                        AnimatedVisibility(
                            visible = viewModel.isAnswerSubmitted,
                            enter = expandVertically() + fadeIn()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                val isUserCorrect = viewModel.selectedAnswerIndex == question.correctIndex
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUserCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isUserCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = "Result",
                                                tint = if (isUserCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isUserCorrect) "CORRECT ANSWER" else "INCORRECT ANSWER",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUserCorrect) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = question.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.nextQuizQuestion() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        if (viewModel.currentQuizQuestionIndex < viewModel.quizQuestions.size - 1) "Next Question"
                                        else "View Final Evaluation"
                                    )
                                    Icon(Icons.Default.NavigateNext, "Next")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quiz History Sidebar (Wide screens)
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.List, contentDescription = "History", tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Historic Transcripts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No past transcripts recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { res ->
                        HistoricQuizCard(res)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricQuizCard(result: QuizResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Topic Evaluation",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Difficulty: ${result.difficulty}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${result.score} / ${result.totalQuestions}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (result.score >= result.totalQuestions / 2) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}
