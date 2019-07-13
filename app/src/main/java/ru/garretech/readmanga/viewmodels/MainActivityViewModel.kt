package ru.garretech.readmanga.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import io.reactivex.CompletableObserver
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import ru.garretech.readmanga.DisposableManager
import ru.garretech.readmanga.models.Manga
import ru.garretech.readmanga.tools.ImageDownloader
import ru.garretech.readmanga.tools.SiteWorker
import java.io.FileNotFoundException

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    var observable: Observable<List<Manga>>? = null


}