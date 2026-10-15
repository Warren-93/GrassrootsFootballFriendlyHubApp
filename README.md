# Grassroots Football Friendly Hub — Mobile

Kotlin Multiplatform + Compose Multiplatform client for [gffh-api](../gffh-api),
targeting Android and iOS from one shared UI codebase.

## Why Compose Multiplatform, given the Technical Specification proposed Ktor

The Technical Specification's backend direction (Kotlin + Ktor) was superseded
by Java + Spring Boot — see [gffh-api/README.md](../gffh-api/README.md) — for
practical reasons specific to the backend team. That decision does not extend
to the client: the specification's client architecture (section 4) already
named Kotlin Multiplatform with "Compose Multiplatform or native UI" as the
two options, and this project takes the first. The backend speaks plain JSON
over HTTPS either way, so the language it happens to be written in is
invisible to the client.

## Environment this was built in

Windows, no Xcode. The Android target builds and has been verified with
`:androidApp:assembleDebug`. The iOS targets compile from this machine too
(`./gradlew :shared:compileKotlinIosSimulatorArm64 -Pgffh.enableIos=true` and
the `iosArm64` equivalent both succeed on plain Windows — Kotlin/Native's
frontend/klib compilation is host-independent) but the final Xcode-level
framework link genuinely does need a Mac, so that step - and the actual
`iosApp` build - is verified by the `iOS Simulator Build` GitHub Actions
workflow (macOS runner) rather than locally on this machine.

To build the full iOS app on a Mac with Xcode:

```bash
./gradlew build -Pgffh.enableIos=true
```

or set `gffh.enableIos=true` in `gradle.properties` permanently once iOS
development is the norm. This flag exists specifically so a disk- and
Xcode-constrained machine (like the one this was built on) can still build
and verify the Android target without Kotlin/Native ever being provisioned.

## Running against the backend

The API needs to be reachable from wherever the app runs:

- **Android**: debug builds default to `http://10.0.2.2:8090` (the emulator's
  alias for the host's `localhost`); release builds default to
  `https://grassrootsfootballfriendlyhubapi.onrender.com`. Override either with
  `./gradlew ... -Pgffh.apiBaseUrl=http://192.168.x.y:8090` (a physical device
  needs the host's LAN address) or the `GFFH_API_BASE_URL` environment variable.
  The release workflow reads the repository variable `GFFH_API_BASE_URL`.
- **iOS**: the `GFFH_API_BASE_URL` build setting (Debug: `http://localhost:8090`,
  Release: the Render URL) lands in Info.plist as `GFFHApiBaseUrl`; pass
  `GFFH_API_BASE_URL=...` to `xcodebuild` to change it.

Start the backend first (from `gffh-api/`):

```bash
docker compose up -d
mvn spring-boot:run
```

## Push notifications

Both platforms send their device token to `POST /api/v1/me/push-tokens` once
there's a token and a signed-in session, and call
`DELETE /api/v1/me/push-tokens` on sign-out before dropping the access token -
see `core/push/PushRegistration.kt`. The API decides what to send, and to
whom, from each manager's notification preferences; it needs its own FCM and
APNs credentials for that (`FCM_SERVICE_ACCOUNT_JSON`, `APNS_KEY_ID`,
`APNS_TEAM_ID`, `APNS_PRIVATE_KEY`, `APNS_BUNDLE_ID`, `APNS_PRODUCTION` - see
`gffh-api/README.md`). Both ends are optional: with nothing configured the app
simply never asks for notification permission, and the API sends no push.

### Android (Firebase Cloud Messaging)

There is no `google-services.json` in the repository and no google-services
Gradle plugin. `KickaboutApplication` builds `FirebaseOptions` itself from four
build properties, each of which can come from a Gradle property or an
environment variable of its own (the same pattern as `GFFH_API_BASE_URL`), and
all of which default to blank:

| Gradle property | Environment variable | Where it comes from in the Firebase console |
|---|---|---|
| `gffh.firebaseProjectId` | `FIREBASE_PROJECT_ID` | Project settings > Project ID |
| `gffh.firebaseApplicationId` | `FIREBASE_APPLICATION_ID` | The Android app's App ID, `1:<sender>:android:<hash>` (`mobilesdk_app_id`) |
| `gffh.firebaseApiKey` | `FIREBASE_API_KEY` | The Android app's Web API key (`current_key`) |
| `gffh.firebaseSenderId` | `FIREBASE_SENDER_ID` | Cloud Messaging > Sender ID (`project_number`) |

```bash
./gradlew :androidApp:assembleDebug \
  -Pgffh.firebaseProjectId=kickabout-123 \
  -Pgffh.firebaseApplicationId=1:1234567890:android:abcdef \
  -Pgffh.firebaseApiKey=AIza... \
  -Pgffh.firebaseSenderId=1234567890
```

If any of the four is blank, Firebase is never initialised, no token is ever
requested and nothing asks for the POST_NOTIFICATIONS permission - the app
behaves exactly as it did before push existed. That's also why the manifest
removes `FirebaseInitProvider`: left in, the library would try to configure
itself from resources a `google-services.json` would have generated.

The app asks for POST_NOTIFICATIONS (Android 13+) after sign-in rather than at
launch, so the prompt arrives when notifications are about to start mattering.
While the app is open, pushes arrive in `KickaboutMessagingService`, which
shows them; in the background Android draws them itself. The manifest points
both at the same "Kickabout" notification channel and status-bar icon, and
tapping one opens the app.

The Play release workflow (`android-release.yml`) reads the four from
repository variables of the same names (Settings > Secrets and variables >
Actions > Variables). Until they're added, a bundle it builds has no push.

### iOS (APNs)

`iosApp/iosApp/iOSApp.swift`'s `AppDelegate` asks
`UNUserNotificationCenter` for alert/badge/sound permission at launch,
registers with APNs, and hands the device token (lowercase hex, which is what
the API stores) to the shared code via `PushBridge.kt`. Notifications arriving
while the app is open are shown as banners.

`iosApp/project.yml` adds the `aps-environment: development` entitlement (the
APNs sandbox, which is what a build run from Xcode uses - exporting an archive
for TestFlight or the App Store switches it to `production`) and the
`remote-notification` background mode. Simulator builds don't sign anything,
so the entitlement is inert there and CI keeps building. The API has to match:
`APNS_PRODUCTION=false` for builds run from Xcode, `true` for TestFlight and
App Store builds.

A real device also needs an APNs key for the bundle ID in the Apple developer
account, the same key the API is given as `APNS_PRIVATE_KEY`.

## Google and Apple sign-in

Welcome, Sign in and Register show "Continue with Google" on Android and
"Continue with Apple" on iOS, under an "or" divider, whenever
`GET /api/v1/config` says that provider is on (`google.enabled` with a
`clientId`; `apple.enabled`). With neither set up on the API, those screens are
just the email and password forms. The platform's own sheet hands over an ID
token (`core/auth/SocialSignIn.kt`, with an actual for each platform), the app
sends it to `POST /api/v1/auth/oauth/google` or `/apple`, and stores the
session exactly as a password sign-in does - which is also what registers the
phone for push. A brand-new account (`newUser: true`) goes on to role
selection, as after registering, unless an invitation the API applied at that
sign-in already put it on a team; anyone else goes to the dashboard. Backing out
of the Google or Apple sheet shows nothing; any other failure shows on the
screen's usual error line. Google on iOS and Apple on Android aren't offered.

### Android: Google OAuth clients

Nothing about Google is built into the app - the client ID comes from the API
at runtime. In the Google Cloud console project (APIs & Services >
Credentials, once the OAuth consent screen is set up), create:

1. A **Web application** OAuth client. Its client ID is the API's
   `GOOGLE_CLIENT_ID`; the API hands it to the app as `config.google.clientId`
   and the app gives it to Credential Manager as `serverClientId`, so it's the
   audience of every ID token Google issues to the app. gffh-web's Google
   button uses the same client.
2. An **Android** OAuth client for each key that signs an installed app:
   package name `com.gffh.mobile.android` and that key's SHA-1 fingerprint.
   Google only uses it to recognise the app, so its client ID isn't configured
   anywhere. Debug builds are signed with your local debug keystore
   (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`),
   release builds with the upload key
   (`keytool -list -v -keystore <upload keystore> -alias <key alias>`), and an
   app installed from Google Play is re-signed by Play - its SHA-1 is under
   App integrity > App signing in the Play Console. Each needs its own
   Android client.

Without a matching Android client, Credential Manager refuses (logged under
the `KickaboutSignIn` tag, typically "Developer console is not set up
correctly") and the app says Google sign-in didn't work. The phone needs a
Google account and Google Play services; Credential Manager uses Play services
on Android 13 and older and the system's own on Android 14 and later.

### iOS: the Sign in with Apple capability

1. In the Apple developer account (Certificates, Identifiers & Profiles >
   Identifiers), turn on **Sign in with Apple** for the app's App ID - bundle
   ID `com.gffh.mobile.iosApp`, from `iosApp/project.yml` - as a primary App
   ID, then regenerate its provisioning profiles.
2. `iosApp/project.yml` already adds the `com.apple.developer.applesignin`
   entitlement (`[Default]`, next to `aps-environment`) and links
   `AuthenticationServices.framework`, since the shared framework is static.
   Simulator CI builds sign nothing, so the entitlement is inert there and the
   build still passes - but Apple only signs in a signed build, so in that
   unsigned simulator build the button shows an error.
3. On the API, set `APPLE_BUNDLE_ID=com.gffh.mobile.iosApp`: a token from the
   app has the bundle ID as its audience, and `apple.enabled` is on once
   `APPLE_BUNDLE_ID` or `APPLE_SERVICES_ID` is set (`APPLE_SERVICES_ID` is
   only for the web).

Apple shares the person's name and email only the first time they sign in to
the app. The app sends that name as `displayName`, so the new account isn't
named after its email address.

## Architecture

Follows Technical Specification section 4/5, simplified where the full
ceremony wasn't worth it at this size:

```
shared/src/commonMain/kotlin/com/gffh/mobile/
├── core/
│   ├── network/       Ktor HttpClient wrapper, ApiResult (success/failure, never throws)
│   ├── auth/           TokenStore (session persistence via multiplatform-settings), expect/actual Google/Apple ID tokens
│   ├── validation/     Field-level rules mirrored from the Screen Build Specification
│   ├── image/          Team badges: data-URL decode, expect/actual photo picker
│   ├── push/           Device token store, API registration, permission prompt
│   └── platform/       expect/actual platform intents (e.g. open mail app)
├── model/              Wire DTOs - one file per backend controller, field-for-field
├── repository/         API access, one per backend resource
├── navigation/         Route (sealed class) + Navigator (simple back-stack, no nav library)
├── session/            Cross-screen state that isn't auth: active team, search filters/results
│                       cache, an in-progress invitation draft - see "Architecture" below
├── theme/              Material3 theme - brand-neutral placeholder palette
└── feature/
    ├── auth/           SCR-AU-01 to 06
    ├── onboarding/     SCR-ON-01 to 06, SCR-PR-01/02
    ├── availability/   SCR-AV-01 to 04 (bulk add included)
    ├── discover/       SCR-FF-01 to 06 (map view included, as a relative-position plot rather than a tile-based map SDK)
    ├── arrange/         SCR-IN-01 to 06, SCR-FX-01 to 06 (fixture messaging now opens the conversations thread - see feature/messages/ - cancellation included)
    ├── messages/       Team-to-team chat: inbox + thread, available as soon as a team publishes availability
    ├── home/           SCR-HM-02 notification centre
    ├── profile/        SCR-PR-03/04/07/10/11/12 - club profile, members, verification, privacy, report/block, help
    └── placeholder/    The dashboard (SCR-HM-01) and anything not yet built
```

The specification's `UI → ViewModel → UseCase → Repository → API` layering
was flattened to `UI → Repository → API` for screens this simple (form state
lives directly in the composable via `remember`). Reintroduce a ViewModel
layer if a screen's logic outgrows that.

Four pieces of state don't belong to any one screen, so they live in
`session/` instead, each `remember`-ed once in `App.kt` and threaded down:

- **`CurrentTeamStore`** — which team the app is acting as. Set once
  onboarding completes; every tab-root screen reads it. The dashboard
  reconciles it against `GET /api/v1/teams/mine` and offers a real switcher
  (a dropdown next to the team name) whenever that returns more than one
  team — see the class's own doc comment.
- **`SearchFilterState`** — SCR-FF-02's filter set, read by both the search
  entry screen's implicit-filter summary and the results screen's filter bar.
- **`SearchResultsCache`** — the last opponent search response, so SCR-FF-05
  (opponent profile) and SCR-FF-06 (match explanation) can read a candidate's
  data without a second network call. `GET /api/v1/teams/{id}` requires
  managing the team, which a searching manager never does for an opponent —
  this cache is the deliberate workaround, not an oversight.
- **`InvitationDraftState`** — carries a composed proposal from SCR-IN-01 to
  SCR-IN-02 without threading a dozen parameters through the route.

## What's built vs. what's next

Screen Build Specification section 12 ("Build Sequence") prescribes an order
of build slices, each a testable end-to-end capability. This client follows
it:

| Slice | Screens | Status |
|---|---|---|
| Access | SCR-AU-01 to 06 | **Built.** Register, sign in, forgot password, email verification, session resolve, and "Continue with Google" (Android) or "Continue with Apple" (iOS) when the API has them on - see "Google and Apple sign-in" above. |
| Setup | SCR-ON-01 to 06, SCR-PR-01/02 | **Built.** Role selection, create club, 4-step create team, add venue, add availability, onboarding complete, team profile, edit team - including uploading the team badge from the phone's photos, shown anywhere the app shows a team. |
| Publish | SCR-AV-01 to 04, SCR-HM-01 | **Built.** Calendar (grid + list parity), day detail, add/edit availability slot, bulk add. The dashboard is real for availability/profile-completeness/fixtures/messages, with a team switcher dropdown whenever a manager runs more than one team. |
| Discover | SCR-FF-01 to 06 | **Built.** Search entry, filters, results list (with score/reason chips), a relative-position map plot, opponent profile, match explanation. |
| Arrange | SCR-IN-01 to 06, SCR-FX-01 to 06 | **Built.** Invitation composer/review/sent, request detail (actions rendered strictly from the server's `availableActions`; names the other team and shows the agreed and any proposed venue), suggest changes, decline, fixtures list, fixture detail with a venue card and Google Maps directions, cancellation. |
| Communicate | SCR-HM-02, messaging | **Built**, and redesigned since the spec: messaging is no longer fixture-scoped (SCR-FX-05) - team-to-team conversations are available as soon as a team publishes availability, from an inbox and a real chat thread, well before any request exists. Notification centre and preferences are built, including whether notifications also arrive by email and as phone notifications - and phone notifications are real now, by FCM on Android and APNs on iOS (see "Push notifications" above). |
| Govern | SCR-PR-04/07/10/11/12, ADM-01 to 09 | **Built.** Team members (an email with no account yet gets an emailed invitation, listed as pending until it's used or cancelled), verification submission, privacy/data export & delete, report/block, help (with an Email support button when the API has a support address), change email and password in Settings. ADM-* is a separate responsive web app (`gffh-admin`), not part of this mobile client. |

Routes not covered by any built slice (the codebase has none right now) would
render `PlaceholderScreen` so the app stays navigable without crashing.

### Real gaps found and fixed while building this

Two bugs surfaced only once the app was actually driven on a physical
device, not from reading the code:

- **The bearer token was silently dropped after sign-in.** Ktor's `Auth`
  plugin calls `loadTokens` once and caches the result; since the first
  request in the app's life is always registration (made while signed out),
  it cached "no token" permanently. Fixed by reading `TokenStore` fresh on
  every request instead of relying on the plugin's cache — see `ApiClient`'s
  doc comment.
- **The keyboard covered the stepper's Next/Create buttons.** The Activity
  was missing `android:windowSoftInputMode="adjustResize"`, so the keyboard
  panned the whole screen instead of resizing above it — the primary action
  on every multi-step form was unreachable while typing.
- **`FilterChip` text wrapped vertically, one letter per line**, whenever a
  plain `Row` of chips didn't fit the screen width (e.g. "COMPETITIVE",
  "INDOOR"). Every chip row in the app now uses `FlowRow`.

### Known remaining gaps

- **Tapping a notification only opens the app.** It doesn't deep-link to the
  request, fixture or conversation the push is about, even though the payload
  carries those ids - the app's `Navigator` has no route parser yet.
- **Badges aren't fetched from https links.** A badge the app uploaded is an
  inlined image and shows everywhere; an `https://` badge set elsewhere falls
  back to the team's initials, since there's no image-loading library here.
- **No email delivery.** The backend logs verification and reset tokens
  instead of emailing them (no provider configured) - see
  `VerificationTokenService` on the backend. The client-side flows are real
  and fully wired; only delivery is stubbed.

## What was simplified from spec, deliberately

- **One social sign-in per platform.** Volume 1/2 both list Apple and Google
  sign-in on SCR-AU-02. Android offers Google and iOS offers Apple, each
  through the platform's own sheet; Google on iOS and Apple on Android would
  each need a browser-based flow, so they're left out.
- **No password-reset confirmation screen.** SCR-AU-05 only specifies the
  *request* screen; the emailed link's destination isn't one of the 38
  screens in Volume 2, so it isn't guessed at here.
  `AuthRepository.confirmPasswordReset` exists for whenever that surface is
  defined.
- **No foreground-resume polling on SCR-AU-06.** True poll-on-foreground
  needs a platform lifecycle hook not wired up in this slice; a "Check now"
  button covers the same need explicitly.
