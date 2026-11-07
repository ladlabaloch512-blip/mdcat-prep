# MDCAT Prep (MDCAT Pakistan) — App, Website & Admin

A complete MDCAT preparation platform built by **ABiz Global Services** for
Nawab Trader: a native Android app, a matching responsive website and a web
admin panel, all running on one shared PHP/MySQL backend and one database,
so anything managed in the admin appears in the app and on the website at
once.

**Live preview:** https://darkslateblue-panther-598842.hostingersite.com/

## What is inside

- **Android app** (`android/`) — native Kotlin, no WebView. Splash with the
  client's branding, sign up / sign in (email, Google-ready, guest mode),
  forgot password, home dashboard with daily progress, PMDC chapter
  selection with weak-topic and solved filters, timed MCQ quiz, results
  out of 200 with per-subject performance chart, saved MCQs, daily reward
  coins, analytics, WhatsApp channel screen, question of the day, daily
  study goals, notifications with read state, offline reading from cache,
  account settings (edit profile, change password, delete account).
- **10 switchable designs** — ten structurally different designs (layout,
  navigation, cards, quiz and result styles), and **every design carries
  its own set of 4 colour palettes**, remembered per design.
- **Website** (`web/`) — plain PHP 8 + vanilla JS on the same API, with
  the same 10 designs and colour palettes, plus the admin panel at
  `/admin/` (MCQs, chapters, subjects, students, results, banners,
  notifications, settings). All content values (app name, tagline, timer,
  counts, thresholds, banners, socials) are database-driven, nothing is
  hardcoded.
- **Custom banner ads** managed from the admin (the client's own banners,
  ordered per placement; never inside the live quiz).
- Chat support is intentionally out of scope and is not present anywhere.

## Downloads

See the **Releases** section for the signed APK (testing) and AAB
(Google Play) builds.

## Notes

- The release signing keystore is **not** in this repository. It is kept
  offline by ABiz Global Services; see `android/` build docs for the
  pipeline (`tools/` holds the manual build + dex linkage audit scripts
  used to produce and gate the release binaries).
- The database seed ships with clearly marked demo content only.
- Google Play publication is pending the client's developer account.
