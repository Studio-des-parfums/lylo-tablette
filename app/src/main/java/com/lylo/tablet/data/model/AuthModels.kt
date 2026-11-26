package com.lylo.tablet.data.model

import kotlinx.serialization.Serializable

// ── Portage de src/context/AuthContext.tsx ──────────────────────────────────

@Serializable
data class AuthUser(
    val email: String,
    @kotlinx.serialization.SerialName("first_name") val firstName: String,
    @kotlinx.serialization.SerialName("last_name") val lastName: String,
    val table: String, // "customers" | "teams"
    val id: Int,
    val phone: String? = null,
    @kotlinx.serialization.SerialName("days_available") val daysAvailable: Int? = null,
    @kotlinx.serialization.SerialName("sessions_available") val sessionsAvailable: Int? = null,
)

enum class AuthError { UNAUTHORIZED, NO_SESSIONS, ERROR }
