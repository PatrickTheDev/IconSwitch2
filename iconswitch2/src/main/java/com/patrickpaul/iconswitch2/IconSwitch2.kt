package com.patrickpaul.iconswitch2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.properties.Delegates

//@Suppress("UNUSED","UNUSED_PARAMETER")
class IconSwitch2(context: Context, attrs: AttributeSet?, defStyle: Int)
    : ViewGroup(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private var thumb: ThumbView
    private var background: Background
    var leftIcon: ImageView
        private set
    var rightIcon: ImageView
        private set

    private val thumbDragHelper: ViewDragHelper
    private var velocityTracker: VelocityTracker? = null
    private var listener: CheckedChangeListener? = null

    private var currentChecked: Checked
    private val downPoint: PointF

    private var inactiveTintIconLeft by correctColorsDelegate(0)
    private  var activeTintIconLeft by correctColorsDelegate(0)
    private var inactiveTintIconRight by correctColorsDelegate(0)
    private  var activeTintIconRight by correctColorsDelegate(0)
    private var thumbColorLeft by correctColorsDelegate(0)
    private  var thumbColorRight by correctColorsDelegate(0)

    private var touchSlopSquare = 0.0
    private var flingMinVelocity = 0
    private var thumbPosition = 0.0F
    private var thumbDragDistance = 0
    private var switchWidth = 0
    private var switchHeight = 0
    private var iconOffset = 0
    private var iconSize = 0
    private var iconTop = 0
    private var iconBottom = 0
    private var thumbStartLeft = 0
    private var thumbEndLeft = 0
    private var thumbDiameter = 0
    private var isClick = false
    private var dragState = 0
    private var translationX = 0
    private var translationY = 0

    init {
        isSaveEnabled = true

        val viewConf = ViewConfiguration.get(context)
        flingMinVelocity = viewConf.scaledMinimumFlingVelocity
        touchSlopSquare = viewConf.scaledTouchSlop.toDouble().pow(2)
        thumbDragHelper = ViewDragHelper.create(this, ThumbDragCallback())
        downPoint = PointF()

        thumb = ThumbView(context)
        addView(thumb)
        leftIcon = ImageView(context)
        addView(leftIcon)
        rightIcon = ImageView(context)
        addView(rightIcon)
        background = Background()
        setBackground(background)
        iconSize = dpToPx(DEFAULT_IMAGE_SIZE_DP, context)

        val colorDefInactive = getAccentColor()
        val colorDefActive = Color.WHITE
        val colorDefBackground = ContextCompat.getColor(context, R.color.purple_700)
        val colorDefThumb = getAccentColor()

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.IconSwitch2,
            0, 0).apply {
                try {
                    iconSize = getDimensionPixelSize(R.styleable.IconSwitch2_icon_size, iconSize)
                    inactiveTintIconLeft = getColor(R.styleable.IconSwitch2_inactive_tint_icon_left, colorDefInactive)
                    activeTintIconLeft = getColor(R.styleable.IconSwitch2_active_tint_icon_left, colorDefActive)
                    inactiveTintIconRight = getColor(R.styleable.IconSwitch2_inactive_tint_icon_right, colorDefInactive)
                    activeTintIconRight = getColor(R.styleable.IconSwitch2_active_tint_icon_right, colorDefActive)
                    thumbColorLeft = getColor(R.styleable.IconSwitch2_thumb_color_left, colorDefThumb)
                    thumbColorRight = getColor(R.styleable.IconSwitch2_thumb_color_right, colorDefThumb)
                    currentChecked = Checked.values()[getInt(R.styleable.IconSwitch2_default_selection, 0)]
                    background.setColor(getColor(R.styleable.IconSwitch2_background_color, colorDefBackground))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        leftIcon.setImageDrawable(getDrawable(R.styleable.IconSwitch2_icon_left))
                        rightIcon.setImageDrawable(getDrawable(R.styleable.IconSwitch2_icon_right))
                    } else {
                        val drawableLeftId = getResourceId(R.styleable.IconSwitch2_icon_left, -1)
                        leftIcon.setImageDrawable(
                            AppCompatResources.getDrawable(context, drawableLeftId)
                        )
                        val drawableRightId = getResourceId(R.styleable.IconSwitch2_icon_right, -1)
                        rightIcon.setImageDrawable(
                            AppCompatResources.getDrawable(context, drawableRightId)
                        )
                    }
                } catch(e: Exception) {
                    currentChecked = Checked.LEFT
                    inactiveTintIconLeft = colorDefInactive
                    activeTintIconLeft = colorDefActive
                    inactiveTintIconRight = colorDefInactive
                    activeTintIconRight = colorDefActive
                    background.setColor(colorDefBackground)
                    thumbColorLeft = colorDefThumb
                    thumbColorRight = colorDefThumb
                } finally {
                    recycle()
                }
        }
        thumbPosition = if (currentChecked === Checked.LEFT) 0F else 1F
        calculateSwitchDimensions()
        ensureCorrectColors()
    }

    @Suppress("SameParameterValue")
    private fun <T> correctColorsDelegate(init: T) =
        Delegates.observable(init) { _, _, _ ->
            ensureCorrectColors()
        }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        leftIcon.layout(iconOffset, iconTop, iconOffset + iconSize, iconBottom)
        val rightIconLeft = switchWidth - iconOffset - iconSize
        rightIcon.layout(rightIconLeft, iconTop, rightIconLeft + iconSize, iconBottom)
        val thumbLeft = (thumbStartLeft + thumbDragDistance * thumbPosition).toInt()
        thumb.layout(thumbLeft, 0, thumbLeft + thumbDiameter, switchHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val overshootPadding = (thumbDiameter * 0.1f).roundToInt()
        val width = getSize(widthMeasureSpec, switchWidth + overshootPadding * 2)
        val height = getSize(heightMeasureSpec, switchHeight)
        background.init(iconSize, width, height)

        val thumbSpec = MeasureSpec.makeMeasureSpec(switchHeight, MeasureSpec.EXACTLY)
        thumb.measure(thumbSpec, thumbSpec)
        val iconSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)
        leftIcon.measure(iconSpec, iconSpec)
        rightIcon.measure(iconSpec, iconSpec)

        translationX = (width/2) - (switchWidth/2)
        translationY = (height/2) - (switchHeight/2)

        setMeasuredDimension(width, height)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            currentChecked = if (state.checked == Checked.LEFT.ordinal) Checked.LEFT else Checked.RIGHT
        }
        super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable {
        val parcelable = super.onSaveInstanceState()
        return SavedState(parcelable, currentChecked)
    }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        canvas?.save()
        canvas?.translate(translationX.toFloat(), translationY.toFloat())
        val result = super.drawChild(canvas, child, drawingTime)
        canvas?.restore()
        return result
    }

    override fun computeScroll() {
        if (thumbDragHelper.continueSettling(true)) {
            postInvalidateOnAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val event = MotionEvent.obtain(e)
        event.setLocation(e.x - translationX, e.y - translationY)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onDown(event)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP -> {
                onUp(event)
                clearTouchInfo()
            }
            MotionEvent.ACTION_CANCEL -> clearTouchInfo()
        }
        thumbDragHelper.processTouchEvent(event)
        event.recycle()
        return true
    }

    private fun onDown(e: MotionEvent) {
        velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(e)
        downPoint[e.x] = e.y
        isClick = true
        thumbDragHelper.captureChildView(thumb, e.getPointerId(0))
    }

    private fun onUp(e: MotionEvent) {
        velocityTracker?.run {
            addMovement(e)
            computeCurrentVelocity(UNITS_VELOCITY)
            if (isClick) {
                isClick = abs(xVelocity) < flingMinVelocity
            }
            if (isClick) {
                toggleSwitch()
                notifyCheckedChanged()
            }
        }
    }

    private fun onMove(e: MotionEvent) {
        velocityTracker?.addMovement(e)
        val distance = hypot((e.x - downPoint.x).toDouble(), (e.y - downPoint.y).toDouble())
        if (isClick) {
            isClick = distance < touchSlopSquare
        }
    }

    private fun clearTouchInfo() {
        if (velocityTracker != null) {
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    private fun applyPositionalTransform() {
        val clampedPosition = 0f.coerceAtLeast(thumbPosition.coerceAtMost(1f)) //Ignore overshooting
        val closenessToCenter = 1f - abs(clampedPosition - 0.5f) / 0.5f
        val iconScale = 1f - closenessToCenter * 0.3f

        val leftColor = ofArgb(clampedPosition, activeTintIconLeft, inactiveTintIconLeft)
        leftIcon.run {
            setColorFilter(leftColor)
            scaleX = iconScale
            scaleY = iconScale
        }
        val rightColor = ofArgb(clampedPosition, inactiveTintIconRight, activeTintIconRight)
        rightIcon.run {
            setColorFilter(rightColor)
            scaleX = iconScale
            scaleY = iconScale
        }
        val thumbColor = ofArgb(clampedPosition, thumbColorLeft, thumbColorRight)
        thumb.setColor(thumbColor)
    }

    private fun calculateSwitchDimensions() {
        iconSize = iconSize.coerceAtLeast(dpToPx(MIN_ICON_SIZE_DP, context))

        switchWidth = iconSize * 4
        switchHeight = (iconSize * 2f).roundToInt()

        iconOffset = (iconSize * 0.6f).roundToInt()
        iconTop = (switchHeight - iconSize) / 2
        iconBottom = iconTop + iconSize

        thumbDiameter = switchHeight
        val thumbRadius = thumbDiameter / 2
        val iconHalfSize = iconSize / 2
        thumbStartLeft = iconOffset + iconHalfSize - thumbRadius
        thumbEndLeft = switchWidth - iconOffset - iconHalfSize - thumbRadius
        thumbDragDistance = thumbEndLeft - thumbStartLeft
    }

    private fun getSize(measureSpec: Int, fallbackSize: Int) : Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.AT_MOST -> size.coerceAtMost(fallbackSize)
            MeasureSpec.EXACTLY -> size
            MeasureSpec.UNSPECIFIED -> fallbackSize
            else -> size
        }
    }

    private fun getLeftAfterFling(direction: Float) : Int {
        return if (direction > 0) thumbEndLeft else thumbStartLeft
    }

    private fun toggleSwitch() {
        currentChecked = currentChecked.toggle()
    }

    private fun notifyCheckedChanged() {
        listener?.onCheckedChange(currentChecked)
    }

    private fun isLeftChecked() : Boolean {
        return currentChecked == Checked.LEFT
    }

    private fun ensureCorrectColors() {
        leftIcon.setColorFilter(if (isLeftChecked()) activeTintIconLeft else inactiveTintIconLeft)
        rightIcon.setColorFilter(if (isLeftChecked()) inactiveTintIconRight else activeTintIconRight)
        thumb.setColor(if (isLeftChecked()) thumbColorLeft else thumbColorRight)
    }

    private fun getAccentColor() : Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    internal class SavedState : BaseSavedState {
        var checked = 0

        constructor(source: Parcel?, loader: ClassLoader?) : super(source, loader) {
            if (source != null) checked = source.readInt()
        }
        constructor(superState: Parcelable?, checked: Checked) : super(superState) {
            this.checked = checked.ordinal
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeInt(checked)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.ClassLoaderCreator<SavedState> = object :
                Parcelable.ClassLoaderCreator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source, null)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)

                override fun createFromParcel(source: Parcel, loader: ClassLoader): SavedState =
                    SavedState(source, loader)
            }
        }
    }

    interface CheckedChangeListener {
        fun onCheckedChange(current: Checked)
    }

    enum class Checked {
        LEFT {
            override fun toggle(): Checked {
                return RIGHT
            }
        },
        RIGHT {
            override fun toggle(): Checked {
                return LEFT
            }
        };
        abstract fun toggle(): Checked
    }

    inner class ThumbDragCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (child != thumb) {
                thumbDragHelper.captureChildView(thumb, pointerId)
                return false
            }
            return true
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            if (isClick) return
            val isFling: Boolean = abs(xvel) >= flingMinVelocity
            val newLeft = if (isFling) getLeftAfterFling(xvel) else getLeftToSettle()
            val newChecked = if (newLeft == thumbStartLeft) Checked.LEFT else Checked.RIGHT
            if (newChecked != currentChecked) {
                currentChecked = newChecked
                notifyCheckedChanged()
            }
            thumbDragHelper.settleCapturedViewAt(newLeft, thumb.top)
            invalidate()
        }

        private fun getLeftToSettle(): Int {
            return if (thumbPosition > 0.5f) thumbEndLeft else thumbStartLeft
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            thumbPosition = (left - thumbStartLeft).toFloat() / thumbDragDistance
            applyPositionalTransform()
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return if (dragState == ViewDragHelper.STATE_DRAGGING) {
                thumbStartLeft.coerceAtLeast(left.coerceAtMost(thumbEndLeft))
            } else left
        }

        override fun onViewDragStateChanged(state: Int) {
            dragState = state
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return if (child === thumb) thumbDragDistance else 0
        }
    }

    companion object {
        private const val DEFAULT_IMAGE_SIZE_DP = 18
        private const val MIN_ICON_SIZE_DP = 12
        private const val UNITS_VELOCITY = 1000
    }

}