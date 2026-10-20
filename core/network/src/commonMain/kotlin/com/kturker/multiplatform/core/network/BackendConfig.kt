package com.kturker.multiplatform.core.network

import com.kturker.multiplatform.core.domain.BuildEnvironment

/**
 * The app's own backend (the `server` module), apart from [ApiConfig]'s public API. Debug builds use the
 * local server (`./gradlew :server:runLocal`), release builds `RELEASE_BASE_URL`. The server serves its API under
 * `/api/v1/`: keep the trailing slash and write request paths without a leading one (core:api ApiRoutes).
 */
object BackendConfig {
    const val LOCAL_SERVER_PORT = 18_001

    /**
     * A placeholder that never resolves: set it to the live server's HTTPS address before you ship, in the form
     * `https://<domain>/<appName>/api/v1/`.
     */
    const val RELEASE_BASE_URL = "https://example.invalid/api/v1/"

    fun baseUrl(environment: BuildEnvironment): String =
        selectBaseUrl(environment.isDebug, "http://${localServerHost()}:$LOCAL_SERVER_PORT/api/v1/", RELEASE_BASE_URL)
}
