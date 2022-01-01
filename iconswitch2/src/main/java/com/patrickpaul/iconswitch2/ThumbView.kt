package com.patrickpaul.iconswitch2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import kotlin.math.min

class ThumbView(context: Context, attrs: AttributeSet?, defStyle: Int)
    : View(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val center: PointF = PointF()
    private var radius: Float = 0.0F
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(center.x, center.y, radius, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        center.set(w * 0.5F, h * 0.5F)
        radius = min(w, h) * 0.5F
    }

    fun setColor(@ColorInt color: Int) {
        paint.color = color
        invalidate()
    }

    @ColorInt
    fun getColor() : Int {
        return paint.color
    }

}