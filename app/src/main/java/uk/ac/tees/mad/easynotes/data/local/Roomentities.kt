package uk.ac.tees.mad.easynotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import uk.ac.tees.mad.easynotes.domain.model.Subject
import uk.ac.tees.mad.easynotes.domain.model.Note

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastNotePreview: String,
    val noteCount: Int
) {
    fun toDomain(): Subject {
        return Subject(
            id = id,
            userId = userId,
            name = name,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastNotePreview = lastNotePreview,
            noteCount = noteCount
        )
    }

    companion object {
        fun fromDomain(subject: Subject): SubjectEntity {
            return SubjectEntity(
                id = subject.id,
                userId = subject.userId,
                name = subject.name,
                color = subject.color,
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt,
                lastNotePreview = subject.lastNotePreview,
                noteCount = subject.noteCount
            )
        }
    }
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            subjectId = subjectId,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                subjectId = note.subjectId,
                title = note.title,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt
            )
        }
    }
}