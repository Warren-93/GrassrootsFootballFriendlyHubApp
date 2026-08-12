package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitVerificationRequest(
    val affiliationNumber: String? = null,
    val contactDetails: String,
    val evidenceUrls: List<String>
)

@Serializable
data class VerificationRequestView(
    val id: String,
    val teamId: String,
    val affiliationNumber: String? = null,
    val contactDetails: String,
    val evidenceUrls: List<String> = emptyList(),
    val status: String,
    val firstRejectionReason: String? = null,
    val finalRejectionReason: String? = null,
    val submittedAt: String,
    val reviewedAt: String? = null
)
