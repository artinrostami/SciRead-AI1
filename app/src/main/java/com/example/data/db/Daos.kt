package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {
    @Query("SELECT * FROM glossary_terms ORDER BY term ASC")
    fun getAllTerms(): Flow<List<GlossaryTerm>>

    @Query("SELECT * FROM glossary_terms WHERE term LIKE :query OR translation LIKE :query ORDER BY term ASC")
    fun searchTerms(query: String): Flow<List<GlossaryTerm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerm(term: GlossaryTerm): Long

    @Delete
    suspend fun deleteTerm(term: GlossaryTerm)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentDate ORDER BY nextReviewDate ASC")
    fun getDueFlashcards(currentDate: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM document_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<DocumentNote>>

    @Query("SELECT * FROM document_notes WHERE documentName = :documentName ORDER BY timestamp DESC")
    fun getNotesForDocument(documentName: String): Flow<List<DocumentNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: DocumentNote): Long

    @Delete
    suspend fun deleteNote(note: DocumentNote)
}

@Dao
interface QuizResultDao {
    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<QuizResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: QuizResult): Long
}
