package uk.ac.tees.mad.easynotes.presentation.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.easynotes.data.local.DatabaseProvider
import uk.ac.tees.mad.easynotes.data.local.NoteEntity
import uk.ac.tees.mad.easynotes.domain.model.Note
import uk.ac.tees.mad.easynotes.domain.model.Subject
import java.util.UUID

data class NotesUiState(
    val subject: Subject? = null,
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedNote: Note? = null,
    val showDeleteDialog: Boolean = false
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = DatabaseProvider.getDatabase(application)
    private val noteDao = database.noteDao()
    private val subjectDao = database.subjectDao()

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private lateinit var subjectId: String

    fun init(subjectId: String) {
        this.subjectId = subjectId
        loadSubject()
        loadNotes()
    }

    private fun loadSubject() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val entity = subjectDao.getSubjectById(subjectId)
            _uiState.update { it.copy(subject = entity?.toDomain()) }

            try {
                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .get()
                    .await()

                val subject = doc.data?.let {
                    Subject(
                        id = doc.id,
                        userId = userId,
                        name = it["name"] as? String ?: "",
                        color = it["color"] as? String ?: "#6200EE",
                        createdAt = it["createdAt"] as? Long ?: 0L,
                        updatedAt = it["updatedAt"] as? Long ?: 0L,
                        lastNotePreview = it["lastNotePreview"] as? String ?: "",
                        noteCount = (it["noteCount"] as? Long)?.toInt() ?: 0
                    )
                }

                _uiState.update { it.copy(subject = subject) }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadNotes() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            noteDao.getNotesFlow(subjectId).collect { entities ->
                val notes = entities.map { it.toDomain() }
                _uiState.update {
                    it.copy(
                        notes = notes,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }

        syncFromFirestore()
    }

    private fun syncFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener

                        snapshot?.documents?.let { docs ->
                            viewModelScope.launch {
                                val entities = docs.mapNotNull { doc ->
                                    NoteEntity(
                                        id = doc.id,
                                        subjectId = subjectId,
                                        title = doc.getString("title") ?: "",
                                        content = doc.getString("content") ?: "",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                    )
                                }
                                noteDao.insertNotes(entities)
                            }
                        }
                    }
            } catch (e: Exception) {
            }
        }
    }

    fun createNote(title: String, content: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val noteId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val entity = NoteEntity(
                    id = noteId,
                    subjectId = subjectId,
                    title = title,
                    content = content,
                    createdAt = now,
                    updatedAt = now
                )

                noteDao.insertNote(entity)

                val note = hashMapOf(
                    "title" to title,
                    "content" to content,
                    "createdAt" to now,
                    "updatedAt" to now
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .document(noteId)
                    .set(note)
                    .await()

                updateSubjectPreview()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateNote(note: Note) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
                noteDao.insertNote(NoteEntity.fromDomain(updatedNote))

                val updates = hashMapOf<String, Any>(
                    "title" to updatedNote.title,
                    "content" to updatedNote.content,
                    "updatedAt" to updatedNote.updatedAt
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .document(note.id)
                    .update(updates)
                    .await()

                updateSubjectPreview()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showDeleteDialog(note: Note) {
        _uiState.update {
            it.copy(
                selectedNote = note,
                showDeleteDialog = true
            )
        }
    }

    fun hideDeleteDialog() {
        _uiState.update {
            it.copy(
                selectedNote = null,
                showDeleteDialog = false
            )
        }
    }

    fun confirmDelete() {
        val note = _uiState.value.selectedNote ?: return
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                noteDao.deleteNote(note.id)

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .document(note.id)
                    .delete()
                    .await()

                updateSubjectPreview()
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                hideDeleteDialog()
            }
        }
    }

    private fun updateSubjectPreview() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val notes = noteDao.getNotesFlow(subjectId).first()
                val preview = notes.firstOrNull()?.content?.take(100) ?: ""
                val count = notes.size

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .update(
                        mapOf(
                            "lastNotePreview" to preview,
                            "noteCount" to count,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (e: Exception) {
            }
        }
    }
}