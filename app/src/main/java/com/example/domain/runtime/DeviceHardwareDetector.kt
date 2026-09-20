package com.example.domain.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.domain.model.DeviceHardwareInfo

object DeviceHardwareDetector {

    fun detect(context: Context): DeviceHardwareInfo {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val model = Build.MODEL ?: "Android Device"
        val manufacturer = (Build.MANUFACTURER ?: "Generic").replaceFirstChar { it.uppercase() }

        // Memory info from ActivityManager
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt().coerceAtLeast(1024)
        val availRamMb = (memInfo.availMem / (1024 * 1024)).toInt().coerceAtLeast(128)
        val isLowRam = actManager?.isLowRamDevice == true || totalRamMb <= 2500

        // Internal Storage info via StatFs
        var totalStorageGb = 32f
        var freeStorageGb = 16f
        try {
            val dataDir = Environment.getDataDirectory()
            val statFs = StatFs(dataDir.path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availBlocks = statFs.availableBlocksLong

            totalStorageGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
            freeStorageGb = (availBlocks * blockSize) / (1024f * 1024f * 1024f)
        } catch (_: Exception) {
            // fallback defaults
        }

        // Chipset identification
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (!socModel.isNullOrBlank() && socModel != Build.UNKNOWN) socModel else Build.HARDWARE
        } else {
            Build.HARDWARE
        } ?: "ARM64 SoC"

        // Compute optimal recommended settings for this specific device:
        val recCores = when {
            cores <= 2 -> 1
            cores <= 4 -> 2
            cores <= 6 -> 4
            else -> 4 // For 8 cores, assign 4 to Tokio/Rayon, keeping 4 for Android UI & OS
        }

        val recRamMb = when {
            totalRamMb <= 2500 -> 256
            totalRamMb <= 4500 -> 512
            totalRamMb <= 6500 -> 1024
            totalRamMb <= 8500 -> 2048
            else -> 2048
        }

        val recStorageMb = when {
            freeStorageGb < 4f -> 1024
            freeStorageGb < 12f -> 2048
            else -> 2048
        }

        val recViewDistance = when {
            totalRamMb <= 3000 || cores <= 4 -> 6
            totalRamMb <= 6500 -> 8
            else -> 10
        }

        val tier = when {
            totalRamMb <= 3000 || cores <= 4 -> "Budget / Low-End Tier"
            totalRamMb <= 6500 -> "Mid-Range Balanced Tier"
            else -> "High-Performance Flagship Tier"
        }

        return DeviceHardwareInfo(
            cpuCores = cores,
            abi = abi,
            socModel = soc,
            deviceModel = model,
            manufacturer = manufacturer,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            isLowRamDevice = isLowRam,
            totalStorageGb = totalStorageGb,
            freeStorageGb = freeStorageGb,
            recommendedCores = recCores,
            recommendedRamMb = recRamMb,
            recommendedStorageMb = recStorageMb,
            recommendedViewDistance = recViewDistance,
            deviceTierName = tier
        )
    }
}
