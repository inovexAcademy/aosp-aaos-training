package de.inovex.aosptraining.graphics

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size

fun getBackFacingCamera(cameraManager: CameraManager): String? {
    val cameraIds = cameraManager.cameraIdList
    for (id in cameraIds) {
        val characteristic = cameraManager.getCameraCharacteristics(id)
        val cameraDirection = characteristic.get(CameraCharacteristics.LENS_FACING)
        if (cameraDirection == CameraCharacteristics.LENS_FACING_BACK)
            return id

    }
    return null
}

fun getValidOutputSizes(cameraManager: CameraManager, id: String): Size? {
    val characteristic = cameraManager.getCameraCharacteristics(id)
    val streamConfigurationMap =
        characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null

    // 640x480 is not 16:9, but a final last resort fallback.
    val preferredSizes = arrayOf(Size(1920, 1080), Size(1280, 720), Size(640, 480))

    for (size in streamConfigurationMap.getOutputSizes(ImageFormat.PRIVATE))
        Log.d("XX", "size $size")

    for (size in preferredSizes) {

        val isSupported =
            streamConfigurationMap.getOutputSizes(ImageFormat.PRIVATE).any { o -> o == size }
        if (isSupported) return size

    }
    return null
}
