package no.nav.omsorgspengermidlertidigalene.wiremock

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.helse.TestUtils

class BarnResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        return Response.Builder.like(response)
            .body(
                getResponse(
                    ident = TestUtils.getIdentFromIdToken(request)
                )
            )
            .build()
    }

    override fun getName(): String {
        return "k9-oppslag-barn"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}

private fun getResponse(ident: String): String {
    when (ident) {
        "290990123456" -> {
            return """
            {
                "barn": [{
                    "fødselsdato": "2000-08-27",
                    "fornavn": "BARN",
                    "mellomnavn": "EN",
                    "etternavn": "BARNESEN",
                    "aktør_id": "1000000000001",
                    "identitetsnummer" : "16012099359"
                }, {
                    "fødselsdato": "2001-04-10",
                    "fornavn": "BARN",
                    "mellomnavn": "TO",
                    "etternavn": "BARNESEN",
                    "aktør_id": "1000000000002",
                    "identitetsnummer" : "16012099359"
                }]
            }
            """.trimIndent()
        }
        else -> {
            return """
                {
                    "barn": []
                }
            """.trimIndent()
        }
    }
}