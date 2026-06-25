package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.db.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// --- Domain Models for AI Responses ---

data class TranslationResult(
    val original: String,
    val persian: String,
    val pronunciation: String = "",
    val partOfSpeech: String = "",
    val scientificContext: String = "",
    val synonyms: String = "",
    val exampleUsage: String = ""
)

data class ExplainResult(
    val simple: String,
    val scientific: String,
    val advanced: String,
    val relatedConcepts: List<String> = emptyList(),
    val commonApplications: List<String> = emptyList()
)

data class ParagraphAnalysisResult(
    val summary: String,
    val simplified: String,
    val translation: String,
    val keyConcepts: List<String> = emptyList(),
    val studyNotes: String = "",
    val examQuestions: List<AiQuestion> = emptyList()
)

data class AiQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class SciReadRepository(
    private val glossaryDao: GlossaryDao,
    private val flashcardDao: FlashcardDao,
    private val noteDao: NoteDao,
    private val quizResultDao: QuizResultDao
) {
    private val apiService = RetrofitClient.service
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // --- DB Flow Accessors ---
    val allGlossaryTerms: Flow<List<GlossaryTerm>> = glossaryDao.getAllTerms()
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    val allNotes: Flow<List<DocumentNote>> = noteDao.getAllNotes()
    val allQuizResults: Flow<List<QuizResult>> = quizResultDao.getAllResults()

    fun searchGlossary(query: String): Flow<List<GlossaryTerm>> = glossaryDao.searchTerms("%$query%")
    fun getNotesForDocument(docName: String): Flow<List<DocumentNote>> = noteDao.getNotesForDocument(docName)
    fun getDueFlashcards(currentDate: Long): Flow<List<Flashcard>> = flashcardDao.getDueFlashcards(currentDate)

    // --- DB Mutators ---
    suspend fun saveGlossaryTerm(term: GlossaryTerm) = withContext(Dispatchers.IO) {
        glossaryDao.insertTerm(term)
    }

    suspend fun deleteGlossaryTerm(term: GlossaryTerm) = withContext(Dispatchers.IO) {
        glossaryDao.deleteTerm(term)
    }

    suspend fun saveFlashcard(flashcard: Flashcard) = withContext(Dispatchers.IO) {
        flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcard(flashcard)
    }

    suspend fun saveNote(note: DocumentNote) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(note: DocumentNote) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun saveQuizResult(result: QuizResult) = withContext(Dispatchers.IO) {
        quizResultDao.insertResult(result)
    }

    // --- Gemini AI Scientific Engines ---

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            // Fallback for empty development environment checks
            ""
        } else {
            key
        }
    }

    suspend fun translateScientificTerm(
        term: String,
        model: String = "gemini-3.5-flash"
    ): TranslationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext TranslationResult(
                original = term,
                persian = "کی برای هوش مصنوعی تنظیم نشده است",
                pronunciation = "/no key/",
                partOfSpeech = "Noun",
                scientificContext = "Please set your GEMINI_API_KEY in the AI Studio Secrets panel.",
                synonyms = "N/A",
                exampleUsage = "Missing API key in workspace configuration."
            )
        }

        val prompt = """
            You are an expert scientific translation engine and biology/medical tutor.
            Analyze the following scientific term or phrase: "$term"
            Provide an accurate Persian translation, phonetic IPA pronunciation, Part of Speech, its Scientific Context in Biology/Science, synonyms, and a real scientific example.
            
            Format your response STRICTLY as a single JSON object with these keys:
            "original": original text
            "persian": accurate Persian translation
            "pronunciation": phonetic IPA pronunciation
            "partOfSpeech": noun/verb/adjective etc.
            "scientificContext": its meaning within biology, biotech or medical science
            "synonyms": comma-separated scientific alternatives
            "exampleUsage": real scientific example in English
            
            Do not include any markdown formatting or prefix like ```json outside of the pure JSON content.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val json = JSONObject(cleanJson)
            TranslationResult(
                original = json.optString("original", term),
                persian = json.optString("persian", "نامشخص"),
                pronunciation = json.optString("pronunciation", ""),
                partOfSpeech = json.optString("partOfSpeech", ""),
                scientificContext = json.optString("scientificContext", ""),
                synonyms = json.optString("synonyms", ""),
                exampleUsage = json.optString("exampleUsage", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            TranslationResult(
                original = term,
                persian = "ترجمه ناموفق بود",
                scientificContext = "Error details: ${e.localizedMessage}. Please verify internet access and API configuration."
            )
        }
    }

    suspend fun explainConcept(
        concept: String,
        model: String = "gemini-3.5-flash"
    ): ExplainResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ExplainResult(
                simple = "API Key not configured. Please add GEMINI_API_KEY.",
                scientific = "Missing key.",
                advanced = "Missing key."
            )
        }

        val prompt = """
            You are a master academic scientific tutor. 
            Explain this scientific concept: "$concept"
            Provide three distinct levels of explanations:
            1. Simple explanation: For beginners, clear analogies, plain English.
            2. Scientific explanation: For university-level readers, standard technical terms.
            3. Advanced explanation: For researchers, detailed biochemical/mechanistic pathways.
            Also, provide related scientific concepts and common research/industrial applications.
            
            Format your response STRICTLY as a single JSON object with these keys:
            "simple": simple explanation
            "scientific": scientific explanation
            "advanced": advanced explanation
            "relatedConcepts": array of strings (3 connected terms)
            "commonApplications": array of strings (2 applications)
            
            Do not include any markdown formatting outside of the pure JSON content.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val json = JSONObject(cleanJson)
            
            val relatedArr = json.optJSONArray("relatedConcepts")
            val relatedList = mutableListOf<String>()
            if (relatedArr != null) {
                for (i in 0 until relatedArr.length()) {
                    relatedList.add(relatedArr.getString(i))
                }
            }
            
            val appsArr = json.optJSONArray("commonApplications")
            val appsList = mutableListOf<String>()
            if (appsArr != null) {
                for (i in 0 until appsArr.length()) {
                    appsList.add(appsArr.getString(i))
                }
            }

            ExplainResult(
                simple = json.optString("simple", "N/A"),
                scientific = json.optString("scientific", "N/A"),
                advanced = json.optString("advanced", "N/A"),
                relatedConcepts = relatedList,
                commonApplications = appsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExplainResult(
                simple = "Failed to fetch. Error: ${e.localizedMessage}",
                scientific = "Failed to fetch. Error: ${e.localizedMessage}",
                advanced = "Failed to fetch. Error: ${e.localizedMessage}"
            )
        }
    }

    suspend fun analyzeParagraph(
        paragraph: String,
        model: String = "gemini-3.5-flash"
    ): ParagraphAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext ParagraphAnalysisResult(
                summary = "API Key not configured.",
                simplified = "Missing key.",
                translation = "Missing key."
            )
        }

        val prompt = """
            You are an advanced academic scientific assistant.
            Analyze this paragraph from a scientific paper: "$paragraph"
            Perform these tasks:
            1. Summarize: Generate a concise summary (1-2 sentences).
            2. Simplify: Rewrite in easier, clear English.
            3. Translate: Translate the core content to Persian with academic precision.
            4. Key Concepts: Extract 3-4 important scientific terms.
            5. Study Notes: Generate structured study notes (bullet points).
            6. Exam Mode: Generate 2 potential exam questions (multiple choice) based on the content, each with options, correctIndex (0-3), and explanation.
            
            Format your response STRICTLY as a single JSON object with these keys:
            "summary": concise summary string
            "simplified": simplified rewrite string
            "translation": Persian translation string
            "keyConcepts": array of strings
            "studyNotes": bullet points string
            "examQuestions": array of objects, where each object has:
                "question": question string
                "options": array of 4 strings
                "correctIndex": integer (0 to 3)
                "explanation": explanation of why it is correct
                
            Do not include any markdown formatting outside of the JSON content.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val json = JSONObject(cleanJson)
            
            val conceptsArr = json.optJSONArray("keyConcepts")
            val conceptsList = mutableListOf<String>()
            if (conceptsArr != null) {
                for (i in 0 until conceptsArr.length()) {
                    conceptsList.add(conceptsArr.getString(i))
                }
            }
            
            val questionsArr = json.optJSONArray("examQuestions")
            val questionsList = mutableListOf<AiQuestion>()
            if (questionsArr != null) {
                for (i in 0 until questionsArr.length()) {
                    val qObj = questionsArr.getJSONObject(i)
                    val optsArr = qObj.getJSONArray("options")
                    val opts = List(optsArr.length()) { optsArr.getString(it) }
                    questionsList.add(
                        AiQuestion(
                            question = qObj.getString("question"),
                            options = opts,
                            correctIndex = qObj.getInt("correctIndex"),
                            explanation = qObj.optString("explanation", "")
                        )
                    )
                }
            }

            ParagraphAnalysisResult(
                summary = json.optString("summary", ""),
                simplified = json.optString("simplified", ""),
                translation = json.optString("translation", ""),
                keyConcepts = conceptsList,
                studyNotes = json.optString("studyNotes", ""),
                examQuestions = questionsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ParagraphAnalysisResult(
                summary = "Could not analyze. Error: ${e.localizedMessage}",
                simplified = "",
                translation = ""
            )
        }
    }

    suspend fun chatWithAi(
        message: String,
        history: List<Content>,
        documentContext: String,
        model: String = "gemini-3.5-flash"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "API Key not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val systemInstruction = """
            You are SciRead AI, an advanced, highly specialized scientific reading assistant and academic tutor.
            The user is reading the following document: 
            === DOCUMENT DETAILS ===
            $documentContext
            ========================
            You maintain document context and help explain terms, sections, experiments, statistical models, or compare concepts.
            Keep explanations scientifically precise, clear, and highly professional.
            You can explain in English and include Persian translations/terms where helpful, especially when asked.
        """.trimIndent()

        val apiHistory = history.map { Content(parts = it.parts, role = it.role) }
        val newContent = Content(parts = listOf(Part(text = message)), role = "user")
        val fullContents = apiHistory + newContent

        val request = GenerateContentRequest(
            contents = fullContents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error communicating with tutor: ${e.localizedMessage}"
        }
    }

    suspend fun generateFlashcardFromConcept(
        concept: String,
        model: String = "gemini-3.5-flash"
    ): FlashcardResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext FlashcardResult("Missing API Key", "Please set GEMINI_API_KEY.")
        }

        val prompt = """
            Generate a high-yield study flashcard for this scientific concept: "$concept".
            Provide a clear, conceptual, biology or medical exam question for the Front, and a complete, comprehensive, clear Answer on the Back.
            
            Format your response STRICTLY as a single JSON object with these keys:
            "front": front question
            "back": back answer
            
            Do not include any markdown formatting outside of the pure JSON content.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val json = JSONObject(cleanJson)
            FlashcardResult(
                front = json.optString("front", "Question about $concept"),
                back = json.optString("back", "Detailed scientific definition of $concept")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FlashcardResult("What is $concept?", "Error: ${e.localizedMessage}")
        }
    }

    suspend fun generateQuizOnTopic(
        topic: String,
        difficulty: String, // Beginner, Intermediate, Advanced
        model: String = "gemini-3.5-flash"
    ): List<AiQuestion> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext emptyList()
        }

        val prompt = """
            You are a university biology and medical sciences examiner.
            Generate an interactive quiz on the topic: "$topic"
            Difficulty level: $difficulty
            Generate exactly 5 distinct multiple choice questions.
            Each question must have 4 options, a correctIndex (0 to 3), and a clear, detailed, educational explanation.
            
            Format your response STRICTLY as a single JSON object with an array key "questions":
            {
               "questions": [
                  {
                     "question": "Question text here...",
                     "options": ["Choice A", "Choice B", "Choice C", "Choice D"],
                     "correctIndex": 0,
                     "explanation": "Explanation here..."
                  },
                  ...
               ]
            }
            
            Do not include any markdown formatting outside of the pure JSON content.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = cleanJsonString(jsonText)
            val json = JSONObject(cleanJson)
            val questionsArr = json.getJSONArray("questions")
            val questionsList = mutableListOf<AiQuestion>()
            for (i in 0 until questionsArr.length()) {
                val qObj = questionsArr.getJSONObject(i)
                val optsArr = qObj.getJSONArray("options")
                val opts = List(optsArr.length()) { optsArr.getString(it) }
                questionsList.add(
                    AiQuestion(
                        question = qObj.getString("question"),
                        options = opts,
                        correctIndex = qObj.getInt("correctIndex"),
                        explanation = qObj.optString("explanation", "")
                    )
                )
            }
            questionsList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}

data class FlashcardResult(
    val front: String,
    val back: String
)
