package com.example.volumeball

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager

    // Floating Joystick
    private var joystickView: JoystickBallView? = null
    private var joystickParams: WindowManager.LayoutParams? = null

    // Volume observers
    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            updateJoystickVolume()
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateJoystickVolume()
        }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) showJoystick()

        try {
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver
            )
            val filter = IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(volumeReceiver, filter)
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_UPDATE_CONFIG -> applyConfig()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(volumeObserver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(volumeReceiver)
        } catch (_: Exception) {}

        try {
            removeJoystick()
        } finally {
            isRunning = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------------
    // Joystick Display & Config
    // ---------------------------------------------------------------------------

    private fun showJoystick() {
        if (joystickView != null || !Settings.canDrawOverlays(this)) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val sizeDp = prefs.getInt(KEY_SIZE, DEFAULT_SIZE_DP).coerceIn(48, 88)
        val sizePx = dp(sizeDp)
        val screen = screenSize()
        val defaultX = screen.first - sizePx - dp(16)
        val x = prefs.getInt(KEY_X, defaultX).coerceIn(0, max(0, screen.first - sizePx))
        val y = prefs.getInt(KEY_Y, screen.second / 2).coerceIn(0, max(0, screen.second - sizePx))

        val opacity = prefs.getInt(KEY_OPACITY, 75).coerceIn(25, 100) / 100f
        val color = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        val style = prefs.getInt(KEY_STYLE, STYLE_JOYSTICK)
        val iconStyle = prefs.getInt(KEY_ICON_STYLE, ICON_STYLE_GEOMETRIC)

        val params = baseOverlayParams(sizePx, sizePx).apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            this.x = x
            this.y = y
        }

        val joystick = JoystickBallView(this).apply {
            alpha = opacity
            applyCustomization(style, color, iconStyle)
            onVolumeAdjust = { direction ->
                adjustVolume(direction)
            }
            onPositionChanged = { newX, newY ->
                params.x = newX
                params.y = newY
                try {
                    windowManager.updateViewLayout(this, params)
                } catch (_: Exception) {}
            }
            onPositionSave = { finalX, finalY ->
                prefs.edit().putInt(KEY_X, finalX).putInt(KEY_Y, finalY).apply()
            }
            onTouchStart = {
                updateJoystickVolume()
            }
        }

        joystickView = joystick
        joystickParams = params
        updateJoystickVolume()
        windowManager.addView(joystick, params)
    }

    private fun applyConfig() {
        val view = joystickView ?: return
        val params = joystickParams ?: return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val sizeDp = prefs.getInt(KEY_SIZE, DEFAULT_SIZE_DP).coerceIn(48, 88)
        val sizePx = dp(sizeDp)
        val opacity = prefs.getInt(KEY_OPACITY, 75).coerceIn(25, 100) / 100f
        val color = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        val style = prefs.getInt(KEY_STYLE, STYLE_JOYSTICK)
        val iconStyle = prefs.getInt(KEY_ICON_STYLE, ICON_STYLE_GEOMETRIC)

        view.alpha = opacity
        view.applyCustomization(style, color, iconStyle)

        if (params.width != sizePx || params.height != sizePx) {
            params.width = sizePx
            params.height = sizePx
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    private fun removeJoystick() {
        try {
            joystickView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        joystickView = null
        joystickParams = null
    }

    /**
     * Determines active stream matching system volume buttons:
     * - In cellular phone call or VoIP call (WhatsApp, Meet, etc.) -> STREAM_VOICE_CALL
     * - Incoming ringing -> STREAM_RING
     * - Otherwise -> STREAM_MUSIC
     */
    private fun getActiveStreamType(): Int {
        return when (audioManager.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION -> AudioManager.STREAM_VOICE_CALL
            AudioManager.MODE_RINGTONE -> AudioManager.STREAM_RING
            else -> AudioManager.STREAM_MUSIC
        }
    }

    private fun updateJoystickVolume() {
        val stream = getActiveStreamType()
        val current = audioManager.getStreamVolume(stream)
        val maxVol = max(1, audioManager.getStreamMaxVolume(stream))
        joystickView?.setVolumeState(current, maxVol, stream)
    }

    private fun adjustVolume(direction: Int) {
        val stream = getActiveStreamType()
        val flags = AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
        try {
            // Uses system default audio router (same as physical hardware volume keys)
            audioManager.adjustSuggestedStreamVolume(direction, stream, flags)
        } catch (_: Exception) {
            audioManager.adjustStreamVolume(stream, direction, flags)
        }
        updateJoystickVolume()
    }

    // ---------------------------------------------------------------------------
    // WindowManager helpers
    // ---------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun baseOverlayParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width, height,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        0,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    // ---------------------------------------------------------------------------
    // Notification
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Joystick Control",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingBallService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appIntent = PendingIntent.getActivity(
            this, 2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Volume Joystick Active")
                .setContentText("Drag joystick Up/Right to raise, Down/Left to lower volume. Hold to reposition.")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(appIntent)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                        "Stop",
                        stopIntent
                    ).build()
                )
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Volume Joystick Active")
                .setContentText("Drag joystick Up/Right to raise, Down/Left to lower volume. Hold to reposition.")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(appIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
                .setOngoing(true)
                .build()
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars() or
                        android.view.WindowInsets.Type.displayCutout()
            )
            val bounds = metrics.bounds
            Pair(
                bounds.width() - insets.left - insets.right,
                bounds.height() - insets.top - insets.bottom
            )
        } else {
            @Suppress("DEPRECATION")
            val dm = resources.displayMetrics
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        const val PREFS = "volume_ball_preferences"
        const val KEY_X = "ball_x"
        const val KEY_Y = "ball_y"
        const val KEY_OPACITY = "ball_opacity"
        const val KEY_SIZE = "ball_size"
        const val KEY_COLOR = "ball_color"
        const val KEY_STYLE = "ball_style"
        const val KEY_ICON_STYLE = "ball_icon_style"

        const val DEFAULT_SIZE_DP = 68
        const val DEFAULT_COLOR = 0xFF7C4DFF.toInt() // Cyber Purple

        const val STYLE_JOYSTICK = 0
        const val STYLE_NEON_GLOW = 1
        const val STYLE_MINIMAL_GLASS = 2
        const val STYLE_RETRO_ARCADE = 3

        const val ICON_STYLE_GEOMETRIC = 0 // Minimalist Geometric (○, ●, ❖, ✦)
        const val ICON_STYLE_CLASSIC = 1   // Classic Volume (🔇, 🔈, 🔉, 🔊)
        const val ICON_STYLE_CYBER = 2     // Cyber Gamer (💤, ⚡, 🎮, 👾)
        const val ICON_STYLE_AUDIO = 3     // Sound Wave (•, ♪, ♫, 🎧)

        const val ACTION_STOP = "com.example.volumeball.STOP"
        const val ACTION_UPDATE_CONFIG = "com.example.volumeball.UPDATE_CONFIG"

        private const val CHANNEL_ID = "floating_volume_control"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}

// ---------------------------------------------------------------------------
// JoystickBallView: Highly Customizable Game Joystick
// ---------------------------------------------------------------------------

private class JoystickBallView(context: Context) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val repositionPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Current knob displacement from center (pixels)
    private var knobX = 0f
    private var knobY = 0f

    // Spring-back animator
    private var springAnimator: ValueAnimator? = null

    // Callbacks
    var onVolumeAdjust: ((Int) -> Unit)? = null
    var onPositionChanged: ((Int, Int) -> Unit)? = null
    var onPositionSave: ((Int, Int) -> Unit)? = null
    var onTouchStart: (() -> Unit)? = null

    // Volume state
    private var currentVolume = 0
    private var maxVolume = 1
    private var volumeIcon = "♪"

    // Customization state
    private var currentStyle = FloatingBallService.STYLE_JOYSTICK
    private var themeColor = FloatingBallService.DEFAULT_COLOR
    private var currentIconStyle = FloatingBallService.ICON_STYLE_GEOMETRIC
    private var currentStream = AudioManager.STREAM_MUSIC

    // Touch & Move state
    private var downRawX = 0f
    private var downRawY = 0f
    private var windowStartX = 0
    private var windowStartY = 0
    private var isDraggingPosition = false
    private var hasMovedBeyondDeadzone = false

    // Auto-repeat volume adjustments when held in direction (steady, controlled cadence)
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var activeDirection = 0
    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (activeDirection != 0 && !isDraggingPosition) {
                onVolumeAdjust?.invoke(activeDirection)
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                repeatHandler.postDelayed(this, 350L) // 350ms repeat interval to prevent fast runaway
            }
        }
    }

    // Hold detector for position change mode (snappy 350ms hold)
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!hasMovedBeyondDeadzone) {
            isDraggingPosition = true
            stopAutoRepeat()
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            knobX = 0f
            knobY = 0f
            invalidate()
        }
    }

    init {
        textPaint.apply {
            textSize = dp(14).toFloat()
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        hintPaint.apply {
            textSize = dp(11).toFloat()
            textAlign = Paint.Align.CENTER
            color = Color.argb(190, 255, 255, 255)
            typeface = Typeface.DEFAULT_BOLD
        }
        repositionPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
            color = Color.rgb(255, 215, 64) // Golden glow when moving
            pathEffect = DashPathEffect(floatArrayOf(dp(6).toFloat(), dp(4).toFloat()), 0f)
        }
    }

    fun applyCustomization(style: Int, color: Int, iconStyle: Int = currentIconStyle) {
        currentStyle = style
        themeColor = color
        currentIconStyle = iconStyle
        updateVolumeIcon()
        invalidate()
    }

    fun setVolumeState(volume: Int, maxVol: Int, streamType: Int = AudioManager.STREAM_MUSIC) {
        currentVolume = volume
        maxVolume = max(1, maxVol)
        currentStream = streamType
        updateVolumeIcon()
        invalidate()
    }

    private fun updateVolumeIcon() {
        val pct = (currentVolume * 100f / maxVolume).toInt()
        volumeIcon = when (currentStream) {
            AudioManager.STREAM_VOICE_CALL -> "📞"
            AudioManager.STREAM_RING -> "🔔"
            else -> when (currentIconStyle) {
                FloatingBallService.ICON_STYLE_GEOMETRIC -> when {
                    currentVolume == 0 -> "○"
                    pct < 35 -> "●"
                    pct < 70 -> "❖"
                    else -> "✦"
                }
                FloatingBallService.ICON_STYLE_CYBER -> when {
                    currentVolume == 0 -> "💤"
                    pct < 35 -> "⚡"
                    pct < 70 -> "🎮"
                    else -> "👾"
                }
                FloatingBallService.ICON_STYLE_AUDIO -> when {
                    currentVolume == 0 -> "•"
                    pct < 35 -> "♪"
                    pct < 70 -> "♫"
                    else -> "🎧"
                }
                else -> when {
                    currentVolume == 0 -> "🔇"
                    pct < 35 -> "🔈"
                    pct < 70 -> "🔉"
                    else -> "🔊"
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width / 2f) - dp(3)
        val knobRadius = (width * 0.28f)

        // Draw based on selected style
        when (currentStyle) {
            FloatingBallService.STYLE_NEON_GLOW -> drawNeonStyle(canvas, cx, cy, baseRadius, knobRadius)
            FloatingBallService.STYLE_MINIMAL_GLASS -> drawMinimalGlassStyle(canvas, cx, cy, baseRadius, knobRadius)
            FloatingBallService.STYLE_RETRO_ARCADE -> drawRetroArcadeStyle(canvas, cx, cy, baseRadius, knobRadius)
            else -> drawJoystickStyle(canvas, cx, cy, baseRadius, knobRadius)
        }

        // 2-Second Hold Position Change Mode Highlight
        if (isDraggingPosition) {
            canvas.drawCircle(cx, cy, baseRadius, repositionPaint)
            textPaint.textSize = dp(10).toFloat()
            canvas.drawText("MOVE", cx, cy - baseRadius + dp(12), textPaint)
        }
    }

    // ---------------------------------------------------------------------------
    // Style Renderers
    // ---------------------------------------------------------------------------

    private fun drawJoystickStyle(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float, knobRadius: Float) {
        // Base ring
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.argb(220, 26, 24, 34)
        canvas.drawCircle(cx, cy, baseRadius, bgPaint)

        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(2).toFloat()
        rimPaint.color = themeColor
        canvas.drawCircle(cx, cy, baseRadius, rimPaint)

        // Directional ticks
        if (!isDraggingPosition) {
            val hintDist = baseRadius - dp(6)
            canvas.drawText("+", cx, cy - hintDist + dp(4), hintPaint)
            canvas.drawText("−", cx, cy + hintDist + dp(4), hintPaint)
            canvas.drawText("−", cx - hintDist, cy + dp(4), hintPaint)
            canvas.drawText("+", cx + hintDist, cy + dp(4), hintPaint)
        }

        // Thumbstick knob
        val stickX = cx + knobX
        val stickY = cy + knobY
        knobPaint.style = Paint.Style.FILL
        knobPaint.shader = RadialGradient(
            stickX, stickY, knobRadius,
            intArrayOf(themeColor, darkenColor(themeColor, 0.6f)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(stickX, stickY, knobRadius, knobPaint)

        knobBorderPaint.style = Paint.Style.STROKE
        knobBorderPaint.strokeWidth = dp(1.5f).toFloat()
        knobBorderPaint.color = Color.argb(220, 255, 255, 255)
        canvas.drawCircle(stickX, stickY, knobRadius, knobBorderPaint)

        drawCenterIcon(canvas, stickX, stickY)
    }

    private fun drawNeonStyle(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float, knobRadius: Float) {
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.argb(240, 10, 10, 15)
        canvas.drawCircle(cx, cy, baseRadius, bgPaint)

        // Neon Glow Outer Rings
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(3).toFloat()
        rimPaint.color = themeColor
        canvas.drawCircle(cx, cy, baseRadius, rimPaint)

        // Reticle cross lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            strokeWidth = dp(1.2f).toFloat()
        }
        canvas.drawLine(cx - baseRadius + dp(4), cy, cx + baseRadius - dp(4), cy, linePaint)
        canvas.drawLine(cx, cy - baseRadius + dp(4), cx, cy + baseRadius - dp(4), linePaint)

        // Thumbstick
        val stickX = cx + knobX
        val stickY = cy + knobY
        knobPaint.style = Paint.Style.FILL
        knobPaint.shader = null
        knobPaint.color = themeColor
        canvas.drawCircle(stickX, stickY, knobRadius, knobPaint)

        knobBorderPaint.style = Paint.Style.STROKE
        knobBorderPaint.strokeWidth = dp(2).toFloat()
        knobBorderPaint.color = Color.WHITE
        canvas.drawCircle(stickX, stickY, knobRadius, knobBorderPaint)

        drawCenterIcon(canvas, stickX, stickY)
    }

    private fun drawMinimalGlassStyle(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float, knobRadius: Float) {
        // Frosted Glass Base
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.argb(140, 45, 45, 55)
        canvas.drawCircle(cx, cy, baseRadius, bgPaint)

        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(1.5f).toFloat()
        rimPaint.color = Color.argb(180, 255, 255, 255)
        canvas.drawCircle(cx, cy, baseRadius, rimPaint)

        val stickX = cx + knobX
        val stickY = cy + knobY
        knobPaint.style = Paint.Style.FILL
        knobPaint.shader = null
        knobPaint.color = themeColor
        canvas.drawCircle(stickX, stickY, knobRadius, knobPaint)

        knobBorderPaint.style = Paint.Style.STROKE
        knobBorderPaint.strokeWidth = dp(1).toFloat()
        knobBorderPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawCircle(stickX, stickY, knobRadius, knobBorderPaint)

        drawCenterIcon(canvas, stickX, stickY)
    }

    private fun drawRetroArcadeStyle(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float, knobRadius: Float) {
        // Segmented Retro Bezel
        bgPaint.style = Paint.Style.FILL
        bgPaint.color = Color.rgb(20, 20, 25)
        canvas.drawCircle(cx, cy, baseRadius, bgPaint)

        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(4).toFloat()
        rimPaint.color = themeColor
        canvas.drawCircle(cx, cy, baseRadius, rimPaint)

        // Retro arcade thumbstick (ball-top)
        val stickX = cx + knobX
        val stickY = cy + knobY
        knobPaint.style = Paint.Style.FILL
        knobPaint.shader = null
        knobPaint.color = themeColor
        canvas.drawCircle(stickX, stickY, knobRadius, knobPaint)

        knobBorderPaint.style = Paint.Style.STROKE
        knobBorderPaint.strokeWidth = dp(2).toFloat()
        knobBorderPaint.color = Color.rgb(255, 240, 100)
        canvas.drawCircle(stickX, stickY, knobRadius, knobBorderPaint)

        drawCenterIcon(canvas, stickX, stickY)
    }

    private fun drawCenterIcon(canvas: Canvas, x: Float, y: Float) {
        textPaint.textSize = dp(13).toFloat()
        val textY = y - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(volumeIcon, x, textY, textPaint)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r, g, b)
    }

    // ---------------------------------------------------------------------------
    // Touch Interaction
    // ---------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                springAnimator?.cancel()
                onTouchStart?.invoke()
                downRawX = event.rawX
                downRawY = event.rawY
                val loc = IntArray(2)
                getLocationOnScreen(loc)
                windowStartX = loc[0]
                windowStartY = loc[1]
                isDraggingPosition = false
                hasMovedBeyondDeadzone = false

                // Short hold (350ms) activates Position Change Mode
                longPressHandler.postDelayed(longPressRunnable, 350L)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val dist = sqrt(dx * dx + dy * dy)

                if (dist > dp(8)) {
                    hasMovedBeyondDeadzone = true
                    if (!isDraggingPosition) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                }

                if (isDraggingPosition) {
                    // Repositioning Mode
                    val screenW = resources.displayMetrics.widthPixels
                    val screenH = resources.displayMetrics.heightPixels
                    val newX = (windowStartX + dx.toInt()).coerceIn(0, max(0, screenW - width))
                    val newY = (windowStartY + dy.toInt()).coerceIn(0, max(0, screenH - height))
                    onPositionChanged?.invoke(newX, newY)
                } else {
                    // Joystick deflection logic
                    val maxTravel = (width * 0.25f)
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    val travel = min(dist, maxTravel)
                    knobX = (cos(angle) * travel).toFloat()
                    knobY = (sin(angle) * travel).toFloat()
                    invalidate()

                    // Precise volume control with generous deadzone and angle hysteresis
                    val deadzone = (width * 0.22f).coerceAtLeast(dp(14).toFloat())
                    val releaseThreshold = deadzone * 0.65f

                    if (dist < releaseThreshold) {
                        // Returned near center deadzone: stop repeating
                        if (activeDirection != 0) {
                            stopAutoRepeat()
                        }
                    } else if (dist >= deadzone) {
                        val angleDeg = Math.toDegrees(angle) // -180.0 to 180.0
                        // -135° to +45° -> Up/Right (Raise)
                        // +45° to +180° or -180° to -135° -> Down/Left (Lower)
                        val targetDir = if (angleDeg in -135.0..45.0) {
                            AudioManager.ADJUST_RAISE
                        } else {
                            AudioManager.ADJUST_LOWER
                        }

                        if (activeDirection == 0) {
                            // First deflection past deadzone: trigger single step & schedule repeat
                            activeDirection = targetDir
                            onVolumeAdjust?.invoke(targetDir)
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            repeatHandler.removeCallbacks(repeatRunnable)
                            repeatHandler.postDelayed(repeatRunnable, 520L) // 520ms initial pause
                        } else if (activeDirection != targetDir) {
                            // Switch direction only when firmly past diagonal boundary to prevent jitter
                            val canSwitch = if (targetDir == AudioManager.ADJUST_RAISE) {
                                angleDeg in -120.0..30.0
                            } else {
                                angleDeg > 60.0 || angleDeg < -150.0
                            }
                            if (canSwitch) {
                                activeDirection = targetDir
                                onVolumeAdjust?.invoke(targetDir)
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                repeatHandler.removeCallbacks(repeatRunnable)
                                repeatHandler.postDelayed(repeatRunnable, 520L)
                            }
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                stopAutoRepeat()

                if (isDraggingPosition) {
                    isDraggingPosition = false
                    val screenW = resources.displayMetrics.widthPixels
                    val snapX = if (event.rawX > screenW / 2f) screenW - width - dp(8) else dp(8)
                    val snapY = (windowStartY + (event.rawY - downRawY).toInt()).coerceIn(0, resources.displayMetrics.heightPixels - height)
                    onPositionChanged?.invoke(snapX, snapY)
                    onPositionSave?.invoke(snapX, snapY)
                    invalidate()
                } else {
                    animateKnobReturn()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun stopAutoRepeat() {
        repeatHandler.removeCallbacks(repeatRunnable)
        activeDirection = 0
    }

    private fun animateKnobReturn() {
        val startX = knobX
        val startY = knobY
        springAnimator?.cancel()
        springAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.6f)
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                knobX = startX * f
                knobY = startY * f
                invalidate()
            }
            start()
        }
    }

    private fun dp(value: Float) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
