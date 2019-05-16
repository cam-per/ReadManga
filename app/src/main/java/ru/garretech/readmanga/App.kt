package ru.garretech.readmanga

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}