package com.histopgambling.looplock.enforcement

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
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
            setPadding(64, 64, 64, 64)
            setBackgroundColor(Color.rgb(244, 247, 240))
            addView(text("LoopLock", 36f, true))
            addView(text("Protection active", 26f, true))
            addView(text("$targetLabel was blocked from a local commitment rule.", 20f, false))
            addView(
                text(
                    "Commitment ends ${DateFormat.getDateTimeInstance().format(Date(endWallMs))}",
                    16f,
                    false,
                ),
            )
            addView(text("AI did not make this blocking decision.", 16f, false))
            addView(Button(service).apply {
                text = "Return to LoopLock"
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

    private fun text(value: String, size: Float, bold: Boolean) = TextView(service).apply {
        text = value
        textSize = size
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(25, 31, 25))
        setPadding(12, 14, 12, 14)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
}
