package com.example.domain.model

data class AppSettings(
    val cpuWakeLockEnabled: Boolean = true,
    val lowPowerIdleMode: Boolean = true,
    val thermalProtectionEnabled: Boolean = true,
    val autoRestartOnCrash: Boolean = true,
    val autoBackupBeforeStart: Boolean = true,
    val autoStopAfterIdle: Boolean = false,
    val autoStopMinutes: Int = 30,
    val multiThreadRayonEnabled: Boolean = true,
    val bedrockCrossplayEnabled: Boolean = true,
    val protocolAutoNegotiate: Boolean = true,
    val autoSyncMarket: Boolean = true,
    val vacuumDbOnExit: Boolean = true,
    val backgroundNotification: Boolean = true,
    val maxStorageAllocationMb: Int = 2048,
    val hasSeenOnboarding: Boolean = false,
    val quickLanBroadcast: Boolean = true,
    val autoClearCrashDumps: Boolean = true,
    val compactMemoryAllocator: Boolean = true,
    val playerJoinAlerts: Boolean = true
)
