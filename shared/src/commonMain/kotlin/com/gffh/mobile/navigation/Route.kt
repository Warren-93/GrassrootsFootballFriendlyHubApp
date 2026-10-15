package com.gffh.mobile.navigation

/**
 * Every screen the app can navigate to, named after its SCR-XX-NN identifier
 * in the Screen Build Specification. Every slice through Govern is built -
 * see gffh-mobile/README.md for what each covers. [Route.Placeholder] is a
 * fallback for routes that don't correspond to any built screen, not a
 * standing "not built yet" state.
 */
sealed class Route {
    // Access (SCR-AU-*)
    data object Splash : Route()
    data object Welcome : Route()
    data object Register : Route()
    data object SignIn : Route()
    data class ForgotPassword(val prefilledEmail: String = "") : Route()
    data object EmailVerification : Route()

    // Setup (SCR-ON-*, SCR-PR-01/02)
    data object RoleSelection : Route()                                    // SCR-ON-01
    data object CreateClub : Route()                                       // SCR-ON-02
    data class CreateTeam(val clubId: String? = null) : Route()             // SCR-ON-03
    data class AddFirstVenue(val clubId: String, val teamId: String) : Route() // SCR-ON-04
    data class AddFirstAvailability(val teamId: String, val clubId: String) : Route() // SCR-ON-05
    data class OnboardingComplete(val teamId: String) : Route()             // SCR-ON-06
    data class TeamProfile(val teamId: String) : Route()                    // SCR-PR-01
    data class EditTeam(val teamId: String) : Route()                       // SCR-PR-02
    data object Settings : Route()                                          // SCR-PR-08
    data class ReportBlock(val teamId: String, val teamName: String, val fixtureId: String? = null) : Route() // SCR-PR-11
    data object BlockedTeams : Route()                                      // SCR-PR-11
    data class VenuesList(val clubId: String) : Route()                     // SCR-PR-05
    data class EditVenue(val venueId: String?, val clubId: String) : Route() // SCR-PR-06
    data class ClubProfile(val clubId: String) : Route()                    // SCR-PR-03
    data class EditClub(val clubId: String) : Route()                       // SCR-PR-03
    data class Members(val teamId: String) : Route()                       // SCR-PR-04
    data class Verification(val teamId: String) : Route()                  // SCR-PR-07
    data object Privacy : Route()                                          // SCR-PR-10
    data object Help : Route()                                             // SCR-PR-12
    data object NotificationPreferences : Route()                          // SCR-PR-09

    // Publish (SCR-AV-*, SCR-HM-01)
    data object Home : Route()                                            // SCR-HM-01
    data object Notifications : Route()                                    // SCR-HM-02
    data object Messages : Route()
    data class ConversationThread(val conversationId: String) : Route()
    data object Calendar : Route()                                         // SCR-AV-01
    data class DayDetail(val date: String) : Route()                       // SCR-AV-02
    data class EditAvailabilitySlot(val slotId: String? = null, val date: String? = null) : Route() // SCR-AV-03
    data object BulkAddAvailability : Route()                              // SCR-AV-04

    // Discover (SCR-FF-*)
    data object SuggestedMatches : Route()                                  // SCR-HM-03
    data object SearchEntry : Route()                                       // SCR-FF-01
    data object Filters : Route()                                           // SCR-FF-02
    data object Results : Route()                                           // SCR-FF-03 (also covers SCR-FF-07's empty state)
    data object ResultsMap : Route()                                        // SCR-FF-04
    data class OpponentProfile(val teamId: String) : Route()                // SCR-FF-05
    data class MatchExplanation(val teamId: String) : Route()               // SCR-FF-06

    // Arrange (SCR-IN-*, SCR-FX-*)
    data class InvitationComposer(val opponentTeamId: String) : Route()      // SCR-IN-01
    data object InvitationReview : Route()                                  // SCR-IN-02
    data class InvitationSent(val requestId: String) : Route()               // SCR-IN-03
    data class RequestDetail(val requestId: String) : Route()                // SCR-IN-04
    data class SuggestChanges(val requestId: String) : Route()               // SCR-IN-05
    data class DeclineRequest(val requestId: String) : Route()               // SCR-IN-06
    data object Fixtures : Route()                                          // SCR-FX-01/02/03
    data class FixtureDetail(val fixtureId: String) : Route()                // SCR-FX-04

    data class Placeholder(val label: String) : Route()
}
