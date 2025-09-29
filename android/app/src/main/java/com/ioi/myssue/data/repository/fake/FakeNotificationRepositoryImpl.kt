package com.ioi.myssue.data.repository.fake


import com.ioi.myssue.data.dto.response.NotificationPageResponse
import com.ioi.myssue.data.dto.response.NotificationResponse
import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.NotificationApi
import com.ioi.myssue.domain.model.NotificationPage
import com.ioi.myssue.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class FakeNotificationRepositoryImpl @Inject constructor(

): NotificationRepository {

    private val all: List<NotificationResponse>

    init {
        val now = OffsetDateTime.now()
        val tmp = mutableListOf<NotificationResponse>()

        // 60개 샘플 생성 (ID 내림차순이 최신)
        for (id in 60 downTo 1) {
            val (odt, read) = when {
                id >= 55 -> now.minusHours((60 - id).toLong()) to (id % 3 == 0)            // 오늘
                id in 48..54 -> now.minusDays(1)
                    .minusHours((54 - id).toLong()) to (id % 4 == 0) // 어제
                else -> {
                    val d = ((id % 28) + 2).toLong() // 2~29일 전
                    now.minusDays(d).minusHours((id % 10).toLong()) to (id % 5 == 0)
                }
            }
            val thumb = if (id % 2 == 0) "https://picsum.photos/seed/$id/300/200" else null

            tmp += NotificationResponse(
                notificationId = id,
                newsId = 20000 + id,
                content = "5년 만에 ‘보급형 태블릿’ 띄우는 삼성... 신흥국 점령 나선다",
                thumbnail = thumb,
                createdAt = odt.toString(),
                read = read
            )
        }
        all = tmp.sortedByDescending { it.notificationId } // 60..1
    }

    override suspend fun getNotifications(lastId: Int?, size: Int): NotificationPage {
        val startIndex = if (lastId == null) 0 else {
            val idx = all.indexOfFirst { it.notificationId == lastId }
            if (idx < 0) 0 else idx + 1
        }
        val endExclusive = max(0, (startIndex + size).coerceAtMost(all.size))
        val slice = all.subList(startIndex, endExclusive)
        val hasNext = endExclusive < all.size

        // 여기서 반드시 DTO → Domain 변환
        return NotificationPageResponse(
            content = slice,
            hasNext = hasNext
        ).toDomain()
    }

    override suspend fun getUnreadNotification(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteNotification(id: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteNotificationAll() {
        TODO("Not yet implemented")
    }

    private var enabled = true
    override suspend fun getStatus(): Boolean { delay(200); return enabled }
    override suspend fun setStatus() { delay(200); }
    override suspend fun readNotification(id: Int) {
        TODO("Not yet implemented")
    }

}