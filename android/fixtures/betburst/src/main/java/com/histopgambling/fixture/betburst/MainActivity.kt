package com.histopgambling.fixture.betburst

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "BetBurst Demo"
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                setBackgroundColor(Color.rgb(255, 245, 236))
                addView(label("BetBurst Demo", 30f))
                addView(label("Harmless local fixture — no betting, payments, accounts, ads, analytics, or network access.", 18f))
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

