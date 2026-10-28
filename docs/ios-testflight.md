# Getting Kickabout onto TestFlight (no Mac needed)

Everything below is done in a web browser, plus a few commands on a Windows
PC. The app itself is built, signed and uploaded by the **iOS TestFlight**
GitHub Actions workflow (`.github/workflows/ios-testflight.yml`), on GitHub's
own Macs - the same way **Android Release Bundle** builds the Play bundle.

The iOS app's bundle ID is **`com.gffh.mobile.ios`** (Android's package is
`com.gffh.mobile.android`). It is permanent once registered, so use exactly
that.

You do this once. After that, each new TestFlight build is one click:
Actions > iOS TestFlight > Run workflow.

## 1. Join the Apple Developer Program

At [developer.apple.com/programs/enroll](https://developer.apple.com/programs/enroll),
sign in with your Apple Account and enrol (99 USD a year).

- **Individual** is quickest (usually same day). The App Store then lists
  your own name as the seller.
- **Organisation** shows the club or company name instead, but needs a
  D-U-N-S number and takes a week or two. An individual account can be
  moved to an organisation later through Apple Developer Support, so
  starting as an individual doesn't hold up TestFlight.

Once you're in, note your **Team ID**. You'll find it under Account >
Membership details (10 characters, such as `AB12CD34EF`).

## 2. Register the app's ID

In [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list),
go to **Identifiers**, then **+** > **App IDs** > **App**:

- Description: `Kickabout`
- Bundle ID: **Explicit**, `com.gffh.mobile.ios`
- Capabilities: tick **Push Notifications** and **Sign In with Apple**

Then **Continue** > **Register**.

## 3. Make the distribution certificate (on Windows)

This is the iOS version of your Android upload keystore. Open **Git Bash**,
which comes with Git for Windows and includes `openssl`. Then run:

```bash
cd /d/mrren/GithubProjects/Keys
openssl genrsa -out kickabout-ios-dist.key 2048
openssl req -new -key kickabout-ios-dist.key -out kickabout-ios-dist.csr \
  -subj "/emailAddress=YOUR_APPLE_ACCOUNT_EMAIL/CN=Mark/C=GB"
```

In the portal, go to **Certificates**, then **+** > **Apple Distribution** >
**Continue**. Upload `kickabout-ios-dist.csr` and download the certificate
(`distribution.cer`) into the same folder. Back in Git Bash:

```bash
openssl x509 -inform DER -in distribution.cer -out kickabout-ios-dist.pem
openssl pkcs12 -export -legacy -inkey kickabout-ios-dist.key -in kickabout-ios-dist.pem \
  -name "Apple Distribution" -out kickabout-ios-dist.p12
```

It asks for an export password. Choose one and keep it, because it becomes
`IOS_DIST_CERT_PASSWORD`. If `-legacy` is rejected, your openssl is older
and doesn't need it, so run the command again without it.

Keep `kickabout-ios-dist.key` and `kickabout-ios-dist.p12` as safe as the
Android keystore, and never commit them.

## 4. Make the App Store provisioning profile

In the portal, go to **Profiles**, then **+**. Under **Distribution**, pick
**App Store Connect** > **Continue**:

- App ID: `Kickabout (com.gffh.mobile.ios)`
- Certificate: the Apple Distribution certificate from step 3
- Name: `Kickabout App Store`

Download it (`Kickabout_App_Store.mobileprovision`).

## 5. Create the app in App Store Connect

At [appstoreconnect.apple.com](https://appstoreconnect.apple.com), go to
**Apps**, then **+** > **New App**:

- Platform: iOS
- Name: `Kickabout`. Store names are unique, so if it's taken, try something
  like `Kickabout: Football Friendlies`. The name under the icon on the phone
  stays "Kickabout" either way.
- Primary language: English (U.K.)
- Bundle ID: `com.gffh.mobile.ios`
- SKU: `kickabout-ios`
- User access: Full Access

## 6. Make an App Store Connect API key

This is what lets GitHub upload builds. Go to **Users and Access** >
**Integrations** > **App Store Connect API**. Under **Team Keys**, click
**+**, name it `GitHub TestFlight`, and set Access to **App Manager**.

Then:

- Download the `.p8` file. Apple only lets you download it once.
- Note the **Key ID**, shown in the key's row.
- Note the **Issuer ID**, shown above the table.

## 7. Add the GitHub secrets

In the App repository, go to **Settings** > **Secrets and variables** >
**Actions** > **New repository secret**. Three of the secrets are files
turned into base64 text. In Command Prompt, this copies a file's base64 to
the clipboard (change the path for each file):

```bat
powershell -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('D:\mrren\GithubProjects\Keys\kickabout-ios-dist.p12')) | Set-Clipboard"
```

| Secret | Value |
| --- | --- |
| `IOS_DIST_CERT_P12_BASE64` | base64 of `kickabout-ios-dist.p12` |
| `IOS_DIST_CERT_PASSWORD` | the export password from step 3 |
| `IOS_PROVISIONING_PROFILE_BASE64` | base64 of `Kickabout_App_Store.mobileprovision` |
| `APP_STORE_CONNECT_KEY_ID` | the Key ID from step 6 |
| `APP_STORE_CONNECT_ISSUER_ID` | the Issuer ID from step 6 |
| `APP_STORE_CONNECT_KEY_BASE64` | base64 of the `AuthKey_XXXX.p8` from step 6 |

Under the **Variables** tab:

| Variable | Value |
| --- | --- |
| `GFFH_API_BASE_URL` | already set for Android; the iOS build reads it too |
| `GOOGLE_IOS_CLIENT_ID` | from step 9 (optional; without it, iOS offers Apple sign-in only) |

## 8. Turn on Sign in with Apple and push on the API (Render)

On the API's Render service, under Environment:

- `APPLE_BUNDLE_ID` = `com.gffh.mobile.ios`. This turns on "Continue with
  Apple" in the iOS app.
- Push notifications need a key. In the portal, go to **Keys**, then **+**,
  name it `Kickabout push`, tick **Apple Push Notifications service (APNs)**,
  and click **Register**. Download the `.p8` (again, only once). Then set:
  - `APNS_KEY_ID` = that key's ID
  - `APNS_TEAM_ID` = your Team ID
  - `APNS_PRIVATE_KEY` = the whole text of the `.p8` file, including the
    BEGIN/END lines
  - `APNS_BUNDLE_ID` = `com.gffh.mobile.ios`
  - `APNS_PRODUCTION` = `true`, because TestFlight and App Store builds use
    Apple's production push service

## 9. Google sign-in on iPhone (optional, recommended)

People who signed up with Google on Android or the web need this to sign in
on an iPhone. Apple's "Hide my email" gives them a different address, so
Apple sign-in alone would make them a second account.

1. In the Google Cloud console, under **APIs & Services** > **Credentials**
   in the same project as the web client, choose **Create credentials** >
   **OAuth client ID** > **iOS**. Use the name `Kickabout iOS` and the bundle
   ID `com.gffh.mobile.ios`. Copy the client ID, which ends in
   `.apps.googleusercontent.com`.
2. On Render, add it to `GOOGLE_ADDITIONAL_CLIENT_IDS`, comma-separated with
   anything already there.
3. In GitHub, set the repository variable `GOOGLE_IOS_CLIENT_ID` to it.

The app only shows "Continue with Google" on iOS when Apple sign-in is on as
well. App Review rejects apps that offer Google sign-in without Apple's.

## 10. Build and upload

In the App repository, go to **Actions** > **iOS TestFlight** > **Run
workflow** on `main`. Leave both boxes blank for version 0.1.0 with the run
number as the build number. It takes about 30-45 minutes. When it's green,
App Store Connect > **TestFlight** shows the build as *Processing* for a few
minutes, then ready.

## 11. Test it on a phone

- **Internal testing** (no Apple review, available straight away): go to
  **TestFlight** > **Internal Testing** > **+**, create a group (for
  example, `Team`), and add yourself and anyone you've added under Users and
  Access (up to 100). They install **TestFlight** from the App Store and
  accept the invite.
- **External testing** (beta testers by email or a public link, up to
  10,000): create an external group. The first build goes through Beta App
  Review, usually within a day. Fill in **Test Information** first:
  - a feedback email and a short description
  - a **demo account** (an email and password for a Kickabout account that
    already has a team), so the reviewer can sign in

## Before the App Store itself (not needed for TestFlight)

- The **App Privacy** questionnaire. Kickabout collects name and email
  (Contact Info), chat messages and badge photos (User Content), a user ID
  (Identifiers), and team postcode/venue locations (Coarse Location). All of
  it is linked to the account and none of it is used for tracking.
- Privacy policy URL:
  `https://grassroots-football-friendly-hub-we.vercel.app/privacy-policy`
- Screenshots (6.9" iPhone), a description, keywords, a support URL and an
  age rating.

## Once a year

The distribution certificate and provisioning profile expire after a year.
Repeat steps 3 and 4, then update `IOS_DIST_CERT_P12_BASE64`,
`IOS_DIST_CERT_PASSWORD` and `IOS_PROVISIONING_PROFILE_BASE64`. The API keys
don't expire.
