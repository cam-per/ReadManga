package ru.garretech.readmanga.adapters

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import ru.garretech.readmanga.R
import ru.garretech.readmanga.interfaces.OnExpandableItemClickListener
import ru.garretech.readmanga.models.Chapter
import ru.garretech.readmanga.models.Volume


class ExpandableItemAdapter(data : List<MultiItemEntity>) : BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(data) {
    var onExpandableItemClickListener : OnExpandableItemClickListener? = null

    init {
        addItemType(Volume.TYPE, R.layout.item_expandable_volume);
        addItemType(Chapter.TYPE, R.layout.item_expandable_chapter);
    }


    fun setOnChapterClickListener(listener : OnExpandableItemClickListener) {
        onExpandableItemClickListener = listener
    }


    override fun convert(helper: BaseViewHolder?, item: MultiItemEntity?) {
        when (helper?.itemViewType) {
            Volume.TYPE -> {
                val transformedItem = item as Volume
                helper.setText(R.id.volumeNameText,transformedItem.volumeNumber)

                helper.itemView.setOnClickListener {
                    val pos = helper.adapterPosition
                    if (item.isExpanded()) {
                        collapse(pos)
                    } else {
                        expand(pos)
                    }
                }
            }
            Chapter.TYPE -> {
                val transformedItem = item as Chapter
                helper.setText(R.id.chapterNameText,transformedItem.chapterName)

                helper.itemView.setOnClickListener {
                    if (onExpandableItemClickListener != null) {
                        onExpandableItemClickListener!!.onChapterClick(transformedItem)
                    }
                }
            }
        }
    }


}