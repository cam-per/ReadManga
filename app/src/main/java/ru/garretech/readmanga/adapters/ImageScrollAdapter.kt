package ru.garretech.readmanga.adapters

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.viewpager.widget.PagerAdapter
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import org.json.JSONArray
import com.github.piasy.biv.view.BigImageView
import ru.garretech.readmanga.R
import ru.garretech.readmanga.interfaces.OnViewPagerClickListener


class ImageScrollAdapter(private val mContext : Context, private val mImageList: JSONArray) : PagerAdapter() {
    lateinit var onViewPagerClickListener : OnViewPagerClickListener

    fun setCustomOnClickListener(listener: OnViewPagerClickListener) {
        onViewPagerClickListener = listener
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return mImageList.length()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        synchronized(ImageScrollAdapter::class) {

            //if (container.getChildAt(position) == null) {

                val imageView = BigImageView(mContext)
                imageView.setFailureImage(getDrawable(mContext,R.drawable.broken_image))
                imageView.setOptimizeDisplay(true)
                imageView.setProgressIndicator(ProgressPieIndicator())


                if (position > container.childCount) {
                    for (index in container.childCount..position) {
                        imageView.showImage(Uri.parse(mImageList.get(index).toString()))
                        container.addView(imageView, index)
                        imageView.setOnClickListener {
                            onViewPagerClickListener.onClick()
                        }
                    }
                } else {
                    imageView.showImage(Uri.parse(mImageList.get(position).toString()))
                    container.addView(imageView, position)

                    imageView.setOnClickListener {
                        onViewPagerClickListener.onClick()
                    }

                }
                return container.getChildAt(position)
           // } else {
             //   return container.getChildAt(position)
            //}
        }
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

    }
}