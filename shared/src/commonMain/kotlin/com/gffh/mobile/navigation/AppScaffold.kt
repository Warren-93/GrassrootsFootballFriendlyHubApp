package com.gffh.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The 5 sections of the app's persistent bottom nav (mirrors gffh-web's top
 * nav bar, adapted for phone width). "Publish availability", "Team & club"
 * and "Account/Settings" don't get their own tabs - they're reachable from
 * inside Home and from the Profile tab, per the concept's mobile IA. See
 * [tabFor] for the exact route-to-tab grouping.
 */
enum class AppTab(val rootRoute: Route, val label: String, val icon: ImageVector) {
    HOME(Route.Home, "Home", Icons.Default.Home),
    DISCOVER(Route.SearchEntry, "Discover", Icons.Default.Search),
    ARRANGE(Route.Fixtures, "Arrange", Icons.AutoMirrored.Filled.List),
    MESSAGES(Route.Messages, "Messages", Icons.AutoMirrored.Filled.Chat),
    PROFILE(Route.Settings, "Profile", Icons.Default.Person),
}

/**
 * Which tab (if any) [route] belongs to. Returns null for routes that render
 * full-screen outside the tab shell (auth, onboarding) - App.kt uses that to
 * decide whether to wrap the screen in [AppScaffold] at all.
 */
fun tabFor(route: Route): AppTab? = when (route) {
    // Home (SCR-HM-01/02)
    is Route.Home, is Route.Notifications -> AppTab.HOME

    // Discover (SCR-FF-*, plus the Home-surfaced SCR-HM-03 suggestions feed)
    is Route.SearchEntry,
    is Route.Filters,
    is Route.Results,
    is Route.ResultsMap,
    is Route.OpponentProfile,
    is Route.MatchExplanation,
    is Route.SuggestedMatches -> AppTab.DISCOVER

    // Arrange (SCR-IN-*, SCR-FX-*)
    is Route.InvitationComposer,
    is Route.InvitationReview,
    is Route.InvitationSent,
    is Route.RequestDetail,
    is Route.SuggestChanges,
    is Route.DeclineRequest,
    is Route.Fixtures,
    is Route.FixtureDetail -> AppTab.ARRANGE

    // Messages
    is Route.Messages, is Route.ConversationThread -> AppTab.MESSAGES

    // Profile (Team & club, Account/Settings, Publish availability all live here on mobile)
    is Route.TeamProfile,
    is Route.EditTeam,
    is Route.Settings,
    is Route.ReportBlock,
    is Route.BlockedTeams,
    is Route.VenuesList,
    is Route.EditVenue,
    is Route.ClubProfile,
    is Route.EditClub,
    is Route.Members,
    is Route.Verification,
    is Route.Privacy,
    is Route.Help,
    is Route.NotificationPreferences,
    is Route.Calendar,
    is Route.DayDetail,
    is Route.EditAvailabilitySlot,
    is Route.BulkAddAvailability -> AppTab.PROFILE

    // Access, Setup and the untargeted Placeholder fallback render full-screen,
    // with no persistent chrome.
    is Route.Splash,
    is Route.Welcome,
    is Route.Register,
    is Route.SignIn,
    is Route.ForgotPassword,
    is Route.EmailVerification,
    is Route.RoleSelection,
    is Route.CreateClub,
    is Route.CreateTeam,
    is Route.AddFirstVenue,
    is Route.AddFirstAvailability,
    is Route.OnboardingComplete,
    is Route.Placeholder -> null
}

/**
 * Wraps [content] in a Material3 [Scaffold] with the persistent 5-item bottom
 * nav bar. Tapping a tab resets the back stack to that tab's root route
 * ([Navigator.resetTo]) so switching sections feels like switching sections
 * rather than piling onto an ever-growing stack; screens still [Navigator.push]
 * for in-section drilldowns exactly as before.
 */
@Composable
fun AppScaffold(navigator: Navigator, selectedTab: AppTab?, content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { if (tab != selectedTab) navigator.resetTo(tab.rootRoute) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            content()
        }
    }
}
