package com.example.domain.model

data class DeviceHardwareInfo(
    val cpuCores: Int,
    val abi: String,
    val socModel: String,
    val deviceModel: String,
    val manufacturer: String,
    val totalRamMb: Int,
    val availableRamMb: Int,
    val isLowRamDevice: Boolean,
    val totalStorageGb: Float,
    val freeStorageGb: Float,
    val recommendedCores: Int,
    val recommendedRamMb: Int,
    val recommendedStorageMb: Int,
    val recommendedViewDistance: Int,
    val deviceTierName: String
) {
    val usedRamMb: Int get() = (totalRamMb - availableRamMb).coerceAtLeast(0)
    val ramUsagePercent: Float get() = if (totalRamMb > 0) (usedRamMb.toFloat() / totalRamMb) * 100f else 0f
    val usedStorageGb: Float get() = (totalStorageGb - freeStorageGb).coerceAtLeast(0f)
    val storageUsagePercent: Float get() = if (totalStorageGb > 0) (usedStorageGb / totalStorageGb) * 100f else 0f

    val hardwareSummary: String
        get() = "$manufacturer $deviceModel ($socModel, $cpuCores Cores, ${totalRamMb / 1024}GB RAM)"
}
