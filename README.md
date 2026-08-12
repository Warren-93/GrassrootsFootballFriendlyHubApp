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
`:androidApp:assembleDebug`. The iOS targets are written (`iosMain` source
sets exist and compile against the same `expect`/`actual` contracts as
Android) but are **not** built here — Kotlin/Native's iOS toolchain needs
several hundred MB to a GB of additional downloads per target, which this
machine's disk didn't have room for on top of the Android SDK, Gradle, and
the backend's own tooling.

To build the iOS target on a Mac with Xcode:

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
    ├── availability/   SCR-AV-01 to 03 (SCR-AV-04 bulk add is P2 in the spec - not built)
    ├── discover/       SCR-FF-01, 02, 03, 05, 06 (SCR-FF-04 map view needs a maps SDK - not built)
    ├── arrange/         SCR-IN-01 to 06, SCR-FX-01 to 04
    └── placeholder/    Everything else, until its slice is built
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
| Publish | SCR-AV-01 to 03, SCR-HM-01 | **Built.** Calendar (grid + list parity), day detail, add/edit availability slot. SCR-AV-04 (bulk add) is explicitly P2 in the spec and wasn't built. The dashboard is real for availability/profile-completeness/fixtures; the team switcher is honestly limited (see `CurrentTeamStore`). |
| Discover | SCR-FF-01, 02, 03, 05, 06 | **Built.** Search entry, filters, results list (with score/reason chips), opponent profile, match explanation. SCR-FF-04 (map view) needs a maps SDK integration and wasn't built. |
| Arrange | SCR-IN-01 to 06, SCR-FX-01 to 04 | **Built.** Invitation composer/review/sent, request detail (actions rendered strictly from the server's `availableActions`), suggest changes, decline, fixtures list (Pending/Confirmed/Completed), fixture detail. |
| Communicate | SCR-FX-05/06, SCR-HM-02, SCR-PR-09 | Blocked — needs backend work (see below) |
| Govern | SCR-PR-07/10/11/12, ADM-01 to 09 | Blocked — needs backend work; ADM-* is a separate web app |

Every route beyond the built slices renders `PlaceholderScreen` so the app is
navigable end-to-end without crashing, even where a screen isn't built yet.

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

### Backend gaps for the Communicate and Govern slices

These screens have no server-side support yet in `gffh-api` and need backend
work before their client screens can be built against a real contract:

- **Messaging** (SCR-FX-05) — `GET/POST /api/v1/fixtures/{id}/messages` don't exist.
- **Fixture cancellation** (SCR-FX-06) — `POST /api/v1/fixtures/{id}/cancel` doesn't
  exist as its own endpoint; a CONFIRMED request can still be cancelled via
  the friendly-request `cancel` action, which is what SCR-FX-04/SCR-IN-04
  expose today.
- **Team member management** (SCR-PR-04) — `Membership` exists server-side
  but has no REST surface (list/invite/change-role/remove).
- **Verification submission** (SCR-PR-07) — evidence upload and a
  verification-request workflow don't exist; verification is currently
  flipped directly in the database for testing.
- **Notifications** (SCR-HM-02, SCR-PR-09) — no notification records, no
  preferences endpoint, no push integration.
- **Reporting and blocking** (SCR-PR-11) — `BlockRepository` exists
  server-side but has no controller; there's no `reports` collection at all.
- **Privacy/data rights** (SCR-PR-10) — no export or delete endpoints.
- **Admin dashboard** (ADM-01 to 09) — a separate responsive web app per the
  specification, not part of this mobile client at all.

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
- **No real email delivery.** The backend logs verification and reset tokens
  instead of emailing them (no provider configured) — see
  `VerificationTokenService` on the backend. The client-side flows are real
  and fully wired; only delivery is stubbed.
- **No foreground-resume polling on SCR-AU-06.** True poll-on-foreground
  needs a platform lifecycle hook not wired up in this slice; a "Check now"
  button covers the same need explicitly.
