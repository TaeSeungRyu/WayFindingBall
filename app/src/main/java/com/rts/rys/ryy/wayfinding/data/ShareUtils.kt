package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 단일 기록 카드 비트맵을 그리고, 시스템 공유 시트로 내보낸다.
 * cache/share/ 폴더에 PNG로 저장한 뒤 FileProvider URI로 공유.
 */
object ShareUtils {

    private const val W = 1080
    private const val H = 1080

    fun renderRecordCard(
        bgColor: Int,
        levelText: String,
        titleText: String,
        valueLabel: String,
        valueText: String,
        dateText: String,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 배경 그라데이션 (단계 색 → 약간 어두운 색)
        val darker = darken(bgColor, 0.55f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), bgColor, darker, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bgPaint)

        // 상단 장식 원
        val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, 32) }
        canvas.drawCircle(W * 0.85f, H * 0.18f, 220f, deco)
        canvas.drawCircle(W * 0.10f, H * 0.05f, 160f, deco.apply { color = withAlpha(Color.WHITE, 22) })

        // 중앙 흰 카드
        val cardMargin = 80f
        val cardTop = 220f
        val cardBottom = H - 220f
        val cardRect = RectF(cardMargin, cardTop, W - cardMargin, cardBottom)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val cardShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.BLACK, 50)
            setShadowLayer(28f, 0f, 12f, withAlpha(Color.BLACK, 90))
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, cardShadow)
        canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

        // 단계 배지 원 (카드 상단 중앙)
        val badgeY = cardTop + 30f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(W / 2f, badgeY, 96f, badgePaint)
        val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val baseline = badgeY + (badgeText.descent() - badgeText.ascent()) / 2 - badgeText.descent()
        canvas.drawText(levelText, W / 2f, baseline, badgeText)

        // 타이틀 (난이도 이름)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2D2D2D")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(titleText, W / 2f, badgeY + 200f, titlePaint)

        // "최고 기록" 라벨
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A8A8A")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(valueLabel, W / 2f, badgeY + 290f, labelPaint)

        // 값 (시간 또는 단계)
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            textSize = 140f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(valueText, W / 2f, badgeY + 460f, valuePaint)

        // 별 장식 (값 위)
        drawStar(canvas, W / 2f - 200f, badgeY + 410f, 30f, bgColor)
        drawStar(canvas, W / 2f + 200f, badgeY + 410f, 30f, bgColor)

        // 날짜
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B6B6B")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateText, W / 2f, cardBottom - 60f, datePaint)

        // 하단 워터마크 (앱 이름)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 200)
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("또르르 미로", W / 2f, H - 80f, brandPaint)

        return bmp
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String = "record_${System.currentTimeMillis()}.png") {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "기록 공유하기").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val path = Path()
        val points = 5
        val innerR = r * 0.45f
        val step = Math.PI / points
        var angle = -Math.PI / 2
        path.moveTo(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
        for (i in 1 until points * 2) {
            val rr = if (i % 2 == 0) r else innerR
            angle += step
            path.lineTo(cx + (rr * Math.cos(angle)).toFloat(), cy + (rr * Math.sin(angle)).toFloat())
        }
        path.close()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawPath(path, paint)
    }

    private fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
