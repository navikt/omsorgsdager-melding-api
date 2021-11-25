package no.nav.omsorgsdagermeldingapi.barn

import com.github.benmanes.caffeine.cache.Cache
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.general.oppslag.TilgangNektetException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BarnService(
    private val barnGateway: BarnGateway,
    private val cache: Cache<String, List<Barn>>
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(BarnService::class.java)
    }

    internal suspend fun hentNåværendeBarn(
        idToken: IdToken,
        callId: CallId
    ): List<Barn>{
        var listeOverBarn = cache.getIfPresent(idToken.getSubject().toString())
        if(listeOverBarn != null) return listeOverBarn

        return try {
            val barn = barnGateway.hentBarn(
                idToken = idToken,
                callId = callId)
                .map{ it.tilBarn() }
            cache.put(idToken.getSubject().toString(), barn)
            barn
        } catch (cause: Throwable) {
            when(cause){
                is TilgangNektetException -> throw cause
                else -> {
                    logger.error("Feil ved henting av barn, returnerer en tom liste", cause)
                    emptyList()
                }
            }
        }
    }

    private fun BarnGateway.BarnOppslagDTO.tilBarn() = Barn(
        fødselsdato = fødselsdato,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        aktørId = aktør_id,
        identitetsnummer = identitetsnummer
    )
}