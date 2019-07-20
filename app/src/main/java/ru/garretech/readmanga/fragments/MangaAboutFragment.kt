package ru.garretech.readmanga.fragments

//import android.arch.persistence.room.PrimaryKey
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions


import org.json.JSONException
import org.json.JSONObject
import ru.garretech.readmanga.R
import ru.garretech.readmanga.models.Manga
import ru.garretech.readmanga.viewmodels.MangaAboutFragmentViewModel
import java.lang.StringBuilder


class MangaAboutFragment : androidx.fragment.app.Fragment() {


    private var currentManga: Manga? = null
    private lateinit var viewModel : MangaAboutFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MangaAboutFragmentViewModel::class.java)

        viewModel.currentManga = currentManga
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_manga_about, container, false)
        val mangaAgeView = view.findViewById<TextView>(R.id.movie_age_text)
        val mangaGenresView = view.findViewById<TextView>(R.id.movie_genres_text)
        val mangaProductionCountryView = view.findViewById<TextView>(R.id.movie_production_country_text)
        val mangaChaptersNumberView = view.findViewById<TextView>(R.id.movie_series_number_text)
        val mangaDurationView = view.findViewById<TextView>(R.id.movie_duration_text)
        val imageView = view.findViewById<ImageView>(R.id.movie_image_about)
        val mangaDescriptionView = view.findViewById<TextView>(R.id.movie_description_text)

        var genresString = StringBuilder()

        for (genre in viewModel.currentManga?.genres!!)
            genresString.append("$genre, ")




        mangaGenresView.text = getString(R.string.genres_description) + " " + genresString.substring(0,genresString.length - 2)
        mangaProductionCountryView.text = getString(R.string.production_country_description)  + " " + viewModel.currentManga?.productionCountry
        mangaChaptersNumberView.text = viewModel.currentManga?.chaptersNumber
        mangaDurationView.text = viewModel.currentManga?.duration
        mangaAgeView.text = getString(R.string.age_description)  + " " + viewModel.currentManga?.productionYear
        mangaDescriptionView.text = viewModel.currentManga?.description

        Glide
            .with(context!!)
            .load(viewModel.currentManga?.mangaImageURL!!)
            .fitCenter()
            .transition(DrawableTransitionOptions.withCrossFade())
            //.placeholder(R.drawable.loading_spinner)
            .into(imageView)

        return view
    }


    companion object {

        @Throws(JSONException::class)
        fun newInstance(manga: Manga) = MangaAboutFragment().also {
            it.currentManga = manga
        }
    }
}


