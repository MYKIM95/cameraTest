package com.example.cameratest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var guidePaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    private val _isInGuideLine = MutableLiveData(false)
    val isInGuideLine : LiveData<Boolean> =  _isInGuideLine

    init {
        initPaints()
    }

    fun clear() {
        results = null
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        guidePaint.color = ContextCompat.getColor(context!!, R.color.icActive)
        guidePaint.strokeWidth = 8F
        guidePaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results?.let {
            for (detection in it.detections()) {
                val boundingBox = detection.boundingBox()

                val top = boundingBox.top * scaleFactor * 0.75F
                val bottom = boundingBox.bottom * scaleFactor
                val left = boundingBox.left * scaleFactor
                val right = boundingBox.right * scaleFactor

                // Draw bounding box guideline
                val guideTop = 200f
                val guideBottom = 1050f
                val guideLeft = 150f
                val guideRight = 900f

                _isInGuideLine.value = top > guideTop && bottom < guideBottom && left > guideLeft && right < guideRight
                boxPaint.color = ContextCompat.getColor(context!!, if(isInGuideLine.value!!) R.color.isInGuideLine else R.color.isOutGuideLine)

                // Draw bounding box around detected faces
                val drawableRect = RectF(left, top, right, bottom)
                canvas.drawRect(drawableRect, boxPaint)

                val drawableGuideRect = RectF(guideLeft, guideTop, guideRight, guideBottom)
                canvas.drawRect(drawableGuideRect, guidePaint)

                // Create text to display alongside detected faces
                val drawableText =
                    detection.categories()[0].categoryName() +
                            " " +
                            String.format(
                                "%.2f",
                                detection.categories()[0].score()
                            )

                // Draw rect behind display text
                textBackgroundPaint.getTextBounds(
                    drawableText,
                    0,
                    drawableText.length,
                    bounds
                )
                val textWidth = bounds.width()
                val textHeight = bounds.height()
                canvas.drawRect(
                    left,
                    top,
                    left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                    top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                    textBackgroundPaint
                )

                // Draw text for detected face
                canvas.drawText(
                    drawableText,
                    left,
                    top + bounds.height(),
                    textPaint
                )
            }
        }
    }

    fun setResults(
        detectionResults: FaceDetectorResult,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        // Images, videos and camera live streams are displayed in FIT_START mode. So we need to scale
        // up the bounding box to match with the size that the images/videos/live streams being
        // displayed.
        scaleFactor = min(width * 1f / imageWidth, height * 1f / imageHeight)

        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}