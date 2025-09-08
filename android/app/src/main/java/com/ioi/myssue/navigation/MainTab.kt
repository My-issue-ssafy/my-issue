package com.ioi.myssue.navigation

import com.ioi.myssue.R

enum class MainTab(
    val activeIconResId: Int,
    val inactiveIconResId: Int,
    internal val contentDescription: String,
    val label: String,
    val route: BottomTabRoute
) {
    NEWS(
        activeIconResId = R.drawable.ic_news_selected,
        inactiveIconResId = R.drawable.ic_news,
        contentDescription = "News Icon",
        label = "뉴스",
        route = BottomTabRoute.News,
    ),
    SEARCH(
        activeIconResId = R.drawable.ic_search_selected,
        inactiveIconResId = R.drawable.ic_search,
        contentDescription = "Search Icon",
        label = "검색",
        route = BottomTabRoute.Search,
    ),
    CARTOON(
        activeIconResId = R.drawable.ic_toon_selected,
        inactiveIconResId = R.drawable.ic_toon,
        contentDescription = "Cartoon Icon",
        label = "네컷뉴스",
        route = BottomTabRoute.Cartoon,
    ),
    PODCAST(
        activeIconResId = R.drawable.ic_podcast_selected,
        inactiveIconResId = R.drawable.ic_podcast,
        contentDescription = "Podcast Icon",
        label = "팟캐스트",
        route = BottomTabRoute.Podcast,
    ),
    MYPAGE(
        activeIconResId = R.drawable.ic_mypage_selected,
        inactiveIconResId = R.drawable.ic_mypage,
        contentDescription = "MyPage Icon",
        label = "마이페이지",
        route = BottomTabRoute.MyPage,
    )
}