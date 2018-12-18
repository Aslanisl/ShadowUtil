import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader.TileMode.CLAMP
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import android.support.annotation.ColorInt
import android.support.annotation.IntRange
import com.foodsoul.R
import com.foodsoul.extension.getColorCompatAttr
import com.foodsoul.extension.getDimensionPixel
import com.foodsoul.extension.setPrivateField
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType.ALONE
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType.FIRST
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType.INSIDE
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType.LAST
import com.foodsoul.presentation.base.view.shadowRoundedView.ShadowType.NONE
import kotlin.math.roundToInt

object DrawableUtil {
    private val shadowPadding by lazy { getDimensionPixel(R.dimen.shadow_content_padding) }

    fun getCircleShadowBackground(context: Context): Drawable {
        val shadowColor = context.getColorCompatAttr(R.attr.colorShadow)
        val backgroundColor = context.getColorCompatAttr(R.attr.colorBackground)

        return ShapeDrawable().apply {
            shape = OvalShadow(backgroundColor, shadowColor, shadowPadding)
            setPadding(shadowPadding, shadowPadding, shadowPadding, shadowPadding)
        }
    }

    internal class OvalShadow constructor(
        @ColorInt private val backgroundColor: Int,
        @ColorInt private val shadowColor: Int,
        private val shadowRadius: Int
    ) : OvalShape() {

        private var radialGradient: RadialGradient? = null
        private val backgroundPaint: Paint = Paint().apply { color = backgroundColor }
        private val shadowPaint: Paint = Paint()

        init {
            updateRadialGradient(rect().width().toInt())
        }

        override fun onResize(width: Float, height: Float) {
            super.onResize(width, height)
            updateRadialGradient(width.toInt())
        }

        override fun draw(canvas: Canvas, paint: Paint) {
            val width = rect().width()
            val height = rect().height()
            canvas.drawCircle(width / 2, height / 2, width / 2, shadowPaint)
            canvas.drawCircle(width / 2, height / 2, width / 2 - shadowRadius, backgroundPaint)
        }

        private fun updateRadialGradient(diameter: Int) {
            val radius = diameter / 2f
            if (radius < 1) return
            val center = diameter / 2f
            radialGradient = RadialGradient(center, center, radius, intArrayOf(shadowColor, Color.TRANSPARENT), null, CLAMP)
            shadowPaint.shader = radialGradient
        }
    }

    fun getRoundedShadowBackground(
        context: Context,
        shadowType: ShadowType,
        radiusCorner: Float,
        backgroundColor: Int
    ): Drawable {
        val padding = Rect()
        padding.left = if (shadowType == NONE) 0 else shadowPadding
        padding.top = if (shadowType == ALONE || shadowType == FIRST) shadowPadding else 0
        padding.right = if (shadowType == NONE) 0 else shadowPadding
        padding.bottom = if (shadowType == ALONE || shadowType == LAST) shadowPadding else 0

        return when (shadowType) {
            ALONE -> getRoundedShadowBackground(context, true, true, true, true, radiusCorner, padding, backgroundColor)
            FIRST -> getRoundedShadowBackground(context, true, true, false, false, radiusCorner, padding, backgroundColor)
            INSIDE -> getRoundedShadowBackground(context, false, false, false, false, radiusCorner, padding, backgroundColor)
            LAST -> getRoundedShadowBackground(context, false, false, true, true, radiusCorner, padding, backgroundColor)
            NONE -> getRoundedShadowBackground(context, false, false, false, false, radiusCorner, padding, backgroundColor)
        }
    }

    fun getRoundedShadowBackground(
        context: Context,
        leftTopCorner: Boolean,
        rightTopCorner: Boolean,
        rightBottomCorner: Boolean,
        leftBottomCorner: Boolean,
        radiusCorner: Float,
        padding: Rect,
        backgroundColor: Int
    ): Drawable {
        val shadowColor: Int = context.getColorCompatAttr(R.attr.colorShadow)
        val maxShadowAlpha = 30

        val layers = mutableListOf<Drawable>()
        // We just hardcore shadow layers count. Can change it if need
        val shadowCount = 5
        val shadowAlphaStep = maxShadowAlpha / shadowCount

        val leftShadowPaddingStep = padding.left.toFloat() / shadowCount
        val topShadowPaddingStep = padding.top.toFloat() / shadowCount
        val rightShadowPaddingStep = padding.right.toFloat() / shadowCount
        val bottomShadowPaddingStep = padding.bottom.toFloat() / shadowCount
        for (i in 0 until shadowCount) {
            val layerPadding = Rect()
            // Don't add padding to first layer
            if (i > 0) {
                layerPadding.left = leftShadowPaddingStep.roundToInt()
                layerPadding.top = topShadowPaddingStep.roundToInt()
                layerPadding.right = rightShadowPaddingStep.roundToInt()
                layerPadding.bottom = bottomShadowPaddingStep.roundToInt()
            }

            layers += getRoundedShadowLayer(
                shadowColor,
                i * shadowAlphaStep,
                false, leftTopCorner, rightTopCorner, rightBottomCorner, leftBottomCorner, radiusCorner, layerPadding)
        }

        layers += getRoundedShadowLayer(backgroundColor, 255, true, leftTopCorner, rightTopCorner, rightBottomCorner, leftBottomCorner, radiusCorner, padding)

        return LayerDrawable(layers.toTypedArray())
    }

    private fun getRoundedShadowLayer(
        @ColorInt color: Int,
        @IntRange(from = 0L, to = 255L) alpha: Int = 255,
        removePadding: Boolean = false,
        leftTopCorner: Boolean,
        rightTopCorner: Boolean,
        rightBottomCorner: Boolean,
        leftBottomCorner: Boolean,
        radiusCorner: Float,
        padding: Rect
    ): Drawable {

        val drawable = GradientDrawable()
        val radii = FloatArray(8)
        radii[0] = if (leftTopCorner) radiusCorner else 0f
        radii[1] = if (leftTopCorner) radiusCorner else 0f

        radii[2] = if (rightTopCorner) radiusCorner else 0f
        radii[3] = if (rightTopCorner) radiusCorner else 0f

        radii[4] = if (rightBottomCorner) radiusCorner else 0f
        radii[5] = if (rightBottomCorner) radiusCorner else 0f

        radii[6] = if (leftBottomCorner) radiusCorner else 0f
        radii[7] = if (leftBottomCorner) radiusCorner else 0f

        drawable.cornerRadii = radii

        val shadowPadding = Rect(padding)
        if (removePadding) shadowPadding.set(0, 0, 0, 0)
        drawable.setPrivateField("mPadding", shadowPadding)
        drawable.setColor(color)
        drawable.alpha = alpha
        return drawable
    }

    fun getRoundedShadowBackground(
        context: Context,
        leftTopCorner: Boolean,
        rightTopCorner: Boolean,
        rightBottomCorner: Boolean,
        leftBottomCorner: Boolean,
        radiusCorner: Float,
        padding: Rect,
        backgroundColor: Int
    ): Drawable {
        val shadowColor: Int = context.getColorCompatAttr(R.attr.colorShadow)

        return ShapeDrawable().apply {
            shape = RoundedShadow(
                backgroundColor,
                shadowColor,
                leftTopCorner,
                rightTopCorner,
                rightBottomCorner,
                leftBottomCorner,
                radiusCorner,
                padding
            )
            setPadding(padding.left, padding.top, padding.right, padding.bottom)
        }
    }

    internal class RoundedShadow constructor(
        @ColorInt private val backgroundColor: Int,
        @ColorInt private val shadowColor: Int,
        private val leftTopCorner: Boolean,
        private val rightTopCorner: Boolean,
        private val rightBottomCorner: Boolean,
        private val leftBottomCorner: Boolean,
        private val radiusCorner: Float,
        private val padding: Rect
    ) : RectShape() {

        private var radialGradient: RadialGradient? = null
        private val backgroundPaint: Paint = Paint().apply { color = backgroundColor }
        private val shadowPaint: Paint = Paint().apply {
            isAntiAlias = true
            style = Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = shadowPadding.toFloat()
        }

        private lateinit var shadowRect: Path
        private lateinit var backgroundRect: Path

        init {
            updateRadialGradient(rect().width().toInt())
            updateRoundedRect()
        }

        override fun onResize(width: Float, height: Float) {
            super.onResize(width, height)
            updateRadialGradient(width.toInt())
            updateRoundedRect()
        }

        override fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawPath(shadowRect, shadowPaint)
            canvas.drawPath(backgroundRect, backgroundPaint)
        }

        private fun updateRadialGradient(diameter: Int) {
            val radius = diameter / 2f
            if (radius < 1) return
//            shadowPaint.maskFilter = BlurMaskFilter(diameter.toFloat(), Blur.NORMAL)

            val center = diameter / 2f
//            radialGradient = RadialGradient(center, center, radius, intArrayOf(shadowColor, Color.TRANSPARENT), null, CLAMP)
//            shadowPaint.shader = radialGradient
        }

        private fun updateRoundedRect() {
            val width = rect().width()
            val height = rect().height()

            shadowRect = getRoundedRect(0f, 0f, width, height, radiusCorner, radiusCorner, leftTopCorner, rightTopCorner, rightBottomCorner, leftBottomCorner)
            backgroundRect = getRoundedRect(
                padding.left.toFloat(),
                padding.top.toFloat(),
                width - padding.right,
                height - padding.bottom,
                radiusCorner,
                radiusCorner,
                leftTopCorner,
                rightTopCorner,
                rightBottomCorner,
                leftBottomCorner
            )
        }

        private fun getRoundedRect(left: Float, top: Float, right: Float, bottom: Float, rxRadius: Float, ryRadius: Float, tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean): Path {
            var rx = rxRadius
            var ry = ryRadius
            val path = Path()
            if (rx < 0) rx = 0f
            if (ry < 0) ry = 0f
            val width = right - left
            val height = bottom - top
            if (rx > width / 2) rx = width / 2
            if (ry > height / 2) ry = height / 2
            val widthMinusCorners = width - 2 * rx
            val heightMinusCorners = height - 2 * ry

            path.moveTo(right, top + ry)
            if (tr)
                path.rQuadTo(0f, -ry, -rx, -ry)//top-right corner
            else {
                path.rLineTo(0f, -ry)
                path.rLineTo(-rx, 0f)
            }
            path.rLineTo(-widthMinusCorners, 0f)
            if (tl)
                path.rQuadTo(-rx, 0f, -rx, ry) //top-left corner
            else {
                path.rLineTo(-rx, 0f)
                path.rLineTo(0f, ry)
            }
            path.rLineTo(0f, heightMinusCorners)

            if (bl)
                path.rQuadTo(0f, ry, rx, ry)//bottom-left corner
            else {
                path.rLineTo(0f, ry)
                path.rLineTo(rx, 0f)
            }

            path.rLineTo(widthMinusCorners, 0f)
            if (br)
                path.rQuadTo(rx, 0f, rx, -ry) //bottom-right corner
            else {
                path.rLineTo(rx, 0f)
                path.rLineTo(0f, -ry)
            }

            path.rLineTo(0f, -heightMinusCorners)
            path.close()
            return path
        }
    }

        /**
     * @param maxShadowAlpha is max shadow alpha for drawable percent
     */
    fun getCircleShadowBackground(context: Context, maxShadowAlpha: Int = 30): Drawable {
        val shadowColor = context.getColorCompatAttr(R.attr.colorShadow)
        val backgroundColor = context.getColorCompatAttr(R.attr.colorBackground)

        val layers = mutableListOf<Drawable>()
        val shadowCount = 5
        val shadowStep = maxShadowAlpha / shadowCount
        for (i in 0 until shadowCount) {
            layers += getShadowLayer(shadowColor, i * shadowStep)
        }

        layers += getShadowLayer(backgroundColor, removePadding = true)

        return LayerDrawable(layers.toTypedArray())
    }

    private fun getShadowLayer(@ColorInt color: Int, @IntRange(from = 0L, to = 255L) alpha: Int = 255, removePadding: Boolean = false): Drawable {
        val drawable = ShapeDrawable()
        drawable.shape = OvalShape()
        drawable.paint.color = color
        drawable.paint.alpha = alpha
        val padding = if (removePadding) 0 else shadowPadding
        drawable.setPadding(padding, padding, padding, padding)
        return drawable
    }
}
