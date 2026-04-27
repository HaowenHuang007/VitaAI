package com.vitaai.app.api

import retrofit2.http.*

// ===== 登录/注册 =====
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val access_token: String, val token_type: String)
data class RegisterRequest(val username: String, val email: String, val password: String)

// ===== 问卷 =====
data class QuestionnaireRequest(
    val age: Int,
    val weight: Double,
    val height: Double,
    val goal: String,
    val activity_level: String
)

// ===== 计划 =====
data class DietPlan(val plan: String)
data class ExercisePlan(val plan: String)

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @POST("diet/generate")
    suspend fun getDietPlan(
        @Header("Authorization") token: String,
        @Body request: QuestionnaireRequest
    ): DietPlan

    @POST("exercise/generate")
    suspend fun getExercisePlan(
        @Header("Authorization") token: String,
        @Body request: QuestionnaireRequest
    ): ExercisePlan
}