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

- **Android emulator**: `10.0.2.2` reaches the host machine's `localhost` —
  this is the default in `HttpClientEngine.android.kt`.
- **iOS simulator**: shares the host's network namespace, so `localhost`
  works directly — the default in `HttpClientEngine.ios.kt`.
- **A physical device**: neither applies. Override `defaultApiBaseUrl()` with
  the host machine's real LAN address, or add a build-time config mechanism
  before shipping past local development.

Start the backend first (from `gffh-api/`):

```bash
docker compose up -d
mvn spring-boot:run
```

## Architecture

Follows Technical Specification section 4/5, simplified where the full
ceremony wasn't worth it at this size:

```
shared/src/commonMain/kotlin/com/gffh/mobile/
├── core/
│   ├── network/       Ktor HttpClient wrapper, ApiResult (success/failure, never throws)
│   ├── auth/           TokenStore (session persistence via multiplatform-settings)
│   ├── validation/     Field-level rules mirrored from the Screen Build Specification
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
  onboarding completes; every tab-root screen reads it. The spec assumes a
  "list every team I manage" endpoint for its team switcher (SCR-HM-01); that
  endpoint doesn't exist yet, so this holds only the one most-recently-active
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
| Access | SCR-AU-01 to 06 | **Built.** Register, sign in, forgot password, email verification, session resolve. |
| Setup | SCR-ON-01 to 06, SCR-PR-01/02 | **Built.** Role selection, create club, 4-step create team, add venue, add availability, onboarding complete, team profile, edit team. |
| Publish | SCR-AV-01 to 04, SCR-HM-01 | **Built.** Calendar (grid + list parity), day detail, add/edit availability slot, bulk add. The dashboard is real for availability/profile-completeness/fixtures/messages; the team switcher is still limited to one active team (see `CurrentTeamStore`) since the "list every team I manage" endpoint doesn't exist yet. |
| Discover | SCR-FF-01 to 06 | **Built.** Search entry, filters, results list (with score/reason chips), a relative-position map plot, opponent profile, match explanation. |
| Arrange | SCR-IN-01 to 06, SCR-FX-01 to 06 | **Built.** Invitation composer/review/sent, request detail (actions rendered strictly from the server's `availableActions`), suggest changes, decline, fixtures list, fixture detail, cancellation. |
| Communicate | SCR-HM-02, messaging | **Built**, and redesigned since the spec: messaging is no longer fixture-scoped (SCR-FX-05) - team-to-team conversations are available as soon as a team publishes availability, from an inbox and a real chat thread, well before any request exists. Notification centre and preferences are built. |
| Govern | SCR-PR-04/07/10/11/12, ADM-01 to 09 | **Built.** Team members, verification submission, privacy/data export & delete, report/block, help. ADM-* is a separate responsive web app (`gffh-admin`), not part of this mobile client. |

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

- **No real multi-team switcher.** `CurrentTeamStore` holds only the single
  most-recently-active team; the backend has no "list every team I manage"
  endpoint yet for `SCR-HM-01`'s team switcher.
- **No push notification delivery.** The notification centre and unread
  count are real, server-backed data; there's no push transport, only
  poll-on-open.
- **No email delivery.** The backend logs verification and reset tokens
  instead of emailing them (no provider configured) - see
  `VerificationTokenService` on the backend. The client-side flows are real
  and fully wired; only delivery is stubbed.

## What was simplified from spec, deliberately

- **No social sign-in.** Volume 1/2 both list Apple and Google sign-in on
  SCR-AU-02. The backend only implements email/password
  (`gffh-api/src/main/java/com/gffh/api/web/AuthController.java`), so those
  buttons are omitted rather than shown non-functional.
- **No password-reset confirmation screen.** SCR-AU-05 only specifies the
  *request* screen; the emailed link's destination isn't one of the 38
  screens in Volume 2, so it isn't guessed at here.
  `AuthRepository.confirmPasswordReset` exists for whenever that surface is
  defined.
- **No foreground-resume polling on SCR-AU-06.** True poll-on-foreground
  needs a platform lifecycle hook not wired up in this slice; a "Check now"
  button covers the same need explicitly.
