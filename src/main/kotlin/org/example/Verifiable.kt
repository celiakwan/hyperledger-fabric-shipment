package org.example

class Verifiable(
    private val key: String,
    private val shipmentState: String?,
    private val input: Shipment?,
    private val output: Shipment,
    private val transition: Transition
) {
    enum class ShipmentErrors {
        RECORD_ALREADY_EXISTS,
        RECORD_NOT_FOUND,
        INPUT_STATUS_INVALID,
        OUTPUT_STATUS_INVALID
    }

    fun verify() {
        if (transition.inputStatus == null) {
            require(shipmentState.isNullOrEmpty()) { "${ShipmentErrors.RECORD_ALREADY_EXISTS}: $key" }
        } else {
            require(!shipmentState.isNullOrEmpty()) { "${ShipmentErrors.RECORD_NOT_FOUND}: $key" }
        }
        require(input?.status == transition.inputStatus) { "${ShipmentErrors.INPUT_STATUS_INVALID}: ${input?.status}" }
        require(output.status == transition.outputStatus) { "${ShipmentErrors.OUTPUT_STATUS_INVALID}: ${output.status}" }
    }
}

class Transition(val inputStatus: Status?, val outputStatus: Status)
