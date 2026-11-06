package de.inovex.aosptraining.automotive

import android.app.Application
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val model: String = "Unknown", val gear: String = "Unknown"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val car = Car.createCar(application)
    private val carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    val uiState = MutableStateFlow(UiState())

    init {
        val model = carPropertyManager.getProperty<String>(
            VehiclePropertyIds.INFO_MODEL, 0
        )

        updateModelInUi(model.value)

        // Add your code to receive gear changes immediately and display the received value on the UI
        // You can use `updateGearInUi(Int)` to update the UI
    }

    private fun updateGearInUi(gear: Int) {
        uiState.update {
            UiState(
                model = uiState.value.model, gear = VehicleGear.toString(gear)
            )
        }
    }

    private fun updateModelInUi(model: String) {
        uiState.update {
            UiState(
                model = model, gear = uiState.value.gear
            )
        }
    }

    companion object {
        private const val TAG = "AAOS_example"
    }
}