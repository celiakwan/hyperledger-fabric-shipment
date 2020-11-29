package org.example

import com.owlike.genson.annotation.JsonProperty
import org.hyperledger.fabric.contract.annotation.DataType
import org.hyperledger.fabric.contract.annotation.Property

@DataType
data class Shipment(
    @Property
    @JsonProperty("id")
    val id: String,

    @Property
    @JsonProperty("origin")
    val origin: String,

    @Property
    @JsonProperty("destination")
    val destination: String,

    @Property
    @JsonProperty("status")
    val status: Status
)
