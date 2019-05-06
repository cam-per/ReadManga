package ru.garretech.readmanga.activities

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import ru.garretech.readmanga.R
import kotlinx.android.synthetic.main.activity_manga_reader.*
import org.json.JSONArray
import ru.garretech.readmanga.adapters.ImageScrollAdapter


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class MangaReaderActivity : AppCompatActivity() {
    val image: Bitmap? = null
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manga_reader)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val imageListString = intent.getStringExtra("imageList")
        val imageList = JSONArray(imageListString)


        mVisible = true

        //mangaContentView.setOnClickListener { toggle() }


        dummy_button.setOnTouchListener(mDelayHideTouchListener)

        val adapter = ImageScrollAdapter(this,imageList)
        mangaContentView.adapter = adapter

    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide(100)
    }

    public fun toggle(view: View) {
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

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }

    fun convertToDp(input: Int): Int {
        // Get the screen's density scale
        val scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return Math.ceil(input * scale.toDouble()).toInt()
    }
}
