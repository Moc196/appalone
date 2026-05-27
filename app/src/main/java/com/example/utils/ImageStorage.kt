package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageStorage {
    fun saveBitmapToInternal(context: Context, bitmap: Bitmap, prefix: String = "moment"): String {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val directory = File(context.filesDir, "moments_photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    // Creates an artistic retro Polaroid template with dynamic text overlays, soft grains, and colors
    // if the user snaps a simulated aesthetic cinematic scene without a live physical camera!
    fun createRetroSimulatedBitmap(
        timeOfDay: String,
        themeTitle: String,
        customCaption: String,
        mood: String,
        filterName: String
    ): Bitmap {
        // Create a square 800x800 bitmap
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw elegant colors based on time of day
        // Morning: warm gold, Noon: clear sky, Evening: peachy orange, Night: dark navy/indigos
        val paint = Paint().apply { isAntiAlias = true }
        
        // Solid background base
        paint.color = when (timeOfDay.lowercase(Locale.ROOT)) {
            "morning" -> Color.parseColor("#FEF3C7")
            "noon" -> Color.parseColor("#E0F2FE")
            "evening" -> Color.parseColor("#FFedd5")
            else -> Color.parseColor("#1e1b4b")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw an abstract artistic lens/gradient glow representation
        val radius = 350f
        paint.shader = when (timeOfDay.lowercase(Locale.ROOT)) {
            "morning" -> android.graphics.RadialGradient(
                400f, 300f, radius,
                Color.parseColor("#FBBF24"), Color.parseColor("#FEF3C7"),
                android.graphics.Shader.TileMode.CLAMP
            )
            "noon" -> android.graphics.RadialGradient(
                450f, 250f, radius,
                Color.parseColor("#38BDF8"), Color.parseColor("#E0F2FE"),
                android.graphics.Shader.TileMode.CLAMP
            )
            "evening" -> android.graphics.RadialGradient(
                400f, 400f, radius,
                Color.parseColor("#FB923C"), Color.parseColor("#FFedd5"),
                android.graphics.Shader.TileMode.CLAMP
            )
            else -> android.graphics.RadialGradient(
                350f, 300f, radius,
                Color.parseColor("#4338CA"), Color.parseColor("#1E1B4B"),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(400f, 350f, radius, paint)

        // Reset shader
        paint.shader = null

        // Apply visual analog vintage filter color overlays (Vibe presets)
        paint.color = when (filterName.lowercase(Locale.ROOT)) {
            "vintage chrome" -> Color.argb(40, 210, 180, 140) // warm sepia tint
            "lomo glow" -> Color.argb(30, 0, 150, 255) // cyan cold leak
            "noir" -> Color.argb(0, 0, 0, 0) // handled by desaturating
            else -> Color.argb(15, 255, 215, 0) // standard soft golden glow
        }
        if (filterName.lowercase(Locale.ROOT) != "noir") {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            // Apply monochrome look on draw or keep simple vintage B&W overlay
            paint.color = Color.argb(50, 120, 120, 120)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        // Draw soft retro vignette
        val vignetteRad = 600f
        paint.shader = android.graphics.RadialGradient(
            400f, 400f, vignetteRad,
            Color.TRANSPARENT, Color.parseColor("#AA000000"),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Add vintage "Date / Time Stamp" overlay in classic amber pixel typography style
        val dateFormat = SimpleDateFormat("yyyy . MM . dd  HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        // Draw retro stamp text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FB923C") // Retro amber orange
            textSize = 28f
            style = Paint.Style.FILL
            letterSpacing = 0.1f
        }
        canvas.drawText(dateString, 50f, 740f, textPaint)

        // Draw Time of day badge
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFFFFF")
            alpha = 180
            textSize = 24f
            style = Paint.Style.FILL
        }
        canvas.drawText("${timeOfDay.uppercase()} MOMENT: $themeTitle", 50f, 700f, badgePaint)

        return bitmap
    }
}
