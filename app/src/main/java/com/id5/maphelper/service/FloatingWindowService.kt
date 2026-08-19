package com.id5.maphelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.id5.maphelper.R
import com.id5.maphelper.data.MapDatabase
import com.id5.maphelper.data.MapEntity
import com.id5.maphelper.util.ImageUtil
import kotlinx.coroutines.launch

class FloatingWindowService : Service(), LifecycleOwner {

    private lateinit var windowManager: WindowManager
    private var floatingBall: View? = null
    private var floatingPanel: View? = null
    private val lifecycleRegistry = LifecycleRegistry(this)

    private var ballParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isPanelShowing = false
    private var screenWidth = 0
    private var screenHeight = 0

    private val db by lazy { MapDatabase.getDatabase(this) }
    private var allMaps: List<MapEntity> = emptyList()
    private var currentMapIndex = 0

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        loadMaps()
        showFloatingBall()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗地图助手",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "加页手记地图助手悬浮窗服务"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("加页手记地图助手")
                .setContentText("悬浮窗运行中，点击可展开地图")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("加页手记地图助手")
                .setContentText("悬浮窗运行中")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build()
        }
    }

    private fun loadMaps() {
        lifecycleScope.launch {
            allMaps = db.mapDao().getAllMapsOnce()
        }
    }

    private fun showFloatingBall() {
        if (floatingBall != null) return

        floatingBall = LayoutInflater.from(this).inflate(R.layout.floating_ball, null)
        val ball = floatingBall ?: return

        ballParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 150
            y = screenHeight / 3
        }

        setupBallTouch(ball)
        windowManager.addView(ball, ballParams)
    }

    private fun setupBallTouch(ball: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var startDownTime = 0L
        val touchSlop = 10

        ball.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = ballParams?.x ?: 0
                    initialY = ballParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    startDownTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    ballParams?.x = initialX + dx.toInt()
                    ballParams?.y = initialY + dy.toInt()
                    try {
                        windowManager.updateViewLayout(ball, ballParams)
                    } catch (e: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val duration = System.currentTimeMillis() - startDownTime
                    if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop && duration < 300) {
                        togglePanel()
                    } else {
                        snapToEdge(ball)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(ball: View) {
        val centerX = (ballParams?.x ?: 0) + ball.width / 2
        ballParams?.x = if (centerX < screenWidth / 2) {
            0
        } else {
            screenWidth - ball.width
        }
        try {
            windowManager.updateViewLayout(ball, ballParams)
        } catch (e: Exception) {
        }
    }

    private fun togglePanel() {
        if (isPanelShowing) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun showPanel() {
        if (floatingPanel != null) return
        if (allMaps.isEmpty()) {
            loadMaps()
            Toast.makeText(this, "暂无地图，请先在主界面添加", Toast.LENGTH_SHORT).show()
            return
        }

        floatingPanel = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)
        val panel = floatingPanel ?: return

        val panelWidth = (screenWidth * 0.85f).toInt()
        val panelHeight = (screenHeight * 0.7f).toInt()

        panelParams = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        setupPanelViews(panel)
        windowManager.addView(panel, panelParams)
        isPanelShowing = true
        updatePanelContent()
    }

    private fun setupPanelViews(panel: View) {
        panel.findViewById<View>(R.id.btnClose).setOnClickListener {
            hidePanel()
        }

        panel.findViewById<View>(R.id.btnPrev).setOnClickListener {
            if (allMaps.isNotEmpty()) {
                currentMapIndex = (currentMapIndex - 1 + allMaps.size) % allMaps.size
                updatePanelContent()
            }
        }

        panel.findViewById<View>(R.id.btnNext).setOnClickListener {
            if (allMaps.isNotEmpty()) {
                currentMapIndex = (currentMapIndex + 1) % allMaps.size
                updatePanelContent()
            }
        }

        panel.findViewById<View>(R.id.btnMatch).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.putExtra("open_match", true)
            startActivity(intent)
            hidePanel()
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val header = panel.findViewById<View>(R.id.panelHeader)
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = panelParams?.x ?: 0
                    initialY = panelParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    panelParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(panel, panelParams)
                    } catch (e: Exception) {
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updatePanelContent() {
        val panel = floatingPanel ?: return
        if (allMaps.isEmpty()) return

        val map = allMaps[currentMapIndex]
        panel.findViewById<TextView>(R.id.tvMapName).text = map.name
        panel.findViewById<TextView>(R.id.tvMapIndex).text = "${currentMapIndex + 1}/${allMaps.size}"

        val features = buildString {
            if (map.doorType.isNotEmpty()) append(map.doorType)
            if (map.doorDirection.isNotEmpty()) append(" · ${map.doorDirection}")
            if (map.sideDoor.isNotEmpty()) append(" · 侧门${map.sideDoor}")
            if (map.secondFloorDoor == "有") append(" · 二楼")
        }
        panel.findViewById<TextView>(R.id.tvMapFeatures).text = features.ifEmpty { "无特征" }
        panel.findViewById<TextView>(R.id.tvRouteNote).text = map.routeNote.ifEmpty { "暂无路线说明" }

        val imageView = panel.findViewById<ImageView>(R.id.ivMapImage)
        if (map.imagePath.isNotEmpty()) {
            val bitmap = ImageUtil.loadBitmap(map.imagePath, 600, 600)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        } else {
            imageView.visibility = View.GONE
        }
    }

    private fun hidePanel() {
        floatingPanel?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
            }
        }
        floatingPanel = null
        isPanelShowing = false
    }

    override fun onDestroy() {
        hidePanel()
        floatingBall?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
            }
        }
        floatingBall = null
        isRunning = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "floating_map_channel"
        private const val NOTIFICATION_ID = 1001
        var isRunning = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }
}
