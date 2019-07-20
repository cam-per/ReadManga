package ru.garretech.readmanga.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.chad.library.adapter.base.BaseQuickAdapter

import org.json.JSONException
import org.json.JSONObject


import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ExecutionException
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONArray
import ru.garretech.readmanga.DisposableManager
import ru.garretech.readmanga.R
import ru.garretech.readmanga.Settings
import ru.garretech.readmanga.adapters.RecyclerAdapter
import ru.garretech.readmanga.database.AppDataSource
import ru.garretech.readmanga.fragments.CustomLoadMoreView
import ru.garretech.readmanga.fragments.DisclaimerFragment
import ru.garretech.readmanga.fragments.ProgressBottomSheet
import ru.garretech.readmanga.fragments.SortingFragment
import ru.garretech.readmanga.models.Manga
import ru.garretech.readmanga.tools.SiteWorker
import ru.garretech.readmanga.viewmodels.MainActivityViewModel

class MainActivity : AppCompatActivity(), BaseQuickAdapter.OnItemClickListener, MenuItem.OnActionExpandListener, NavigationView.OnNavigationItemSelectedListener, BaseQuickAdapter.RequestLoadMoreListener, SwipeRefreshLayout.OnRefreshListener, SortingFragment.OnFragmentInteractionListener {

    private lateinit var searchView: SearchView
    private var mangaAdapter: RecyclerAdapter? = null
    private var mSiteWorker: SiteWorker = SiteWorker()
    private var requestQuery: SiteWorker.RequestQuery? = null
    private lateinit var appDataSource: AppDataSource
    private lateinit var mAdViewContainer: RelativeLayout
    private lateinit var menu: Menu
    private val sortingMenuItem by lazy { menu.findItem(R.id.action_sort) }
    private var bag : CompositeDisposable = CompositeDisposable()
    private lateinit var viewModel: MainActivityViewModel


    private var activityState = ACTIVITY_STATE.LOST_CONNECTION

    val getMangaListObserver by lazy {
        object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {}

            override fun onComplete() {
                DisposableManager.add(viewModel.observable!!
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(updateListConsumer))
            }
            override fun onError(e: Throwable) {
                Log.e("List observer", "Failed to get manga list",e)
            }
        }
    }


    val getOnLoadMoreObserver by lazy {
        object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {}

            override fun onComplete() {
                DisposableManager.add(viewModel.observable!!
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onLoadMoreConsumer))
            }
            override fun onError(e: Throwable) {
                Log.e("ONLOAD MORE OBSERVER", "Failed to perform on load more request",e)
            }
        }
    }


    private val onLoadMoreConsumer by lazy {
        Consumer<List<Manga>> { movies ->
            mangaAdapter!!.addData(movies)
            mangaAdapter!!.loadMoreComplete()
        }
    }

    private val updateListConsumer by lazy {
        Consumer<List<Manga>> { movies ->
            updateDataList(movies)

            if (swipeContainer.isRefreshing) swipeContainer.isRefreshing = false

            dismissProgressBar()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        viewModel.progressBottomSheet = ProgressBottomSheet()
        appDataSource = AppDataSource(applicationContext)
        mAdViewContainer = relativeContainer

        navigationView.setNavigationItemSelectedListener(this)
        swipeContainer.setOnRefreshListener(this)

        setSupportActionBar(toolbarActionBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu)


        val metrics = resources.displayMetrics
        var spanCount = (metrics.widthPixels / (115 * metrics.scaledDensity)).toInt()
        Settings.max_loaded_in_screen = spanCount * 8

        movieListRecyclerView!!.layoutManager = GridLayoutManager(this,spanCount)
        movieListRecyclerView!!.setHasFixedSize(true)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbarActionBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        mangaAdapter = RecyclerAdapter(R.layout.cardview_manga, ArrayList())
        movieListRecyclerView!!.adapter = mangaAdapter
        mangaAdapter!!.onItemClickListener = this
        mangaAdapter!!.setOnLoadMoreListener(this, movieListRecyclerView)
        mangaAdapter!!.setEnableLoadMore(false)
        mangaAdapter!!.setLoadMoreView(CustomLoadMoreView())

        firstStartDisclaimer()

        if (hasConnection()) {
            activityState = ACTIVITY_STATE.ONLINE



            getRequestQueryCompletable(SiteWorker.EDITOR_CHOICE_QUERY)
                .subscribeOn(Schedulers.io())
                .subscribe(getMangaListObserver)
        } else showConnectionError()

        //refreshBannerAd()
    }

    fun showProgressBar() {
        if (!viewModel.progressBottomSheet.isAdded) {
            viewModel.progressBottomSheet.show(supportFragmentManager, "progressBar")
        }
    }

    fun dismissProgressBar() {
        if (viewModel.progressBottomSheet.isAdded)
            viewModel.progressBottomSheet.dismissAllowingStateLoss()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        this.menu = menu
        val myActionMenuItem = menu.findItem(R.id.action_search)
        searchView = myActionMenuItem.actionView as SearchView

        dismissSortingMenu()

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryString: String): Boolean {

                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE

                    showProgressBar()

                    val params = HashMap<String, String>()
                    params["q"] = queryString

                    getRequestQueryCompletable(SiteWorker.SEARCH_QUERY, SiteWorker.SEARCH_PREFIX, params)
                        .subscribeOn(Schedulers.io())
                        .subscribe(getMangaListObserver)

                    title = getString(R.string.search_hint) + ": $queryString"

                    if (!searchView.isIconified) {
                        searchView.isIconified = true
                    }
                } else showConnectionError()

                myActionMenuItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean { return false }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_sort -> {
                if (requestQuery!!.requestUri() != null) {

                    showProgressBar()

                    bag.add(Single.create<JSONArray> { observer ->
                        val jsonArray  = SiteWorker.getSortingParams(requestQuery?.requestUri()?.build()!!)

                        observer.onSuccess(jsonArray)
                    }.observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe( { jsonArray ->

                            if (jsonArray.length() != 0) {
                                val sortingFragment = SortingFragment.newInstance(jsonArray,requestQuery?.requestUri()?.toString()!!)
                                sortingFragment.show(supportFragmentManager, "sortingFragment")
                            }
                            else
                                Toast.makeText(applicationContext,"Не удалось загрузить список сортиовок, попробуйте еще раз",Toast.LENGTH_SHORT).show()

                            dismissProgressBar()
                        }, { Log.e("SORTING OBSERVER","Не удалось загрузить список сотрировок",it) }))
                }
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

                    showProgressBar()
                    dismissSortingMenu()

                    getRequestQueryCompletable(SiteWorker.EDITOR_CHOICE_QUERY)
                        .subscribeOn(Schedulers.io())
                        .subscribe(getMangaListObserver)

                    title = getString(R.string.editor_choice_title)

                } else showConnectionError()
            }
            R.id.nav_list -> {

                if (hasConnection()) {
                    activityState = ACTIVITY_STATE.ONLINE

                    showProgressBar()
                    showSortingMenu()

                    getRequestQueryCompletable(SiteWorker.SIMPLE_QUERY, SiteWorker.LIST_PREFIX)
                        .subscribeOn(Schedulers.io())
                        .subscribe(getMangaListObserver)

                    title = getString(R.string.list_manga_title)

                } else showConnectionError()
            }

            R.id.nav_random -> {

                if (hasConnection()) {

                    val intent = Intent(this@MainActivity, MangaInfoActivity::class.java)

                    intent.putExtra("is_random",true)

                    startActivity(intent)

                } else showConnectionError()
            }

            R.id.nav_genres -> {

                if (hasConnection()) {

                    activityState = ACTIVITY_STATE.ONLINE
                    try {
                        val jsonArray = SiteWorker.genresList

                        if (jsonArray.length() != 0) {
                            val intent = Intent(this@MainActivity, GenresActivity::class.java)
                            intent.putExtra("genres", jsonArray.toString())
                            startActivityForResult(intent, GENRES_CODE)
                        } else
                            Toast.makeText(this,"Ошибка при получении списка жанров, повторите попытку еще раз", Toast.LENGTH_SHORT).show()

                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: ExecutionException) {
                        showConnectionError()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        showConnectionError()
                    }
                } else showConnectionError()
            }
            R.id.nav_favourites -> {

                //List<Movie> favoritesMovies = appDataSource.getListOfFavorites();
                activityState = ACTIVITY_STATE.FAVORITES
                Completable.fromCallable {
                    viewModel.observable = appDataSource.listOfFavorites
                    null
                }.subscribeOn(Schedulers.io())
                        .subscribe(getMangaListObserver)

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
        } else { super.onBackPressed() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GENRES_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val resultPrefix = data!!.getStringExtra("link")
                val genreName = data.getStringExtra("name")

                showProgressBar()

                showSortingMenu()
                mangaAdapter?.clear()

                getRequestQueryCompletable(SiteWorker.SIMPLE_QUERY, resultPrefix)
                    .subscribeOn(Schedulers.io())
                    .subscribe(getMangaListObserver)

                title = genreName.substring(0, 1).toUpperCase() + genreName.substring(1)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            bag.dispose()
        }
        return super.onKeyDown(keyCode, event)
    }


    public override fun onPause() {
        super.onPause()

        dismissProgressBar()
    }

    public override fun onResume() {
        super.onResume()

       dismissProgressBar()

        //mAdMobView?.resume()
    }


    public override fun onStop() {
        super.onStop()

        dismissProgressBar()
    }

    public override fun onDestroy() {
        super.onDestroy()
        bag.dispose()
    }


    override fun onFragmentInteraction(result: Map<String,Any>) {

        if (hasConnection()) {
            activityState = ACTIVITY_STATE.ONLINE

            showProgressBar()

            val params = result.get("params") as HashMap<String,String>
            val completable : Completable
            if (params.size == 0)
                completable = getRequestQueryCompletable(SiteWorker.SIMPLE_QUERY, result["path"] as String)
            else
                completable = getRequestQueryCompletable(SiteWorker.SIMPLE_QUERY, result["path"] as String, params)

            completable.subscribeOn(Schedulers.io())
                .subscribe(getMangaListObserver)

        } else showConnectionError()
    }


    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        val selectedManga = mangaAdapter!!.data[position]
        if (hasConnection()) {

            val intent = Intent(this@MainActivity, MangaInfoActivity::class.java)
            intent.putExtra("is_random", false)
            intent.putExtra("manga_url",selectedManga.url)

            startActivity(intent)
        } else showConnectionError()
    }


    override fun onLoadMoreRequested() {
        if (activityState == ACTIVITY_STATE.ONLINE) {
             if (requestQuery != null) {

                 if (requestQuery!!.offset() >= requestQuery!!.queryAmount()) {
                     mangaAdapter!!.loadMoreComplete()
                     mangaAdapter!!.setEnableLoadMore(false)
                 } else {
                     if (hasConnection()) {

                         Completable.fromCallable {
                             try {
                                 viewModel.observable = requestQuery!!.nextQuery
                             } catch (e: InterruptedException) {
                                 e.printStackTrace()
                             } catch (e: ExecutionException) {
                                 showConnectionError()
                             } catch (e: NullPointerException) {
                                 showConnectionError()
                             }
                             null
                         }.subscribeOn(Schedulers.io())
                         .subscribe(getOnLoadMoreObserver)

                     } else {
                         //Get more data failed
                         Toast.makeText(this@MainActivity, R.string.cant_connect_error, Toast.LENGTH_LONG).show()
                         mangaAdapter!!.loadMoreFail()

                     }
                 }
             } else {
                if (mangaAdapter!!.isLoading)
                    mangaAdapter!!.loadMoreComplete()
                mangaAdapter!!.setEnableLoadMore(false)
             }
         } else {
            if (mangaAdapter!!.isLoading)
                mangaAdapter!!.loadMoreComplete()
            mangaAdapter!!.setEnableLoadMore(false)
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
                                requestQuery = mSiteWorker.RequestQuery(applicationContext, SiteWorker.EDITOR_CHOICE_QUERY)

                            viewModel.observable = requestQuery!!.nextQuery
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            showConnectionError()
                        } catch (e: NullPointerException) {
                            showConnectionError()
                        }

                        null
                    }.subscribeOn(Schedulers.io())
                    .subscribe(getMangaListObserver)
                } else showConnectionError()
            }
            ACTIVITY_STATE.FAVORITES -> {
                activityState = ACTIVITY_STATE.FAVORITES
                Completable.fromCallable {
                    viewModel.observable = appDataSource.listOfFavorites
                    null
                }.subscribeOn(Schedulers.io())
                .subscribe(getMangaListObserver)
            }
        }
    }

    private fun updateDataList(list: List<Manga>) {

        mangaAdapter!!.addAll(list)
        movieListRecyclerView!!.recycledViewPool.clear()

        if (mangaAdapter!!.data.size != 0)
            movieListRecyclerView!!.scrollToPosition(0)

        if (activityState == ACTIVITY_STATE.ONLINE) {
            if (requestQuery != null && requestQuery!!.offset() < requestQuery!!.queryAmount())
                mangaAdapter!!.setEnableLoadMore(true)
        }
    }

    internal fun showConnectionError() {
        activityState = ACTIVITY_STATE.LOST_CONNECTION
        Toast.makeText(applicationContext, getText(R.string.cant_connect_error), Toast.LENGTH_SHORT).show()
    }

    internal fun hasConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = cm.activeNetworkInfo
        return ni != null && ni.isConnected
    }

    private fun showSortingMenu() {
        if (!sortingMenuItem.isVisible)
            sortingMenuItem.isVisible = true
    }

    private fun dismissSortingMenu() {
        sortingMenuItem.isVisible = false
    }

    private fun firstStartDisclaimer() {
        val mSettings = getSharedPreferences(Settings.APP_PREFERENCES, Context.MODE_PRIVATE)
        val APP_FIRST_RUN = "first_run_check"
        var isFirstRun = mSettings.getBoolean(APP_FIRST_RUN,true)

        if (isFirstRun) {
            val editor = mSettings.edit()
            editor.putBoolean(APP_FIRST_RUN, false)
            editor.apply()

            val disclaimerFragment = DisclaimerFragment()
            disclaimerFragment.show(supportFragmentManager, "disclaimer")
        }
    }


    @Throws(InterruptedException::class, ExecutionException::class, NullPointerException::class)
    fun getRequestQueryCompletable(requestType : Int, path : String, params : HashMap<String,String>) : Completable {
        return Completable.fromCallable {
            requestQuery = mSiteWorker.RequestQuery(applicationContext, requestType, path, params)
            viewModel.observable = requestQuery!!.nextQuery
            null
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, NullPointerException::class)
    fun getRequestQueryCompletable(requestType : Int, path : String) =
        Completable.fromCallable {
            requestQuery = mSiteWorker.RequestQuery(applicationContext, requestType, path)
            viewModel.observable = requestQuery!!.nextQuery
            null
        }

    @Throws(InterruptedException::class, ExecutionException::class, NullPointerException::class)
    fun getRequestQueryCompletable(requestType : Int) =
        Completable.fromCallable {
            requestQuery = mSiteWorker.RequestQuery(applicationContext, requestType)
            viewModel.observable = requestQuery!!.nextQuery
            null
        }

    companion object {

        val GENRES_CODE = 15

        internal enum class ACTIVITY_STATE {
            ONLINE,
            FAVORITES,
            LOST_CONNECTION
        }

    }


}
