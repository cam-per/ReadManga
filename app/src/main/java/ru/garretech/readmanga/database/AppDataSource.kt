package ru.garretech.readmanga.database

import android.content.Context
import android.util.Log

import java.util.ArrayList

import io.reactivex.Observable
import ru.garretech.readmanga.models.Favorites
import ru.garretech.readmanga.models.Manga

class AppDataSource(context: Context) {
    private val appDatabase : AppDatabase by lazy { AppDatabase.getInstance(context)!! }
    private val mangaDAO: MangaDAO by lazy {
        appDatabase.movieDAO()
    }
    private val favoritesDAO: FavoritesDAO by lazy {
        appDatabase.favoritesDAO()
    }

    val allMovies: List<Manga>
        get() = mangaDAO.allCachedMovies

    val listOfFavorites: Observable<List<Manga>>
        get() {
            val list = ArrayList<Manga>()

            val favorites = favoritesDAO.allFavorites

            for (favorite in favorites) {
                val movie = mangaDAO.getMovie(favorite.mangaURL)
                list.add(movie)
            }
            return Observable.fromArray(list)
        }

    init {

    }

    fun addMovie(manga: Manga) {
        mangaDAO.addMovie(manga)
    }

    fun getMovie(URL: String): Manga {
        return mangaDAO.getMovie(URL)
    }

    fun isFavorite(URL: String): Boolean {
        val favorites = favoritesDAO.getFavoriteByURL(URL)
        return favorites != null
    }

    fun addFavorites(manga: Manga) {
        mangaDAO.addMovie(manga)
        Log.d("Database", "Manga " + manga.url + " added")
        val favorite = Favorites()
        favorite.mangaURL = manga.url
        favoritesDAO.addFavorites(favorite)
    }


    fun deleteFavorites(manga: Manga) {
        val favorites = favoritesDAO.getFavoriteByURL(manga.url)
        favoritesDAO.deleteFavorites(favorites)
    }

}
