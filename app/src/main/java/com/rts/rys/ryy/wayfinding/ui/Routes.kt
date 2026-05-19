package com.rts.rys.ryy.wayfinding.ui

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val STAGE_SELECT = "stages"
    const val GAME = "game/{stageId}"
    const val RESULT = "result/{stageId}/{elapsed}"
    const val RECORDS = "records"

    fun game(stageId: Int) = "game/$stageId"
    fun result(stageId: Int, elapsedMs: Long) = "result/$stageId/$elapsedMs"
}
