package ru.garretech.readmanga.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.io.*
import java.net.URL

class ImageDownloader1(val context: Context) : AsyncTask<String, Void, Bitmap>() {

    override fun doInBackground(vararg strings: String): Bitmap? {
        var image: Bitmap? = null
        val url = URL(strings[0])
        try {
            val f = File(context.cacheDir, transformFileName(strings[0]))
            val fis = FileInputStream(f)

            image = BitmapFactory.decodeStream(fis)
            fis.close()
            return image

        } catch (e : FileNotFoundException) {
            try {

                image = BitmapFactory.decodeStream(url.openConnection().getInputStream())

                val f = File(context.cacheDir, transformFileName(strings[0]))
                f.createNewFile()

                val bos = ByteArrayOutputStream()
                image?.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
                val bitmapdata = bos.toByteArray()

                val fos = FileOutputStream(f)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()

                return image

            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return image
    }


    companion object {


        private fun transformFileName(url: String): String {
            val pathParts = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val stringBuilder = StringBuilder()
            stringBuilder.append(pathParts[pathParts.size - 3])
            stringBuilder.append(pathParts[pathParts.size - 2])
            stringBuilder.append(pathParts[pathParts.size - 1])
            return stringBuilder.toString()
        }

    }

}


