package uk.ac.tees.mad.easynotes.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.easynotes.data.local.DatabaseProvider
import uk.ac.tees.mad.easynotes.data.local.SubjectEntity
import uk.ac.tees.mad.easynotes.domain.model.Subject
import java.util.UUID

data class HomeUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSyncing: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = DatabaseProvider.getDatabase(application)
    private val subjectDao = database.subjectDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            subjectDao.getSubjectsFlow(userId).collect { entities ->
                val subjects = entities.map { it.toDomain() }
                _uiState.update {
                    it.copy(
                        subjects = subjects,
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
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener

                        snapshot?.documents?.let { docs ->
                            viewModelScope.launch {
                                val entities = docs.mapNotNull { doc ->
                                    SubjectEntity(
                                        id = doc.id,
                                        userId = userId,
                                        name = doc.getString("name") ?: "",
                                        color = doc.getString("color") ?: "#6200EE",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                        lastNotePreview = doc.getString("lastNotePreview") ?: "",
                                        noteCount = doc.getLong("noteCount")?.toInt() ?: 0
                                    )
                                }
                                subjectDao.insertSubjects(entities)
                            }
                        }
                    }
            } catch (e: Exception) {
            }
        }
    }

    fun createSubject(name: String, color: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val subjectId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val entity = SubjectEntity(
                    id = subjectId,
                    userId = userId,
                    name = name,
                    color = color,
                    createdAt = now,
                    updatedAt = now,
                    lastNotePreview = "",
                    noteCount = 0
                )

                subjectDao.insertSubject(entity)

                val subject = hashMapOf(
                    "name" to name,
                    "color" to color,
                    "createdAt" to now,
                    "updatedAt" to now,
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
                subjectDao.deleteSubject(subjectId)
                database.noteDao().deleteAllNotesForSubject(subjectId)

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
            syncFromFirestore()
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}