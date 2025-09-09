package com.ioi.myssue.domain.model

sealed class NewsBlock {
    data class Image(val url: String) : NewsBlock()
    data class Desc(val text: String)  : NewsBlock()
    data class Text(val text: String)  : NewsBlock()
}