package com.rts.rys.ryy.wayfinding.game

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Generates the daily challenge stage for a given date.
 *
 * Same date → same maze for every device. The day-of-week selects which
 * mechanic the stage runs under, so each weekday plays distinctly.
 *
 * The returned stage reuses an existing mechanic level (5, 7, 8, 9, 10, 12)
 * so the GameScreen branching applies the right controllers without
 * special-casing daily mode.
 */
object DailyChallenge {
    private const val MAZE_SIZE = 13
    private const val DAILY_ID_BASE = 2_000_000

    fun today(): Stage = stageFor(LocalDate.now())

    fun idFor(date: LocalDate): Int = DAILY_ID_BASE + date.toEpochDay().toInt()

    fun stageFor(date: LocalDate): Stage {
        val epoch = date.toEpochDay()
        val rng = Random(epoch xor 0x5EE_D1L)
        val level = levelForDayOfWeek(date.dayOfWeek)
        val starCount = if (level == 9 || level == 10) 4 else 0
        val withPortals = false
        val maze = generateRandomMaze(
            size = MAZE_SIZE,
            random = rng,
            starCount = starCount,
            withPortals = withPortals,
        )
        return Stage(
            id = idFor(date),
            level = level,
            indexInLevel = 1,
            name = "오늘의 도전",
            difficulty = "${date.monthValue}.${date.dayOfMonth} (${dayOfWeekKo(date.dayOfWeek)}) · ${mechanicLabel(level)}",
            maze = maze,
            isCustom = false,
        )
    }

    fun mechanicLabel(level: Int): String = when (level) {
        1 -> "기본 미로"
        5 -> "벽이 움직여요"
        7 -> "깜깜해요"
        8 -> "쫓아와요"
        9 -> "별을 모아요"
        10 -> "최종 보스"
        12 -> "빙글빙글"
        else -> "미로"
    }

    private fun levelForDayOfWeek(dow: DayOfWeek): Int = when (dow) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 5
        DayOfWeek.WEDNESDAY -> 7
        DayOfWeek.THURSDAY -> 8
        DayOfWeek.FRIDAY -> 9
        DayOfWeek.SATURDAY -> 12
        DayOfWeek.SUNDAY -> 10
    }

    private fun dayOfWeekKo(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}
