package uk.ac.tees.mad.easynotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SubjectEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EasyNotesDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun noteDao(): NoteDao
}