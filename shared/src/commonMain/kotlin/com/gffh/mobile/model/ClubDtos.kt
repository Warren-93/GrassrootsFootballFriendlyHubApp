package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateClubRequest(
    val name: String,
    val badgeUrl: String? = null,
    val postcode: String,
    val longitude: Double,
    val latitude: Double,
    val website: String? = null,
    val contactEmail: String? = null
)

@Serializable
data class UpdateClubRequest(
    val name: String? = null,
    val badgeUrl: String? = null,
    val postcode: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val website: String? = null,
    val contactEmail: String? = null
)

@Serializable
data class ClubView(
    val id: String,
    val name: String,
    val badgeUrl: String? = null,
    val postcode: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val website: String? = null,
    val contactEmail: String? = null,
    val createdAt: String
)
