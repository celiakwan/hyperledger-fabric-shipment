package org.example

import org.assertj.core.api.Assertions.assertThat
import org.hyperledger.fabric.contract.Context
import org.hyperledger.fabric.shim.ChaincodeStub
import org.hyperledger.fabric.shim.ledger.KeyValue
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ShipmentTrackingTest {
    @Test
    fun completeShipment() {
        val key = "ship-0001"
        val id = "0001"
        val origin = "London"
        val destination = "New York"

        val ctx = mock(Context::class.java)
        val stub = mock(ChaincodeStub::class.java)
        `when`(ctx.stub).thenReturn(stub)

        val orderResult = ShipmentTracking().order(ctx, key, id, origin, destination)
        assertThat(orderResult.status).isEqualTo(Status.ORDERED)

        `when`(stub.getStringState(key))
            .thenReturn("""{"destination":"$destination","id":"$id","origin":"$origin","status":"${Status.ORDERED}"}""")

        val shipResult = ShipmentTracking().ship(ctx, key)
        assertThat(shipResult.status).isEqualTo(Status.SHIPPED)

        `when`(stub.getStringState(key))
            .thenReturn("""{"destination":"$destination","id":"$id","origin":"$origin","status":"${Status.SHIPPED}"}""")

        val deliverResult = ShipmentTracking().deliver(ctx, key)
        assertThat(deliverResult.status).isEqualTo(Status.DELIVERED)

        `when`(stub.getStringState(key))
            .thenReturn("""{"destination":"$destination","id":"$id","origin":"$origin","status":"${Status.DELIVERED}"}""")

        val record = ShipmentTracking().getRecord(ctx, key)
        assertThat(record.id).isEqualTo(id)
        assertThat(record.origin).isEqualTo(origin)
        assertThat(record.destination).isEqualTo(destination)
        assertThat(record.status).isEqualTo(Status.DELIVERED)
    }

    @Test
    fun queryAllRecords() {
        val key = "ship-0002"
        val id = "0002"
        val origin = "London"
        val destination = "Paris"

        val ctx = mock(Context::class.java)
        val stub = mock(ChaincodeStub::class.java)
        `when`(ctx.stub).thenReturn(stub)
        `when`(stub.getStateByRange("", ""))
            .thenReturn(MockResultsIterator(key, id, origin, destination, Status.ORDERED))

        val shipments = ShipmentTracking().getAllRecords(ctx)
        assertThat(shipments).isEqualTo("""[{"destination":"$destination","id":"$id","origin":"$origin","status":"${Status.ORDERED}"}]""")
    }

    private class MockKeyValue(private val key: String, private val value: String) : KeyValue {
        override fun getKey() = key
        override fun getValue() = value.toByteArray()
        override fun getStringValue() = value
    }

    private class MockResultsIterator(
        val key: String,
        val id: String,
        val origin: String,
        val destination: String,
        val status: Status
    ) : QueryResultsIterator<KeyValue?> {
        override fun iterator() = mutableListOf(
            MockKeyValue(key, """{"destination":"$destination","id":"$id","origin":"$origin","status":"$status"}""")
        ).iterator()

        @Throws(Exception::class)
        override fun close() {}
    }
}
