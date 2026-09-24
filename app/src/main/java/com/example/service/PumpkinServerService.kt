package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class PumpkinServerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_STOP_FROM_NOTIFICATION = "com.example.service.ACTION_STOP_FROM_NOTIFICATION"
        const val ACTION_RESTART_FROM_NOTIFICATION = "com.example.service.ACTION_RESTART_FROM_NOTIFICATION"

        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_BEDROCK_PORT = "extra_bedrock_port"
        const val EXTRA_JAVA_PORT = "extra_java_port"
        const val EXTRA_LOCAL_IP = "extra_local_ip"
        const val EXTRA_PUBLIC_ADDRESS = "extra_public_address"

        private const val NOTIFICATION_ID = 2051
        private const val CHANNEL_ID = "minecraft_server_channel"

        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var onStopRequested: (() -> Unit)? = null

        @Volatile
        var onRestartRequested: (() -> Unit)? = null

        fun start(
            context: Context,
            serverName: String,
            bedrockPort: Int,
            javaPort: Int,
            localIp: String = "",
            publicAddress: String = ""
        ) {
            val intent = Intent(context, PumpkinServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_BEDROCK_PORT, bedrockPort)
                putExtra(EXTRA_JAVA_PORT, javaPort)
                putExtra(EXTRA_LOCAL_IP, localIp)
                putExtra(EXTRA_PUBLIC_ADDRESS, publicAddress)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PumpkinServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            releaseLocks()
            isServiceRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_STOP_FROM_NOTIFICATION) {
            onStopRequested?.invoke()
            releaseLocks()
            isServiceRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_RESTART_FROM_NOTIFICATION) {
            onRestartRequested?.invoke()
            return START_STICKY
        }

        val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME) ?: "Minecraft Server"
        val bedrockPort = intent?.getIntExtra(EXTRA_BEDROCK_PORT, 19132) ?: 19132
        val javaPort = intent?.getIntExtra(EXTRA_JAVA_PORT, 25565) ?: 25565
        val localIp = intent?.getStringExtra(EXTRA_LOCAL_IP) ?: ""
        val publicAddress = intent?.getStringExtra(EXTRA_PUBLIC_ADDRESS) ?: ""

        acquireLocks()
        val notification = buildNotification(serverName, bedrockPort, javaPort, localIp, publicAddress)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isServiceRunning = true
        return START_STICKY
    }

    private fun acquireLocks() {
        if (wakeLock?.isHeld != true) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PumpkinMCHost::ServerWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        if (wifiLock?.isHeld != true) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "PumpkinMCHost::WifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        if (multicastLock?.isHeld != true) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("PumpkinMCHost::MulticastLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
        wifiLock = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Minecraft Server Host",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Minecraft server active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        serverName: String,
        bedrockPort: Int,
        javaPort: Int,
        localIp: String,
        publicAddress: String
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop Server
        val stopIntent = Intent(this, PumpkinServerService::class.java).apply {
            action = ACTION_STOP_FROM_NOTIFICATION
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Restart Server
        val restartIntent = Intent(this, PumpkinServerService::class.java).apply {
            action = ACTION_RESTART_FROM_NOTIFICATION
        }
        val restartPending = PendingIntent.getService(
            this,
            2,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ipDisplay = if (localIp.isNotBlank()) localIp else "127.0.0.1"
        val contentText = if (publicAddress.isNotBlank()) {
            "Playit: $publicAddress • Wi-Fi: $ipDisplay"
        } else {
            "Wi-Fi: $ipDisplay • Bedrock: $bedrockPort | Java: $javaPort"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Minecraft Server: $serverName")
            .setContentText(contentText)
            .setSubText("Online")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop Server", stopPending)
            .addAction(android.R.drawable.ic_popup_sync, "Restart", restartPending)
            .build()
    }

    override fun onDestroy() {
        releaseLocks()
        isServiceRunning = false
        super.onDestroy()
    }
}
