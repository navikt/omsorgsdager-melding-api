package no.nav.omsorgsdagermeldingapi.mellomlagring

import io.ktor.util.*
import no.nav.omsorgsdagermeldingapi.redis.RedisStore
import java.util.*

class MellomlagringService @KtorExperimentalAPI constructor(private val redisStore: RedisStore, private val passphrase:String) {

    private val nøkkelPrefiks = "mellomlagring_"

    fun getMellomlagring(
        fnr: String
    ): String? {
        val krypto = Krypto(passphrase, fnr)
        val encrypted = redisStore.get(nøkkelPrefiks +fnr) ?: return null
        return krypto.decrypt(encrypted)
    }

    fun setMellomlagring(
        fnr: String,
        midlertidigSøknad: String
    ) {
        val krypto = Krypto(passphrase, fnr)
        val expirationDate = Calendar.getInstance().let {
            it.add(Calendar.HOUR, 24)
            it.time
        }
        redisStore.set(nøkkelPrefiks + fnr, krypto.encrypt(midlertidigSøknad),expirationDate)
    }

    fun deleteMellomlagring(
        fnr: String
    ) {
        redisStore.delete(nøkkelPrefiks + fnr)
    }
}