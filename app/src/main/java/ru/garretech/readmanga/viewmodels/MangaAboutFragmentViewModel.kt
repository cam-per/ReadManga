package ru.garretech.readmanga.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ru.garretech.readmanga.models.Manga

class MangaAboutFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var currentManga : Manga? = null



}