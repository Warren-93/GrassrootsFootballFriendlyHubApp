package com.gffh.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.core.auth.createSettings
import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.defaultApiBaseUrl
import com.gffh.mobile.feature.auth.*
import com.gffh.mobile.feature.availability.BulkAddAvailabilityScreen
import com.gffh.mobile.feature.availability.CalendarScreen
import com.gffh.mobile.feature.availability.DayDetailScreen
import com.gffh.mobile.feature.availability.EditAvailabilitySlotScreen
import com.gffh.mobile.feature.arrange.*
import com.gffh.mobile.feature.discover.FiltersScreen
import com.gffh.mobile.feature.discover.SuggestedMatchesScreen
import com.gffh.mobile.feature.discover.ResultsMapScreen
import com.gffh.mobile.feature.discover.MatchExplanationScreen
import com.gffh.mobile.feature.discover.OpponentProfileScreen
import com.gffh.mobile.feature.discover.ResultsListScreen
import com.gffh.mobile.feature.discover.SearchEntryScreen
import com.gffh.mobile.feature.onboarding.*
import com.gffh.mobile.feature.home.NotificationsScreen
import com.gffh.mobile.feature.messages.ConversationsListScreen
import com.gffh.mobile.feature.messages.ConversationThreadScreen
import com.gffh.mobile.feature.placeholder.HomeScreen
import com.gffh.mobile.feature.placeholder.PlaceholderScreen
import com.gffh.mobile.feature.profile.NotificationPreferencesScreen
import com.gffh.mobile.feature.profile.ClubProfileScreen
import com.gffh.mobile.feature.profile.EditClubScreen
import com.gffh.mobile.feature.profile.EditTeamScreen
import com.gffh.mobile.feature.profile.EditVenueScreen
import com.gffh.mobile.feature.profile.MembersScreen
import com.gffh.mobile.feature.profile.ReportBlockScreen
import com.gffh.mobile.feature.profile.SettingsScreen
import com.gffh.mobile.feature.profile.TeamProfileScreen
import com.gffh.mobile.feature.profile.VenuesListScreen
import com.gffh.mobile.feature.profile.VerificationScreen
import com.gffh.mobile.feature.profile.PrivacyScreen
import com.gffh.mobile.feature.profile.HelpScreen
import com.gffh.mobile.navigation.AppScaffold
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.navigation.tabFor
import com.gffh.mobile.repository.*
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.InvitationDraftState
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import com.gffh.mobile.theme.GffhTheme

@Composable
fun App() {
    val settings = remember { createSettings() }
    val tokenStore = remember { TokenStore(settings) }
    val currentTeamStore = remember { CurrentTeamStore(settings) }
    val filterState = remember { SearchFilterState() }
    val resultsCache = remember { SearchResultsCache() }
    val invitationDraft = remember { InvitationDraftState() }
    val apiClient = remember { ApiClient(defaultApiBaseUrl(), tokenStore) }
    val authRepository = remember { AuthRepository(apiClient, tokenStore) }
    val teamRepository = remember { TeamRepository(apiClient) }
    val clubRepository = remember { ClubRepository(apiClient) }
    val venueRepository = remember { VenueRepository(apiClient) }
    val availabilityRepository = remember { AvailabilityRepository(apiClient) }
    val matchRepository = remember { MatchRepository(apiClient) }
    val friendlyRequestRepository = remember { FriendlyRequestRepository(apiClient) }
    val fixtureRepository = remember { FixtureRepository(apiClient) }
    val reportRepository = remember { ReportRepository(apiClient) }
    val memberRepository = remember { MemberRepository(apiClient) }
    val verificationRepository = remember { VerificationRepository(apiClient) }
    val privacyRepository = remember { PrivacyRepository(apiClient) }
    val notificationRepository = remember { NotificationRepository(apiClient) }
    val geocodeRepository = remember { GeocodeRepository() }
    val conversationRepository = remember { ConversationRepository(apiClient) }
    val navigator = remember { Navigator(Route.Splash) }

    GffhTheme {
        val route = navigator.current
        val tab = tabFor(route)
        val screen: @Composable () -> Unit = {
            when (route) {
            is Route.Splash -> SplashScreen(authRepository, navigator)
            is Route.Welcome -> WelcomeScreen(navigator)
            is Route.Register -> RegisterScreen(authRepository, navigator)
            is Route.SignIn -> SignInScreen(authRepository, navigator)
            is Route.ForgotPassword -> ForgotPasswordScreen(authRepository, navigator, route.prefilledEmail)
            is Route.EmailVerification -> EmailVerificationScreen(authRepository, navigator)

            is Route.RoleSelection -> RoleSelectionScreen(memberRepository, currentTeamStore, navigator)
            is Route.CreateClub -> CreateClubScreen(clubRepository, geocodeRepository, authRepository, navigator)
            is Route.CreateTeam -> CreateTeamScreen(teamRepository, geocodeRepository, navigator, route.clubId)
            is Route.AddFirstVenue -> AddFirstVenueScreen(venueRepository, geocodeRepository, navigator, route.clubId, route.teamId)
            is Route.AddFirstAvailability -> AddFirstAvailabilityScreen(
                availabilityRepository, venueRepository, navigator, route.teamId, route.clubId
            )
            is Route.OnboardingComplete -> OnboardingCompleteScreen(teamRepository, currentTeamStore, navigator, route.teamId)
            is Route.TeamProfile -> TeamProfileScreen(teamRepository, venueRepository, navigator, route.teamId)
            is Route.EditTeam -> EditTeamScreen(teamRepository, navigator, route.teamId)
            is Route.Settings -> SettingsScreen(authRepository, currentTeamStore, navigator)
            is Route.ReportBlock -> ReportBlockScreen(
                reportRepository, currentTeamStore, navigator, route.teamId, route.teamName, route.fixtureId
            )
            is Route.VenuesList -> VenuesListScreen(venueRepository, navigator, route.clubId)
            is Route.EditVenue -> EditVenueScreen(venueRepository, geocodeRepository, navigator, route.venueId, route.clubId)
            is Route.ClubProfile -> ClubProfileScreen(
                clubRepository, teamRepository, currentTeamStore, navigator, route.clubId
            )
            is Route.EditClub -> EditClubScreen(clubRepository, navigator, route.clubId)
            is Route.Members -> MembersScreen(memberRepository, navigator, route.teamId)
            is Route.Verification -> VerificationScreen(verificationRepository, teamRepository, navigator, route.teamId)
            is Route.Privacy -> PrivacyScreen(privacyRepository, authRepository, currentTeamStore, navigator)
            is Route.Help -> HelpScreen(navigator)
            is Route.NotificationPreferences -> NotificationPreferencesScreen(notificationRepository, navigator)

            is Route.Home -> HomeScreen(
                authRepository, teamRepository, availabilityRepository, fixtureRepository,
                notificationRepository, currentTeamStore, navigator
            )
            is Route.Notifications -> NotificationsScreen(notificationRepository, navigator)
            is Route.Messages -> ConversationsListScreen(conversationRepository, currentTeamStore, navigator)
            is Route.ConversationThread -> ConversationThreadScreen(
                conversationRepository, currentTeamStore, navigator, route.conversationId
            )
            is Route.Calendar -> CalendarScreen(availabilityRepository, fixtureRepository, friendlyRequestRepository, currentTeamStore, navigator)
            is Route.DayDetail -> DayDetailScreen(availabilityRepository, fixtureRepository, friendlyRequestRepository, currentTeamStore, navigator, route.date)
            is Route.EditAvailabilitySlot -> EditAvailabilitySlotScreen(
                availabilityRepository, venueRepository, currentTeamStore, navigator, route.slotId, route.date
            )
            is Route.BulkAddAvailability -> BulkAddAvailabilityScreen(
                availabilityRepository, venueRepository, currentTeamStore, navigator
            )

            is Route.SuggestedMatches -> SuggestedMatchesScreen(matchRepository, currentTeamStore, resultsCache, navigator)
            is Route.SearchEntry -> SearchEntryScreen(
                matchRepository, teamRepository, availabilityRepository, currentTeamStore, filterState, resultsCache, navigator
            )
            is Route.Filters -> FiltersScreen(filterState, navigator)
            is Route.Results -> ResultsListScreen(matchRepository, currentTeamStore, filterState, resultsCache, navigator)
            is Route.ResultsMap -> ResultsMapScreen(teamRepository, geocodeRepository, currentTeamStore, resultsCache, navigator)
            is Route.OpponentProfile -> OpponentProfileScreen(
                conversationRepository, currentTeamStore, resultsCache, navigator, route.teamId
            )
            is Route.MatchExplanation -> MatchExplanationScreen(resultsCache, navigator, route.teamId)

            is Route.InvitationComposer -> InvitationComposerScreen(
                teamRepository, venueRepository, currentTeamStore, resultsCache, invitationDraft, navigator, route.opponentTeamId
            )
            is Route.InvitationReview -> InvitationReviewScreen(friendlyRequestRepository, invitationDraft, navigator)
            is Route.InvitationSent -> InvitationSentScreen(navigator, route.requestId)
            is Route.RequestDetail -> RequestDetailScreen(
                friendlyRequestRepository, conversationRepository, currentTeamStore, navigator, route.requestId
            )
            is Route.SuggestChanges -> SuggestChangesScreen(friendlyRequestRepository, navigator, route.requestId)
            is Route.DeclineRequest -> DeclineScreen(friendlyRequestRepository, navigator, route.requestId)
            is Route.Fixtures -> FixturesScreen(friendlyRequestRepository, fixtureRepository, currentTeamStore, navigator)
            is Route.FixtureDetail -> FixtureDetailScreen(
                fixtureRepository, conversationRepository, currentTeamStore, navigator, route.fixtureId
            )

            is Route.Placeholder -> PlaceholderScreen(route.label, navigator)
            }
        }

        if (tab != null) {
            AppScaffold(navigator, tab) { screen() }
        } else {
            screen()
        }
    }
}
