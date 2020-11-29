package org.example

import com.owlike.genson.Genson
import org.hyperledger.fabric.contract.Context
import org.hyperledger.fabric.contract.ContractInterface
import org.hyperledger.fabric.contract.annotation.Contract
import org.hyperledger.fabric.contract.annotation.Default
import org.hyperledger.fabric.contract.annotation.Transaction

@Contract(name = "ShipmentTracking")

@Default
class ShipmentTracking : ContractInterface {
    private enum class Flow {
        ORDER,
        SHIP,
        DELIVER
    }

    private val genson = Genson()
    private val transitionMap = mapOf(
        Flow.ORDER.name to Transition(null, Status.ORDERED),
        Flow.SHIP.name to Transition(Status.ORDERED, Status.SHIPPED),
        Flow.DELIVER.name to Transition(Status.SHIPPED, Status.DELIVERED)
    )

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    fun initLedger(ctx: Context) {
        // Just for demo. We should avoid doing this in real cases
        order(ctx, "ship-0001", "0001", "London", "New York")
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    fun order(ctx: Context, key: String, id: String, origin: String, destination: String): Shipment {
        val stub = ctx.stub
        val shipmentState = stub.getStringState(key)
        val shipmentOutput = Shipment(id, origin, destination, Status.ORDERED)

        val verifiable = Verifiable(
            key,
            shipmentState,
            null,
            shipmentOutput,
            transitionMap.getValue(Flow.ORDER.name)
        )
        verifiable.verify()

        val newShipmentState = genson.serialize(shipmentOutput)
        stub.putStringState(key, newShipmentState)

        return shipmentOutput
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    fun ship(ctx: Context, key: String): Shipment {
        return changeStatus(ctx, key, Status.SHIPPED, Flow.SHIP)
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    fun deliver(ctx: Context, key: String): Shipment {
        return changeStatus(ctx, key, Status.DELIVERED, Flow.DELIVER)
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    fun getRecord(ctx: Context, key: String): Shipment {
        val stub = ctx.stub
        val shipmentState = stub.getStringState(key)

        require(!shipmentState.isNullOrEmpty()) { "${Verifiable.ShipmentErrors.RECORD_NOT_FOUND}: $key" }

        return genson.deserialize(shipmentState, Shipment::class.java)
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    fun getAllRecords(ctx: Context): String {
        val stub = ctx.stub
        val shipmentStates = stub.getStateByRange("", "")

        val result = shipmentStates.map {
            genson.deserialize(it.stringValue, Shipment::class.java)
        }

        return genson.serialize(result)
    }

    private fun changeStatus(ctx: Context, key: String, status: Status, flow: Flow): Shipment {
        val stub = ctx.stub
        val shipmentState = stub.getStringState(key)
        val shipmentInput = genson.deserialize(shipmentState, Shipment::class.java)
        val shipmentOutput = shipmentInput.copy(status = status)

        val verifiable = Verifiable(
            key,
            shipmentState,
            shipmentInput,
            shipmentOutput,
            transitionMap.getValue(flow.name)
        )
        verifiable.verify()

        val newShipmentState = genson.serialize(shipmentOutput)
        stub.putStringState(key, newShipmentState)

        return shipmentOutput
    }
}
