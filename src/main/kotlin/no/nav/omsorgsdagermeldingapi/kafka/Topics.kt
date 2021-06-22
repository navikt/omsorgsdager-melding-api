package no.nav.omsorgsdagermeldingapi.kafka

import no.nav.omsorgsdagermeldingapi.felles.Metadata
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(
    val metadata: Metadata,
    val data: V
)

internal data class TopicUse<V>(
    val name: String,
    val valueSerializer : Serializer<TopicEntry<V>>
) {
    internal fun keySerializer() = StringSerializer()
}

object Topics {
    const val MOTTATT_OMSORGSDAGER_MELDING = "dusseldorf.privat-omsorgsdager-melding-mottatt"
}