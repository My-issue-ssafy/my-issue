package com.ioi.myssue.designsystem.theme

import android.R.attr.fontFamily
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ioi.myssue.R

val spoqaHanSansNeo = FontFamily(
    Font(R.font.spoqa_han_sans_neo_regular, FontWeight.Normal),
    Font(R.font.spoqa_han_sans_neo_bold, FontWeight.Bold),
    Font(R.font.spoqa_han_sans_neo_light, FontWeight.Light),
    Font(R.font.spoqa_han_sans_neo_medium, FontWeight.Medium),
    Font(R.font.spoqa_han_sans_neo_thin, FontWeight.Thin)
)

val Typography = Typography(
    // 큰 제목 (앱 타이틀, 메인 헤더)
    headlineLarge = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 44.sp
    ),

    // 일반 제목 (카드 제목, 섹션 헤더)
    titleLarge = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 40.sp
    ),

    // 작은 제목 (서브 헤더)
    titleMedium = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),

    // 본문 (가장 많이 사용)
    bodyLarge = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    // 작은 본문 (설명, 부연설명)
    bodyMedium = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),

    // 버튼, 라벨
    labelLarge = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),

    // 캡션, 메타 정보
    labelMedium = TextStyle(
        fontFamily = spoqaHanSansNeo,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)