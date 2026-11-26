package com.lylo.tablet.data.remote

import com.lylo.tablet.data.model.Question
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface Retrofit miroir des endpoints appelés par le front web (fetch(`${API_BASE}/...`)).
 * Base URL configurée via BuildConfig.API_BASE_URL (voir app/build.gradle.kts + local.properties).
 */
interface LyloApiService {

    // ── Session (SessionContext.startSession / endSession) ──────────────────
    @POST("api/session/start")
    suspend fun startSession(@Body body: StartSessionRequest): Response<StartSessionResponse>

    @GET("api/session/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: String): Response<SessionInfoResponse>

    @DELETE("api/session/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<Unit>

    // ── Auth (AuthContext.login) ─────────────────────────────────────────────
    @GET("lookup")
    suspend fun lookup(@Query("email") email: String): Response<LookupResponse>

    // ── Réseau / diagnostics (hooks/useConnectionTest.ts) ────────────────────
    @GET("api/ping")
    suspend fun ping(): Response<Unit>

    @GET("api/speedtest")
    suspend fun speedtest(): Response<okhttp3.ResponseBody>

    // ── Impression : PDF de la formule (même mise en page que PrintableFormula côté web),
    // imprimé via la boîte de dialogue native Android (PrintManager), comme window.print().
    @POST("printers/formula-pdf")
    suspend fun getFormulaPdf(@Body body: PrintFormulaPayload): Response<okhttp3.ResponseBody>

    // ── Branding : logo partenaire secondaire (hooks/useBranding.ts) ─────────
    @GET("branding")
    suspend fun getBranding(): Response<BrandingResponse>

    // ── Quiz visuel (app/quiz/page.tsx + quiz/results/page.tsx) ──────────────
    // Réponse brute en JsonElement : le backend renvoie tantôt un tableau `[...]`, tantôt un
    // objet `{"group_id":...,"group_name":...,"questions":[...]}` — portage fidèle du
    // `Array.isArray(data) ? data : data.questions ?? []` de quiz/page.tsx. Voir
    // QuizViewModel.loadQuestionsIfNeeded() pour l'extraction.
    @GET("api/questions")
    suspend fun getQuestions(
        @Query("count") count: Int,
        @Query("language") language: String,
        @Query("brand") brand: String,
    ): Response<kotlinx.serialization.json.JsonElement>

    // Réponses brutes (JsonElement) : les formules sont conservées telles que reçues du back
    // (voir Formula.raw) pour être renvoyées intactes à save / replace-note.
    @POST("api/formulas/generate")
    suspend fun generateFormulas(@Body body: GenerateFormulasRequest): Response<kotlinx.serialization.json.JsonObject>

    @POST("api/formulas/generate-multi")
    suspend fun generateMultiFormulas(@Body body: GenerateMultiFormulasRequest): Response<kotlinx.serialization.json.JsonObject>

    @POST("api/formulas/save")
    suspend fun saveFormula(@Body body: SaveFormulaRequest): Response<SaveFormulaResponse>

    @POST("api/formulas/replace-note")
    suspend fun replaceNote(@Body body: ReplaceNoteRequest): Response<kotlinx.serialization.json.JsonObject>

    @POST("api/formulas/similar-perfumes")
    suspend fun similarPerfumes(@Body body: SimilarPerfumesRequest): Response<SimilarPerfumesResponse>

    @POST("api/formulas/{reference}/send-mail")
    suspend fun sendFormulaMail(
        @Path("reference") reference: String,
        @Body body: SendFormulaMailRequest,
    ): Response<Unit>

    // ── Page /formulas (réservée à l'équipe) ─────────────────────────────────
    @GET("api/formulas")
    suspend fun listFormulas(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String,
    ): Response<FormulasResponse>
}
