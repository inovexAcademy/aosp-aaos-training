package de.inovex.aosptraining.automotive

import android.app.Application
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.seconds

data class UiState(
    val model: String = "Unknown",
    val gear: String = "Unknown"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val carPropertyManager = callbackFlow {
        val car = Car.createCar(
            getApplication<Application>().applicationContext,
            null,
            1_000
        ) { car, ready ->
            if (ready) {
                Log.d(TAG, "Connected to CarService")

                trySend(
                    car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                )
            }
        }

        awaitClose {
            car?.disconnect() // createCar could return null in case of an error
            Log.d(TAG, "Connection to the CarService closed.")
        }
    }.shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(60.seconds.inWholeMilliseconds),
        replay = 1
    )

    private val vehicleModel = flow {
        Log.d(TAG, "Getting INFO_MODEL")

        val model = carPropertyManager.first().getProperty<String>(
            VehiclePropertyIds.INFO_MODEL, 0
        )

        Log.d(TAG, "Received INFO_MODEL: ${model.value}")

        emit(model.value)
    }

    private val currentGear = callbackFlow {
        Log.d(TAG, "Observing CURRENT_GEAR changes")

        val property = VehiclePropertyIds.CURRENT_GEAR
        val manager = carPropertyManager.first()

        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                Log.d(TAG, "CURRENT_GEAR value received: ${value.value}")
                trySend(value.value as Int)
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.d(TAG, "Error for property $propertyId in area $areaId")
            }
        }

        // Subscribing delivers the current value immediately, then every change.
        manager.subscribePropertyEvents(property, callback)

        awaitClose {
            runCatching { manager.unsubscribePropertyEvents(property, callback) }
                .onFailure { Log.w(TAG, "Failed to unsubscribe property $property: ${it.message}") }
            Log.d(TAG, "Finished observing CURRENT_GEAR changes")
        }
    }

    val uiState = combine(
        vehicleModel, currentGear
    ) { model, gear ->
        UiState(model, VehicleGear.toString(gear))
    }

    companion object {
        private const val TAG = "AAOS_example"
    }
}