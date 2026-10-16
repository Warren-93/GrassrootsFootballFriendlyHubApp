package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SendFriendlyRequest(
    val senderTeamId: String,
    val recipientTeamId: String,
    val senderSlotId: String,
    val recipientSlotId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venueId: String? = null,
    val homeTeamId: String,
    val costShare: String,
    val refereeArrangement: String,
    val message: String? = null
)

/**
 * proposedStartTime/proposedEndTime/proposedVenueId are only meaningful on
 * suggestChanges - date isn't proposable (see the backend's FriendlyRequest
 * javadoc), only time/venue.
 */
@Serializable
data class FriendlyRequestActionRequest(
    val reason: String? = null,
    val proposedStartTime: String? = null,
    val proposedEndTime: String? = null,
    val proposedVenueId: String? = null
)

@Serializable
data class TeamContact(
    val teamId: String,
    val teamName: String? = null,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val venueId: String? = null
)

@Serializable
data class FriendlyRequestView(
    val id: String,
    val senderTeamId: String,
    val recipientTeamId: String,
    val status: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venueId: String? = null,
    val homeTeamId: String,
    val awayTeamId: String,
    val costShare: String,
    val refereeArrangement: String,
    val message: String? = null,
    val actionReason: String? = null,
    val availableActions: List<String>,
    val senderContact: TeamContact? = null,
    val recipientContact: TeamContact? = null,
    val proposedStartTime: String? = null,
    val proposedEndTime: String? = null,
    val proposedVenueId: String? = null,
    /** The currently agreed venue ([venueId]), if any. */
    val venue: VenueSummary? = null,
    /** The venue a pending suggested change proposes ([proposedVenueId]), if any. */
    val proposedVenue: VenueSummary? = null,
    val senderTeam: TeamRef? = null,
    val recipientTeam: TeamRef? = null
)

/** Who is on each side of a request, so screens can name them without another lookup. */
@Serializable
data class TeamRef(
    val id: String,
    val name: String,
    val clubName: String? = null,
    val badgeUrl: String? = null
)
