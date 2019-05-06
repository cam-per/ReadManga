package ru.garretech.readmanga

import android.app.Application
import androidx.multidex.MultiDex
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        BigImageViewer.initialize(GlideImageLoader.with(this))
    }
}