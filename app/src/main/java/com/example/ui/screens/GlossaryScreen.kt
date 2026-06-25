package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.GlossaryTerm
import com.example.ui.viewmodel.SciReadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    viewModel: SciReadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val terms by viewModel.filteredGlossaryTerms.collectAsState()
    
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var newTerm by remember { mutableStateOf("") }
    var newTrans by remember { mutableStateOf("") }
    var newPron by remember { mutableStateOf("") }
    var newPos by remember { mutableStateOf("") }
    var newContext by remember { mutableStateOf("") }
    var newExample by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Scientific Glossary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your personal biology, biotech, and medical dictionary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Export Button
                FilledTonalButton(
                    onClick = {
                        if (terms.isEmpty()) {
                            Toast.makeText(context, "Glossary is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val sb = StringBuilder()
                            sb.append("SCIREAD AI SCIENTIFIC GLOSSARY EXPORT\n\n")
                            terms.forEach {
                                sb.append("${it.term} [${it.partOfSpeech}]: ${it.translation}\n")
                                sb.append("Context: ${it.scientificContext}\n")
                                sb.append("Example: ${it.exampleUsage}\n")
                                sb.append("----------------------------\n")
                            }
                            clipboard.setText(AnnotatedString(sb.toString()))
                            Toast.makeText(context, "Glossary copied to clipboard!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("export_glossary_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }

                // Add Term Button
                Button(
                    onClick = { isAddDialogOpen = true },
                    modifier = Modifier.testTag("add_term_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Term")
                    Spacer(Modifier.width(4.dp))
                    Text("Add Term")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = viewModel.glossarySearchQuery,
            onValueChange = { viewModel.glossarySearchQuery = it },
            placeholder = { Text("Search terms, definitions, translations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (viewModel.glossarySearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.glossarySearchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("glossary_search_field"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (terms.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Empty Glossary",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No terms in your Glossary yet.",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Highlight scientific terms while reading to instantly translate and save.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(terms) { term ->
                    GlossaryTermCard(
                        term = term,
                        onDelete = { viewModel.deleteGlossaryTerm(term) }
                    )
                }
            }
        }
    }

    // Manual Add Dialog
    if (isAddDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddDialogOpen = false },
            title = { Text("Add Scientific Term", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTerm,
                        onValueChange = { newTerm = it },
                        label = { Text("Scientific Term (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTrans,
                        onValueChange = { newTrans = it },
                        label = { Text("Translation (Persian)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPron,
                            onValueChange = { newPron = it },
                            label = { Text("IPA Phonetic") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPos,
                            onValueChange = { newPos = it },
                            label = { Text("Part of Speech") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newContext,
                        onValueChange = { newContext = it },
                        label = { Text("Scientific Context") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExample,
                        onValueChange = { newExample = it },
                        label = { Text("Example Scientific Usage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTerm.isNotBlank() && newTrans.isNotBlank()) {
                            viewModel.saveGlossaryTerm(
                                GlossaryTerm(
                                    term = newTerm,
                                    translation = newTrans,
                                    pronunciation = newPron,
                                    partOfSpeech = newPos,
                                    scientificContext = newContext,
                                    exampleUsage = newExample
                                )
                            )
                            isAddDialogOpen = false
                            newTerm = ""
                            newTrans = ""
                            newPron = ""
                            newPos = ""
                            newContext = ""
                            newExample = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GlossaryTermCard(
    term: GlossaryTerm,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = term.term,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (term.partOfSpeech.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = term.partOfSpeech,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (term.pronunciation.isNotEmpty()) {
                        Text(
                            text = "[${term.pronunciation}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_term_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete term",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Persian Translation Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "معادل علمی",
                        fontSize = 11.sp,
                        color = Color(0xFF004D40),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = term.translation,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00796B),
                        textAlign = TextAlign.Right
                    )
                }
            }

            if (term.scientificContext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BIOLOGICAL / MEDICAL CONTEXT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = term.scientificContext,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }

            if (term.exampleUsage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SCIENTIFIC EXAMPLE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = term.exampleUsage,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}
