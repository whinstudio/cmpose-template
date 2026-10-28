This project was generated with **[cmpose](https://github.com/whinstudio/cmpose)**, the fastest way to bootstrap Compose Multiplatform projects.

## Requirements

<!-- cmpose:requirements -->
| Requirement | Version |
|---|---|
| JDK | 21 |
| Kotlin | 2.3.21 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 9.2.1 |
| Gradle (wrapper) | 9.7.0 |
| Android compileSdk / targetSdk | 36 / 36 |
| Android minSdk | 30 |
| iOS minimum | 18.2 |
| Xcode | 16 or newer |

iOS targets are arm64 only (`iosArm64`, `iosSimulatorArm64`): the simulator needs an Apple Silicon Mac.
<!-- /cmpose:requirements -->

Gradle runs on JDK 21 (`gradle/gradle-daemon-jvm.properties`) and downloads it when it is missing.
Android Studio's bundled JDK (JBR 21) works as is.

## Run

- **Android:** `./gradlew :androidApp:installDebug`, or run `androidApp` from Android Studio.
- **iOS:** open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on a simulator.
  To run on a device, set your Apple team ID as `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.
  It only picks the signing team: the bundle ID stays `PRODUCT_BUNDLE_IDENTIFIER`, the ID to register in Firebase
  and App Store Connect.

## Checks

- `./gradlew detekt`: static analysis (only in projects generated with detekt).
- `./gradlew allTests`: unit tests on the JVM and the iOS simulator. `./gradlew testAndroidHostTest` runs the JVM ones only.
<!-- cmpose:ci -->
- `.github/workflows/ci.yml` runs these on every pull request; start it by hand from the Actions tab (`workflow_dispatch`).
<!-- /cmpose:ci -->

## Logging

`core/logging` sets up [Kermit](https://kermit.touchlab.co) when the app starts (`configureLogging` in `initKoin`): debug builds write every level, release builds only warnings and errors. Android writes to Logcat, iOS to the unified log (Xcode's console, Console.app). Every tag starts with the project's name, so filter on it.

Log through a tagged logger: `private val log = Logger.withTag("Checkout")`, then `log.d { "…" }` or `log.w(throwable) { "…" }`. `println` is not used in this project. Never log tokens, keys, request or response bodies, headers or user data. Debug builds also log each HTTP request's method, URL and status (`core/network`, `RequestLogging.kt`); release builds do not.

## Release

- **Android:** release builds are minified (R8, `androidApp/proguard-rules.pro`). To sign them, copy
  `keystore.properties.example` to `keystore.properties` and fill it in; the file and `*.jks` are git-ignored.
  Without it the release APK is unsigned.
- **Base URL** (projects with the network module): `core/network/.../ApiConfig.kt` holds `DEBUG_BASE_URL`
  and `RELEASE_BASE_URL`; debug builds use the first, release builds the second.

## In-app review

`core/review` asks for a review through the platform's own dialog: Play In-App Review on Android, StoreKit on iOS. Call `ReviewPrompter.recordPositiveEvent()` (Koin) right after a moment the user wanted, such as a finished task or a saved item; the third such moment asks, then every tenth. `ReviewRequestEffect()` in `MainScreen.kt` passes the request on, so keep it. Do not ask at launch or in the middle of a task, do not ask "Do you like the app?" first and do not build a rating screen of your own: Apple and Google allow only the system dialog, and the system decides whether it shows.

A **Rate the app** row or button opens the store page (`ReviewPrompter.openStorePage()`); the showcase has one on its settings screen. On Android this is the Play Store page of your application ID. On iOS, set your App Store ID in `core/review/.../AppStoreId.kt` (`APP_STORE_ID`, App Store Connect → App Information → Apple ID); until then the row shows the review dialog.

Testing: Android shows the dialog only to an app installed from Google Play (use an internal test track); on an emulator nothing appears. iOS shows it every time in debug builds and never in TestFlight.

## Connectivity and updates

- **Offline band** (projects with the network module): `core/network` watches the connection (`ConnectivityMonitor`, from `core/domain`); `OfflineBanner` in `MainScreen.kt` shows "No connection". To load again when the connection comes back, inject `ConnectivityMonitor` and call your retry on `reconnections()` when the last error was `DataError.Network` (the showcase's Home and Onboarding do).
- **Updates** (`core/update`, every project): `AppUpdateEffect()` in `MainScreen.kt` checks at most once a day. Android uses Play In-App Updates (flexible; only for an app installed from Google Play, use an internal test track). iOS compares the App Store's version (`itunes.apple.com/lookup?bundleId=…`) and offers the store page; an app that is not on the store yet is skipped.
- **Forced update:** host a JSON file `{ "minVersion": { "android": 12, "ios": "1.4.0" } }` (Android: `versionCode`, iOS: `CFBundleShortVersionString`) and set `MIN_VERSION_URL` in `core/update/.../UpdateConfig.kt`; below it, the app shows an "Update required" screen that cannot be closed. Android projects without the network module and monetization have no `INTERNET` permission: add it back to `AndroidManifest.xml` when you set the URL.

## Images

Images load with [Coil 3](https://coil-kt.github.io/coil/) (`core/image`): `AsyncImage(model = url, …)` in any feature screen, or `KtAsyncImage` from the design system. `ImageLoaderSetup()` in `MainScreen.kt` configures the one loader: a memory cache, a 50 MB disk cache in the platform's cache directory and, in debug builds, Coil's logs in Kermit (tag `Image`). Network images need the network module (`coil-network-ktor3` lives in `core/network`); without it, local images (`Res`, files) still load.

## Deep links

The app opens links with its own scheme, `multiplatform://` (`DEEP_LINK_SCHEME` in `core/navigation/.../DeepLinkConfig.kt`; the generator sets it to your project's name in lower case). A link maps to a screen through a `DeepLinkRoute` (`@Single(binds = [DeepLinkRoute::class])`): `pattern = "orders/{id}"`, and `destination(arguments)` returns the screen's destination or `null`. Each generated feature module has one (`<Module>DeepLink`, path `<module>`); the showcase opens `multiplatform://user/octocat` and `multiplatform://settings`; `multiplatform://` alone just opens the app. Two routes with the same path: the first wins.

Try it: `adb shell am start -a android.intent.action.VIEW -d "multiplatform://settings"` and `xcrun simctl openurl booted "multiplatform://settings"`.

Another app may claim the same scheme; for a more unique one, use your package name (e.g. `com.example.app`) in `DeepLinkConfig.kt`, `AndroidManifest.xml` and `Info.plist` together.

**App Links and Universal Links** (https links to your domain) are ready but off:
1. Set `APP_LINK_HOST` in `DeepLinkConfig.kt` to your domain.
2. Android: uncomment the `autoVerify` intent filter in `AndroidManifest.xml` with your host, and serve `https://<domain>/.well-known/assetlinks.json` with your package name and signing certificate's SHA-256 (`keytool -list -v -keystore …`; Play App Signing shows it in the Play Console).
3. iOS: serve `https://<domain>/.well-known/apple-app-site-association` (`applinks` → your `TEAMID.bundleId`), copy `iosApp/Configuration/UniversalLinks.entitlements.example` to `iosApp/iosApp/iosApp.entitlements` with your domain, add `CODE_SIGN_ENTITLEMENTS = iosApp/iosApp.entitlements` to `Config.xcconfig` and enable Associated Domains for the App ID.

## Before you ship

- **Privacy policy and terms:** set `PRIVACY_POLICY_URL` and `TERMS_URL` in `core/about/.../LegalUrls.kt`. Both stores ask for a privacy policy URL; while a URL is empty its row stays hidden.
- **Version:** raise `versionCode`/`versionName` in `build-logic/.../AppConfig.kt` and `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION` in `iosApp/Configuration/Config.xcconfig` together; the About row shows them.
<!-- cmpose:backend -->
- **Version and the server:** `MARKETING_VERSION` must equal `versionName`, and both must be `MAJOR.MINOR.PATCH` (for example `1.4.0`): the app sends it as `X-Client-Version` and the server's `MIN_CLIENT_VERSION` compares it, one version for both platforms. A version in any other form is not sent, so the server's minimum does not apply to that build.
<!-- /cmpose:backend -->
- **iOS privacy manifest:** `iosApp/iosApp/PrivacyInfo.xcprivacy` declares the required reason APIs this app's own code uses (the Kotlin code is linked into the app) and, with ads or purchases, the data they collect. When you add code or a Kotlin library that reads `UserDefaults`, file dates, disk space or the boot time, or collects data, update it. After an archive, Xcode's *Generate Privacy Report* lists every declaration, the SDKs' own included.
- **App Store privacy form** (App Store Connect → App Privacy) and **Play data safety form:** answer them for the app as a whole, SDKs included; the privacy report is a starting point.
- **Open source licences** are listed on their own screen (Settings, or About on the start screen); the list is generated from your dependencies on every build.

<!-- cmpose:monetization -->
## Monetization

<!-- cmpose:purchases -->
- **In-app purchases** (`core/purchases`, RevenueCat): purchases are simulated (RevenueCat *Test Store*) and nothing is charged: `revenueCatApiKey` (`PurchasesKeys.android.kt` and `PurchasesKeys.ios.kt`) is a Test Store key of the cmpose demo project on both platforms. `PurchasesManager` is the app's only door to RevenueCat: `offer()` (the *current* offering's packages with the store's prices, the monthly price of annual and weekly plans, the annual saving and any free trial), `purchase(packageId)`, `restore()` (`PurchaseResult`: `Success`, `Cancelled`, `Error(message)`) and `isPremium` (the `premium` entitlement); app code never imports a RevenueCat type. The paywall (`feature/monetization` → `paywall/`) is a plain Compose screen on that API: redraw it in your design and keep `PaywallViewModel`. `FirstLaunchPaywall()` in `MainScreen` opens it once at the first launch (never for a premium user; the flag `paywall_first_shown` stays on the device until the app's data is cleared). In the RevenueCat dashboard: mark an offering *current*, give it packages (`$rc_annual`, `$rc_monthly`, `$rc_weekly`, `$rc_lifetime`) and attach the products to the `premium` entitlement. Release builds with a Test Store key keep purchases disabled on both platforms (RevenueCat's Android SDK would close the app; iOS does the same for consistency) and the paywall says so. Before you ship, create your own RevenueCat project, add your App Store / Play Store apps and replace the keys with your platform keys: `goog_…` in `androidMain` (`PurchasesKeys.android.kt`), `appl_…` in `iosMain` (`PurchasesKeys.ios.kt`).
<!-- /cmpose:purchases -->
<!-- cmpose:ads -->
- **AdMob** (`core/ads`): Android uses the GMA Next-Gen SDK, iOS the Google Mobile Ads SDK (Next-Gen is Android-only). Ads are Google's test ads (test app IDs and test ad units), which always fill and never pay. Before you ship, replace the test app IDs (Android: `androidApp/src/main/AndroidManifest.xml` and `ADMOB_APP_ID` in `AdUnitIds.android.kt`, the same ID in both; iOS: `iosApp/iosApp/Info.plist` `GADApplicationIdentifier`) and the test units (`AdUnitIds`), publish a GDPR message in AdMob → Privacy & messaging and keep `SKAdNetworkItems` current. The demo asks for consent (UMP, then App Tracking Transparency on iOS) the first time the ads screen opens; a real app usually asks at launch. Where the consent rules require it (GDPR regions), the ads screen shows a *Privacy options* button that reopens the UMP form so the user can change their choice; a real app puts this entry in its settings. Debug builds force the EEA consent form (Android `ConsentDebugSettings` in `AdsController.android.kt`, iOS `DebugSettings` in `iosApp/iosApp/AdsBridge.swift`); release builds use the user's real location. Emulators and simulators are test devices; on a physical device the EEA debug geography also needs the device's test ID (`addTestDeviceHashedId` on Android, `testDeviceIdentifiers` on iOS). The iOS Swift packages (Google Mobile Ads, UMP) are pinned by `iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`. With in-app purchases, until RevenueCat answers (first launch, offline) premium is unknown and the ads demo shows no banner or interstitial. `InterstitialPacer` (`core/ads`, one per app from Koin) keeps interstitials to natural breaks: none in the first minute of a session and at most one per session; ask `shouldShow()` at a break, then call `recordShown()` once the ad was on screen. Premium is the caller's check (`AdFreeSource`).
<!-- /cmpose:ads -->
<!-- /cmpose:monetization -->

<!-- cmpose:room -->
## Local database (Room)

`core/room` holds the app's Room database, `AppDatabase`: SQLite through the bundled driver, the same on Android and iOS. Koin provides the database and each DAO (`AppDatabase.kt`, `di/RoomModule.kt`); a module that uses a DAO takes it with `@Provided`.
<!-- cmpose:room-favorites -->
- **Favorites** (`core/room` → `favorites/`): the star on a profile saves the GitHub user (`FavoriteUserEntity`, `FavoriteUserDao`) and the onboarding screen lists the saved users live. `feature/home` wraps the DAO in `FavoritesRepository` (`domain`) and `FavoritesRepositoryImpl` (`data`).
<!-- /cmpose:room-favorites -->
<!-- cmpose:room-notes -->
- **Notes demo** (`feature/notes`, `core/room` → `notes/`): add and delete notes on one screen. The ViewModel calls `NoteDao` directly to keep the demo short; in a real feature, put a repository in a `data` layer and keep Room out of the screen.
<!-- /cmpose:room-notes -->

To add a table, add an `@Entity` and a `@Dao` in their own package, list the entity in `@Database(entities = …)` and add an abstract function and a `@Single` provider for the DAO in `AppDatabase.kt`. Once the app has shipped, changing a table needs a new `version` and a migration (`autoMigrations` or a `Migration`). `core/room/schemas/` keeps the schema of every version: commit it.
<!-- /cmpose:room -->

<!-- cmpose:insights -->
## Insights: crash reports and analytics (Firebase)

`core/insights` sends crashes to Firebase Crashlytics and events to Firebase Analytics, on Android and iOS. It stays off (every call does nothing) until you add your Firebase project's config files, which git ignores: `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist` (Firebase console → Project settings → your apps). With them, Android applies the google-services and Crashlytics Gradle plugins and iOS configures Firebase at launch; release builds upload the R8 mapping (Android) and the dSYMs (iOS, the *Upload Crashlytics symbols if configured* build step) so crash reports are readable.

- **Crashes** are reported on the next launch. On iOS a Kotlin crash arrives with its Kotlin type and message (CrashKiOS), which is why `iosApp/iosApp/iOSApp.swift` configures Firebase before Kotlin starts. Warnings and errors logged through Kermit become Crashlytics breadcrumbs, and an error logged with a throwable (`log.e(throwable) { "…" }`) becomes a non-fatal issue (`InsightsLogWriter`). Never log tokens, request or response bodies, emails or names.
- **The API** is `Insights` (Koin): `logEvent(name, params)`, `setUserId(id)` (your own user id, never an email), `recordNonFatal(throwable)`, `log(message)`, `setCollectionEnabled(enabled)`. Collection is on by default in every build; where your app needs consent first, call `setCollectionEnabled(false)` at start and turn it on once the user agrees.
- **Try it:** with the config files in place, throw once (`throw RuntimeException("Test crash")` behind a button), open the app again, and the crash shows in the Crashlytics console within minutes. On iOS try it with a Release build: a Debug build makes no dSYMs, so its crash is not processed. For Analytics, turn on DebugView: `adb shell setprop debug.firebase.analytics.app <applicationId>` on Android, the `-FIRDebugEnabled` launch argument on iOS.
- On iOS the Firebase SDK comes through Swift Package Manager (`FirebaseCrashlytics`, `FirebaseAnalytics`); Kotlin reaches it through `IosInsightsBridge`, which `iosApp/iosApp/InsightsBridge.swift` implements.
<!-- /cmpose:insights -->

<!-- cmpose:backend -->
## Local server

The Ktor server (`server/`) is the cmpose backend template, generated into the project as an included build: its own
`server/README.md` covers its layout, API conventions and configuration, `server/SECURITY-CHECKLIST.md` what every
route needs, and `server/deploy/README.md` the deployment. It runs on your computer while the apps are tried on an
emulator or simulator:

- `./gradlew :server:runLocal` starts it on `127.0.0.1:18001` (loopback only, never on the network) with an
  embedded PostgreSQL 16. It prints `LOCAL_SERVER_READY port=18001` once it answers; Ctrl-C stops it.
- Everything lives in the project's `.local-server/` (not in git; `localServerDir=../.local-server` in
  `server/gradle.properties`): `config.properties`, the database (`postgres/`) and the uploaded files (`files/`).
  Data survives restarts.
- `config.properties` needs `firebaseProjectId=<Firebase project id>` and may set `port=<n>`. A missing file, a bad
  port or a busy port stops the runner with a message and exit code 1.
- `./gradlew :server:resetLocal` deletes `.local-server/` (database, files and config) to start from scratch.

The runner and the PostgreSQL binaries sit in the `local` source set, so the release jar (`:server:shadowJar`)
carries neither. `./gradlew :server:build` compiles the server and runs its detekt and its tests (an included build,
so `./gradlew allTests` does not run them).

## Backend in the app

Sign-in is required: `MainScreen.kt` shows the sign-in screen while nobody is signed in and the app's first screen
after it (`SessionGate.kt`). When the user changes (sign-out, another account, a session the server ended), the app
drops the cached token and the previous user's selected space.

- `core/api`: the app's client models of the backend API, its requests and answers (`ApiModels.kt`) and paths
  (`ApiRoutes.kt`), checked against the server's OpenAPI description by `ApiContractTest`; the server does not use
  them.
- `core/auth`: Firebase Authentication (email and password, Google, Apple) and the signed-in user's ID token.
- `core/network`: the backend's own `HttpClient`, named `BACKEND_CLIENT`; it sends the ID token to the backend's host
  only and retries once after a 401. The app's other `HttpClient` stays without a token. `BackendConfig.kt` holds the
  addresses: the local server in debug builds, `RELEASE_BASE_URL` (a placeholder) in release builds.
- `core/network`, the server's answers: every request to the backend carries the app's `MAJOR.MINOR.PATCH` version
  as `X-Client-Version`; a 426 (`CLIENT_UPGRADE_REQUIRED`, the app is below the server's `MIN_CLIENT_VERSION`) opens
  the "Update required" screen (`core/update`); a 429 (`RATE_LIMITED`) shows "Too many requests. Please try again in
  a minute.". `RELEASE_BASE_URL` must end in `/api/v1/` (the server serves its API there; request paths have no leading slash).
- `core/image`: images the backend serves load with the token (`core/auth` adds the backend client's fetcher through
  an `ImageLoaderCustomizer`); `normalizeImageForUpload` turns a picked photo into a JPEG (at most 2048 px and 5 MB,
  HEIC included) before an upload.
- `feature/account`: sign-in and account settings (sign-out, account deletion).
- `feature/sharing`: the user's spaces, their members and invite links (`multiplatform://invite/<token>`); an invite
  opened while signed out shows after the sign-in.
- `server/`: the Ktor server (above), an included build.

**Reaching the local server** (debug builds; release builds use `RELEASE_BASE_URL`):

- Android emulator: `10.0.2.2:18001`, the emulator's alias for the computer, chosen automatically.
- Android phone over USB: run `adb reverse tcp:18001 tcp:18001` once per connection; the app then uses
  `localhost:18001`, which the tunnel carries to the computer.
- iOS simulator: `localhost:18001`, the Mac's own loopback. A physical iPhone cannot use `localhost`; it would need
  the Mac's address on the local network, which the template does not set and the debug App Transport Security
  exception does not cover.

Plain HTTP to these addresses is allowed in debug builds only (`androidApp/src/debug/res/xml/network_security_config.xml`,
`NSAllowsLocalNetworking` in the Debug `Info.plist`).

**Firebase files.** Each developer adds their own, and git ignores both:

- `androidApp/google-services.json` (Firebase console → Project settings → your Android app). Without it the
  google-services plugin is not applied.
- `iosApp/iosApp/GoogleService-Info.plist` (the same page, your iOS app). The build copies it into the app only when
  it exists. With it, also set `GOOGLE_REVERSED_CLIENT_ID` in `iosApp/Configuration/Config.xcconfig` to the
  plist's `REVERSED_CLIENT_ID`: Google sign-in on iOS crashes when the plist is there and that value is empty, so set
  both together. Firebase keeps the session in the keychain, which needs a signed build, on the simulator too: an
  unsigned one (`CODE_SIGNING_ALLOWED=NO`) fails every sign-in with a keychain error (17995).
- The server's `.local-server/config.properties` takes the same project's id (`firebaseProjectId`).

Without the Firebase files the app still builds, its tests pass and it starts: the sign-in screen says that Firebase is
not configured, and nobody can sign in. An invite link then waits for a sign-in that never comes; the app's other
deep links are not gated and still open their screens over the sign-in screen.

**Sharing model.** Every record belongs to a space. Each user has a personal space, created on their first request;
they can create more and invite others as editors or viewers through a link (single use, valid for 7 days). Roles
rank `OWNER` > `EDITOR` > `VIEWER`. To a non-member a space does not exist: the server answers 404, never 403.

**Adding a table** (copy the shape of `server/src/main/kotlin/.../files/`: repository, routes, migration, tests; then
go through `server/SECURITY-CHECKLIST.md`):

1. A Flyway migration, `server/src/main/resources/db/migration/V<next>__<feature>.sql` (the number after the highest
   one there), creates the table with a `space_id` column referencing `spaces(id) ON DELETE CASCADE`, so deleting a
   space deletes its rows.
2. Its routes live under `/api/v1/spaces/{id}/...`, registered next to the other routes in `Application.kt`, and are
   described in `openapi/OpenApiSpec.kt` (`OpenApiTest` keeps the two in step).
3. Every handler starts with `call.requireRole(spaces, call.parameters["id"].orEmpty(), Role.VIEWER)` (`Role.EDITOR`
   to write) and queries with the returned `membership.spaceId`, never with the raw path value. Do not check
   membership by hand.
4. In the app, add the requests, answers and paths to `core/api` and list them in `ApiContractTest`'s
   `CLIENT_ROUTES` and `MODELS`: it checks only what is listed there against the server's API description. Read the
   current space from `SelectedSpace` (`feature/sharing`) and call the routes with the `BACKEND_CLIENT` HttpClient.
<!-- /cmpose:backend -->

## Modules

<!-- cmpose:modules -->
- `:androidApp`: Android entry point (Application, Activity)
- `:shared`: app shell: navigation graph, Koin start (`di/initKoin.kt`), the iOS framework
<!-- cmpose:backend -->
- `:core:api`: the app's client models and routes for the backend API, checked by `ApiContractTest`
- `:core:auth`: Firebase sign-in (email, Google, Apple) and the backend's ID token
- `:feature:account`: contract, presentation
- `:feature:sharing`: contract, presentation
- `server/`: Ktor server from the cmpose backend template (an included build): sign-in check, spaces, sharing, files; `./gradlew :server:runLocal`; deploying: `server/deploy/README.md`
<!-- /cmpose:backend -->
<!-- /cmpose:modules -->

## Documentation

👉 **[Developer Guide](https://whinstudio.github.io/cmpose/guide.html)**: architecture, navigation, Koin, networking, theming.

## Connect

- 🌐 **Website:** [github.com/whinstudio/cmpose](https://github.com/whinstudio/cmpose)
- 💼 **LinkedIn:** [Compose Wiki](https://www.linkedin.com/company/compose-wiki/)
