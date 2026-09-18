package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val fontId: String,
    val fontSizePt: Float,
    val textColorLong: Long,
    val pageColorLong: Long,
    val textAlign: String,
    val marginTopMm: Float,
    val marginBottomMm: Float,
    val marginRightMm: Float,
    val marginLeftMm: Float,
    val marginUnit: String,
    val paperSize: String,
    val kashidaEnabled: Boolean,
    val kashidaLevel: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_fonts")
data class CustomFontEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filePath: String,
    val dateAdded: Long = System.currentTimeMillis()
)
