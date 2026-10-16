package com.gffh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.core.auth.createSettings
import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.network.defaultApiBaseUrl
import com.gffh.mobile.core.push.NotificationTaps
import com.gffh.mobile.core.push.PushNotificationSetup
import com.gffh.mobile.core.push.PushRegistration
import com.gffh.mobile.core.push.teamToSwitchTo
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
import com.gffh.mobile.feature.profile.BlockedTeamsScreen
import com.gffh.mobile.feature.profile.ReportBlockScreen
import com.gffh.mobile.feature.profile.SettingsScreen
import com.gffh.mobile.feature.profile.TeamProfileScreen
import com.gffh.mobile.feature.profile.VenuesListScreen
import com.gffh.mobile.feature.profile.VerificationScreen
import com.gffh.mobile.feature.profile.PrivacyScreen
import com.gffh.mobile.feature.profile.HelpScreen
import com.gffh.mobile.feature.placeholder.MemberFixtureDetailScreen
import com.gffh.mobile.feature.placeholder.MemberNoticeScreen
import com.gffh.mobile.navigation.AppScaffold
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.navigation.tabFor
import com.gffh.mobile.repository.*
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.InvitationDraftState
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import com.gffh.mobile.session.toActiveTeam
import com.gffh.mobile.theme.GffhTheme

/**
 * What a member who can't manage their team (joined with a code) may still open:
 * sign-in and onboarding (to create their own team), their team's profile, a
 * read-only view of its fixtures (MemberFixtureDetailScreen), and their own
 * account settings.
 */
private fun memberCanOpen(route: Route, teamId: String): Boolean = when (route) {
    is Route.TeamProfile -> route.teamId == teamId
    is Route.Home, is Route.Calendar, is Route.DayDetail, is Route.EditAvailabilitySlot,
    is Route.BulkAddAvailability, is Route.SuggestedMatches, is Route.SearchEntry, is Route.Filters,
    is Route.Results, is Route.ResultsMap, is Route.OpponentProfile, is Route.MatchExplanation,
    is Route.InvitationComposer, is Route.InvitationReview, is Route.InvitationSent, is Route.RequestDetail,
    is Route.SuggestChanges, is Route.DeclineRequest, is Route.Fixtures,
    is Route.Messages, is Route.ConversationThread, is Route.EditTeam, is Route.VenuesList, is Route.EditVenue,
    is Route.ClubProfile, is Route.EditClub, is Route.Members, is Route.Verification, is Route.ReportBlock,
    is Route.BlockedTeams -> false
    else -> true
}

@Composable
fun App(apiBaseUrl: String = defaultApiBaseUrl()) {
    val settings = remember { createSettings() }
    val tokenStore = remember { TokenStore(settings) }
    val currentTeamStore = remember { CurrentTeamStore(settings) }
    val filterState = remember { SearchFilterState() }
    val resultsCache = remember { SearchResultsCache() }
    val invitationDraft = remember { InvitationDraftState() }
    val apiClient = remember { ApiClient(apiBaseUrl, tokenStore) }
    val pushTokenRepository = remember { PushTokenRepository(apiClient) }
    val pushRegistration = remember { PushRegistration(pushTokenRepository, tokenStore, settings) }
    val authRepository = remember { AuthRepository(apiClient, tokenStore, pushRegistration) }
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
    val configRepository = remember { ConfigRepository(apiClient) }
    val navigator = remember { Navigator(Route.Splash) }

    // Sends this device's push token to the API whenever there's both a token
    // and a signed-in session, for the life of the app - see PushRegistration.
    LaunchedEffect(Unit) { pushRegistration.keepRegistered() }

    GffhTheme {
        val route = navigator.current
        val tab = tabFor(route)
        val activeTeam = currentTeamStore.active.collectAsState().value
        val signedIn = tokenStore.session.collectAsState().value != null
        PushNotificationSetup(signedIn)
        val memberTeam = activeTeam?.takeIf { !it.canManage }
        val memberBlocked = memberTeam != null && !memberCanOpen(route, memberTeam.teamId)
        // A member may since have been made a manager: check again whenever the app opens on them.
        LaunchedEffect(activeTeam?.teamId, activeTeam?.canManage) {
            val current = activeTeam ?: return@LaunchedEffect
            if (current.canManage) return@LaunchedEffect
            val mine = teamRepository.mine()
            if (mine is ApiResult.Success) {
                val t = mine.value.firstOrNull { it.id == current.teamId } ?: return@LaunchedEffect
                if (t.canManage == true) currentTeamStore.set(t.toActiveTeam())
            }
        }
        // A tapped push (NotificationTaps) opens its screen once there's somewhere
        // to open it from: someone signed in, on a team, past splash and onboarding.
        // Pushes go to a team's managers, so one about another team the user
        // manages switches to that team first, as the dashboard's switcher would,
        // and opens over that team's dashboard: the screens underneath were the
        // other team's, and would act for the wrong team if you went back to them.
        val pendingTap = NotificationTaps.pending.collectAsState().value
        val readyForTaps = signedIn && activeTeam != null && tab != null
        LaunchedEffect(pendingTap, readyForTaps) {
            val tap = pendingTap ?: return@LaunchedEffect
            if (!readyForTaps) return@LaunchedEffect
            val activeTeamId = currentTeamStore.active.value?.teamId
            if (tap.teamId != null && tap.teamId != activeTeamId) {
                val mine = teamRepository.mine()
                if (mine is ApiResult.Success) {
                    teamToSwitchTo(tap, activeTeamId, mine.value)?.let { t ->
                        currentTeamStore.set(t.toActiveTeam())
                        navigator.resetTo(Route.Home)
                    }
                }
            }
            val target = tap.route() ?: Route.Notifications
            if (navigator.current != target) navigator.push(target)
            NotificationTaps.consume(tap)
        }
        val screen: @Composable () -> Unit = {
            if (memberBlocked) MemberNoticeScreen(memberTeam, fixtureRepository, navigator) else when (route) {
            is Route.Splash -> SplashScreen(authRepository, navigator)
            is Route.Welcome -> WelcomeScreen(authRepository, configRepository, teamRepository, navigator)
            is Route.Register -> RegisterScreen(authRepository, configRepository, teamRepository, navigator)
            is Route.SignIn -> SignInScreen(authRepository, configRepository, teamRepository, navigator)
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
            is Route.TeamProfile -> TeamProfileScreen(teamRepository, venueRepository, currentTeamStore, navigator, route.teamId)
            is Route.EditTeam -> EditTeamScreen(teamRepository, currentTeamStore, navigator, route.teamId)
            is Route.Settings -> SettingsScreen(authRepository, currentTeamStore, navigator)
            is Route.ReportBlock -> ReportBlockScreen(
                reportRepository, currentTeamStore, navigator, route.teamId, route.teamName, route.fixtureId
            )
            is Route.BlockedTeams -> BlockedTeamsScreen(reportRepository, currentTeamStore, navigator)
            is Route.VenuesList -> VenuesListScreen(venueRepository, currentTeamStore, navigator, route.clubId)
            is Route.EditVenue -> EditVenueScreen(
                venueRepository, geocodeRepository, currentTeamStore, navigator, route.venueId, route.clubId
            )
            is Route.ClubProfile -> ClubProfileScreen(
                clubRepository, teamRepository, currentTeamStore, navigator, route.clubId
            )
            is Route.EditClub -> EditClubScreen(clubRepository, navigator, route.clubId)
            is Route.Members -> MembersScreen(memberRepository, navigator, route.teamId)
            is Route.Verification -> VerificationScreen(verificationRepository, teamRepository, navigator, route.teamId)
            is Route.Privacy -> PrivacyScreen(privacyRepository, authRepository, teamRepository, currentTeamStore, navigator)
            is Route.Help -> HelpScreen(configRepository, navigator)
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

            is Route.SuggestedMatches -> SuggestedMatchesScreen(matchRepository, currentTeamStore, filterState, resultsCache, navigator)
            is Route.SearchEntry -> SearchEntryScreen(
                matchRepository, teamRepository, availabilityRepository, currentTeamStore, filterState, resultsCache, navigator
            )
            is Route.Filters -> FiltersScreen(matchRepository, currentTeamStore, filterState, resultsCache, navigator)
            is Route.Results -> ResultsListScreen(filterState, resultsCache, navigator)
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
            is Route.SuggestChanges -> SuggestChangesScreen(
                friendlyRequestRepository, venueRepository, currentTeamStore, navigator, route.requestId
            )
            is Route.DeclineRequest -> DeclineScreen(friendlyRequestRepository, navigator, route.requestId)
            is Route.Fixtures -> FixturesScreen(friendlyRequestRepository, fixtureRepository, currentTeamStore, navigator)
            is Route.FixtureDetail -> if (memberTeam != null) {
                MemberFixtureDetailScreen(fixtureRepository, memberTeam, navigator, route.fixtureId)
            } else {
                FixtureDetailScreen(
                    fixtureRepository, conversationRepository, currentTeamStore, navigator, route.fixtureId
                )
            }

            is Route.Placeholder -> PlaceholderScreen(route.label, navigator)
            }
        }

        // Keyed on the route, so a screen opened over another of its kind - a
        // tapped push's conversation, fixture or request on top of the one you
        // were reading - starts afresh instead of keeping that one's messages,
        // draft or fixture until its own have loaded.
        if (tab != null) {
            AppScaffold(navigator, tab) { key(route) { screen() } }
        } else {
            // Android draws edge to edge (and iOS always has), so screens without the
            // tab scaffold keep clear of the status bar, notch and keyboard here.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
                key(route) { screen() }
            }
        }
    }
}
