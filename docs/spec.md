# Katsumi — spec

Local-first Android anime app. Point it at any folder, it parses your files,
matches them to AniList, tracks progress, syncs when online. MPV playback.

## Non-goals

- No streaming sources, no extensions, no torrent anything. Local files only.
- No manga. Anime only.
- No accounts besides AniList OAuth.

## Stack

- Kotlin, Jetpack Compose, Material 3 Expressive. minSdk 26.
- Player: vanilla mpv-android (`github.com/mpv-android/mpv-android`).
  Custom gesture layer (incl. hold-for-2x via `speed` property).
- Parser: parsium (`NepiRaw/Parsium`, MIT) running in embedded QuickJS.
- AniList: raw GraphQL over Ktor client. OAuth via Custom Tabs.
- Storage: Room (source of truth), DataStore (prefs), Coil (covers).
- Background: WorkManager for sync drain.

## Architecture

```
ui (compose screens)
 └─ viewmodels (per-screen state, one-shot events)
     └─ repository (single entry to data layer)
         ├─ libraryDao ── Room: shows, episodes, file mappings, match overrides
         ├─ outboxDao ─── Room: pending AniList mutations
         ├─ scanner ───── SAF traversal + parsium + AniList matcher
         ├─ syncer ────── outbox drain ↔ AniList (WorkManager, network constraint)
         └─ tracker ───── AniList GraphQL client (search, list, SaveMediaListEntry)
player (mpv-android wrapper: lifecycle, gestures, audio-focus, notification)
```

Data flows one way: scanner/tracker write Room, UI reads Room via Flow.
Player reports progress → repository → Room + outbox → syncer.

## Scanner

1. SAF `ACTION_OPEN_DOCUMENT_TREE`, persist URI permission. Recursive walk,
   video extensions only (mkv, mp4, webm, avi, m4v).
2. Per file: parsium → title / season / episode / resolution / group.
   Folder names as fallback signals (`Season 2` dir, bare `01.mkv` → parent name).
3. Match: parsed title → AniList `Media` search → auto-take top hit when
   unambiguous, else file goes to review queue.
4. Review queue UI: search, tap correct show, override stored permanently.
   Same pattern auto-matches on future scans.
5. Post-match hydration: episode count, covers, colours from AniList.
   Movies → single Episode 1. Absolute numbering normalised against
   sequel/prequel relations where detectable.

## Sync

- Every progress change writes Room + appends to outbox (idempotent by
  `mediaId+episode`, last-write-wins).
- WorkManager drains outbox when network available, exponential backoff,
  90-req/min AniList limit respected via client-side throttle.
- First login pulls full AniList list → seeds library matching state.
- Fully usable airplane-mode; sync is silent and invisible.

## Player

- MPV surface in Compose via AndroidView. Lifecycle-bound (pause on background).
- Gestures: tap toggle UI, double-tap ±10s (zones), swipe brightness/volume,
  **hold for 2x** (long-press → `speed=2`, release → restore).
- Resume from Room position per episode. Auto-mark watched at 85%.
- Audio focus + media notification. External subs selectable from file.

## Self-update

- No Play Store. GitHub Releases is the distribution channel.
- In-app updater: on launch (and daily via WorkManager), check
  `api.github.com/repos/aor-rex/katsumi/releases/latest`. Compare
  `tagName` to `BuildConfig.VERSION_NAME`.
- If newer: show update card (release notes from body) → download APK
  to cache → install via FileProvider + `ACTION_INSTALL_PACKAGE`.
- `REQUEST_INSTALL_PACKAGES` permission declared. Pre-release channel
  opt-in via settings (include `prerelease` assets).
- Update check is silent on failure (offline → skip).

## Milestones

1. Scaffold + first frame (hardcoded file plays).
2. Folder setting + file enumeration.
3. Parser evaluation gate (real filenames → logged output).
4. AniList match + review queue.
5. Library UI (shows, episodes, continue watching).
6. OAuth + sync outbox.
7. Player gestures + polish.
