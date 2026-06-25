package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiQuestion
import com.example.ui.theme.HighlightYellow
import com.example.ui.viewmodel.SciReadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: SciReadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importedPdfUri = uri
            viewModel.importedPdfName = "Imported PDF Documents"
            viewModel.selectedPaperId = "imported"
            viewModel.pdfCurrentPage = 0
            viewModel.bookmarks.value = emptySet()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Main Viewport (Reader)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header Action controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown to select papers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Academic Library",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "SciRead Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Import Button
                FilledTonalButton(
                    onClick = { openPdfLauncher.launch("application/pdf") },
                    modifier = Modifier.testTag("import_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Import PDF",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Import PDF", fontSize = 13.sp)
                }
            }

            // Doc Tabs
            ScrollableTabRow(
                selectedTabIndex = if (viewModel.selectedPaperId == "imported") viewModel.preloadedPapers.size else viewModel.preloadedPapers.indexOfFirst { it.id == viewModel.selectedPaperId },
                edgePadding = 0.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                viewModel.preloadedPapers.forEachIndexed { index, paper ->
                    Tab(
                        selected = viewModel.selectedPaperId == paper.id,
                        onClick = {
                            viewModel.selectedPaperId = paper.id
                            viewModel.importedPdfUri = null
                        },
                        text = {
                            Text(
                                paper.title.split(":")[0],
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    )
                }
                
                if (viewModel.importedPdfUri != null) {
                    Tab(
                        selected = viewModel.selectedPaperId == "imported",
                        onClick = { viewModel.selectedPaperId = "imported" },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Icon",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text("Local PDF", fontWeight = FontWeight.Medium)
                            }
                        }
                    )
                }
            }

            // Reader Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                if (viewModel.selectedPaperId == "imported" && viewModel.importedPdfUri != null) {
                    // PDF Reader Mode
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            PdfPageViewer(
                                uri = viewModel.importedPdfUri!!,
                                currentPageIndex = viewModel.pdfCurrentPage,
                                onPageCountReady = { viewModel.pdfPageCount = it }
                            )

                            // Floating AI page Analyzer
                            FloatingActionButton(
                                onClick = {
                                    viewModel.selectedTextForAction = "Please analyze page ${viewModel.pdfCurrentPage + 1} of this scientific document."
                                    viewModel.isSelectionPanelOpen = true
                                    viewModel.runConceptExplanation()
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .testTag("floating_page_ai_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Page AI")
                            }
                        }

                        // Bottom Page Controllers
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.toggleBookmark(viewModel.pdfCurrentPage) }
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.bookmarks.value.contains(viewModel.pdfCurrentPage)) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Bookmark page",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        "Page ${viewModel.pdfCurrentPage + 1} of ${viewModel.pdfPageCount}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { if (viewModel.pdfCurrentPage > 0) viewModel.pdfCurrentPage-- },
                                        enabled = viewModel.pdfCurrentPage > 0
                                    ) {
                                        Icon(Icons.Default.NavigateBefore, "Prev Page")
                                    }
                                    IconButton(
                                        onClick = { if (viewModel.pdfCurrentPage < viewModel.pdfPageCount - 1) viewModel.pdfCurrentPage++ },
                                        enabled = viewModel.pdfCurrentPage < viewModel.pdfPageCount - 1
                                    ) {
                                        Icon(Icons.Default.NavigateNext, "Next Page")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Preloaded Paper Reader Mode
                    val activePaper = viewModel.activePaper
                    if (activePaper != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            item {
                                Text(
                                    text = activePaper.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = activePaper.authors,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = activePaper.journal,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                            }

                            activePaper.sections.forEach { section ->
                                item {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(section.paragraphs) { paragraph ->
                                    val isSelected = viewModel.selectedTextForAction == paragraph
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) HighlightYellow.copy(alpha = 0.3f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectedTextForAction = paragraph
                                                viewModel.isSelectionPanelOpen = true
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = paragraph,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 24.sp,
                                            textAlign = TextAlign.Justify
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contextual AI Overlay Panel (Side drawer layout for large screens)
        AnimatedVisibility(
            visible = viewModel.isSelectionPanelOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(0.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header of Drawer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Smart Actions",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Smart Tutor Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { viewModel.isSelectionPanelOpen = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Panel")
                    }
                }

                // Show Selected Text Snippet
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "SELECTED SNIPPET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (viewModel.selectedTextForAction.length > 150) "${viewModel.selectedTextForAction.take(150)}..." else viewModel.selectedTextForAction,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Action Selector Buttons Grid
                Text(
                    "TUTORIAL ACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.runScientificTranslation() },
                        modifier = Modifier.weight(1f).testTag("action_translate")
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = "Translate")
                        Spacer(Modifier.width(4.dp))
                        Text("Translate", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.runConceptExplanation() },
                        modifier = Modifier.weight(1f).testTag("action_explain")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Explain")
                        Spacer(Modifier.width(4.dp))
                        Text("Explain", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.runParagraphAnalysis() },
                        modifier = Modifier.weight(1f).testTag("action_analyze")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analyze")
                        Spacer(Modifier.width(4.dp))
                        Text("Analyze", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.generateFlashcardForConcept() },
                        modifier = Modifier.weight(1f).testTag("action_flashcard")
                    ) {
                        Icon(Icons.Default.Style, contentDescription = "Card")
                        Spacer(Modifier.width(4.dp))
                        Text("Flashcard", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.showBilingualSideBySide,
                        onCheckedChange = { viewModel.showBilingualSideBySide = it }
                    )
                    Text("Show Persian explanations side-by-side", fontSize = 12.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Output Display Panel
                if (viewModel.isAiLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 1. Translation Results
                    val trans = viewModel.translationResult
                    if (trans != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trans.original,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = trans.partOfSpeech,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (trans.pronunciation.isNotEmpty()) {
                                    Text(
                                        text = "Phonetic: ${trans.pronunciation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Persian Translation / ترجمه فارسی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = trans.persian,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F9B8E),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )

                                if (trans.scientificContext.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Scientific Context",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = trans.scientificContext,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (trans.synonyms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Alternative Synonyms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = trans.synonyms,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (trans.exampleUsage.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Scientific Example Usage",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = trans.exampleUsage,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = { viewModel.saveTranslationToGlossary() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save to glossary")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add to Scientific Glossary")
                                }
                            }
                        }
                    }

                    // 2. Explanation Results
                    val exp = viewModel.explanationResult
                    if (exp != null) {
                        TabRow(
                            selectedTabIndex = viewModel.selectedTabInExplainResult,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Tab(
                                selected = viewModel.selectedTabInExplainResult == 0,
                                onClick = { viewModel.selectedTabInExplainResult = 0 },
                                text = { Text("Beginner", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = viewModel.selectedTabInExplainResult == 1,
                                onClick = { viewModel.selectedTabInExplainResult = 1 },
                                text = { Text("Scientific", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = viewModel.selectedTabInExplainResult == 2,
                                onClick = { viewModel.selectedTabInExplainResult = 2 },
                                text = { Text("Researcher", fontSize = 12.sp) }
                            )
                        }

                        val activeExplanation = when (viewModel.selectedTabInExplainResult) {
                            0 -> exp.simple
                            1 -> exp.scientific
                            else -> exp.advanced
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Explanation Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )

                                if (exp.relatedConcepts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Related Biological Concepts",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        exp.relatedConcepts.forEach { concept ->
                                            SuggestionChip(
                                                onClick = {
                                                    viewModel.selectedTextForAction = concept
                                                    viewModel.runConceptExplanation()
                                                },
                                                label = { Text(concept, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                if (exp.commonApplications.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Research & Industrial Applications",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    exp.commonApplications.forEach { app ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, "App Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(app, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Paragraph Analysis Results
                    val analysis = viewModel.paragraphAnalysisResult
                    if (analysis != null) {
                        Column {
                            // Section: Summary
                            Text(
                                "SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(analysis.summary, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section: Simplified
                            Text(
                                "SIMPLIFIED ENGLISH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(analysis.simplified, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }

                            if (viewModel.showBilingualSideBySide && analysis.translation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "PERSIAN TRANSLATION / ترجمه فارسی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F9B8E)
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        analysis.translation,
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Right,
                                        color = Color(0xFF004D40)
                                    )
                                }
                            }

                            if (analysis.keyConcepts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "EXTRACTED KEY CONCEPTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(modifier = Modifier.padding(vertical = 4.dp)) {
                                    analysis.keyConcepts.forEach { concept ->
                                        SuggestionChip(
                                            onClick = {
                                                viewModel.selectedTextForAction = concept
                                                viewModel.runConceptExplanation()
                                            },
                                            label = { Text(concept) },
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (analysis.studyNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "STRUCTURED STUDY NOTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(analysis.studyNotes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (analysis.examQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "EXAM REHEARSAL QUESTIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                analysis.examQuestions.forEachIndexed { qIdx, question ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                "Q${qIdx + 1}: ${question.question}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            question.options.forEachIndexed { oIdx, opt ->
                                                Text(
                                                    "${oIdx + 1}. $opt ${if (oIdx == question.correctIndex) "✓" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                                )
                                            }
                                            Text(
                                                "Ex: ${question.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Note Saving Input Box
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                var customNoteText by remember { mutableStateOf("") }
                Text(
                    "ADD PRIVATE NOTE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customNoteText,
                    onValueChange = { customNoteText = it },
                    placeholder = { Text("Write personal study commentary...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.saveCustomNote(customNoteText)
                        customNoteText = ""
                    },
                    modifier = Modifier.align(Alignment.End).testTag("save_note_button"),
                    enabled = customNoteText.isNotBlank()
                ) {
                    Icon(Icons.Default.NoteAdd, "Save Note")
                    Spacer(Modifier.width(4.dp))
                    Text("Save Study Note")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Top,
        content = { content() }
    )
}

@Composable
fun PdfPageViewer(
    uri: Uri,
    currentPageIndex: Int,
    onPageCountReady: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pageBitmap by remember(uri, currentPageIndex) { mutableStateOf<Bitmap?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri, currentPageIndex) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                    val renderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
                    onPageCountReady(renderer.pageCount)
                    if (currentPageIndex < renderer.pageCount) {
                        renderer.openPage(currentPageIndex).use { page ->
                            // Scale down rendering factor slightly for JVM performance/memory balance
                            val width = (page.width * 1.5).toInt()
                            val height = (page.height * 1.5).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageBitmap = bitmap
                        }
                    }
                    renderer.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = e.localizedMessage
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${currentPageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        } else if (errorMsg != null) {
            Text(
                text = "Failed to load page: $errorMsg",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
