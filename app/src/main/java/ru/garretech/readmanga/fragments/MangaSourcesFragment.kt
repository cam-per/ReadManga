package ru.garretech.readmanga.fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.garretech.readmanga.R
import ru.garretech.readmanga.activities.MangaReaderActivity

import java.util.ArrayList

import ru.garretech.readmanga.tools.SiteWorker


class MangaSourcesFragment : androidx.fragment.app.Fragment() {

    // TODO: Rename and change types of parameters
    private var arrayAdapter: ArrayAdapter<String>? = null
    internal lateinit var episodesList: JSONArray
    internal lateinit var listViewList: ArrayList<String>
    internal lateinit var listView: ListView
    internal lateinit var sourcesInfo: JSONObject
    internal lateinit var URL: String
    internal lateinit var progressBottomSheet: ProgressBottomSheet
    internal lateinit var lastChapter: String
    internal var empty: Boolean = false

    /*  * Переходим по ссылке http://readmanga.me/tower_of_god/vol3/6
    * Считываем количество страниц из элемента с классом pages-count
    * Берем первое фото из div с id=fotocontext. Содержимое аттрибута src из тега img
    * http://e5.mangas.rocks/auto/30/35/40/TowerOfGod_s3_ch06_p01_SIU_Gemini.jpg_res.jpg?t=1556875730&u=0&h=i1nwNAGZO2AF_mAe3BzlHQ
    * Подставляем вместо p01 номера с 1 по количество страниц. Полученный массив ссылок и будет текущий эпизоп манги
    *
    * */

    private var conMgr: ConnectivityManager? = null
    private var bag : CompositeDisposable = CompositeDisposable()
    val adapterClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->

        if (!progressBottomSheet.isAdded) {
            progressBottomSheet.show(fragmentManager!!, "progressBar")
        }
        Handler().postDelayed({
        val imageJsonArray = SiteWorker.getMangaImageList(URL + (episodesList.get(i) as JSONObject).getString("link"))
        val imageListJson = JSONArray()

        for (index in 0..(imageJsonArray.length()-1)) {
            val jsonTemp = imageJsonArray.getJSONArray(index)
            val link = jsonTemp.get(1).toString()+jsonTemp.get(2).toString()

            imageListJson.put(link)
        }

        if (progressBottomSheet.isAdded && progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()

        val intent = Intent(activity,MangaReaderActivity::class.java)
            intent.putExtra("chapterName",(episodesList.get(i) as JSONObject).getString("name"))
        intent.putExtra("imageList",imageListJson.toString())
        startActivity(intent)
        },100)
    }

    private var mListener: OnFragmentInteractionListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            try {
                progressBottomSheet = ProgressBottomSheet()
                conMgr = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                sourcesInfo = JSONObject(arguments!!.getString(ARG_PARAM1))
                URL = sourcesInfo.getString("url")
                lastChapter = sourcesInfo.getString("last_chapter")
                //episodesList = JSONArray()
                episodesList = SiteWorker.formChaptersList(URL, lastChapter)
                listViewList = ArrayList()

                if (episodesList.length() == 0) {
                    listViewList.add("Пусто")
                    empty = true
                } else {
                    for (i in 0 until episodesList.length()) {
                        listViewList.add((episodesList.get(i) as JSONObject).getString("name"))
                    }
                }

                arrayAdapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, listViewList)
                arrayAdapter!!.setNotifyOnChange(true)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_manga_sources, container, false)
        listView = view.findViewById(R.id.sourcesListView)
        listView.adapter = arrayAdapter

        if (!empty) listView.onItemClickListener = adapterClickListener

        return view
    }


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


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    internal fun showConnectionError() {
        if (progressBottomSheet.isAdded && progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()
        Toast.makeText(context, getText(R.string.cant_connect_error), Toast.LENGTH_SHORT).show()
    }

    internal fun hasConnection(): Boolean {
        //return conMgr!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).state == NetworkInfo.State.CONNECTED || conMgr!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI).state == NetworkInfo.State.CONNECTED
        return true
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "info"


        // TODO: Rename and change types and number of parameters
        fun newInstance(sourcesInfo: JSONObject): MangaSourcesFragment {
            val fragment = MangaSourcesFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, sourcesInfo.toString())
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
