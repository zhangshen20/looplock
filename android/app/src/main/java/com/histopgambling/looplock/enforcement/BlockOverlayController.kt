package com.histopgambling.looplock.enforcement

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.histopgambling.looplock.MainActivity
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import java.text.DateFormat
import java.util.Date

class BlockOverlayController(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlay: View? = null

    fun show(packageName: String, endWallMs: Long) {
        remove()
        val targetLabel = if (packageName == LUCKYMIRROR_PACKAGE) "LuckyMirror Demo" else "BetBurst Demo"
        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(44), dp(28), dp(36))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(7, 23, 45), Color.rgb(10, 47, 70), Color.rgb(8, 25, 47)),
            )
            addView(pill("LOOPLOCK  ·  LOCAL PROTECTION"))
            addView(Space(service).apply { layoutParams = LinearLayout.LayoutParams(1, dp(24)) })
            addView(ProtectionMarkView(service).apply {
                contentDescription = "LoopLock protection mark"
                layoutParams = LinearLayout.LayoutParams(dp(184), dp(184))
            })
            addView(text("Protected pause", 34f, true, Color.rgb(46, 196, 182)).apply {
                isAccessibilityHeading = true
            })
            addView(text("$targetLabel is paused by the commitment you chose.", 20f, false, Color.WHITE))
            addView(infoCard(
                "Commitment ends",
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(endWallMs)),
            ))
            addView(infoCard(
                "Why this happened",
                "A deterministic rule stored on this device blocked the launch. AI did not make this decision.",
            ))
            addView(Space(service).apply { layoutParams = LinearLayout.LayoutParams(1, dp(12)) })
            addView(Button(service).apply {
                text = "Return to LoopLock"
                textSize = 17f
                isAllCaps = false
                setTextColor(Color.rgb(7, 23, 45))
                setTypeface(typeface, Typeface.BOLD)
                minHeight = dp(58)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(18).toFloat()
                    setColor(Color.rgb(46, 196, 182))
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply {
                    topMargin = dp(8)
                }
                setOnClickListener {
                    remove()
                    service.startActivity(
                        Intent(service, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        },
                    )
                }
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(container, params)
        overlay = container
    }

    fun remove() {
        overlay?.let { view -> runCatching { windowManager.removeView(view) } }
        overlay = null
    }

    private fun text(value: String, size: Float, bold: Boolean, color: Int) = TextView(service).apply {
        text = value
        textSize = size
        gravity = Gravity.CENTER
        setTextColor(color)
        setPadding(dp(8), dp(9), dp(8), dp(9))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun pill(value: String) = text(value, 12f, true, Color.rgb(118, 199, 255)).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(50).toFloat()
            setColor(Color.argb(24, 255, 255, 255))
            setStroke(dp(1), Color.argb(28, 255, 255, 255))
        }
        setPadding(dp(14), dp(8), dp(14), dp(8))
    }

    private fun infoCard(label: String, value: String) = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(14), dp(18), dp(14))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.argb(20, 255, 255, 255))
            setStroke(dp(1), Color.argb(25, 255, 255, 255))
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        }
        addView(text(label.uppercase(), 12f, true, Color.rgb(183, 164, 255)))
        addView(text(value, 16f, false, Color.rgb(235, 244, 250)))
    }

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private class ProtectionMarkView(service: AccessibilityService) : View(service) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val min = width.coerceAtMost(height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val stroke = min * 0.065f
            val inset = stroke
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.strokeCap = Paint.Cap.ROUND
            paint.shader = SweepGradient(
                cx,
                cy,
                intArrayOf(
                    Color.rgb(46, 196, 182),
                    Color.rgb(118, 199, 255),
                    Color.rgb(183, 164, 255),
                    Color.rgb(46, 196, 182),
                ),
                null,
            )
            canvas.drawArc(RectF(inset, inset, width - inset, height - inset), -72f, 304f, false, paint)
            paint.shader = null

            val r = min * 0.22f
            val shield = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.78f, cy - r * 0.58f)
                lineTo(cx + r * 0.68f, cy + r * 0.35f)
                quadTo(cx, cy + r * 1.05f, cx - r * 0.68f, cy + r * 0.35f)
                lineTo(cx - r * 0.78f, cy - r * 0.58f)
                close()
            }
            paint.color = Color.rgb(46, 196, 182)
            paint.strokeWidth = stroke * 0.55f
            canvas.drawPath(shield, paint)
            paint.color = Color.WHITE
            canvas.drawLine(cx - r * 0.34f, cy, cx - r * 0.05f, cy + r * 0.30f, paint)
            canvas.drawLine(cx - r * 0.05f, cy + r * 0.30f, cx + r * 0.48f, cy - r * 0.28f, paint)
        }
    }
}
