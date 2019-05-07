package ru.garretech.readmanga.activities

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_manga_reader.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONArray
import ru.garretech.readmanga.adapters.ImageScrollAdapter
import ru.garretech.readmanga.interfaces.OnViewPagerClickListener
import ru.garretech.readmanga.R

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class MangaReaderActivity : AppCompatActivity(), OnViewPagerClickListener {

    override fun onClick() {
        toggle()
    }

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {

        mangaContentView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }

    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manga_reader)
        setSupportActionBar(mangaReaderToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        //    supportActionBar?.setBackgroundDrawable(getDrawable(R.color.black_overlay))

        val imageListString = intent.getStringExtra("imageList")
        val chapterName = intent.getStringExtra("chapterName")
        setTitle(chapterName)
        val imageList = JSONArray(imageListString)

        mVisible = true


        val adapter = ImageScrollAdapter(this,imageList)
        pageCount.text = imageList.length().toString()
        updateCurrentPage(1)
        adapter.setCustomOnClickListener(this)
        mangaContentView.adapter = adapter

        mangaContentView.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateCurrentPage(position+1)
            }

            override fun onPageSelected(position: Int) {

            }

        })

        currentPageText.setOnEditorActionListener { v, actionId, event ->
            when(actionId) {
                KeyEvent.KEYCODE_ENDCALL -> {
                    var value = currentPageText.text.toString().toInt() - 1

                    if (value > imageList.length())
                        value = imageList.length()

                    mangaContentView.setCurrentItem(value,true)
                    true
                }
                else -> false
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide(100)
    }

    fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        mangaContentView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {

        private val AUTO_HIDE = true

        private val AUTO_HIDE_DELAY_MILLIS = 3000

        private val UI_ANIMATION_DELAY = 300
    }

    fun updateCurrentPage(value : Int) {
        currentPageText.setText(value.toString())
    }


    fun slidePrevious(view: View) {
        mangaContentView.setCurrentItem(mangaContentView.currentItem - 1,true)
        updateCurrentPage(mangaContentView.currentItem+1)
    }

    fun slideNext(view: View) {
        mangaContentView.setCurrentItem(mangaContentView.currentItem + 1,true)
        updateCurrentPage(mangaContentView.currentItem+1)
    }
}
