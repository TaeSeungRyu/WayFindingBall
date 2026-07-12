package com.rts.rys.ryy.wayfinding.game

/**
 * 자녀가 밤하늘에 직접 별을 찍어 만든 "나만의 별자리".
 * 좌표는 [ConstellationStar]와 동일한 0..1 정규 좌표라 게임/도감/야경에서 그대로 재사용한다.
 */
data class CustomConstellation(
    val id: String,
    /** 아이가 입력한 이름. 화면에는 "○○자리"로 표시. */
    val name: String,
    /** 완성 시 가운데에 띄울 그림 이모지. */
    val emoji: String,
    val stars: List<ConstellationStar>,
) {
    val displayName: String get() = "${name}자리"

    val recordKey: String get() = "$KEY_PREFIX$id"
    val stageKey: String get() = "$KEY_PREFIX$id"

    /** 게임/도감/야경에서 재사용할 스테이지. level은 id 기반 고정 해시로 기본·황도와 겹치지 않게. */
    fun toStage(): ConstellationStage = ConstellationStage(
        level = STAGE_LEVEL_BASE + (id.hashCode() and 0xFFFF),
        name = displayName,
        description = displayName,
        revealEmoji = emoji,
        stars = stars,
        closeOnComplete = false,
        lore = "${name}(이)가 직접 만든 특별한 별자리예요.",
        myth = "온 하늘에 단 하나뿐인 ${displayName}! ${name}(이)가 별을 하나하나 이어서 만들었답니다.",
    )

    companion object {
        /** 기본(1~6)·황도(101~112)와 겹치지 않도록 충분히 큰 오프셋. */
        const val STAGE_LEVEL_BASE = 100_000
        const val KEY_PREFIX = "custom_"
    }
}
