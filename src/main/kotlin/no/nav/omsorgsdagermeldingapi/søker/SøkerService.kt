package no.nav.omsorgsdagermeldingapi.søker

import com.auth0.jwt.JWT
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import no.nav.omsorgsdagermeldingapi.general.CallId

class SøkerService (
    private val søkerGateway: SøkerGateway
) {
    suspend fun getSøker(
        idToken: IdToken,
        callId: CallId
    ): Søker {
        val ident: String = JWT.decode(idToken.value).subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
        return søkerGateway.hentSøker(idToken, callId).tilSøker(ident)
    }

    private fun  SøkerGateway.SokerOppslagRespons.tilSøker(fodselsnummer: String) = Søker(
        aktørId = aktør_id,
        fødselsnummer = fodselsnummer,
        fødselsdato = fødselsdato,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn
    )
}