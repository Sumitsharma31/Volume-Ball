package com.example.volumeball

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var opacityLabel: TextView
    private lateinit var sizeLabel: TextView
    private var permissionRequestInProgress = false

    // Color choices
    private val themeColors = listOf(
        Pair("Purple", 0xFF7C4DFF.toInt()),
        Pair("Blue", 0xFF00B0FF.toInt()),
        Pair("Emerald", 0xFF00E676.toInt()),
        Pair("Orange", 0xFFFF6D00.toInt()),
        Pair("Crimson", 0xFFFF1744.toInt()),
        Pair("Gold", 0xFFFFD600.toInt())
    )

    // Style choices
    private val styles = listOf(
        Pair("🕹️ Joystick", FloatingBallService.STYLE_JOYSTICK),
        Pair("⚡ Neon Glow", FloatingBallService.STYLE_NEON_GLOW),
        Pair("🫧 Minimal Glass", FloatingBallService.STYLE_MINIMAL_GLASS),
        Pair("👾 Retro Arcade", FloatingBallService.STYLE_RETRO_ARCADE)
    )

    // Ball icon choices
    private val iconStyles = listOf(
        Pair("❖ Geometric", FloatingBallService.ICON_STYLE_GEOMETRIC),
        Pair("🔊 Classic", FloatingBallService.ICON_STYLE_CLASSIC),
        Pair("🎮 Cyber Gamer", FloatingBallService.ICON_STYLE_CYBER),
        Pair("🎧 Music Wave", FloatingBallService.ICON_STYLE_AUDIO)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(FloatingBallService.PREFS, MODE_PRIVATE)
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // System theme colors
        val pageBg = if (isDark) Color.rgb(18, 18, 22) else Color.rgb(246, 245, 250)
        val cardBg = if (isDark) Color.rgb(28, 26, 36) else Color.WHITE
        val cardStroke = if (isDark) Color.rgb(46, 44, 58) else Color.rgb(230, 228, 238)
        val textPrimary = if (isDark) Color.rgb(242, 240, 248) else Color.rgb(28, 26, 34)
        val textSecondary = if (isDark) Color.rgb(162, 158, 176) else Color.rgb(90, 86, 102)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(pageBg)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(36))
        }
        scrollView.addView(root, matchWrap())

        // Header Title
        root.addView(TextView(this).apply {
            text = "Volume Joystick"
            textSize = 28f
            setTextColor(textPrimary)
            typeface = Typeface.DEFAULT_BOLD
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "Interactive floating game thumbstick for system media volume."
            textSize = 15f
            setTextColor(textSecondary)
            setPadding(0, dp(6), 0, dp(14))
        }, matchWrap())

        // -----------------------------------------------------------------------
        // Section: Buy Developer a Coffee (Top Section)
        // -----------------------------------------------------------------------
        val coffeeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = createCardBackground(
                if (isDark) Color.rgb(36, 28, 22) else Color.rgb(255, 248, 240),
                Color.rgb(255, 179, 0),
                dp(14),
                dp(1.2f)
            )
        }

        val coffeeHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        coffeeHeader.addView(TextView(this).apply {
            text = "☕"
            textSize = 22f
            setPadding(0, 0, dp(10), 0)
        })

        val coffeeTitles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        coffeeTitles.addView(TextView(this).apply {
            text = "Buy Developer a Coffee"
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        })
        coffeeTitles.addView(TextView(this).apply {
            text = "Love Volume Ball? Support development & future updates!"
            textSize = 12f
            setTextColor(textSecondary)
        })
        coffeeHeader.addView(coffeeTitles, matchWrap())
        coffeeCard.addView(coffeeHeader, matchWrap())

        val coffeeAmountRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val coffeeAmounts = listOf(
            Pair("☕ ₹49", 49),
            Pair("☕ ₹99", 99),
            Pair("☕ ₹199", 199),
            Pair("☕ ₹499", 499)
        )

        coffeeAmounts.forEach { (label, amount) ->
            val btn = TextView(this).apply {
                text = label
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isDark) Color.rgb(255, 213, 79) else Color.rgb(191, 54, 12))
                setPadding(dp(14), dp(8), dp(14), dp(8))
                background = createCardBackground(
                    if (isDark) Color.rgb(52, 40, 30) else Color.rgb(255, 238, 218),
                    Color.rgb(255, 179, 0),
                    dp(10)
                )
                setOnClickListener {
                    initiateCoffeePayment(amount)
                }
            }
            coffeeAmountRow.addView(btn, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(8)
            })
        }

        val coffeeScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(coffeeAmountRow)
        }
        coffeeCard.addView(coffeeScroll, matchWrap())

        root.addView(coffeeCard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(16)
        })

        // Status Card
        status = TextView(this).apply {
            id = R.id.status_text
            textSize = 14f
            setTextColor(textPrimary)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = createCardBackground(cardBg, cardStroke, dp(14))
        }
        root.addView(status, matchWrap())

        // Start / Stop Main Button
        startButton = Button(this).apply {
            id = R.id.start_stop_button
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = createCardBackground(0xFF7C4DFF.toInt(), 0xFF7C4DFF.toInt(), dp(14))
            setOnClickListener { onStartStopClicked() }
        }
        root.addView(startButton, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(14)
            bottomMargin = dp(24)
        })

        // -----------------------------------------------------------------------
        // Section: Ball Styles
        // -----------------------------------------------------------------------
        root.addView(TextView(this).apply {
            text = "Joystick Style"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, 0, 0, dp(8))
        }, matchWrap())

        val styleScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val styleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(16))
        }
        styleScroll.addView(styleRow)
        root.addView(styleScroll, matchWrap())

        val currentStyle = prefs.getInt(FloatingBallService.KEY_STYLE, FloatingBallService.STYLE_JOYSTICK)
        val styleViews = mutableListOf<TextView>()

        styles.forEach { (name, styleId) ->
            val isSelected = styleId == currentStyle
            val btn = TextView(this).apply {
                text = name
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setTextColor(if (isSelected) Color.WHITE else textPrimary)
                background = if (isSelected) {
                    createCardBackground(0xFF7C4DFF.toInt(), 0xFF7C4DFF.toInt(), dp(12))
                } else {
                    createCardBackground(cardBg, cardStroke, dp(12))
                }
                setOnClickListener {
                    prefs.edit().putInt(FloatingBallService.KEY_STYLE, styleId).apply()
                    styleViews.forEachIndexed { idx, v ->
                        val selected = styles[idx].second == styleId
                        v.setTextColor(if (selected) Color.WHITE else textPrimary)
                        v.background = if (selected) {
                            createCardBackground(0xFF7C4DFF.toInt(), 0xFF7C4DFF.toInt(), dp(12))
                        } else {
                            createCardBackground(cardBg, cardStroke, dp(12))
                        }
                    }
                    notifyServiceConfigChanged()
                }
            }
            styleViews.add(btn)
            styleRow.addView(btn, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(10)
            })
        }

        // -----------------------------------------------------------------------
        // Section: Ball Icon Style
        // -----------------------------------------------------------------------
        root.addView(TextView(this).apply {
            text = "Icons Inside Ball"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, dp(4), 0, dp(8))
        }, matchWrap())

        val iconScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val iconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(16))
        }
        iconScroll.addView(iconRow)
        root.addView(iconScroll, matchWrap())

        val currentIconStyle = prefs.getInt(FloatingBallService.KEY_ICON_STYLE, FloatingBallService.ICON_STYLE_GEOMETRIC)
        val iconViews = mutableListOf<TextView>()

        iconStyles.forEach { (name, iconStyleId) ->
            val isSelected = iconStyleId == currentIconStyle
            val btn = TextView(this).apply {
                text = name
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setTextColor(if (isSelected) Color.WHITE else textPrimary)
                background = if (isSelected) {
                    createCardBackground(0xFF7C4DFF.toInt(), 0xFF7C4DFF.toInt(), dp(12))
                } else {
                    createCardBackground(cardBg, cardStroke, dp(12))
                }
                setOnClickListener {
                    prefs.edit().putInt(FloatingBallService.KEY_ICON_STYLE, iconStyleId).apply()
                    iconViews.forEachIndexed { idx, v ->
                        val selected = iconStyles[idx].second == iconStyleId
                        v.setTextColor(if (selected) Color.WHITE else textPrimary)
                        v.background = if (selected) {
                            createCardBackground(0xFF7C4DFF.toInt(), 0xFF7C4DFF.toInt(), dp(12))
                        } else {
                            createCardBackground(cardBg, cardStroke, dp(12))
                        }
                    }
                    notifyServiceConfigChanged()
                }
            }
            iconViews.add(btn)
            iconRow.addView(btn, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(10)
            })
        }

        // -----------------------------------------------------------------------
        // Section: Accent Color Palette
        // -----------------------------------------------------------------------
        root.addView(TextView(this).apply {
            text = "Accent Color"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, dp(6), 0, dp(8))
        }, matchWrap())

        val colorScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(18))
        }
        colorScroll.addView(colorRow)
        root.addView(colorScroll, matchWrap())

        val currentColor = prefs.getInt(FloatingBallService.KEY_COLOR, FloatingBallService.DEFAULT_COLOR)
        val colorViews = mutableListOf<View>()

        themeColors.forEach { (colorName, colorVal) ->
            val isSelected = colorVal == currentColor
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = if (isSelected) {
                    createCardBackground(cardBg, colorVal, dp(12), dp(2))
                } else {
                    createCardBackground(cardBg, cardStroke, dp(12))
                }

                // Color circle indicator
                addView(View(this@MainActivity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorVal)
                    }
                }, LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                    rightMargin = dp(8)
                })

                addView(TextView(this@MainActivity).apply {
                    text = colorName
                    textSize = 13f
                    setTextColor(textPrimary)
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                })

                setOnClickListener {
                    prefs.edit().putInt(FloatingBallService.KEY_COLOR, colorVal).apply()
                    colorViews.forEachIndexed { idx, v ->
                        val selected = themeColors[idx].second == colorVal
                        v.background = if (selected) {
                            createCardBackground(cardBg, colorVal, dp(12), dp(2))
                        } else {
                            createCardBackground(cardBg, cardStroke, dp(12))
                        }
                    }
                    notifyServiceConfigChanged()
                }
            }
            colorViews.add(chip)
            colorRow.addView(chip, LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(10)
            })
        }

        // -----------------------------------------------------------------------
        // Section: Size Slider
        // -----------------------------------------------------------------------
        val savedSize = prefs.getInt(FloatingBallService.KEY_SIZE, FloatingBallService.DEFAULT_SIZE_DP).coerceIn(48, 88)
        sizeLabel = TextView(this).apply {
            text = "Joystick Size: ${savedSize} dp"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(sizeLabel, matchWrap())

        val sizeSeek = SeekBar(this).apply {
            max = 88 - 48 // Range 48 to 88 dp
            progress = savedSize - 48
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sizeDp = progress + 48
                    sizeLabel.text = "Joystick Size: ${sizeDp} dp"
                    if (fromUser) {
                        prefs.edit().putInt(FloatingBallService.KEY_SIZE, sizeDp).apply()
                        notifyServiceConfigChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(sizeSeek, LinearLayout.LayoutParams(-1, dp(44)).apply {
            bottomMargin = dp(14)
        })

        // -----------------------------------------------------------------------
        // Section: Opacity Slider
        // -----------------------------------------------------------------------
        val savedOpacity = prefs.getInt(FloatingBallService.KEY_OPACITY, 75).coerceIn(25, 100)
        opacityLabel = TextView(this).apply {
            text = "Opacity: $savedOpacity%"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(opacityLabel, matchWrap())

        val opacitySeek = SeekBar(this).apply {
            max = 75 // 25..100
            progress = savedOpacity - 25
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val percent = progress + 25
                    opacityLabel.text = "Opacity: $percent%"
                    if (fromUser) {
                        prefs.edit().putInt(FloatingBallService.KEY_OPACITY, percent).apply()
                        notifyServiceConfigChanged()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(opacitySeek, LinearLayout.LayoutParams(-1, dp(44)).apply {
            bottomMargin = dp(20)
        })

        // -----------------------------------------------------------------------
        // Section: Creator Branding & Profiles Card
        // -----------------------------------------------------------------------
        val creatorCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = createCardBackground(cardBg, cardStroke, dp(14))
        }

        val creatorHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val avatar = TextView(this).apply {
            text = "👨‍💻"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = createCardBackground(if (isDark) Color.rgb(40, 36, 54) else Color.rgb(238, 235, 248), cardStroke, dp(12))
        }
        creatorHeader.addView(avatar, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(12) })

        val creatorTitles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val nameView = TextView(this).apply {
            text = "Sumit Sharma"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
        }
        val subView = TextView(this).apply {
            text = "Creator & Maintainer • Volume Ball"
            textSize = 12.5f
            setTextColor(textSecondary)
        }
        creatorTitles.addView(nameView)
        creatorTitles.addView(subView)
        creatorHeader.addView(creatorTitles, matchWrap())
        creatorCard.addView(creatorHeader, matchWrap())

        // Profile links chips row
        val socialRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }

        fun createSocialChip(label: String, url: String): TextView {
            return TextView(this).apply {
                text = label
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setTextColor(textPrimary)
                background = createCardBackground(if (isDark) Color.rgb(38, 36, 48) else Color.rgb(240, 238, 246), cardStroke, dp(10))
                setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(this@MainActivity, "Could not open: $url", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val githubChip = createSocialChip("🐙 GitHub", "https://github.com/Sumitsharma31")
        val linkedinChip = createSocialChip("💼 LinkedIn", "https://www.linkedin.com/in/sumitsharma31")
        val portfolioChip = createSocialChip("🌐 Portfolio", "https://sumit.dpdns.org/")
        val instagramChip = createSocialChip("📸 Instagram", "https://www.instagram.com/sumit.sharma31/")

        socialRow.addView(githubChip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        socialRow.addView(linkedinChip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        socialRow.addView(portfolioChip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        socialRow.addView(instagramChip, LinearLayout.LayoutParams(-2, -2))

        val socialScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(socialRow)
        }
        creatorCard.addView(socialScroll, matchWrap())
        root.addView(creatorCard, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(16)
        })

        // -----------------------------------------------------------------------
        // Instructions Card (How to Use - after developer profile)
        // -----------------------------------------------------------------------
        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = createCardBackground(cardBg, cardStroke, dp(14))
        }
        infoCard.addView(TextView(this).apply {
            text = "🎮 How to Use Your Joystick"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary)
            setPadding(0, 0, 0, dp(8))
        })
        infoCard.addView(TextView(this).apply {
            text = "• Drag Up or Right: Raise volume (+)\n" +
                   "• Drag Down or Left: Lower volume (−)\n" +
                   "• Hold in direction: Smoothly glides volume\n" +
                   "• Press & Hold: Activates Position Mode (vibrate + golden ring) so you can drag the ball anywhere!"
            textSize = 13.5f
            setTextColor(textSecondary)
            setLineSpacing(dp(4).toFloat(), 1f)
        })
        root.addView(infoCard, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(28)
        })

        setContentView(scrollView)
        applyWindowColors(pageBg, isDark)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        if (permissionRequestInProgress && Settings.canDrawOverlays(this)) {
            permissionRequestInProgress = false
            startOverlay()
        }
    }

    private fun notifyServiceConfigChanged() {
        if (FloatingBallService.isRunning) {
            val intent = Intent(this, FloatingBallService::class.java).apply {
                action = FloatingBallService.ACTION_UPDATE_CONFIG
            }
            startService(intent)
        }
    }

    private fun onStartStopClicked() {
        if (FloatingBallService.isRunning) {
            stopService(Intent(this, FloatingBallService::class.java))
            status.postDelayed({ refreshUi() }, 300)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            permissionRequestInProgress = true
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startOverlay()
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingBallService::class.java))
        } else {
            startService(Intent(this, FloatingBallService::class.java))
        }
        status.postDelayed({ refreshUi() }, 300)
    }

    private fun refreshUi() {
        val running = FloatingBallService.isRunning
        status.text = if (running) {
            "● Running: Joystick is active on your screen"
        } else {
            "○ Stopped: Tap Start to launch the volume joystick"
        }
        startButton.text = if (running) "Stop Volume Joystick" else "Start Volume Joystick"
        val btnColor = if (running) Color.rgb(211, 47, 47) else 0xFF7C4DFF.toInt()
        startButton.background = createCardBackground(btnColor, btnColor, dp(14))
    }

    private fun createCardBackground(fillColor: Int, strokeColor: Int, radius: Int, strokeWidth: Int = dp(1)): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidth, strokeColor)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowColors(bg: Int, isDark: Boolean) {
        try {
            window.statusBarColor = bg
            window.navigationBarColor = bg
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (!isDark) {
                    controller?.setSystemBarsAppearance(
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    controller?.setSystemBarsAppearance(
                        0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            } else {
                if (!isDark) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        } catch (_: Exception) {}
    }



    private fun initiateCoffeePayment(amount: Int) {
        val upiId = "ssumit20kr@ybl"
        val name = "Sumit Sharma"
        val note = "Buy a Coffee for Volume Ball Developer"
        val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode(name)}&am=$amount&cu=INR&tn=${Uri.encode(note)}")
        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)

        try {
            startActivity(Intent.createChooser(upiIntent, "Pay ₹$amount via UPI"))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://sumit.dpdns.org/")))
            } catch (_: Exception) {
                Toast.makeText(this, "Thank you for supporting Volume Ball!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Float) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
