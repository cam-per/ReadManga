package ru.garretech.readmanga.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.Observable
import ru.garretech.readmanga.fragments.ProgressBottomSheet
import ru.garretech.readmanga.models.Manga

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    var observable: Observable<List<Manga>>? = null
    lateinit var progressBottomSheet : ProgressBottomSheet

}