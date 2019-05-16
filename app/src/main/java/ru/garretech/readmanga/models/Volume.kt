package ru.garretech.readmanga.models

import com.chad.library.adapter.base.entity.AbstractExpandableItem
import com.chad.library.adapter.base.entity.MultiItemEntity

class Volume(volumeNumber : Int) : AbstractExpandableItem<Chapter>(), MultiItemEntity {


    override fun getItemType(): Int {
        return TYPE
    }

    val volumeNumber = "Том ${volumeNumber}"

    override fun getLevel(): Int {
        return 0
    }

    companion object {
        const val TYPE = 0
    }


}