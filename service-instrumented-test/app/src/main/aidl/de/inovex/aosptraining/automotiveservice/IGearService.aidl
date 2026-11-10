package de.inovex.aosptraining.automotiveservice;

/**
 * AIDL interface exposing gear state information from the vehicle.
 */
interface IGearService {
    /**
     * Returns true if the vehicle's CURRENT_GEAR property is VehicleGear.GEAR_REVERSE.
     */
    boolean isReverseGear();
}
