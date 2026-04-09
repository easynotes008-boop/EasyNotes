package uk.ac.tees.mad.easynotes.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: EasyNotesDatabase? = null

    fun getDatabase(context: Context): EasyNotesDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                EasyNotesDatabase::class.java,
                "easynotes_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}