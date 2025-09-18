package com.ioi.myssue.ui.search

import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.news.NewsCategory

enum class NewsCategory(val category: String, val apiParam: String?) {
    ALL("전체", null),
    POLITICS("정치", "정치"),
    ECONOMY("경제", "경제"),
    SOCIETY("사회", "사회"),
    WORLD("세계", "세계"),
    LIFE_CULTURE("생활/문화", "생활/문화"),
    IT_SCIENCE("IT/과학", "IT/과학");

    companion object {
        val items = entries
    }
}

data class SearchUiState(
    val query: String = "",
    val selectedCat: NewsCategory = NewsCategory.ALL,
    val newsItems: List<NewsSummary> = emptyList(),
    val selectedId: Long? = null,
    val isInitialLoading: Boolean = false,

    val pageSize: Int = 20,
    val lastId: Long? = null,
    val hasNext: Boolean = true,
    val error: String? = null,
)