package uk.ac.tees.mad.easynotes.domain.model

data class Subject(
    val id: String,
    val userId: String,
    val name: String,
    val color: String = "#6200EE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastNotePreview: String = "",
    val noteCount: Int = 0
)