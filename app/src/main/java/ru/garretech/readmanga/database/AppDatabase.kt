package ru.garretech.readmanga.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

import ru.garretech.readmanga.models.Favorites
import ru.garretech.readmanga.models.Manga

@Database(entities = [Manga::class, Favorites::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDAO(): MangaDAO
    abstract fun favoritesDAO(): FavoritesDAO

    companion object {

        private val DATABASE_NAME = "manga_database"
        private var INSTANCE: AppDatabase? = null


        fun getInstance(context: Context): AppDatabase? {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class.java) {

                    INSTANCE = Room.databaseBuilder<AppDatabase>(context, AppDatabase::class.java, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return INSTANCE
        }

    }
}
