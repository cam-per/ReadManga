package ru.garretech.readmanga.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.entity.MultiItemEntity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray

import org.json.JSONException
import org.json.JSONObject
import ru.garretech.readmanga.R
import ru.garretech.readmanga.activities.MangaReaderActivity
import ru.garretech.readmanga.adapters.ExpandableItemAdapter
import ru.garretech.readmanga.interfaces.OnExpandableItemClickListener
import ru.garretech.readmanga.models.Chapter
import ru.garretech.readmanga.models.Manga

import ru.garretech.readmanga.tools.SiteWorker


class MangaSourcesFragment : androidx.fragment.app.Fragment(), OnExpandableItemClickListener {

    private lateinit var newArrayAdapter : ExpandableItemAdapter
    private lateinit var adapterList : List<MultiItemEntity>
    private lateinit var chapterJsonArray : JSONArray
    private lateinit var recyclerView: RecyclerView
    lateinit var currentManga : Manga

    private lateinit var progressBottomSheet: ProgressBottomSheet
    private lateinit var sourcesProgress : ProgressBar

    /*  * Переходим по ссылке http://readmanga.me/tower_of_god/vol3/6
    * Считываем количество страниц из элемента с классом pages-count
    * Берем первое фото из div с id=fotocontext. Содержимое аттрибута src из тега img
    * http://e5.mangas.rocks/auto/30/35/40/TowerOfGod_s3_ch06_p01_SIU_Gemini.jpg_res.jpg?t=1556875730&u=0&h=i1nwNAGZO2AF_mAe3BzlHQ
    * Подставляем вместо p01 номера с 1 по количество страниц. Полученный массив ссылок и будет текущий эпизоп манги
    *
    * */

    private var bag : CompositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressBottomSheet = ProgressBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_manga_sources, container, false)
        recyclerView = view.findViewById(R.id.sourcesRecyclerView)
        sourcesProgress  = view.findViewById(R.id.sourcesProgress)

        showProgressBar()


        bag.add(SiteWorker.formChaptersList(currentManga.url, currentManga.lastChapter!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( { map ->
                adapterList = map["adapterList"] as List<MultiItemEntity>
                chapterJsonArray = map["chapterJsonArray"] as JSONArray

                newArrayAdapter = ExpandableItemAdapter(adapterList)
                newArrayAdapter.onExpandableItemClickListener = this
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = newArrayAdapter
                dismissProgressBar()
            },{
                Log.e("Chapter observer","Error getting chapter list",it)
            }))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mDivider = context?.getDrawable(R.drawable.line_divider)
            val mDividerItemDecoration = CustomDivider(mDivider!!, 10, 10)
            recyclerView.addItemDecoration(mDividerItemDecoration)
        }

        return view
    }

    internal fun showConnectionError() {
        if (progressBottomSheet.isAdded && progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()
        Toast.makeText(context, getText(R.string.cant_connect_error), Toast.LENGTH_SHORT).show()
    }

    internal fun hasConnection(): Boolean {
        val cm = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = cm.activeNetworkInfo
        return ni != null && ni.isConnected
    }

    override fun onChapterClick(item: Chapter) {

        /*
        *  jsonObject.put("chapterName", element1.text())
            jsonObject.put("chapterNumber", chapterNumber)
            jsonObject.put("volumeNumber", currentVolumeNumber)
            jsonObject.put("link", link)
        *
        * */

        val intent = Intent(activity,MangaReaderActivity::class.java)
        intent.putExtra("selectedChapterIndex",item.chapterNumber)
        intent.putExtra("chapterArray",chapterJsonArray.toString())
        intent.putExtra("mangaURL",currentManga.url)

        startActivity(intent)

    }

    fun showProgressBar() {
        sourcesProgress.visibility = View.VISIBLE
    }

    fun dismissProgressBar() {
        sourcesProgress.visibility = View.GONE
    }

    companion object {

        fun newInstance(manga: Manga) = MangaSourcesFragment().also {
            it.currentManga = manga
        }
    }



    internal inner class CustomDivider(val mDivider: Drawable, val topOffset: Int, val bottomOffset: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)

            outRect.top = topOffset
            outRect.bottom = bottomOffset
        }
    }
}// Required empty public constructor
