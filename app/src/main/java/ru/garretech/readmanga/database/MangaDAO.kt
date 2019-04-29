package ru.garretech.readmanga.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import ru.garretech.readmanga.models.Manga

@Dao
interface MangaDAO {


    @get:Query("SELECT * FROM manga")
    val allCachedMovies: List<Manga>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addMovie(movie: Manga): Long

    @Query("SELECT * FROM Manga WHERE URL = :URL")
    fun getMovie(URL: String): Manga

}
