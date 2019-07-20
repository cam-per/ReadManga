package ru.garretech.readmanga.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.chad.library.adapter.base.entity.MultiItemEntity

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import ru.garretech.readmanga.Settings
import ru.garretech.readmanga.models.Chapter
import ru.garretech.readmanga.models.Manga
import ru.garretech.readmanga.models.Volume

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/*
* Класс для работы с сайтом
* Парсит списки с дорамками. Парсит списки с источниками
* При вызове конструктора сохраняет в себе контекст
* В отдельном методе формируется список фильмов editorChoice
*
* */
class SiteWorker {

    inner class RequestQuery {
        private var requestType: Int = 0
        private var queryAmount = -1
        private var limit: Int = 0
        private var path: String? = null
        private var currentOffset = 0
        private var uriQuery: Uri.Builder? = null
        var list: ArrayList<Manga>? = null
            private set
        private var parameters: HashMap<String, String>? = null
        private var context: Context? = null


        val nextQuery: Observable<List<Manga>>
            @Throws(ExecutionException::class, InterruptedException::class, NullPointerException::class)
            get() = if (queryAmount == -1 || currentOffset < queryAmount) {
                when (requestType) {
                    SIMPLE_QUERY -> {
                        val pageDownloader = PageDownloader()
                        uriQuery = standartUri

                        if (path!!.contains("/")) {
                            val pathArray = path!!.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                            for (s in pathArray) {
                                uriQuery!!.appendPath(s)
                            }
                        } else {
                            uriQuery!!.appendPath(path)
                        }

                        parameters!![OFFSET_PARAM] = currentOffset.toString()

                        for ((key, value) in parameters!!) {
                            uriQuery!!.appendQueryParameter(key, value)
                        }

                        val pageContent = pageDownloader.execute(uriQuery!!.toString()).get()

                        if (pageContent != null) {

                            if (queryAmount == -1)
                                queryAmount = getMaxQueryElementCount(pageContent)

                            val result = mangaListContentParse(context, pageContent, limit)

                            val resultArray = result["list"] as List<Manga>
                            list?.addAll(resultArray)

                            currentOffset += (result["offset"] as Int?)!!
                            Observable.fromArray(resultArray)
                        }
                        else
                            Observable.fromArray(emptyList())
                    }

                    SEARCH_QUERY -> {
                        val client = OkHttpClient()

                        uriQuery = standartUri
                        uriQuery!!.appendPath(path)

                        parameters!![OFFSET_PARAM] = currentOffset.toString()

                        val request = searchRequest(uriQuery!!.toString(), parameters!!["q"], parameters!![OFFSET_PARAM])

                        val response = client.newCall(request).execute()

                        val pageContent = Jsoup.parse(response.body()?.string())

                        if (queryAmount == -1)
                            queryAmount = getMaxSearchElementCount(pageContent)

                        val result = mangaListContentParse(context, pageContent, limit)

                        val resultArray = result["list"] as List<Manga>
                        list?.addAll(resultArray)

                        currentOffset += (result["offset"] as Int?)!!
                        Observable.fromArray(resultArray)
                    }

                    EDITOR_CHOICE_QUERY -> {
                        queryAmount = 5
                        currentOffset = 5
                        getEditorChoiceMangasList(context).let { list = ArrayList(); list?.addAll(it); Observable.fromArray(it) }
                    }
                    else -> Observable.empty()
                }
            } else
                Observable.empty()

        constructor(context: Context, requestType: Int, path: String, params: HashMap<String, String>, limit: Int) {
            this.context = context
            this.requestType = requestType
            this.limit = limit
            this.path = path
            this.parameters = params
        }

        constructor(context: Context, requestType: Int, path: String, params: HashMap<String, String>) {
            this.context = context
            this.requestType = requestType
            this.limit = Settings.max_loaded_in_screen()
            this.path = path
            this.parameters = params
        }

        constructor(context: Context, requestType: Int, path: String) {
            this.context = context
            this.requestType = requestType
            this.limit = Settings.max_loaded_in_screen()
            this.path = path
            parameters = HashMap()
        }

        constructor(context: Context, requestType: Int) {
            this.context = context
            this.requestType = requestType
            this.limit = Settings.max_loaded_in_screen()
            this.path = ""
            parameters = HashMap()
        }

        fun requestUri() : Uri.Builder? {
            return uriQuery
        }

        fun queryAmount(): Int {
            return queryAmount
        }

        fun limit(): Int {
            return limit
        }

        fun offset(): Int {
            return currentOffset
        }

        fun resetOffset() {
            currentOffset = 0
        }

    }

    companion object {
        val SITE_URL = "http://readmanga.me"
        private val SITE_NAME = "readmanga.me"
        private val editorChoice = "row tiles-row short"
        val NEW_MOVIES_PARAMS = arrayOf("sortType", "created")
        val LIST_PREFIX = "list"
        val SEARCH_PREFIX = "search"
        val RANDOM_MOVIE_PREFIX = "/internal/random"
        private val OFFSET_PARAM = "offset"
        val SIMPLE_QUERY = 0
        val SEARCH_QUERY = 1
        val EDITOR_CHOICE_QUERY = 2

        /*
    *  Сформировать ссылку запроса (или из поискового запроса или из выбранного жанра)
    *  Загрузить контент по ссылке
    *
    *
    * */

        private fun getMaxSearchElementCount(pageContent: Document): Int {
            val pattern = Pattern.compile("\\((\\d+)\\)")
            val matcher: Matcher
            var resultAmount = 0

            val element = pageContent.getElementById("mangaResults").getElementsByTag("h3").first()

            matcher = pattern.matcher(element.text())

            if (matcher.find())
                resultAmount = Integer.valueOf(matcher.group(1))

            return resultAmount
        }


        @Throws(InterruptedException::class, ExecutionException::class, NullPointerException::class,IOException::class, FileNotFoundException::class)
        fun getEditorChoiceMangasList(context: Context?): List<Manga> {
            val pageDownloader = PageDownloader()
            val pageContent: Document?
            val movieList = ArrayList<Manga>()
            pageContent = pageDownloader.execute(SITE_URL).get()
            var movie: Manga

            if (pageContent == null)
                return emptyList()

            val tempElements = pageContent.getElementsByClass(editorChoice)
            if (tempElements == null)
                throw NullPointerException()
            else {
                val editorChoiceElements = tempElements.first().getElementsByClass("simple-tile ")

                for (i in editorChoiceElements.indices) {
                    val element1 = editorChoiceElements[i]
                    var genres = element1.attr("title")
                    genres = genres.substring(genres.indexOf(". ") + 2)
                    var url = element1.getElementsByTag("a")[0].attr("href").substring(1)
                    url = "/" + url.substring(0,url.indexOf("/"))
                    val title = element1.getElementsByTag("img")[0].attr("alt")
                    val imageURL: String
                    imageURL = element1.getElementsByTag("img")[0].attr("data-original")
                    movie = Manga(title, ArrayList(Arrays.asList(*genres.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())), imageURL, url)

                    movieList.add(movie)
                }
            }
            return movieList
        }


        val genresList: JSONArray
            @Throws(InterruptedException::class, ExecutionException::class, JSONException::class, NullPointerException::class)
            get() {
                val genresList = JSONArray()
                val URL_PREFIX = "/list/genres/sort_name"
                val pageDownloader = PageDownloader()
                val pageContent: Document?

                pageContent = pageDownloader.execute(SITE_URL + URL_PREFIX).get()

                if (pageContent == null)
                    return genresList

                var element = pageContent.getElementsByClass("table table-hover").first()
                element = element.getElementsByTag("tbody").first()
                val elements = element.getElementsByTag("tr")
                var index = 0
                for (element1 in elements) {
                    val tempElement =
                        element1.getElementsByTag("td").first().getElementsByTag("a").first()
                    val jsonObject = JSONObject()
                    val genreName = tempElement.text()
                    var genreLink = tempElement.attr("href")
                    genreLink = genreLink.substring(1)
                    jsonObject.put("name", genreName)
                    jsonObject.put("link", genreLink)
                    genresList.put(index, jsonObject)
                    index++
                }

                return genresList
            }

        @Throws(InterruptedException::class, ExecutionException::class, NullPointerException::class)
        fun getMangaImageList(link : String) : JSONArray {
            /*  * Переходим по ссылке http://readmanga.me/tower_of_god/vol3/6
            * Считываем количество страниц из элемента с классом pages-count
            * Берем первое фото из div с id=fotocontext. Содержимое аттрибута src из тега img
            * http://e5.mangas.rocks/auto/30/35/40/TowerOfGod_s3_ch06_p01_SIU_Gemini.jpg_res.jpg?t=1556875730&u=0&h=i1nwNAGZO2AF_mAe3BzlHQ
            * Подставляем вместо p01 номера с 1 по количество страниц. Полученный массив ссылок и будет текущий эпизоп манги
            *
            * */
            val pattern = Pattern.compile("rm_h\\.init\\(\\s(.*\\))")
            var matcher : Matcher?
            var resultAmount = ""
            val pageDownloader = PageDownloader()
            val pageContent: Document?
            val jsonArray by lazy {
                if (resultAmount.isNotEmpty())
                    JSONArray(resultAmount)
                else
                    JSONArray()}

            pageContent = pageDownloader.execute(SITE_URL + link).get()

            if (pageContent == null)
                return jsonArray

            matcher = pattern.matcher(pageContent.toString())

            if (matcher.find())
                resultAmount = matcher.group(1)

            resultAmount = resultAmount.substring(0,resultAmount.lastIndexOf("]")+1)

            //val pageCount = pageContent.getElementsByClass("pages-count")?.first()?.text()
            //var imageUrl = pageContent.getElementById("fotocontext").getElementsByTag("img").attr("src")
            //imageUrl = imageUrl.substring(imageUrl.indexOf("?"))

            return jsonArray
        }


        private fun getMaxQueryElementCount(pageContent: Document): Int {
            val pattern = Pattern.compile("(\\d+)")
            val matcher: Matcher
            var resultAmount = 0

            var elements = pageContent.getElementsByTag("h4")
            var patternText: String? = null

            for (element in elements) {

                if (element.getElementsContainingText("Список").let {
                            letElements -> elements = letElements; letElements.size != 0 }) {

                    patternText = elements.first().text()
                    break
                }
            }
            if (patternText != null) {
                matcher = pattern.matcher(patternText)

                if (matcher.find())
                    resultAmount = Integer.valueOf(matcher.group(1))
            } else
                resultAmount = 16870 // Текущее количество дорам на сайте
            return resultAmount
        }

        private fun getCurrentListElementCount(pageContent: Document): Int {
            val elements = pageContent.getElementsByClass("tile col-sm-6 ")
            return elements.size
        }

        //@Throws(InterruptedException::class, ExecutionException::class, JSONException::class, NullPointerException::class)
        fun getMangaInfo(URL: String) =
        Single.create<Manga> {
            val info = JSONObject()
            val pageDownloader = PageDownloader()
            val pageContent: Document?
            var name = ""
            var eng_name = ""
            var original_name = ""
            var image_url = ""
            var url = ""
            var genres : StringBuilder = StringBuilder()
            var description = ""
            var age = ""
            var production = ""

            pageContent = pageDownloader.execute(SITE_URL + URL).get()

            if (pageContent == null) {
                throw NullPointerException()
            }

            var tempElement: Element?
            var tempElements: Elements

            tempElement = pageContent.getElementsByAttributeValue("itemprop", "url").first()
            if (tempElement != null) {
                url = tempElement.attr("content")
                url = url.substring(url.lastIndexOf("/"))
            }

            var lastChapter = pageContent.getElementsByClass("subject-actions col-sm-7").first().getElementsByTag("a").last().attr("href")
            //lastChapter = lastChapter.substring(lastChapter.lastIndexOf("/"))
            lastChapter = lastChapter.substring(url.length)

            tempElement = pageContent.getElementsByClass("name").first()
            if (tempElement != null)
                name = tempElement.text()

            tempElement = pageContent.getElementsByClass("eng-name").first()
            if (tempElement != null)
                eng_name = tempElement.text()

            tempElement = pageContent.getElementsByClass("original-name").first()
            if (tempElement != null)
                original_name = tempElement.text()

            tempElements = pageContent.getElementsByClass("elem_genre ")
            for (element1 in tempElements) {
                genres.append(element1.tagName("a").text())
            }

            tempElement = pageContent.getElementsByClass("manga-description").first()
            if (tempElement != null)
                description = tempElement.text()

            tempElement = pageContent.getElementsByClass("picture-fotorama").first()
            tempElement = tempElement!!.getElementsByTag("img").first()
            if (tempElement != null)
                image_url = tempElement.attr("data-thumb")

            tempElement = pageContent.getElementsByClass("elem_year ").first()
            if (tempElement != null)
                age = tempElement.text()

            tempElement = pageContent.getElementsByClass("elem_country ").first()
            if (tempElement != null)
                production = tempElement.text()

            tempElement = pageContent.getElementsByClass("subject-meta col-sm-7").first()
            tempElements = tempElement!!.getElementsByTag("p")

            tempElement = tempElements[0]
            val chaptersNumber = tempElement!!.text()

            tempElement = tempElements[1]
            val duration = tempElement!!.text()


            val manga = Manga("$name | $eng_name | $original_name",genres.split(", "),image_url,url).also {
                it.lastChapter = lastChapter
                it.productionCountry = production
                it.chaptersNumber = chaptersNumber
                it.duration = duration
                it.description = description
                it.productionYear = age
            }

            it.onSuccess(manga)
        }

        @Throws(NullPointerException::class)
        fun getSortingParams(uri: Uri) : JSONArray{
            /*
            * sortType = name,rate,votes,created,updated
            (По алфавиту,по популярности,по рейтингу,новинки,по дате добавления)
            filter = high_rate,single,mature,completed,translated,many_chapters,wait_upload
            (Все,Высокий рейтинг,Полнометражка,Для взрослых,Завершенная,Переведено,Длинная,Ожидает загрузки)

            Выбор жанра /genre/%жанр

            Выбор страны /country/%страна : vetnam, hong_kong, indoneziia, china, malaiziia, north_korea, singapore, thailand, taiwan, philippines, south_korea, japan

            Прочее /tags/%тэг : web, stopped, mini_drama, ongoing, omnibus, coming_soon

            Рубрики : страна, жанр, прочее (/country/%страна, /genre/%жанр, /tags/%тэг)

            Модификаторы : сортировка, фильтр (sortType, filter)

            * Сам объект массив параметров с возможными значениями
            * Один элемент содержит:
            * sortingName =
            * type = prefix, param
            * key = /genre/, /tags/, /country/, sortType, filter
            * values = { , , }
            * translatedValues = { , , }
            * */

            var pageDownloader = PageDownloader()
            var pageContent: Document?
            var sortingContent : Element
            var tempElements : Elements
            val firstParamPattern = Pattern.compile("\\?(\\w+)=(\\w+)")
            val secondParamPattern = Pattern.compile("\\&(\\w+)=(\\w+)")
            val prefixPattern = Pattern.compile("\\/(\\w+)\\/(\\w+)\\?")

            pageContent = pageDownloader.execute(uri.toString()).get()

            if (pageContent == null) {
                throw NullPointerException()
            }

            sortingContent = pageContent.getElementsByClass("rightContent").first()
            // Формируем список возможных параметров (список хранится в теге ul)
            tempElements = sortingContent.getElementsByTag("ul")

            var index = 0
            val sortingVarJsonArray = JSONArray()
            val selectedOptionsJsonArray = JSONArray()
            for (element in tempElements) {
                val jsonObject = JSONObject()
                val loopElements = element.getElementsByTag("li")
                val selectedElements = element.getElementsByClass("listSelected")
                var name : String

                // Собираем основную информацию для json объекта
                /* sortingName =
                * type = prefix, param
                * isSelected
                * key = /genre/, /tags/, /country/, sortType, filter
                * */

                val element1 = loopElements.last().getElementsByTag("a")
                val link = element1.attr("href")
                var matcher : Matcher

                name = when (index)  {
                    0 -> "Сортировка"
                    1 -> "Фильтр"
                    2 -> "Жанры"
                    3 -> "Страны"
                    4 -> "Прочее"
                    else -> ""
                }

                matcher = when(index) {
                    0 -> firstParamPattern.matcher(link)
                    1 -> secondParamPattern.matcher(link)
                    2,3,4 -> prefixPattern.matcher(link)
                    else -> firstParamPattern.matcher(link)
                }


                if (matcher.find()) {
                    jsonObject.put("sortingName", name)

                    if (index < 2)
                        jsonObject.put("type", "param")
                    else
                        jsonObject.put("type", "prefix")

                    jsonObject.put("key", matcher.group(1))

                    val valuesArray = JSONArray()
                    val translatedValuesArray = JSONArray()
                    var position = 0
                    var selectedPosition : Int = -1

                    for (liElement in loopElements) {
                        val element1 = liElement.getElementsByTag("a")
                        val link = element1.attr("href")
                        val translatedValue = element1.text()
                        var matcherInternalLoop: Matcher

                        if (liElement.toString().contains("listSelected")) {
                            selectedPosition = position
                        }

                        if (liElement.toString().contains("Все")) {
                            valuesArray.put("")
                            translatedValuesArray.put("Все")
                        }

                        matcherInternalLoop = when(index) {
                            0 -> firstParamPattern.matcher(link)
                            1 -> secondParamPattern.matcher(link)
                            2,3,4 -> prefixPattern.matcher(link)
                            else -> firstParamPattern.matcher(link)
                        }

                        if (matcherInternalLoop.find()) {
                            valuesArray.put(matcherInternalLoop.group(2))
                            translatedValuesArray.put(translatedValue)
                        }
                        position++
                    }
                    jsonObject.put("selectedPosition", selectedPosition)
                    jsonObject.put("values", valuesArray)
                    jsonObject.put("translatedValues", translatedValuesArray)

                    sortingVarJsonArray.put(jsonObject)
                }
                index++
            }

            return sortingVarJsonArray
        }


        private fun mangaListContentParse(context: Context?, pageContent: Document, limit: Int): HashMap<String, Any> {
            val mangaList = ArrayList<Manga>()
            val result = HashMap<String, Any>()
            val elements = pageContent.getElementsByClass("tile col-sm-6 ")
            //var imageDownloader: ImageDownloader
            var manga: Manga
            var iteration = 0
            for (element in elements) {

                if (limit != 0 && iteration > limit - 1)
                    break

                // Отсев книг и манги из результатов поиска
                val tempElements = element.getElementsByClass("tile-info").first().getElementsByTag("a")
                if (tempElements.size != 0) {
                    val genres: String
                    if (tempElements.size > 1) {
                        val stringBuilder = StringBuilder()
                        for (element1 in tempElements) {
                            stringBuilder.append(element1.text())
                            stringBuilder.append(", ")
                        }
                        genres = stringBuilder.toString().substring(0, stringBuilder.toString().lastIndexOf(", "))
                    } else {
                        genres = tempElements.first().text()
                    }

                    var tempElement = element.getElementsByClass("img").first()
                    val url = tempElement.getElementsByTag("a")[0].attr("href")
                    tempElement = tempElement.getElementsByTag("img").first()
                    val title = tempElement.attr("title")
                    val imageURL = tempElement.attr("data-original")
                    tempElement = element.getElementsByClass("tags").first()

                    manga = Manga(title, ArrayList(Arrays.asList(*genres.split(", ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())), imageURL, url)

                  /*  var image: Bitmap? = null
                    try {
                        image = getCachedImage(context!!, imageURL)
                        Log.d("STATUS: ", "$imageURL found")
                    } catch (e: FileNotFoundException) {
                        try {
                            imageDownloader = ImageDownloader()
                            image = imageDownloader.execute(imageURL).get()
                            saveImage(context!!, image!!, imageURL)
                        } catch (e1: ExecutionException) {
                            e.printStackTrace()
                        } catch (e1: InterruptedException) {
                            e.printStackTrace()
                        } catch (e1: FileNotFoundException) {
                            e1.printStackTrace()
                        } catch (e1: IOException) {
                            e1.printStackTrace()
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    manga.image = image*/
                    mangaList.add(manga)
                }
                iteration++
            }
            result["list"] = mangaList
            result["offset"] = Integer.valueOf(iteration)
            return result
        }


        fun formChaptersList(URL: String, lastChapter: String): Single<HashMap<String, Any>> {
            return Single.create {
                val resultingMap = HashMap<String, Any>()

                val chaptersList = JSONArray()
                val pattern = Pattern.compile("(\\d+)\\s-\\s(\\d+)")
                val patternChapterName = Pattern.compile("\\s\\w*\\s(.*\$)")
                val ADULT_PREFIX = "?mtr=1"
                val adapterList: ArrayList<MultiItemEntity> = ArrayList<MultiItemEntity>()
                val pageDownloader = PageDownloader()
                val pageContent: Document?
                var doesntHaveNumberInTitle = false

                try {
                    if (lastChapter == "/") {
                        it.onSuccess(resultingMap)
                    }

                    pageContent = pageDownloader.execute(SITE_URL + URL + lastChapter + ADULT_PREFIX).get()

                    if (pageContent == null)
                        it.onError(NullPointerException())

                    val element = pageContent?.getElementById("chapterSelectorSelect")
                    var elements = element?.getElementsByTag("option")
                    elements!!.reverse()

                    var index = 0
                    var volumeIndex = 0
                    var currentVolume: Volume? = null

                    for (element1 in elements) {
                        var matcher = pattern.matcher(element1.text())

                    var currentVolumeNumber : Int

                    doesntHaveNumberInTitle = false
                    if (matcher.find()) {
                        currentVolumeNumber = (matcher.group(1) ?: "0").toInt()
                    } else {
                        doesntHaveNumberInTitle = true
                        currentVolumeNumber = 1
                    }

                    if (volumeIndex != currentVolumeNumber) {
                        if (currentVolume != null && volumeIndex != 0)
                            adapterList.add(currentVolume)

                        currentVolume = Volume(currentVolumeNumber)
                        volumeIndex = currentVolumeNumber
                    }


                    val chapterNumber = index + 1
                    var link = element1.attr("value")
                    link = link.substring(URL.length)

                    matcher = pattern.matcher(element1.text())

                    var chapterName : String
                    if (doesntHaveNumberInTitle)
                        chapterName = element1.text()
                    else {
                        chapterName = element1.text().substring(element1.text().lastIndexOf("- ") + 3)
                        chapterName = chapterName.substring(chapterName.indexOf(" ") + 1)
                    }

                    /*if (matcher.find())
                        chapterName = matcher.group(1)
                    else
                        chapterName = element1.text()*/

                    var currentChapter = Chapter(chapterName, chapterNumber, currentVolumeNumber, link)

                    currentVolume?.addSubItem(currentChapter)

                    val jsonObject = JSONObject()

                    jsonObject.put("chapterName", chapterName)
                    jsonObject.put("chapterNumber", chapterNumber)
                    jsonObject.put("volumeNumber", currentVolumeNumber)
                    jsonObject.put("link", link)
                    chaptersList.put(index, jsonObject)
                    index++
                }
                if (currentVolume != null && volumeIndex != 0)
                    adapterList.add(currentVolume)

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }

                resultingMap.put("adapterList", adapterList)
                resultingMap.put("chapterJsonArray", chaptersList)
                it.onSuccess(resultingMap)
            }
        }

        val standartUri: Uri.Builder
            get() {
                val builder = Uri.Builder()
                builder.scheme("http")
                        .authority(SITE_NAME)
                return builder
            }

        private fun transformFileName(url: String): String {
            val pathParts = url.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val stringBuilder = StringBuilder()
            stringBuilder.append(pathParts[pathParts.size - 3])
            stringBuilder.append(pathParts[pathParts.size - 2])
            stringBuilder.append(pathParts[pathParts.size - 1])
            return stringBuilder.toString()
        }

        @Throws(FileNotFoundException::class, IOException::class)
        fun saveImage(context: Context, image: Bitmap, url: String) {

            val f = File(context.cacheDir, transformFileName(url))
            f.createNewFile()

            val bos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
            val bitmapdata = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(f)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
        }

        @Throws(FileNotFoundException::class, IOException::class)
        fun getCachedImage(context: Context, url: String): Bitmap? {
            var image: Bitmap?

            val f = File(context.cacheDir, transformFileName(url))
            val fis = FileInputStream(f)

            image = BitmapFactory.decodeStream(fis)
            fis.close()

            return image
        }
    }

    @Throws(Exception::class)
    fun searchRequest(vararg params : String?) : Request {
        val body = FormBody.Builder()
            .add("q",params[1]!!)
            .add("offset",params[2]!!)
            .build()

        return Request.Builder().url(params[0]!!).post(body).build()
    }

}
