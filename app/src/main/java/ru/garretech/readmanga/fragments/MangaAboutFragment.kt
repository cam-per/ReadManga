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


import org.json.JSONException
import org.json.JSONObject
import ru.garretech.readmanga.R
import ru.garretech.readmanga.tools.ImageDownloader
import ru.garretech.readmanga.tools.SiteWorker

import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutionException



class MangaAboutFragment : androidx.fragment.app.Fragment() {


    private val mangaAge: String by lazy { arguments!!.getString(ARG_PARAM6)}
    private val mangaTitle: String by lazy { arguments!!.getString(ARG_PARAM1) }
    private val mangaGenres: String by lazy { arguments!!.getString(ARG_PARAM2) }
    private val mangaProduction: String by lazy { arguments!!.getString(ARG_PARAM3) }
    private val mangaSeriesNumber: String by lazy { arguments!!.getString(ARG_PARAM4) }
    private val mangaDuration: String by lazy { arguments!!.getString(ARG_PARAM5) }
    private val mangaDescription: String by lazy { arguments!!.getString(ARG_PARAM7) }
    private val mangaImageURL: String by lazy { arguments!!.getString(ARG_PARAM8) }
    private val mangaURL: String by lazy { arguments!!.getString(ARG_PARAM9) }
    private var image: Bitmap? = null


    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            /*mangaTitle = arguments!!.getString(ARG_PARAM1)
            mangaGenres = arguments!!.getString(ARG_PARAM2)
            mangaProduction = arguments!!.getString(ARG_PARAM3)
            mangaSeriesNumber = arguments!!.getString(ARG_PARAM4)
            mangaDuration = arguments!!.getString(ARG_PARAM5)
            //mangaAge = arguments!!.getString(ARG_PARAM6)
            mangaDescription = arguments!!.getString(ARG_PARAM7)
            mangaImageURL = arguments!!.getString(ARG_PARAM8)
            mangaURL = arguments!!.getString(ARG_PARAM9)*/

            try {
                image = SiteWorker.getCachedImage(context!!, mangaImageURL!!)
            } catch (e: FileNotFoundException) {
                val imageDownloader = ImageDownloader()
                try {
                    image = imageDownloader.execute(mangaImageURL).get()
                } catch (e1: ExecutionException) {
                    e1.printStackTrace()
                } catch (e1: InterruptedException) {
                    e1.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
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

        mangaGenresView.text = getString(R.string.genres_description) + " " + mangaGenres!!
        mangaProductionCountryView.text = getString(R.string.production_country_description)  + " " +  mangaProduction!!
        mangaChaptersNumberView.text = mangaSeriesNumber
        mangaDurationView.text = mangaDuration
        mangaAgeView.text = getString(R.string.age_description)  + " " +  mangaAge!!
        mangaDescriptionView.text = mangaDescription
        imageView.setImageBitmap(image)

        return view
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "title"
        private val ARG_PARAM2 = "genres"
        private val ARG_PARAM3 = "production"
        private val ARG_PARAM4 = "chaptes_number"
        private val ARG_PARAM5 = "duration"
        private val ARG_PARAM6 = "age"
        private val ARG_PARAM7 = "description"
        private val ARG_PARAM8 = "imageURL"
        private val ARG_PARAM9 = "url"


        private val AGE = "Год: "
        private val GENRES = "Жанры: "
        private val PRODUCTION_COUNTRY = "Производство: "


        // TODO: Rename and change types and number of parameters
        @Throws(JSONException::class)
        fun newInstance(movieInfo: JSONObject): MangaAboutFragment {
            val fragment = MangaAboutFragment()
            val args = Bundle()

            args.putString(ARG_PARAM1, movieInfo.getString("title"))
            args.putString(ARG_PARAM2, movieInfo.getString("genres"))
            args.putString(ARG_PARAM3, movieInfo.getString("production"))
            args.putString(ARG_PARAM4, movieInfo.getString("chapters_number"))
            args.putString(ARG_PARAM5, movieInfo.getString("duration"))
            args.putString(ARG_PARAM6, movieInfo.getString("age"))
            args.putString(ARG_PARAM7, movieInfo.getString("description"))
            args.putString(ARG_PARAM8, movieInfo.getString("image_url"))
            args.putString(ARG_PARAM9, movieInfo.getString("url"))

            fragment.arguments = args

            return fragment
        }
    }
}// Required empty public constructor


