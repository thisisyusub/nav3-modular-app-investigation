package com.example.nav3example.jetRouter

// ============================================================================
// NAVIGATION EVENT LOGGING
//
// Allows developers to hook into navigation events for debugging, analytics,
// screen tracking, etc.
//
// Usage:
//   val router = rememberJetRouter(
//       routes = ...,
//       navigationLogger = LogcatNavigationLogger(tag = "MyApp"),
//   )
//
// Or custom:
//   val router = rememberJetRouter(
//       routes = ...,
//       navigationLogger = object : NavigationLogger {
//           override fun onNavigate(event: NavigationEvent) {
//               analytics.trackScreen(event.location)
//           }
//       },
//   )
// ============================================================================

/**
 * Represents a navigation event for logging/tracking purposes.
 */
data class NavigationLogEvent(
    val type: NavigationType,
    val location: String,
    val previousLocation: String? = null,
    val stackDepth: Int = 0,
    val extra: Any? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    override fun toString(): String = buildString {
        append("[${type.name}] $location")
        previousLocation?.let { append(" (from: $it)") }
        append(" [depth=$stackDepth]")
    }
}

enum class NavigationType {
    GO,
    PUSH,
    PUSH_REPLACEMENT,
    POP,
    RESTORE,
    REDIRECT,
    ERROR,
}

/**
 * Interface for intercepting navigation events.
 * Implement this to add custom logging, analytics, or debugging.
 */
interface NavigationLogger {
    fun onNavigate(event: NavigationLogEvent)
}

/**
 * Default logger that prints to Logcat / stdout.
 */
class LogcatNavigationLogger(
    private val tag: String = "JetRouter",
) : NavigationLogger {
    override fun onNavigate(event: NavigationLogEvent) {
        val emoji = when (event.type) {
            NavigationType.GO -> "🔄"
            NavigationType.PUSH -> "➡️"
            NavigationType.PUSH_REPLACEMENT -> "🔁"
            NavigationType.POP -> "⬅️"
            NavigationType.RESTORE -> "♻️"
            NavigationType.REDIRECT -> "↪️"
            NavigationType.ERROR -> "❌"
        }
        println("$tag $emoji $event")
    }
}

/**
 * Composite logger that forwards events to multiple loggers.
 */
class CompositeNavigationLogger(
    private val loggers: List<NavigationLogger>,
) : NavigationLogger {
    override fun onNavigate(event: NavigationLogEvent) {
        loggers.forEach { it.onNavigate(event) }
    }
}