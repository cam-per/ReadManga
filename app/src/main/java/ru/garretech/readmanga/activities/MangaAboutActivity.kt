package ru.garretech.readmanga.activities

import android.content.Intent
import android.net.Uri
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

import org.json.JSONException
import org.json.JSONObject

import java.util.Arrays

import kotlinx.android.synthetic.main.activity_manga_about.*
import ru.garretech.readmanga.R
import ru.garretech.readmanga.adapters.MovieAboutPagerAdapter
import ru.garretech.readmanga.database.AppDataSource
import ru.garretech.readmanga.fragments.MangaAboutFragment
import ru.garretech.readmanga.fragments.MangaSourcesFragment
import ru.garretech.readmanga.models.Manga

class MangaAboutActivity : AppCompatActivity(), MangaAboutFragment.OnFragmentInteractionListener, MangaSourcesFragment.OnFragmentInteractionListener {

/*  * Переходим по ссылке http://readmanga.me/tower_of_god/vol3/6
    * Считываем количество страниц из элемента с классом pages-count
    * Берем первое фото из div с id=fotocontext. Содержимое аттрибута src из тега img
    * http://e5.mangas.rocks/auto/30/35/40/TowerOfGod_s3_ch06_p01_SIU_Gemini.jpg_res.jpg?t=1556875730&u=0&h=i1nwNAGZO2AF_mAe3BzlHQ
    * Подставляем вместо p01 номера с 1 по количество страниц. Полученный массив ссылок и будет текущий эпизоп манги
    *
    * */

    internal lateinit var mFragmentAdapter: MovieAboutPagerAdapter
    internal lateinit var mangaInfo: JSONObject
    internal lateinit var title: String
    internal lateinit var age: String
    internal lateinit var genres: String
    internal lateinit var production: String
    internal lateinit var chaptersNumber: String
    internal lateinit var duration: String
    internal lateinit var description: String
    internal lateinit var imageURL: String
    internal lateinit var mangaURL: String
    internal lateinit var lastChapter: String
    internal lateinit var currentManga: Manga
    internal lateinit var dataSource: AppDataSource
    internal var isFavorite: Boolean = false
    internal var observable: Subject<Boolean> = PublishSubject.create()
    internal var disposable: Disposable? = null
    internal lateinit var optionsMenu: Menu


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga_about)
        val intent = intent
        dataSource = AppDataSource(applicationContext)
        try {
            var mangaInfoString : String? = intent.getStringExtra("manga_info")

            if (mangaInfoString == null)
                throw NullPointerException()

            mangaInfo = JSONObject(mangaInfoString!!)



            this.title = mangaInfo.getString("title")
            this.genres = mangaInfo.getString("genres")
            this.imageURL = mangaInfo.getString("image_url")
            this.mangaURL = mangaInfo.getString("url")
            this.age = mangaInfo.getString("age")
            this.description = mangaInfo.getString("description")
            this.lastChapter = mangaInfo.getString("last_chapter")
            this.production = mangaInfo.getString("production")
            this.chaptersNumber = mangaInfo.getString("chapters_number")
            this.duration = mangaInfo.getString("duration")

            val bundle = intent.getBundleExtra("bundle")
            try {
                currentManga = bundle.getSerializable("manga") as Manga
            } catch (e: NullPointerException) {
                currentManga = Manga(title, Arrays.asList(*genres.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()), imageURL, mangaURL)
                currentManga.productionYear = age
                currentManga.description = description
                currentManga.lastChapter = lastChapter
                currentManga.productionCountry = production
                currentManga.chaptersNumber = chaptersNumber
                currentManga.duration = duration
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            finish()
        }

        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(viewPager))


    }


    private fun setupViewPager(viewPager: androidx.viewpager.widget.ViewPager) {
        mFragmentAdapter = MovieAboutPagerAdapter(supportFragmentManager)

        try {
            val sourcesInfo = JSONObject()
            sourcesInfo.put("url", mangaURL)
            sourcesInfo.put("last_chapter", lastChapter)
            mFragmentAdapter.addFragment(MangaAboutFragment.newInstance(mangaInfo), "О манге")
            mFragmentAdapter.addFragment(MangaSourcesFragment.newInstance(sourcesInfo), "Эпизоды")
            viewPager.adapter = mFragmentAdapter
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = title
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun flagFavorite(flag: Boolean) {
        val item = optionsMenu.getItem(0)
        if (flag)
            item.setIcon(R.drawable.ic_favorite_white_24dp)
        else
            item.setIcon(R.drawable.ic_favorite_border_white_24dp)

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_manga_about, menu)
        optionsMenu = menu


        disposable = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Consumer<Boolean> {
                    override fun accept(t: Boolean) {
                        flagFavorite(t)
                    }
                })

        Completable.fromCallable {
            isFavorite = dataSource.isFavorite(currentManga.url)
            emmitFavorite(isFavorite)
            null
        }.subscribeOn(Schedulers.io())
                .subscribe(object : CompletableObserver {
                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onComplete() {
                        Log.d("Task", "Subscribe to favorite changes completed")
                    }

                    override fun onError(e: Throwable) {

                    }
                })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_favorite -> {
                if (isFavorite) {
                    Completable.fromCallable {
                        dataSource.deleteFavorites(currentManga)
                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(object : CompletableObserver {
                                override fun onSubscribe(d: Disposable) {

                                }

                                override fun onComplete() {
                                    emmitFavorite(false)
                                    Log.d("Task", "Delete completable completed")
                                }

                                override fun onError(e: Throwable) {
                                    Log.d("Task", "Delete completable error")
                                }
                            })
                } else {
                    Completable.fromCallable {
                        dataSource.addFavorites(currentManga)
                        null
                    }.subscribeOn(Schedulers.io())
                            .subscribe(object : CompletableObserver {
                                override fun onSubscribe(d: Disposable) {

                                }

                                override fun onComplete() {
                                    emmitFavorite(true)
                                    Log.d("Task", "Add completable completed")
                                }

                                override fun onError(e: Throwable) {
                                    Log.d("Task", "Add completable error")
                                }
                            })
                }
            }
            R.id.action_settings -> {
                val intent = Intent(this@MangaAboutActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    internal fun emmitFavorite(value: Boolean) {
        isFavorite = value
        observable.onNext(isFavorite)
    }

}

