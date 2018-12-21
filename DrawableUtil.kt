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

        return ShapeDrawable().apply {
            shape = RoundedShadow(
                backgroundColor,
                shadowColor,
                leftTopCorner,
                rightTopCorner,
                rightBottomCorner,
                leftBottomCorner,
                radiusCorner
            )
            setPadding(padding.left, padding.top, padding.right, padding.bottom)
        }
    }

    private const val COUNT_LAYERS = 5

    internal class RoundedShadow constructor(
        @ColorInt private val backgroundColor: Int,
        @ColorInt private val shadowColor: Int,
        private val leftTopCorner: Boolean,
        private val rightTopCorner: Boolean,
        private val rightBottomCorner: Boolean,
        private val leftBottomCorner: Boolean,
        private val radiusCorner: Float
    ) : RectShape() {

        private val shadowPaint: Paint = Paint().apply {
            isAntiAlias = true
            style = Style.STROKE
        }

        private val backgroundPaint = Paint().apply {
            style = Style.FILL
            color = backgroundColor
        }

        private val colors = IntArray(COUNT_LAYERS) { position ->
            ColorUtils.setAlphaComponent(shadowColor, 5 * position)
        }

        private val paths = mutableListOf<Path>()
        private val backgroundPath = Path()

        private val shadowRadius
            get() = shadowPadding

        init {
            updatePaths()
        }

        override fun onResize(width: Float, height: Float) {
            super.onResize(width, height)
            updatePaths()
        }

        override fun draw(canvas: Canvas, paint: Paint) {
            paths.forEachIndexed { index, path ->
                shadowPaint.color = colors[index]
                canvas.drawPath(path, shadowPaint)
            }
            canvas.drawPath(backgroundPath, backgroundPaint)
        }

        private fun updatePaths() {
            val width = rect().width()
            val height = rect().height()

            paths.clear()
            val shadowStep = shadowRadius.toFloat() / COUNT_LAYERS
            for (i in 0 until COUNT_LAYERS) {
                val rectF = RectF(0f + shadowStep * i, 0f + shadowStep * i, width - shadowStep * i, height - shadowStep * i)
                val path = CanvasUtils.getRoundedRectPath(rectF, radiusCorner, radiusCorner, leftTopCorner, rightTopCorner, rightBottomCorner, leftBottomCorner)
                paths.add(path)
            }
            shadowPaint.strokeWidth = shadowStep

            val backgroundPath = CanvasUtils.getRoundedRectPath(
                RectF(shadowRadius.toFloat(), shadowRadius.toFloat(), width - shadowRadius, height - shadowRadius),
                radiusCorner,
                radiusCorner,
                leftTopCorner,
                rightTopCorner,
                rightBottomCorner,
                leftBottomCorner
            )
            this.backgroundPath.set(backgroundPath)
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
