package no.nav.omsorgsdagermeldingapi.general.auth

class CookieNotSetException(cookieName : String) : RuntimeException("Ingen cookie med navnet '$cookieName' satt.")