package ru.garretech.readmanga.models

import com.chad.library.adapter.base.entity.MultiItemEntity

class Chapter(name : String, number : Int, link : String) : MultiItemEntity {
    override fun getItemType(): Int {
        return TYPE
    }

    val chapterName : String = name.substring(name.indexOf("- ") + 2)
    val chapterNumber : Int = number
    val link : String = link

    companion object {
        const val TYPE = 1
    }
}