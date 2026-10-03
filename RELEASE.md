# Releasing Konstanz Transit

What is ready, what only you can do, and how to build a release.

## Status

| | |
|---|---|
| Version | 1.0.0 (versionCode 1) — `app/build.gradle.kts` |
| Build | R8 code + resource shrinking, lint clean, release build tested on the emulator |
| Tests | 31 unit tests, 26 device tests incl. 8 Compose UI journeys (`./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest`) |
| Data | NVBW timetable (valid until 12 Dec 2026), OpenStreetMap map of 29 Sep 2026, City of Konstanz stop register |
| Permissions | Location (optional), network state. **No internet permission** — nothing leaves the phone. |

## Before the first upload — needs you

1. **App id.** `com.example.konstanz` is rejected by Google Play and can never be changed after the first
   upload. Choose one you own, e.g. `de.<yourname>.konstanztransit`, and set `applicationId` in
   `app/build.gradle.kts` (the Kotlin package can stay as it is).
2. **Upload key.** Create it once and keep it safe (losing it means asking Google to reset it):

   ```bash
   keytool -genkeypair -v -keystore upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

   Then create `keystore.properties` in the project root (both files are git-ignored):

   ```properties
   storeFile=upload-key.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```

   Without this file the release is signed with the debug key: fine for testing, rejected by Play.
3. **Support e-mail.** Set `AppInfo.SUPPORT_EMAIL` (`data/AppInfo.kt`): it adds the contact line to the
   privacy policy and the "Send feedback" row in About.
4. **Privacy policy URL.** Play needs a public web page. Publish the text of the in-app policy
   (`ui/about/PrivacyScreen.kt`), e.g. on GitHub Pages, and enter the URL in the Play Console.
5. **Bus stop data licence.** Confirm where `Bushaltestellen.geojson` comes from and its licence
   (City of Konstanz open data?), so the credit in About and Licenses is exact.

## Build

```bash
./gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab`. Keep
`app/build/outputs/mapping/release/mapping.txt` for each release (it turns crash reports readable).

## Play Console answers

- **Data safety:** no data collected, no data shared (the app has no internet permission; location is
  used on the device only). No account, no ads.
- **Permissions:** location — "show your position, nearby stops and routes from where you are; optional".
- **Content rating:** everyone (no user content, no ads, no purchases).
- **Target audience:** general; not designed for children.
- **Store listing:** screenshots of map, stop, route results, journey; describe it as offline public
  transport for Konstanz and Kreuzlingen with timetable data from NVBW (scheduled times, no live data yet).

## Updating the data (each timetable change)

The NVBW timetable is valid until the date shown in Settings → Offline data; the app warns 14 days
before and marks it expired after. For a new timetable:

```bash
python3 tools/gtfs-import/import_gtfs.py           # new vhb.zip in tools/gtfs-import/data/
tools/places-import/.venv/bin/python tools/places-import/import_places.py
```

then raise `versionCode`, build and upload. For a newer map, see `tools/map/make_style.py`.

## Known limits (not blockers)

- Scheduled times only; live times need a realtime feed (PLAN.md L4).
- English only; a German translation would suit most users in Konstanz.
- Phones in portrait only; tablets get the phone layout.
- Touch targets follow the design's 44 dp; Android recommends 48 dp.
- Toolchain updates left for a quiet moment: Kotlin 2.4, Gradle 9.8.
