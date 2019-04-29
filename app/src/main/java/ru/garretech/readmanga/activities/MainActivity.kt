package ru.garretech.readmanga.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast

import com.chad.library.adapter.base.BaseQuickAdapter

import org.json.JSONException
import org.json.JSONObject


import java.io.FileNotFoundException
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ExecutionException
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.garretech.readmanga.R
import ru.garretech.readmanga.adapters.RecyclerAdapter
import ru.garretech.readmanga.database.AppDataSource
import ru.garretech.readmanga.fragments.CustomLoadMoreView
import ru.garretech.readmanga.fragments.ProgressBottomSheet
import ru.garretech.readmanga.models.Manga
import ru.garretech.readmanga.tools.ImageDownloader
import ru.garretech.readmanga.tools.SiteWorker

class MainActivity : AppCompatActivity(), BaseQuickAdapter.OnItemClickListener, MenuItem.OnActionExpandListener, NavigationView.OnNavigationItemSelectedListener, BaseQuickAdapter.RequestLoadMoreListener, androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {

    private var searchView: SearchView? = null
    private var newMangaAdapter: RecyclerAdapter? = null
    private lateinit var progressBottomSheet : ProgressBottomSheet
    private var conMgr: ConnectivityManager? = null
    private var mSiteWorker: SiteWorker? = null
    private var requestQuery: SiteWorker.RequestQuery? = null
    private var appDataSource: AppDataSource? = null

    private var observable: Observable<List<Manga>>? = null

    private val getListMangasObserver by lazy {
        object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {

            }

            override fun onComplete() {
                observable!!
                        .subscribeOn(Schedulers.io())
                        .map { movies ->
                            for (movie in movies) {
                                var image: Bitmap?
                                try {
                                    image = SiteWorker.getCachedImage(applicationContext, movie.mangaImageURL)
                                } catch (e: FileNotFoundException) {
                                    val imageDownloader = ImageDownloader()
                                    image = imageDownloader.execute(movie.mangaImageURL).get()
                                    SiteWorker.saveImage(applicationContext, image!!, movie.mangaImageURL)
                                }

                                movie.image = image
                            }
                            movies
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(updateListConsumer!!)
                //Log.d("Task", "get favorites completable completed");
            }

            override fun onError(e: Throwable) {

            }
        }
    }

    private val getOnLoadMoreObserver by lazy {
        object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {

            }

            override fun onComplete() {
                observable!!
                        .subscribeOn(Schedulers.io())
                        .map { movies ->
                            for (movie in movies) {
                                var image: Bitmap?
                                try {
                                    image = SiteWorker.getCachedImage(applicationContext, movie.mangaImageURL)
                                } catch (e: FileNotFoundException) {
                                    val imageDownloader = ImageDownloader()
                                    image = imageDownloader.execute(movie.mangaImageURL).get()
                                    SiteWorker.saveImage(applicationContext, image!!, movie.mangaImageURL)
                                }

                                movie.image = image
                            }
                            movies
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(onLoadMoreConsumer!!)
                //Log.d("Task", "get favorites completable completed");
            }

            override fun onError(e: Throwable) {

            }
        }
    }

    private val onLoadMoreConsumer by lazy {
        Consumer<List<Manga>> { movies ->
            newMangaAdapter!!.addData(movies)
            newMangaAdapter!!.loadMoreComplete()
        }
    }

    private val updateListConsumer by lazy {
        Consumer<List<Manga>> { movies ->
            updateDataList(movies)

            if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false

            if (progressBottomSheet.isAdded && progressBottomSheet.isVisible)
                progressBottomSheet.dismissAllowingStateLoss()
        }
    }

    private var mAdViewContainer: RelativeLayout? = null



    private val GENRES_CODE = 15

    private var activityState = ACTIVITY_STATE.LOST_CONNECTION

    internal enum class ACTIVITY_STATE {
        ONLINE,
        FAVORITES,
        LOST_CONNECTION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val crashlyticsKit = Crashlytics.Builder().core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build()
        //if (BuildConfig.DEBUG) Fabric.with(this, crashlyticsKit)
        setContentView(R.layout.activity_main)
        mSiteWorker = SiteWorker()
        appDataSource = AppDataSource(applicationContext)
        progressBottomSheet = ProgressBottomSheet()

        mAdViewContainer = relativeContainer


        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        conMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        navigationView.setNavigationItemSelectedListener(this)


        swipeContainer.setOnRefreshListener(this)

        setSupportActionBar(toolbarActionBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mDivider = applicationContext.getDrawable(R.drawable.line_divider)
            val mDividerItemDecoration = CustomDivider(mDivider, 10, 10)
            movieListRecyclerView!!.addItemDecoration(mDividerItemDecoration)
        }


        movieListRecyclerView!!.layoutManager = object : androidx.recyclerview.widget.LinearLayoutManager(this) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }
        }
        movieListRecyclerView!!.setHasFixedSize(true)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbarActionBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        newMangaAdapter = RecyclerAdapter(R.layout.fragment_manga, ArrayList())
        movieListRecyclerView!!.adapter = newMangaAdapter
        newMangaAdapter!!.onItemClickListener = this
        newMangaAdapter!!.setOnLoadMoreListener(this, movieListRecyclerView)
        newMangaAdapter!!.setEnableLoadMore(false)
        newMangaAdapter!!.setLoadMoreView(CustomLoadMoreView())




        if (hasConnection()) {
            activityState = ACTIVITY_STATE.ONLINE

            Completable.fromCallable {
                try {
                    requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.EDITOR_CHOICE_QUERY)
                    observable = requestQuery!!.nextQuery
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    showConnectionError()
                } catch (e: NullPointerException) {
                    showConnectionError()
                }

                null
            }.subscribeOn(Schedulers.io())
                    .subscribe(getListMangasObserver!!)

        } else
            showConnectionError()

        //refreshBannerAd()
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        val myActionMenuItem = menu.findItem(R.id.action_search)
        searchView = myActionMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryString: String): Boolean {

                // Поиск дорам
                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE

                    if (!progressBottomSheet.isAdded) {
                        progressBottomSheet.show(supportFragmentManager, "progressBar")
                    }

                    Completable.fromCallable {
                        try {
                            val params = HashMap<String, String>()
                            params["q"] = queryString

                            requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.SEARCH_QUERY, SiteWorker.SEARCH_PREFIX, params)
                            observable = requestQuery!!.nextQuery
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            showConnectionError()
                        } catch (e: NullPointerException) {
                            showConnectionError()
                        }

                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(getListMangasObserver!!)

                    title = getString(R.string.search_hint) + ": $queryString"


                    if (!searchView!!.isIconified) {
                        searchView!!.isIconified = true
                    }
                } else
                    showConnectionError()

                myActionMenuItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_editorchoice -> {
                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE

                    if (!progressBottomSheet.isAdded) {
                        progressBottomSheet.show(supportFragmentManager, "progressBar")
                    }

                    Completable.fromCallable {
                    try {
                        requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.EDITOR_CHOICE_QUERY)
                        observable = requestQuery!!.nextQuery
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        showConnectionError()
                    } catch (e: NullPointerException) {
                        showConnectionError()
                    }
                    null
                }.subscribeOn(Schedulers.io())
                        .subscribe(getListMangasObserver!!)

                    title = getString(R.string.editor_choice_title)

                } else
                    showConnectionError();
            }
            R.id.nav_new -> {

                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE


                    if (!progressBottomSheet.isAdded) {
                        progressBottomSheet.show(supportFragmentManager, "progressBar")
                    }

                    Completable.fromCallable {
                        try {
                            val params = HashMap<String, String>()
                            params[SiteWorker.NEW_MOVIES_PARAMS[0]] = SiteWorker.NEW_MOVIES_PARAMS[1]
                            requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.SIMPLE_QUERY, SiteWorker.LIST_PREFIX, params)
                            observable = requestQuery!!.nextQuery
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            showConnectionError()
                        } catch (e: NullPointerException) {
                            showConnectionError()
                        }

                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(getListMangasObserver!!)

                    title = getString(R.string.new_movie_title)

                    //newMangaAdapter!!.clear()
                } else
                    showConnectionError();
            }
            R.id.nav_best -> {

                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE

                    if (!progressBottomSheet.isAdded) {
                        progressBottomSheet.show(supportFragmentManager, "progressBar")
                    }

                    Completable.fromCallable {
                        try {
                            requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.SIMPLE_QUERY, SiteWorker.LIST_PREFIX)
                            observable = requestQuery!!.nextQuery
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            showConnectionError()
                        } catch (e: NullPointerException) {
                            showConnectionError()
                        }
                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(getListMangasObserver!!)

                    title = getString(R.string.best_movie_title)

                    //newMangaAdapter!!.clear()
                } else
                    showConnectionError();
            }

            R.id.nav_random -> {

                if (hasConnection()) {


                    if (!progressBottomSheet.isAdded) {
                        progressBottomSheet.show(supportFragmentManager, "progressBar")
                    }


                    getMangaRequestSingle(SiteWorker.RANDOM_MOVIE_PREFIX).observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe( { json ->
                                val intent = Intent(this@MainActivity, MangaAboutActivity::class.java)

                                intent.putExtra("manga_info", json.toString())

                                if (progressBottomSheet.isAdded)
                                    progressBottomSheet.dismissAllowingStateLoss()

                                startActivity(intent)
                            }, { error ->
                                Log.d("Error occured",error.localizedMessage)
                            })


                } else
                    showConnectionError();
            }

            R.id.nav_genres -> {

                if (hasConnection()) {

                    activityState = ACTIVITY_STATE.ONLINE
                    try {
                        val jsonArray = SiteWorker.genresList
                        val intent = Intent(this@MainActivity, GenresActivity::class.java)
                        intent.putExtra("genres", jsonArray.toString())
                        startActivityForResult(intent, GENRES_CODE)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        showConnectionError()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        showConnectionError()
                    }
                } else
                    showConnectionError();

            }
            R.id.nav_favourites -> {

                //List<Movie> favoritesMovies = appDataSource.getListOfFavorites();
                activityState = ACTIVITY_STATE.FAVORITES
                Completable.fromCallable {
                    observable = appDataSource!!.listOfFavorites
                    null
                }.subscribeOn(Schedulers.io())
                        .subscribe(getListMangasObserver!!)

                title = getString(R.string.action_favorite)

            }

            R.id.nav_history -> {
                Toast.makeText(applicationContext, "Функция в разработке", Toast.LENGTH_LONG).show()
            }

            R.id.nav_about -> {
                val intent = Intent(this@MainActivity, AboutApplicationActivity::class.java)
                startActivity(intent)
            }

            else -> { }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true

    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GENRES_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val resultPrefix = data!!.getStringExtra("link")
                val genreName = data.getStringExtra("name")

                progressBottomSheet = ProgressBottomSheet()


                Completable.fromCallable {
                    try {
                        requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.SIMPLE_QUERY, resultPrefix)
                        observable = requestQuery!!.nextQuery
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        showConnectionError()
                    } catch (e: NullPointerException) {
                        showConnectionError()
                    }

                    null
                }.subscribeOn(Schedulers.io())
                        .subscribe(getListMangasObserver!!)

                setTitle(genreName.substring(0, 1).toUpperCase() + genreName.substring(1))
            }
        }
    }


    public override fun onPause() {
        super.onPause()

        if (progressBottomSheet.isResumed || progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()
    }

    /** Called when returning to the activity  */
    public override fun onResume() {
        super.onResume()

        if (progressBottomSheet.isResumed || progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()

        //mAdMobView?.resume()
    }



    /** Called before the activity is destroyed  */
    public override fun onStop() {
        super.onStop()

        if (progressBottomSheet.isResumed || progressBottomSheet.isVisible)
            progressBottomSheet.dismissAllowingStateLoss()
    }


    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        val selectedManga = newMangaAdapter!!.data[position]
        if (hasConnection()) {

            val jsonObject: JSONObject
            if (!progressBottomSheet.isAdded) {
                progressBottomSheet.show(supportFragmentManager, "progressBar")
            }

            getMangaRequestSingle(selectedManga.url).observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe( { json ->
                        val intent = Intent(this@MainActivity, MangaAboutActivity::class.java)
                        selectedManga.title = json.getString("title")
                        selectedManga.initialEpisode = json.getString("initial_episode")
                        selectedManga.productionCountry = json.getString("production")
                        selectedManga.episodesNumber = json.getString("episodes_number")
                        selectedManga.duration = json.getString("duration")
                        selectedManga.description = json.getString("description")
                        selectedManga.productionYear = json.getString("age")

                        val bundle = Bundle()

                        bundle.putSerializable("manga", selectedManga)

                        intent.putExtra("bundle", bundle)
                        intent.putExtra("manga_info", json.toString())

                        if (progressBottomSheet.isAdded)
                            progressBottomSheet.dismissAllowingStateLoss()

                        startActivity(intent)
                    }, { error ->
                        Log.d("Error occured",error.localizedMessage)
                    })
        } else
            showConnectionError()

    }


    override fun onLoadMoreRequested() {
        if (activityState == ACTIVITY_STATE.ONLINE) {
            if (requestQuery != null) {

                if (requestQuery!!.offset() >= requestQuery!!.queryAmount()) {
                    newMangaAdapter!!.loadMoreComplete()
                    newMangaAdapter!!.setEnableLoadMore(false)
                } else {
                    if (hasConnection()) {

                        Completable.fromCallable {
                            try {
                                observable = requestQuery!!.nextQuery
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            } catch (e: ExecutionException) {
                                showConnectionError()
                            } catch (e: NullPointerException) {
                                showConnectionError()
                            }

                            null
                        }.subscribeOn(Schedulers.io())
                                .subscribe(getOnLoadMoreObserver!!)

                    } else {
                        //Get more data failed
                        Toast.makeText(this@MainActivity, R.string.cant_connect_error, Toast.LENGTH_LONG).show()
                        newMangaAdapter!!.loadMoreFail()

                    }
                }
            } else {
                if (newMangaAdapter!!.isLoading) newMangaAdapter!!.loadMoreComplete()
                newMangaAdapter!!.setEnableLoadMore(false)
            }
        } else {
            if (newMangaAdapter!!.isLoading) newMangaAdapter!!.loadMoreComplete()
            newMangaAdapter!!.setEnableLoadMore(false)
        }
    }


    override fun onRefresh() {

        when(activityState) {

            ACTIVITY_STATE.ONLINE,ACTIVITY_STATE.LOST_CONNECTION -> {
                if (hasConnection()) {
                    Completable.fromCallable {
                        try {
                            if (requestQuery != null)
                                requestQuery!!.resetOffset()
                            else
                                requestQuery = mSiteWorker!!.RequestQuery(applicationContext, SiteWorker.EDITOR_CHOICE_QUERY)

                            observable = requestQuery!!.nextQuery
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            showConnectionError()
                        } catch (e: NullPointerException) {
                            showConnectionError()
                        }

                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(getListMangasObserver!!)
                } else {
                    showConnectionError()
                }
            }
            ACTIVITY_STATE.FAVORITES -> {
                /*8activityState = ACTIVITY_STATE.FAVORITES
                Completable.fromCallable {
                    observable = appDataSource!!.listOfFavorites
                    null
                }.subscribeOn(Schedulers.io())
                        .subscribe(getListMangasObserver!!)*/
            }
        }
    }

    private fun updateDataList(list: List<Manga>) {

        newMangaAdapter!!.addAll(list)
        movieListRecyclerView!!.recycledViewPool.clear()

        if (newMangaAdapter!!.data.size != 0)
            movieListRecyclerView!!.scrollToPosition(0)

        if (activityState == ACTIVITY_STATE.ONLINE) {
            if (requestQuery != null && requestQuery!!.offset() < requestQuery!!.queryAmount())
                newMangaAdapter!!.setEnableLoadMore(true)
        }
    }

    internal fun showConnectionError() {
        activityState = ACTIVITY_STATE.LOST_CONNECTION
        Toast.makeText(applicationContext, getText(R.string.cant_connect_error), Toast.LENGTH_SHORT).show()
    }

    internal fun hasConnection(): Boolean {
        //return conMgr!!.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).state == NetworkInfo.State.CONNECTED || conMgr!!.getNetworkInfo(ConnectivityManager.TYPE_WIFI).state == NetworkInfo.State.CONNECTED
        return true
    }


    internal inner class CustomDivider(val mDivider: Drawable, val topOffset: Int, val bottomOffset: Int) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)

            outRect.top = topOffset
            outRect.bottom = bottomOffset
        }

    }

    fun getMangaRequestSingle(url: String) : Single<JSONObject> {
        return Single.create<JSONObject> { observer ->
            val jsonObject = SiteWorker.getMangaInfo(url)

            observer.onSuccess(jsonObject)
        }
    }
}
