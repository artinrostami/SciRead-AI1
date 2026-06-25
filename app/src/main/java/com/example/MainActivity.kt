package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SciReadViewModel

enum class SciReadScreen(val title: String, val icon: ImageVector, val tag: String) {
    WORKSPACE("Workspace", Icons.Default.MenuBook, "nav_workspace"),
    GLOSSARY("Glossary", Icons.Default.Book, "nav_glossary"),
    STUDY_HUB("Study Hub", Icons.Default.School, "nav_study_hub"),
    AI_TUTOR("AI Tutor", Icons.Default.AutoAwesome, "nav_ai_tutor")
}

class MainActivity : ComponentActivity() {
    private val viewModel: SciReadViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                var currentScreen by remember { mutableStateOf(SciReadScreen.WORKSPACE) }
                var showModelMenu by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = "App Logo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "SciRead AI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Scientific Reading Tutor",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Model Switcher Dropdown Anchor
                                Box(modifier = Modifier.padding(end = 4.dp)) {
                                    AssistChip(
                                        onClick = { showModelMenu = true },
                                        label = {
                                            Text(
                                                if (viewModel.activeModel == "gemini-3.5-flash") "Gemini 3.5 Flash"
                                                else "Gemini 3.1 Pro",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Memory,
                                                contentDescription = "Model",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.testTag("model_switcher_chip")
                                    )

                                    DropdownMenu(
                                        expanded = showModelMenu,
                                        onDismissRequest = { showModelMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Gemini 3.5 Flash (Fast)", fontWeight = FontWeight.Bold)
                                                    Text("Perfect for instant definitions & summaries", fontSize = 10.sp)
                                                }
                                            },
                                            onClick = {
                                                viewModel.activeModel = "gemini-3.5-flash"
                                                showModelMenu = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = "Flash") }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("Gemini 3.1 Pro (Deep)", fontWeight = FontWeight.Bold)
                                                    Text("Recommended for detailed scientific analyses", fontSize = 10.sp)
                                                }
                                            },
                                            onClick = {
                                                viewModel.activeModel = "gemini-3.1-pro-preview"
                                                showModelMenu = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Pro") }
                                        )
                                    }
                                }

                                // Dark Mode Switcher
                                IconButton(
                                    onClick = { viewModel.isDarkMode = !viewModel.isDarkMode },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Dark Mode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            SciReadScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag(screen.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { target ->
                            when (target) {
                                SciReadScreen.WORKSPACE -> ReaderScreen(viewModel = viewModel)
                                SciReadScreen.GLOSSARY -> GlossaryScreen(viewModel = viewModel)
                                SciReadScreen.STUDY_HUB -> StudyHubScreen(viewModel = viewModel)
                                SciReadScreen.AI_TUTOR -> ChatAssistantScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
