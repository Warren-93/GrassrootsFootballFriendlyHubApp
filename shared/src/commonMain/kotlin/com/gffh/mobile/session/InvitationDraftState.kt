package com.gffh.mobile.session

/**
 * Carries a composed proposal from SCR-IN-01 to SCR-IN-02 without threading
 * a dozen navigation parameters through the route. Cleared once sent.
 */
class InvitationDraftState {
    var opponentTeamId: String? = null
    var opponentTeamName: String = ""
    var senderTeamId: String? = null
    var senderSlotId: String? = null
    var recipientSlotId: String? = null
    var date: String = ""
    var startTime: String = ""
    var endTime: String = ""
    var venueId: String? = null
    var venueName: String? = null
    var homeTeamId: String = ""
    var isSenderHome: Boolean = true
    var costShare: String = "SPLIT"
    var refereeArrangement: String = "NONE"
    var notes: String = ""

    fun clear() {
        opponentTeamId = null
        opponentTeamName = ""
        senderTeamId = null
        senderSlotId = null
        recipientSlotId = null
        date = ""; startTime = ""; endTime = ""
        venueId = null; venueName = null
        homeTeamId = ""
        isSenderHome = true
        costShare = "SPLIT"
        refereeArrangement = "NONE"
        notes = ""
    }
}
