package com.lylo.tablet.data.remote

import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.FormulaSizes
import com.lylo.tablet.data.model.Question
import com.lylo.tablet.data.model.QuizAnswer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Requêtes / réponses miroir des appels fetch() du front web ─────────────

@Serializable
data class StartSessionRequest(
    val language: String,
    @SerialName("voice_gender") val voiceGender: String,
    @SerialName("question_count") val questionCount: Int,
    val mode: String,
    @SerialName("input_mode") val inputMode: String,
    val brand: String,
    // Compte connecté (client ou membre de l'équipe) : rattache la session à son propriétaire et
    // décompte le quota du client côté back. L'email du visiteur est saisi à la fin (modale mail).
    @SerialName("owner_email") val ownerEmail: String? = null,
    val avatar: Boolean = true,
    // Capacité « accompagnement » : le robot peut emmener le visiteur jusqu'à ce lieu en fin de
    // parcours vocal ; l'agent le propose alors lui-même. Null = pas de robot / pas de lieu.
    @SerialName("guide_destination") val guideDestination: String? = null,
)

@Serializable
data class StartSessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("room_name") val roomName: String,
    val token: String,
    @SerialName("livekit_url") val livekitUrl: String,
    val identity: String,
)

@Serializable
data class SessionInfoResponse(
    val questions: List<Question>? = null,
    @SerialName("answer_format") val answerFormat: JsonElement? = null,
)

@Serializable
data class LookupResponse(
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val table: String,
    val id: Int,
    val phone: String? = null,
    @SerialName("days_available") val daysAvailable: Int? = null,
    @SerialName("sessions_available") val sessionsAvailable: Int? = null,
)

@Serializable
data class PrintNoteEntry(
    val name: String,
    val ml: Double,
)

@Serializable
data class PrintNotes(
    val top: List<PrintNoteEntry>,
    val heart: List<PrintNoteEntry>,
    val base: List<PrintNoteEntry>,
)

@Serializable
data class PrintFormulaPayload(
    val profile: String,
    val notes: PrintNotes,
    val date: String,
    val reference: String? = null,
)

@Serializable
data class GenerateFormulasRequest(
    val language: String,
    val brand: String,
    val gender: String,
    val age: String,
    @SerialName("has_disliked_notes") val hasDislikedNotes: String,
    @SerialName("disliked_notes") val dislikedNotes: String? = null,
    @SerialName("has_allergies") val hasAllergies: String,
    val allergies: String? = null,
    val pregnant: Boolean,
    val answers: List<QuizAnswer>,
    @SerialName("formula_type") val formulaType: String? = null,
    @SerialName("perfume_name") val perfumeName: String? = null,
)

@Serializable
data class MultiParticipantPayload(
    val color: String,
    val gender: String,
    val age: String,
    @SerialName("has_disliked_notes") val hasDislikedNotes: String,
    @SerialName("disliked_notes") val dislikedNotes: String? = null,
    @SerialName("has_allergies") val hasAllergies: String,
    val allergies: String? = null,
    val pregnant: Boolean,
    val answers: List<QuizAnswer>,
    @SerialName("formula_type") val formulaType: String? = null,
    @SerialName("perfume_name") val perfumeName: String? = null,
)

@Serializable
data class GenerateMultiFormulasRequest(
    val language: String,
    val brand: String,
    val participants: List<MultiParticipantPayload>,
)

// Les requêtes ci-dessous transportent la formule brute (JsonObject) : le web renvoie l'objet
// complet tel que reçu du back (perfume_name, session_id, alternatives…).
@Serializable
data class SaveFormulaRequest(
    val formula: JsonObject,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("participant_color") val participantColor: String? = null,
    val language: String,
)

@Serializable
data class SaveFormulaResponse(
    val reference: String? = null,
)

@Serializable
data class ReplaceNoteRequest(
    val formula: JsonObject,
    @SerialName("note_type") val noteType: String,
    @SerialName("old_note") val oldNote: String,
    @SerialName("new_note") val newNote: String,
    val language: String,
    val reference: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val brand: String,
)

@Serializable
data class SimilarPerfumesFormula(
    val profile: String,
    val description: String? = null,
    @SerialName("top_notes") val topNotes: List<String> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<String> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<String> = emptyList(),
)

@Serializable
data class SimilarPerfumesRequest(
    val formula: SimilarPerfumesFormula,
    val language: String,
)

@Serializable
data class SimilarPerfume(
    val brand: String = "",
    val name: String = "",
    val reason: String = "",
    @SerialName("source_url") val sourceUrl: String = "",
)

@Serializable
data class SimilarPerfumesResponse(
    @SerialName("similar_perfumes") val similarPerfumes: List<SimilarPerfume> = emptyList(),
)

@Serializable
data class SendFormulaMailRequest(
    val email: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val company: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
)

@Serializable
data class BrandingResponse(
    @SerialName("logo_url") val logoUrl: String? = null,
)

/** Ligne de GET /api/formulas (page /formulas, réservée à l'équipe). */
@Serializable
data class FormulaRow(
    val id: Int,
    val reference: String,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_first_name") val customerFirstName: String? = null,
    @SerialName("perfume_name") val perfumeName: String? = null,
    val profile: String? = null,
    @SerialName("formula_type") val formulaType: String? = null,
    val language: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val sizes: FormulaSizes? = null,
)

@Serializable
data class FormulasResponse(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 100,
    val results: List<FormulaRow> = emptyList(),
)
