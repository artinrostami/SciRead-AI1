package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.db.*
import com.example.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Preloaded Scientific Papers Data Model ---
data class ScientificPaper(
    val id: String,
    val title: String,
    val authors: String,
    val journal: String,
    val sections: List<PaperSection>
)

data class PaperSection(
    val title: String,
    val paragraphs: List<String>
)

class SciReadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SciReadRepository(
        glossaryDao = db.glossaryDao(),
        flashcardDao = db.flashcardDao(),
        noteDao = db.noteDao(),
        quizResultDao = db.quizResultDao()
    )

    // --- State: General Settings ---
    var isDarkMode by mutableStateOf(false)
    var activeModel by mutableStateOf("gemini-3.5-flash") // "gemini-3.5-flash" or "gemini-3.1-pro-preview"
    var showBilingualSideBySide by mutableStateOf(true)

    // --- State: Active Document ---
    var selectedPaperId by mutableStateOf("crispr") // Default preloaded
    var importedPdfUri by mutableStateOf<Uri?>(null)
    var importedPdfName by mutableStateOf<String?>(null)
    var pdfPageCount by mutableStateOf(0)
    var pdfCurrentPage by mutableStateOf(0)
    var bookmarks = mutableStateOf<Set<Int>>(emptySet()) // Bookmarked pages for imported PDF

    // --- State: Selection / Highlight ---
    var selectedTextForAction by mutableStateOf("")
    var isSelectionPanelOpen by mutableStateOf(false)
    
    // AI Results States
    var isAiLoading by mutableStateOf(false)
    var translationResult by mutableStateOf<TranslationResult?>(null)
    var explanationResult by mutableStateOf<ExplainResult?>(null)
    var paragraphAnalysisResult by mutableStateOf<ParagraphAnalysisResult?>(null)
    var selectedTabInExplainResult by mutableStateOf(0) // 0: Simple, 1: Scientific, 2: Advanced

    // --- State: Glossary ---
    val glossaryTerms: StateFlow<List<GlossaryTerm>> = repository.allGlossaryTerms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var glossarySearchQuery by mutableStateOf("")
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredGlossaryTerms: StateFlow<List<GlossaryTerm>> = snapshotFlow { glossarySearchQuery }
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allGlossaryTerms
            else repository.searchGlossary(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Study Hub (Flashcards & Quizzes) ---
    val flashcards: StateFlow<List<Flashcard>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val dueFlashcards: StateFlow<List<Flashcard>> = repository.getDueFlashcards(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quiz States
    var activeQuizTopic by mutableStateOf("CRISPR-Cas9 Pluripotency")
    var activeQuizDifficulty by mutableStateOf("Intermediate") // Beginner, Intermediate, Advanced
    var quizQuestions by mutableStateOf<List<AiQuestion>>(emptyList())
    var currentQuizQuestionIndex by mutableStateOf(0)
    var selectedAnswerIndex by mutableStateOf<Int?>(null)
    var isAnswerSubmitted by mutableStateOf(false)
    var correctAnswersCount by mutableStateOf(0)
    var isQuizLoading by mutableStateOf(false)
    var isQuizCompleted by mutableStateOf(false)
    val quizHistory: StateFlow<List<QuizResult>> = repository.allQuizResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Chat Assistant ---
    var chatMessages = mutableStateOf<List<Content>>(emptyList())
    var chatInput by mutableStateOf("")
    var isChatLoading by mutableStateOf(false)

    // --- State: Document Notes ---
    val documentNotes: StateFlow<List<DocumentNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Static Preloaded Academic Papers ---
    val preloadedPapers = listOf(
        ScientificPaper(
            id = "crispr",
            title = "CRISPR-Cas9 Therapeutic Applications in Genetic Disorders",
            authors = "Dr. Helen Vance, Dr. Arthur Dent, Dept of Cellular Biology",
            journal = "Pluripotent Systems & Biotechnology Review, 2026",
            sections = listOf(
                PaperSection(
                    title = "1. Introduction to Genome Editing",
                    paragraphs = listOf(
                        "The development of Programmable Genome Editing technologies has revolutionized biomedical research. Among these, the CRISPR-Cas9 system has emerged as a uniquely efficient, robust, and versatile platform for targeted genomic modifications.",
                        "CRISPR (Clustered Regularly Interspaced Short Palindromic Repeats) represents an adaptive immune system evolved in prokaryotes. It relies on RNA-guided nucleases to cleave foreign genetic elements, safeguarding organisms from bacteriophage infections."
                    )
                ),
                PaperSection(
                    title = "2. Mechanism of Double-Stranded Cleavage",
                    paragraphs = listOf(
                        "The mechanistic core of CRISPR-Cas9 involves the hybridization of a single guide RNA (sgRNA) with a complementary 20-nucleotide genomic sequence, which is immediately upstream of a Protospacer Adjacent Motif (PAM) consisting of 5'-NGG.",
                        "Upon hybridization, the Cas9 endonuclease undergoes a conformational change that triggers its dual lobe structure (REC and NUC) to execute double-strand breaks (DSBs) through its HNH and RuvC catalytic domains."
                    )
                ),
                PaperSection(
                    title = "3. Therapeutic Correction of Single-Gene Mutations",
                    paragraphs = listOf(
                        "Following DSB creation, cellular repair mechanisms dictate the editing outcome. Non-Homologous End Joining (NHEJ) often introduces insertions or deletions (indels), yielding Gene Knockout. Conversely, Homology-Directed Repair (HDR) enables precise gene insertion or correction in the presence of an exogenous donor DNA template.",
                        "Clinical trials targeting Sickle Cell Anemia (SCD) utilize these mechanisms to mutate the BCL11A erythroid enhancer, reactivating fetal hemoglobin (HbF) and effectively correcting the sickle phenotype."
                    )
                ),
                PaperSection(
                    title = "4. Future Challenges & Ethical Frameworks",
                    paragraphs = listOf(
                        "Despite monumental clinical progress, concerns regarding off-target cleavage (unintended DNA modification) persist. High-fidelity Cas9 variants, such as Cas9-HF1, have been engineered to enhance accuracy.",
                        "Furthermore, germline gene editing raises profound ethical concerns, necessitating unified regulatory policies across academic and clinical institutions worldwide."
                    )
                )
            )
        ),
        ScientificPaper(
            id = "epigenetics",
            title = "Epigenetic Regulation of Stem Cell Pluripotency",
            authors = "Prof. Alice Carter, Dr. Thomas Vance, Molecular Genetics Inst.",
            journal = "Journal of Biochemistry & Epigenetics, 2025",
            sections = listOf(
                PaperSection(
                    title = "1. Epigenetic Landscapes in Pluripotency",
                    paragraphs = listOf(
                        "The pluripotency of embryonic stem cells (ESCs) is maintained by a delicate equilibrium between core transcription factors (Oct4, Sox2, and Nanog) and dense chromatin modifications, often referred to as the Epigenetic Landscape.",
                        "Unlike somatic cells, pluripotent stem cells feature a uniquely open, highly accessible chromatin configuration characterized by rapid nucleosome exchange and abundant active transcriptional marks."
                    )
                ),
                PaperSection(
                    title = "2. DNA Methylation Dynamics",
                    paragraphs = listOf(
                        "DNA Methylation involves the covalent addition of a methyl group to the 5th carbon of cytosine residues (5mC), primarily in CpG islands. This reaction is catalyzed by DNA Methyltransferases (DNMTs).",
                        "In pluripotent ESCs, DNMT3A and DNMT3B mediate de novo methylation, while TET (Ten-Eleven Translocation) enzymes actively oxidize 5mC to 5hmC, facilitating locus-specific demethylation that permits high expression of pluripotency genes."
                    )
                ),
                PaperSection(
                    title = "3. Histone Tail Modification & Chromatin State",
                    paragraphs = listOf(
                        "Histone Acetylation (mediated by Histone Acetyltransferases, HATs) neutralizes the positive charge of lysine residues, reducing the affinity between histones and DNA, which yields active euchromatin.",
                        "In contrast, Polycomb Repressive Complexes (PRC1 and PRC2) catalyze Histone H3 Lysine 27 trimethylation (H3K27me3), forming transient transcriptionally repressed domains that control the timing of differentiation lineages."
                    )
                )
            )
        ),
        ScientificPaper(
            id = "vaccines",
            title = "mRNA Vaccines: A Molecular Revolution in Immunization",
            authors = "Dr. Marcus Thorne, Dr. Sarah Jenkins, Dept of Vaccine Design",
            journal = "Immunological Research Trends, 2026",
            sections = listOf(
                PaperSection(
                    title = "1. Evolution of Nucleic Acid Therapeutics",
                    paragraphs = listOf(
                        "Messenger RNA (mRNA) has emerged as a revolutionary nucleic acid vaccine platform. It encodes targeted immunogens, which are translated inside host cells to prompt strong adaptive immune responses without entering the host nucleus.",
                        "Unlike conventional inactivated or live-attenuated vaccines, mRNA platforms enable exceptionally rapid, cell-free synthesis via in vitro transcription (IVT), facilitating highly agile pandemic responses."
                    )
                ),
                PaperSection(
                    title = "2. Lipid Nanoparticle (LNP) Delivery Systems",
                    paragraphs = listOf(
                        "Naked mRNA molecules are highly unstable and prone to rapid degradation by extracellular ribonucleases (RNases). Thus, Lipid Nanoparticles (LNPs) are vital for delivery.",
                        "LNPs typically consist of ionizable lipids, cholesterol, helper phospholipids, and PEGylated lipids. These lipids facilitate efficient endosomal escape, releasing the mRNA directly into the host cytoplasm."
                    )
                ),
                PaperSection(
                    title = "3. Translational Kinetics & Antigen Presentation",
                    paragraphs = listOf(
                        "Once inside the cytoplasm, ribosomes translate the vaccine mRNA to synthesize the viral antigen (such as the SARS-CoV-2 spike protein).",
                        "These newly synthesized proteins are processed and displayed via Major Histocompatibility Complex (MHC) Class I and Class II pathways, activating robust CD8+ cytotoxic T cells and CD4+ helper T cells, alongside inducing neutralizing antibodies."
                    )
                )
            )
        )
    )

    val activePaper: ScientificPaper?
        get() = preloadedPapers.find { it.id == selectedPaperId }

    // --- Helper for Doc Context in AI ---
    private fun getActiveDocContext(): String {
        return if (importedPdfUri != null) {
            "PDF File: ${importedPdfName ?: "Imported PDF"}, Page ${pdfCurrentPage + 1} of $pdfPageCount"
        } else {
            val paper = activePaper
            if (paper != null) {
                "Title: ${paper.title}, Authors: ${paper.authors}, Journal: ${paper.journal}"
            } else {
                "General scientific reading context"
            }
        }
    }

    // --- Action Methods ---

    // Toggle Bookmarks for PDF Page
    fun toggleBookmark(pageIndex: Int) {
        val current = bookmarks.value
        if (current.contains(pageIndex)) {
            bookmarks.value = current - pageIndex
        } else {
            bookmarks.value = current + pageIndex
        }
    }

    // Highlighting & Saving Notes
    fun highlightSelectedText() {
        if (selectedTextForAction.isBlank()) return
        viewModelScope.launch {
            repository.saveNote(
                DocumentNote(
                    documentName = getActiveDocContext(),
                    selectedText = selectedTextForAction,
                    noteText = "Highlighted Text",
                    type = "highlight"
                )
            )
        }
    }

    fun saveCustomNote(noteText: String) {
        if (selectedTextForAction.isBlank() || noteText.isBlank()) return
        viewModelScope.launch {
            repository.saveNote(
                DocumentNote(
                    documentName = getActiveDocContext(),
                    selectedText = selectedTextForAction,
                    noteText = noteText,
                    type = "note"
                )
            )
            isSelectionPanelOpen = false
        }
    }

    fun deleteNote(note: DocumentNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // AI Translation
    fun runScientificTranslation() {
        if (selectedTextForAction.isBlank()) return
        isAiLoading = true
        translationResult = null
        explanationResult = null
        paragraphAnalysisResult = null
        viewModelScope.launch {
            translationResult = repository.translateScientificTerm(selectedTextForAction, activeModel)
            isAiLoading = false
        }
    }

    // AI Explain Concept
    fun runConceptExplanation() {
        if (selectedTextForAction.isBlank()) return
        isAiLoading = true
        translationResult = null
        explanationResult = null
        paragraphAnalysisResult = null
        selectedTabInExplainResult = 0
        viewModelScope.launch {
            explanationResult = repository.explainConcept(selectedTextForAction, activeModel)
            isAiLoading = false
        }
    }

    // AI Paragraph Analysis
    fun runParagraphAnalysis() {
        if (selectedTextForAction.isBlank()) return
        isAiLoading = true
        translationResult = null
        explanationResult = null
        paragraphAnalysisResult = null
        viewModelScope.launch {
            paragraphAnalysisResult = repository.analyzeParagraph(selectedTextForAction, activeModel)
            isAiLoading = false
        }
    }

    // Save active translation results to glossary
    fun saveTranslationToGlossary() {
        val result = translationResult ?: return
        viewModelScope.launch {
            repository.saveGlossaryTerm(
                GlossaryTerm(
                    term = result.original,
                    translation = result.persian,
                    pronunciation = result.pronunciation,
                    partOfSpeech = result.partOfSpeech,
                    scientificContext = result.scientificContext,
                    synonyms = result.synonyms,
                    exampleUsage = result.exampleUsage,
                    notes = "Added via Smart Reader translation tool"
                )
            )
        }
    }

    fun saveGlossaryTerm(term: GlossaryTerm) {
        viewModelScope.launch {
            repository.saveGlossaryTerm(term)
        }
    }

    fun deleteGlossaryTerm(term: GlossaryTerm) {
        viewModelScope.launch {
            repository.deleteGlossaryTerm(term)
        }
    }

    // Save concept explanations to flashcard
    fun generateFlashcardForConcept() {
        if (selectedTextForAction.isBlank()) return
        isAiLoading = true
        viewModelScope.launch {
            val result = repository.generateFlashcardFromConcept(selectedTextForAction, activeModel)
            repository.saveFlashcard(
                Flashcard(
                    front = result.front,
                    back = result.back
                )
            )
            isAiLoading = false
            isSelectionPanelOpen = false
        }
    }

    fun addManualFlashcard(front: String, back: String) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.saveFlashcard(Flashcard(front = front, back = back))
        }
    }

    // Flashcard Grading with SM-2 Spaced Repetition Algorithm
    fun rateFlashcard(flashcard: Flashcard, rating: String) {
        // rating can be: "hard", "good", "easy"
        val nextRepetitions: Int
        val nextInterval: Int
        val nextEaseFactor: Float

        when (rating) {
            "easy" -> {
                nextRepetitions = flashcard.repetitions + 1
                nextInterval = if (flashcard.repetitions == 0) 1 else if (flashcard.repetitions == 1) 6 else (flashcard.interval * flashcard.easeFactor).toInt()
                nextEaseFactor = flashcard.easeFactor + 0.15f
            }
            "good" -> {
                nextRepetitions = flashcard.repetitions + 1
                nextInterval = if (flashcard.repetitions == 0) 1 else if (flashcard.repetitions == 1) 4 else (flashcard.interval * flashcard.easeFactor * 0.85f).toInt()
                nextEaseFactor = flashcard.easeFactor
            }
            else -> { // hard / forgot
                nextRepetitions = 0
                nextInterval = 1
                nextEaseFactor = maxOf(1.3f, flashcard.easeFactor - 0.2f)
            }
        }

        // Days to milliseconds
        val offsetMillis = nextInterval * 24L * 60L * 60L * 1000L
        val nextReviewDate = System.currentTimeMillis() + offsetMillis

        viewModelScope.launch {
            repository.saveFlashcard(
                flashcard.copy(
                    repetitions = nextRepetitions,
                    interval = nextInterval,
                    easeFactor = nextEaseFactor,
                    nextReviewDate = nextReviewDate
                )
            )
        }
    }

    // Interactive Quiz Generation
    fun startNewQuiz(topic: String, difficulty: String) {
        activeQuizTopic = topic
        activeQuizDifficulty = difficulty
        isQuizLoading = true
        isQuizCompleted = false
        currentQuizQuestionIndex = 0
        selectedAnswerIndex = null
        isAnswerSubmitted = false
        correctAnswersCount = 0
        
        viewModelScope.launch {
            val questions = repository.generateQuizOnTopic(topic, difficulty, activeModel)
            if (questions.isNotEmpty()) {
                quizQuestions = questions
            } else {
                // Fallback offline mock questions to prevent dead-end UI
                quizQuestions = listOf(
                    AiQuestion(
                        question = "Which enzyme catalyzes the methylation of CpG islands in de novo DNA synthesis?",
                        options = listOf("DNMT3A & DNMT3B", "TET1 & TET2", "PRC1 Complex", "HAT Enzyme"),
                        correctIndex = 0,
                        explanation = "DNMT3A and DNMT3B are DNA methyltransferases that specialize in de novo DNA methylation, whereas TET enzymes oxidize methylcytosine to facilitate demethylation."
                    ),
                    AiQuestion(
                        question = "What is the critical spacing adjacent motif sequence required for Cas9 targeting?",
                        options = listOf("5'-TATA", "5'-NGG", "5'-AAG", "3'-CCA"),
                        correctIndex = 1,
                        explanation = "Cas9 relies on the Protospacer Adjacent Motif (PAM), which is strictly a 5'-NGG sequence for standard Streptococcus pyogenes Cas9."
                    )
                )
            }
            isQuizLoading = false
        }
    }

    fun submitQuizAnswer(choiceIndex: Int) {
        if (isAnswerSubmitted) return
        selectedAnswerIndex = choiceIndex
        isAnswerSubmitted = true
        if (choiceIndex == quizQuestions[currentQuizQuestionIndex].correctIndex) {
            correctAnswersCount++
        }
    }

    fun nextQuizQuestion() {
        if (currentQuizQuestionIndex < quizQuestions.size - 1) {
            currentQuizQuestionIndex++
            selectedAnswerIndex = null
            isAnswerSubmitted = false
        } else {
            isQuizCompleted = true
            // Save to DB
            viewModelScope.launch {
                repository.saveQuizResult(
                    QuizResult(
                        documentName = getActiveDocContext(),
                        score = correctAnswersCount,
                        totalQuestions = quizQuestions.size,
                        difficulty = activeQuizDifficulty
                    )
                )
            }
        }
    }

    // Chat Assistant
    fun sendChatMessage() {
        val input = chatInput.trim()
        if (input.isBlank() || isChatLoading) return

        val userPart = Part(text = input)
        val userContent = Content(parts = listOf(userPart), role = "user")
        
        chatMessages.value = chatMessages.value + userContent
        chatInput = ""
        isChatLoading = true

        viewModelScope.launch {
            val replyText = repository.chatWithAi(
                message = input,
                history = chatMessages.value,
                documentContext = getActiveDocContext(),
                model = activeModel
            )
            val assistantPart = Part(text = replyText)
            val assistantContent = Content(parts = listOf(assistantPart), role = "model")
            chatMessages.value = chatMessages.value + assistantContent
            isChatLoading = false
        }
    }

    fun clearChatHistory() {
        chatMessages.value = emptyList()
    }
}
