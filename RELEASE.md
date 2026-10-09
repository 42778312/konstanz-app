# Releasing Konstant

What is ready, what only you can do, and the steps to Google Play.

## Status (9 Oct 2026)

| | |
|---|---|
| App | **Konstant** · `de.konstant.app` · version 1.0.0 (versionCode 1), `app/build.gradle.kts` |
| SDK | minSdk 24, **targetSdk 37** (Play requires ≥ 36 for new apps since 31 Aug 2026) |
| Build | `bundleRelease` passes, incl. lint-vital. R8 code + resource shrinking. Release build tested on the emulator: splash, map, stop sheet, day/time picker, route search, settings; no crashes |
| Bundle | ~34 MB `.aab` (Play delivers each phone only its own CPU type) |
| Tests | 40 unit tests (`./gradlew :app:testDebugUnitTest`), device tests (`./gradlew :app:connectedDebugAndroidTest`) |
| Languages | English, German |
| Permissions | Location only (optional). **No internet permission**: nothing leaves the phone |
| Data | NVBW timetable (valid until 12 Dec 2026), OpenStreetMap map, City of Konstanz stop register |
| Store assets | `branding/play-store/`: icon 512, feature graphic 1024×500, 4 phone screenshots 1080×1920 |
| Privacy policy | `docs/privacy-policy.html` (EN + DE), ready to host |

## 1 · Decisions only you can make (before the first upload)

1. ~~App id~~ Done: **`de.konstant.app`** (`app/build.gradle.kts`). Permanent after the first upload.
2. ~~Support e-mail~~ Done: **konstant@werkflow.cc** (`AppInfo.SUPPORT_EMAIL`, privacy policy page). Use the
   same address as the Play Console contact e-mail.
3. **Bus stop data licence.** Confirm the licence of the City of Konstanz stop data so the credit is exact.
4. **Line colours (optional).** Add the official colours from the Stadtwerke network map to
   `ui/components/LineColors.kt`.

## 2 · Upload key (once, keep it safe)

```bash
keytool -genkeypair -v -keystore upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Create `keystore.properties` in the project root (both files are git-ignored):

```properties
storeFile=upload-key.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

Back up both files and the passwords outside this computer. With **Play App Signing** (default) Google holds
the real signing key; this upload key can be reset through Play support if lost, but that takes days.
Without `keystore.properties` the release is signed with the debug key, which Play rejects.

## 3 · Build

```bash
./gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab`. Keep
`app/build/outputs/mapping/release/mapping.txt` for every release (upload it in Play Console → App bundle
explorer → Downloads, so crash reports are readable).

## 4 · Host the privacy policy

Play needs a public URL. The repo is public on GitHub: Settings → Pages → Source "Deploy from a branch" →
Branch `main` (once merged) or `map-keep-alive`, folder `/docs` → Save. After about a minute the policy is at
`https://42778312.github.io/konstanz-app/privacy-policy.html`.

## 5 · Play Console

1. **Developer account**: play.google.com/console, one-time $25 fee, identity verification (can take days).
   - *Personal* accounts created after 13 Nov 2023 must run a **closed test with ≥ 12 testers for 14 days in a
     row** before production is unlocked (testers must stay opted in and actually use the app).
   - *Organisation* accounts (needs a D-U-N-S number) skip that rule.
2. **Create app**: name "Konstant", default language German or English, App, Free.
3. **App content** (left menu → Policy → App content):
   - Privacy policy: the URL from step 4.
   - Ads: **No ads**.
   - App access: **All functionality available without special access**.
   - Content rating questionnaire: category *Utility/Tools*, answer No to everything → rated for everyone.
   - Target audience: 18+ / not designed for children (avoids the Families policy).
   - Data safety: **No data collected, no data shared** (on-device processing of location is not "collection"
     because it never leaves the phone). Encrypted in transit: not applicable. Deletion: not applicable.
   - Government app: No. Financial features: None. Health: No. News: No.
4. **Store listing** (Grow → Store presence → Main store listing). Every text and answer, ready to paste:
   **`branding/play-store/listing.md`**.
   - App name: `Konstant: Bus & Wege Konstanz` (EN: `Konstant: Konstanz Bus & Walk`)
   - Short + full description: see the brand book's "Store listing" section (DE + EN).
   - Icon `branding/play-store/icon-512.png`, feature graphic `feature-graphic.png`, phone screenshots
     `phone-1.png` … `phone-4.png`.
   - Category: **Maps & Navigation**. Contact e-mail (required).
5. **Testing → Closed testing**: create a track, add ≥ 12 testers (Google Group or e-mail list), upload the
   `.aab`, send for review, share the opt-in link. Wait 14 days with ≥ 12 opted in.
6. **Production**: apply for production access (questions about the test), then create a release with the
   same `.aab` (or a newer versionCode), countries: Germany, Switzerland (+ others if you like), roll out.
   First review usually takes a few days.

## Each update

Raise `versionCode` (and `versionName`) in `app/build.gradle.kts`, build, upload to a track, roll out.
New timetable (before 12 Dec 2026, when the current one ends):

```bash
python3 tools/gtfs-import/import_gtfs.py           # new vhb.zip in tools/gtfs-import/data/
tools/places-import/.venv/bin/python tools/places-import/import_places.py
```

## Known limits (not blockers)

- Scheduled times only; live times need a realtime feed.
- Phones in portrait only; tablets get the phone layout.
- Seenachtfest dates for 2027+ need adding to `data/SeasonalEvents.kt` once announced.
