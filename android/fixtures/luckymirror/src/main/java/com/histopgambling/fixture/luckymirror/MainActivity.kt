package com.histopgambling.fixture.luckymirror

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "LuckyMirror Demo"
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                setBackgroundColor(Color.rgb(238, 246, 255))
                addView(label("LuckyMirror Demo", 30f))
                addView(label("Harmless workaround fixture — installed only to prove the one-way protection ratchet.", 18f))
                addView(label("No betting, payments, accounts, ads, analytics, or network access.", 16f))
            },
        )
    }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        gravity = Gravity.CENTER
        setPadding(12, 20, 12, 20)
    }
}

