package uk.ac.tees.mad.easynotes.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

class NotesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadNotes() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message
                                )
                            }
                            return@addSnapshotListener
                        }

                        val notes = snapshot?.documents?.mapNotNull { doc ->
                            Note(
                                id = doc.id,
                                subjectId = subjectId,
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        }?.sortedByDescending { it.updatedAt } ?: emptyList()

                        _uiState.update {
                            it.copy(
                                notes = notes,
                                isLoading = false,
                                error = null
                            )
                        }

                        updateSubjectPreview(notes)
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun createNote(title: String, content: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val noteId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

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

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateNote(note: Note) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "title" to note.title,
                    "content" to note.content,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .document(note.id)
                    .update(updates)
                    .await()

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
                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .document(note.id)
                    .delete()
                    .await()

                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                hideDeleteDialog()
            }
        }
    }

    private fun updateSubjectPreview(notes: List<Note>) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
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