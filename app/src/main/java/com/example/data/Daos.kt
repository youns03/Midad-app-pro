package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentSnapshot(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDocument(): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDocument(doc: DocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Delete
    suspend fun deleteDocument(doc: DocumentEntity)
}

@Dao
interface CustomFontDao {
    @Query("SELECT * FROM custom_fonts ORDER BY dateAdded DESC")
    fun getAllCustomFonts(): Flow<List<CustomFontEntity>>

    @Query("SELECT * FROM custom_fonts ORDER BY dateAdded DESC")
    suspend fun getAllCustomFontsList(): List<CustomFontEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFont(font: CustomFontEntity)

    @Query("DELETE FROM custom_fonts WHERE id = :id")
    suspend fun deleteCustomFont(id: String)
}
