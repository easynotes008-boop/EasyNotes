package uk.ac.tees.mad.easynotes.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.easynotes.domain.model.Subject
import java.util.UUID

data class HomeUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSyncing: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
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

                        val subjects = snapshot?.documents?.mapNotNull { doc ->
                            Subject(
                                id = doc.id,
                                userId = userId,
                                name = doc.getString("name") ?: "",
                                color = doc.getString("color") ?: "#6200EE",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                lastNotePreview = doc.getString("lastNotePreview") ?: "",
                                noteCount = doc.getLong("noteCount")?.toInt() ?: 0
                            )
                        }?.sortedByDescending { it.updatedAt } ?: emptyList()

                        _uiState.update {
                            it.copy(
                                subjects = subjects,
                                isLoading = false,
                                error = null
                            )
                        }
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

    fun createSubject(name: String, color: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val subjectId = UUID.randomUUID().toString()
                val subject = hashMapOf(
                    "name" to name,
                    "color" to color,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "lastNotePreview" to "",
                    "noteCount" to 0
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .set(subject)
                    .await()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .collection("notes")
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete() }

                firestore.collection("users")
                    .document(userId)
                    .collection("subjects")
                    .document(subjectId)
                    .delete()
                    .await()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}