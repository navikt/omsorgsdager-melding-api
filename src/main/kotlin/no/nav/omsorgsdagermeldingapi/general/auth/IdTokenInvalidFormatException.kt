package no.nav.omsorgsdagermeldingapi.general.auth

class IdTokenInvalidFormatException(idToken: IdToken, cause: Throwable? = null) : RuntimeException("$idToken er på ugyldig format.", cause)