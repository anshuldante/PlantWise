# Release Checklist: LeafIQ Sideload Distribution

Use this checklist for every LeafIQ release. This is a copy-paste working checklist for sideload distribution (APK installed directly via ADB, no Play Store).

## Section 1: Pre-Release

- [ ] All unit tests pass: `./gradlew test`
- [ ] All instrumented tests pass: `./gradlew connectedAndroidTest`
- [ ] Lint check passes: `./gradlew lint`
- [ ] No uncommitted changes: `git status` clean
- [ ] Version bump in `app/build.gradle`: increment `versionCode` and update `versionName`
- [ ] Update `CHANGELOG.md` with release notes (if exists, otherwise skip)
- [ ] Commit version bump: `git commit -m "chore: bump version to X.Y.Z"`

## Section 2: Build

- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] APK is signed with release keystore (verify: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`)
- [ ] Note APK size (track for regression)
- [ ] Copy APK to distribution location

## Section 3: Testing

- [ ] Install APK on physical device via `adb install app/build/outputs/apk/release/app-release.apk`
- [ ] Smoke test: launch app, verify no crash on startup
- [ ] Smoke test: take photo, run analysis, verify result displays
- [ ] Smoke test: check plant library shows existing plants
- [ ] Smoke test: verify Settings opens, API key saves/loads
- [ ] Smoke test: verify care reminders fire (if scheduled)
- [ ] Test on minimum SDK device if available (Android 8.0 / API 26)

## Section 4: Distribution

- [ ] Sideload APK to target devices via `adb install`
- [ ] Verify installation succeeds without errors
- [ ] Keep previous APK available for rollback

## Section 5: Post-Release

- [ ] Create git tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] Update release notes (GitHub release page if applicable)
- [ ] Archive build artifacts

## Section 6: Rollback Plan

If a critical issue is found post-release:

1. Reinstall previous APK (kept from distribution step): `adb install previous-app-release.apk`
2. No database migration reversal needed — migrations are non-destructive
3. Users can downgrade by reinstalling older version
4. Document issue and plan hotfix release

## Section 7: Notes

- **Sideload verification:** Developer verification for sideloading may be required starting September 2026
- **Keystore security:** Keystore file should be stored outside project directory in secure location
- **Never commit secrets:** Never commit keystore passwords or signing credentials to git
- **APK signing:** Release builds must be signed with production keystore (not debug keystore)
- **Version tracking:** Keep `CHANGELOG.md` updated with user-facing changes
