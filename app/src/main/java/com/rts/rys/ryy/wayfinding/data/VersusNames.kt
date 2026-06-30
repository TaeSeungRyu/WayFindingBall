package com.rts.rys.ryy.wayfinding.data

import kotlin.random.Random

/**
 * 대전에서 상대에게 보일 닉네임은 자유 입력이 아니라 이 목록에서 "선택"만 한다.
 * 아동 앱 UGC/실명/식별자 입력 경로를 원천 차단하기 위함.
 * 닉네임 = 동물 + 숫자(두 자리)로 충돌 확률을 낮춘다.
 */
object VersusNames {
    val ANIMALS: List<String> = listOf(
        "토끼", "고양이", "강아지", "곰돌이", "여우", "사자", "호랑이", "판다",
        "펭귄", "다람쥐", "코끼리", "기린", "거북이", "햄스터", "병아리", "부엉이",
    )

    /** 선택지로 보여줄 닉네임 후보(동물 × 무작위 두 자리). */
    fun samplePool(random: Random = Random.Default, count: Int = 12): List<String> =
        ANIMALS.shuffled(random).take(count).map { "$it${10 + random.nextInt(90)}" }

    /** 미설정 시 자동 배정용 기본 닉네임. */
    fun randomDefault(random: Random = Random.Default): String {
        val animal = ANIMALS[random.nextInt(ANIMALS.size)]
        return "$animal${10 + random.nextInt(90)}"
    }
}
