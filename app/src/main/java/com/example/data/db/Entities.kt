package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glossary_terms")
data class GlossaryTerm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val term: String,
    val translation: String,
    val pronunciation: String = "",
    val partOfSpeech: String = "",
    val scientificContext: String = "",
    val synonyms: String = "",
    val exampleUsage: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val front: String,
    val back: String,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val interval: Int = 0, // days until next review
    val nextReviewDate: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_notes")
data class DocumentNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentName: String,
    val selectedText: String,
    val noteText: String,
    val type: String, // "highlight" or "note"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_results")
data class QuizResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentName: String,
    val score: Int,
    val totalQuestions: Int,
    val difficulty: String,
    val timestamp: Long = System.currentTimeMillis()
)
