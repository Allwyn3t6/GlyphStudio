package com.allwyn.glyphstudio

import android.app.Activity
import android.content.ComponentName
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager

/** Manual and animated controller for Nothing Phone (2a) Glyph zones. */
class MainActivity : Activity() {
    private lateinit var glyphManager: GlyphManager
    private var sessionOpen = false
    private var managerInitialized = false
    private val selected = BooleanArray(26)
    private val handler = Handler(Looper.getMainLooper())
    private var animationRunning = false
    private var animationStep = 0
    private var animationName = ""
    private var speedMs = 350L
    private lateinit var status: TextView
    private lateinit var zoneChecks: List<CheckBox>

    private val animationTick = object : Runnable {
        override fun run() {
            if (!animationRunning || !sessionOpen) return
            val active = when (animationName) {
                "Chase" -> listOf(animationStep % 24)
                "Bounce" -> listOf(if ((animationStep / 24) % 2 == 0) animationStep % 24 else 23 - (animationStep % 24))
                "Pulse" -> (0 until 24).filter { (it + animationStep) % 3 == 0 }
                "Pairs" -> listOf(animationStep % 24, (animationStep + 12) % 24)
                "Fill" -> (0..(animationStep % 25)).toList()
                else -> listOf(animationStep % 24)
            }
            applyChannels(active)
            animationStep++
            handler.postDelayed(this, speedMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 16, 16)
        window.navigationBarColor = android.graphics.Color.rgb(16, 16, 16)
        buildUi()
        try {
            glyphManager = GlyphManager.getInstance(applicationContext)
            glyphManager.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    runOnUiThread {
                        try {
                            val registered = glyphManager.register(Glyph.DEVICE_23111)
                            if (registered) {
                                glyphManager.openSession()
                                sessionOpen = true
                                status.text = "Connected • Phone (2a) • 26 zones ready"
                            } else {
                                status.text = "Glyph service connected, but registration was denied. Enable Glyph debug mode."
                            }
                        } catch (e: Exception) {
                            sessionOpen = false
                            status.text = "Glyph setup failed: \${e.message ?: "unknown error"}"
                        }
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    runOnUiThread {
                        sessionOpen = false
                        status.text = "Glyph service disconnected"
                    }
                }
            })
            managerInitialized = true
        } catch (e: Exception) {
            status.text = "Unable to initialise Glyph SDK: \${e.message ?: "unknown error"}"
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundColor(0xFF101010.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(TextView(this).apply {
            text = "GLYPH STUDIO"
            textSize = 27f
            setTextColor(0xFFFFFFFF.toInt())
            letterSpacing = .12f
        })
        content.addView(TextView(this).apply {
            text = "NOTHING PHONE (2a)  /  MANUAL + ANIMATION"
            textSize = 10f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(0, 5, 0, 16)
        })
        status = TextView(this).apply {
            text = "Connecting to Nothing Glyph service…"
            textSize = 12f
            setTextColor(0xFFB8B8B8.toInt())
            setPadding(0, 8, 0, 16)
        }
        content.addView(status)
        content.addView(section("INDIVIDUAL ZONES  ·  C1–C24"))

        val grid = GridLayout(this).apply { columnCount = 4 }
        val checks = ArrayList<CheckBox>()
        for (i in 0 until 24) {
            val cb = CheckBox(this).apply {
                text = "C\${i + 1}"
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                buttonTintList = ColorStateList.valueOf(0xFFE5E5E5.toInt())
                setOnCheckedChangeListener { _, checked -> selected[i] = checked }
            }
            checks.add(cb)
            grid.addView(cb, GridLayout.LayoutParams().apply {
                width = 0
                height = 52
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        content.addView(grid)

        content.addView(section("ADDITIONAL ZONES"))
        val ab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in 24..25) {
            val cb = CheckBox(this).apply {
                text = if (i == 24) "B" else "A"
                textSize = 15f
                setTextColor(0xFFFFFFFF.toInt())
                buttonTintList = ColorStateList.valueOf(0xFFE5E5E5.toInt())
                setOnCheckedChangeListener { _, checked -> selected[i] = checked }
            }
            checks.add(cb)
            ab.addView(cb, LinearLayout.LayoutParams(0, 52, 1f))
        }
        content.addView(ab)
        zoneChecks = checks

        content.addView(actionButton("APPLY SELECTED ZONES") {
            stopAnimation()
            applyChannels(selected.indices.filter { selected[it] })
            status.text = "Applied \${selected.count { it }} selected zones"
        })
        content.addView(actionButton("ALL ON") {
            stopAnimation()
            selected.fill(true)
            refreshChecks()
            applyChannels((0..25).toList())
            status.text = "All 26 zones on"
        })
        content.addView(actionButton("ALL OFF") {
            stopAnimation()
            selected.fill(false)
            refreshChecks()
            turnOff()
        })
        content.addView(actionButton("SAVE CURRENT PATTERN") { savePattern() })
        content.addView(actionButton("LOAD SAVED PATTERN") { loadPattern() })

        content.addView(section("ANIMATION STUDIO"))
        content.addView(TextView(this).apply {
            text = "Choose an effect"
            textSize = 12f
            setTextColor(0xFFBBBBBB.toInt())
            setPadding(0, 0, 0, 8)
        })
        listOf("Chase", "Bounce", "Pulse", "Pairs", "Fill").forEach { name ->
            content.addView(actionButton("▶  $name") { startAnimation(name) })
        }
        content.addView(TextView(this).apply {
            text = "Animation speed"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 14, 0, 4)
        })
        val speed = SeekBar(this).apply {
            max = 900
            progress = 650
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    speedMs = (1000 - progress).toLong().coerceAtLeast(100)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        content.addView(speed)
        content.addView(actionButton("STOP ANIMATION") {
            stopAnimation()
            status.text = "Animation stopped"
        })
        content.addView(actionButton("TURN OFF GLYPH") {
            stopAnimation()
            selected.fill(false)
            refreshChecks()
            turnOff()
        })
        content.addView(TextView(this).apply {
            text = "Glyph LEDs are monochrome. This app controls illumination zones, not RGB colour. Keep the app in the foreground while controlling Glyphs."
            textSize = 11f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 18, 0, 20)
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun section(label: String) = TextView(this).apply {
        text = label
        textSize = 11f
        letterSpacing = .08f
        setTextColor(0xFFE35B5B.toInt())
        setPadding(0, 18, 0, 8)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = ColorStateList.valueOf(0xFF292929.toInt())
        setOnClickListener {
            if (!sessionOpen) toast("Glyph service is not ready. Enable Glyph debug mode and reopen the app.") else action()
        }
    }

    private fun refreshChecks() {
        zoneChecks.forEachIndexed { index, checkBox -> checkBox.isChecked = selected[index] }
    }

    /** Clear first because toggle() is a toggle API, not an absolute set API. */
    private fun applyChannels(channels: List<Int>) {
        if (!sessionOpen) return
        try {
            glyphManager.turnOff()
            if (channels.isEmpty()) return
            val builder = GlyphFrame.Builder(Glyph.DEVICE_23111)
            channels.distinct().filter { it in 0..25 }.forEach { builder.buildChannel(it) }
            glyphManager.toggle(builder.build())
        } catch (e: Exception) {
            toast("Glyph command failed: \${e.message ?: "unknown error"}")
        }
    }

    private fun startAnimation(name: String) {
        stopAnimation()
        animationName = name
        animationStep = 0
        animationRunning = true
        status.text = "Playing $name animation"
        handler.post(animationTick)
    }

    private fun stopAnimation() {
        animationRunning = false
        handler.removeCallbacks(animationTick)
    }

    private fun turnOff() {
        try {
            if (sessionOpen) glyphManager.turnOff()
            status.text = "All Glyphs off"
        } catch (e: Exception) {
            toast(e.message ?: "Unable to turn off Glyphs")
        }
    }

    private fun savePattern() {
        val encoded = selected.joinToString("") { if (it) "1" else "0" }
        getSharedPreferences("glyph_studio", MODE_PRIVATE).edit().putString("saved_pattern", encoded).apply()
        toast("Pattern saved on this phone")
    }

    private fun loadPattern() {
        val encoded = getSharedPreferences("glyph_studio", MODE_PRIVATE).getString("saved_pattern", null)
        if (encoded == null || encoded.length != 26) {
            toast("No saved pattern found")
            return
        }
        encoded.forEachIndexed { index, value -> selected[index] = value == '1' }
        refreshChecks()
        stopAnimation()
        applyChannels(selected.indices.filter { selected[it] })
        status.text = "Saved pattern loaded"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStop() {
        stopAnimation()
        super.onStop()
    }

    override fun onDestroy() {
        stopAnimation()
        try { if (sessionOpen) glyphManager.closeSession() } catch (_: Exception) { }
        try { if (managerInitialized) glyphManager.unInit() } catch (_: Exception) { }
        super.onDestroy()
    }
}