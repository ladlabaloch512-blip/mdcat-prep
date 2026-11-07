package com.abiz.mdcatprep

/**
 * API configuration — the ONLY place the backend base URL is defined.
 *
 * Points at the deployed backend. Current value is the live preview
 * deployment (Hostinger temporary domain). At client go-live, change
 * this one line to the production domain and rebuild. The URL must end
 * with a trailing slash.
 */
object ApiConfig {
    const val APP_VERSION: String = "1.0"
    const val BASE_URL: String = "https://darkslateblue-panther-598842.hostingersite.com/"

    /** Full endpoint URL for a route such as "settings" or "auth/login". */
    fun url(route: String): String = BASE_URL + "api/index.php?r=" + route
}
