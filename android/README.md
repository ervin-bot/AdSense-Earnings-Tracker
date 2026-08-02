# AdSense Tracker Android

Native Android version of the AdSense earnings tracker.

## What It Includes

- Google sign-in with the `https://www.googleapis.com/auth/adsense.readonly` scope
- Live AdSense Management API v2 account discovery and `reports:generate`
- Demo mode for local UI checks
- Report periods:
  - Today
  - Yesterday
  - This Week
  - Last Week
  - This Month
  - Last Month
  - Last 30 Days
  - This Year
  - Last Year
  - Last 365 Days
- Top 7 sites per selected period, deduplicated by normalized host
- Daily 7-day and 30-day earnings charts with an aligned previous-period comparison
- Trend summaries with daily average, best day, percentage change, and touch details
- Ranked Top Sites bars showing each site's share of the selected period
- Currency selector using the official AdSense currency codes
- Open-app refresh interval from 5 to 60 minutes
- Pull-to-refresh gesture at the top of the earnings view
- Background widget refresh with a smaller payload for today, yesterday, and month-to-date totals
- Serialized foreground/background API refreshes so the fast widget payload cannot race the full in-app report
- Lifecycle-safe cancellation, closed HTTP connections, and isolated widget/job finalization
- Two-stage in-app refresh: totals and trend first, then an atomic full snapshot with site breakdowns
- Account-timezone date ranges, explicit freshness status, and cached-section warnings for partial refreshes
- Launcher icons from the generated icon pack in `app/src/main/res/mipmap-*`
- Google Play listing icon at `playstore.png`

## Open In Android Studio

1. Open Android Studio.
2. Choose `Open`.
3. Select the `android/` folder from the repository:

```text
android/
```

4. Let Android Studio sync Gradle. It will download the Android Gradle plugin and Google Play Services Auth dependency if they are not cached locally.

## Google Cloud OAuth Setup

The Android app uses this package name:

```text
ro.mobilissimo.adsensetracker
```

In Google Cloud Console:

1. Enable the AdSense Management API.
2. Configure the OAuth consent screen.
3. Create an OAuth Client ID with application type `Android`.
4. Set the package name to `ro.mobilissimo.adsensetracker`.
5. Set the SHA-1 certificate fingerprint for the signing key you use.

For a debug build, Android Studio can show the debug SHA-1 from the Gradle signing report, or you can run this from the Android project after Gradle is available:

```sh
./gradlew signingReport
```

The app does not store a client ID in code. Google Play Services matches the Android OAuth client by package name and SHA-1.

## Verification

Run the unit tests, Android lint, and debug build together:

```sh
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

The project requires JDK 17 and Android SDK 35. `assembleDebug` produces an APK signed with the local Android debug certificate for testing and direct installation. A store release requires a separately configured private release keystore.
