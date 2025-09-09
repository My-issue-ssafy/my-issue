package com.ioi.myssue.data.repository.fake

import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.repository.CartoonRepository
import javax.inject.Inject

class FakeCartoonRepositoryImpl @Inject constructor() : CartoonRepository {

    override suspend fun getCartoonNews() = listOf(
        CartoonNews(
            newsTitle = "어른이? 얼음이?\n말장난 속 오해 발생",
            newsDescription = "병아리가 '어른이'를 묻자\n닭이 설을 시작하지만,\n대화는 '얼음이'라는 말장난으로 이어져 웃음을 자아냈다.",
            toonImageUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_1"
        ),
        CartoonNews(
            newsTitle = "뉴스 제목 2",
            newsDescription = "뉴스 설명 2",
            toonImageUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_2"
        ),
        CartoonNews(
            newsTitle = "어른이? 얼음이?\n말장난 속 오해 발생1",
            newsDescription = "병아리가 '어른이'를 묻자\n닭이 설을 시작하지만,\n대화는 '얼음이'라는 말장난으로 이어져 웃음을 자아냈다.",
            toonImageUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_1"
        ),
        CartoonNews(
            newsTitle = "뉴스 제목 4",
            newsDescription = "뉴스 설명4",
            toonImageUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_2"
        )
    )

    override suspend fun getLikedCartoons(
        lastId: Long,
        size: Int
    ): List<CartoonNews> {
        TODO("Not yet implemented")
    }

    override suspend fun likeCartoon(toonId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun hateCartoon(toonId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelLike(toonId: Long) {
        TODO("Not yet implemented")
    }
}