package uk.ac.tees.mad.easynotes.domain.model

data class Note(
    val id: String,
    val subjectId: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)