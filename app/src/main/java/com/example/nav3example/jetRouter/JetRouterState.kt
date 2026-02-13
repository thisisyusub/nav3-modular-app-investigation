package com.example.nav3example.jetRouter

import android.net.Uri

// ============================================================================
// 2. ROUTER STATE
// ============================================================================

/**
 * Holds state information about the currently matched route.
 *
 * @param uri The full matched URI
 * @param path The route's path pattern (e.g., "/users/:id")
 * @param matchedLocation The actual matched location (e.g., "/users/42")
 * @param name The named route (if any)
 * @param pathParameters Extracted path parameters (e.g., {"id": "42"})
 * @param queryParameters Extracted query parameters
 * @param extra Arbitrary extra data passed during navigation
 * @param error Any error that occurred during matching
 */
data class JetRouterState(
    val uri: Uri = Uri.EMPTY,
    val path: String = "",
    val matchedLocation: String = "",
    val name: String? = null,
    val pathParameters: Map<String, String> = emptyMap(),
    val queryParameters: Map<String, String> = emptyMap(),
    val extra: Any? = null,
    val error: Exception? = null,
    val fullPath: String = "",
) {
    /**
     * Get a required path parameter by name.
     * Throws if not found.
     */
    fun pathParam(name: String): String =
        pathParameters[name]
            ?: throw IllegalArgumentException("Missing path parameter: $name")

    /**
     * Get an optional query parameter by name.
     */
    fun queryParam(name: String): String? = queryParameters[name]
}