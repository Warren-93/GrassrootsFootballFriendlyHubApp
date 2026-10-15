package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class PasswordResetRequest(val email: String)

@Serializable
data class PasswordResetConfirmRequest(val token: String, val newPassword: String)

@Serializable
data class VerifyConfirmRequest(val token: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class ChangeEmailRequest(val newEmail: String, val currentPassword: String)

@Serializable
data class ChangeEmailResponse(
    val user: UserView,
    /** Only present while no real email provider is connected - see EmailVerificationScreen. */
    val verificationToken: String? = null
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserView,
    /** Only present while no real email provider is connected - see EmailVerificationScreen. */
    val verificationToken: String? = null
)

@Serializable
data class VerificationResendResponse(
    val verificationToken: String? = null,
    /** True when the API emailed a new link (it only returns the token itself while no email provider is set up). */
    val emailSent: Boolean = false
)

@Serializable
data class UserView(
    val id: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorDetail
)

@Serializable
data class ApiErrorDetail(
    val code: String,
    val message: String,
    val requestId: String
)
