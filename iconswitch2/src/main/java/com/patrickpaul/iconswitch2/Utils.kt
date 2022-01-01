package com.patrickpaul.iconswitch2

import android.content.Context
import kotlin.math.roundToInt

fun ofArgb(fraction: Float, startColor: Int, endColor: Int) : Int {
    val startA = startColor shr 24 and 0xff
    val startR = startColor shr 16 and 0xff
    val startG = startColor shr 8 and 0xff
    val startB = startColor and 0xff

    val endA = endColor shr 24 and 0xff
    val endR = endColor shr 16 and 0xff
    val endG = endColor shr 8 and 0xff
    val endB = endColor and 0xff

    return (startA + (fraction * (endA - startA)).toInt() shl 24 or
            (startR + (fraction * (endR - startR)).toInt() shl 16) or
            (startG + (fraction * (endG - startG)).toInt() shl 8) or
            (startB + (fraction * (endB - startB)).toInt()))
}

fun dpToPx(dp: Int, context: Context): Int {
    return (context.resources.displayMetrics.density * dp).roundToInt()
}