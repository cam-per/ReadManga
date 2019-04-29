package ru.garretech.readmanga.adapters

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

import ru.garretech.readmanga.R
import ru.garretech.readmanga.models.Manga

class RecyclerAdapter(layoutResId: Int, data: List<Manga>?) : BaseQuickAdapter<Manga, BaseViewHolder>(layoutResId, data) {

    override fun addData(data: Manga) {
        super.addData(data)
    }

    fun addAll(movies: Collection<Manga>) {
        data.clear()
        var index = 0
        for (movie in movies) {
            data.add(movie)
            notifyItemInserted(index)
            index++
        }
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    override fun convert(helper: BaseViewHolder, item: Manga) {
        helper.setText(R.id.manga_title, item.title)
        helper.setImageBitmap(R.id.manga_photo, item.image)
    }
}
