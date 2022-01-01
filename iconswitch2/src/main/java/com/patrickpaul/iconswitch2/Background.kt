package com.patrickpaul.iconswitch2

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class Background : Drawable() {

    private var radiusX: Float = 0F
    private val bounds: RectF = RectF()
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun init(imageSize: Int, width: Int, height: Int) {
        val centerX = width * 0.5F
        val centerY = height * 0.5F
        val halfWidth = imageSize * 1.75F
        val halfHeight = imageSize * 0.75F

        bounds.set(centerX - halfWidth, centerY - halfHeight,
            centerX + halfWidth, centerY + halfHeight)

        radiusX = bounds.height() * 0.5F
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(bounds, radiusX, radiusX, paint)
    }

    override fun setAlpha(alpha: Int) {
        // No-op
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No-op
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    fun setColor(@ColorInt color: Int) {
        paint.color = color
        invalidateSelf()
    }

}