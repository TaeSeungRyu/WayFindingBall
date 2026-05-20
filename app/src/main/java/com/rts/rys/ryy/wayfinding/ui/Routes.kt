package com.rts.rys.ryy.wayfinding.ui

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val LEVEL_SELECT = "levels"
    const val STAGE_SELECT = "stages/{level}"
    const val GAME = "game/{stageId}"
    const val RESULT = "result/{stageId}/{elapsed}/{caught}"
    const val RECORDS = "records"
    const val EDITOR = "editor/{level}"

    fun stages(level: Int) = "stages/$level"
    fun game(stageId: Int) = "game/$stageId"
    fun result(stageId: Int, elapsedMs: Long, caught: Boolean = false) =
        "result/$stageId/$elapsedMs/$caught"
    fun editor(level: Int) = "editor/$level"
}
