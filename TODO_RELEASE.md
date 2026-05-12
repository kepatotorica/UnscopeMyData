# TODO: Google Play Store Release Checklist

Since you already have a Google Play Console account, here are the steps to get **UnscopeMyData** ready for production.

## 1. Technical Preparation
- [ ] **App Icon:** Create a high-resolution (512x512) icon. The current one is the default Android "green bot."
- [ ] **Feature Graphic:** Create a 1024x500 banner for the store page.
- [ ] **Package Name:** Confirm `com.kepat.unscopemydata` is your final choice (it cannot be changed after upload).
- [ ] **Version Management:** Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
- [ ] **Release Signing:**
    - [ ] Create a Keystore file (`.jks`).
    - [ ] Configure signing in Gradle or manually sign the AAB.
    - [ ] **IMPORTANT:** Back up your keystore file safely. If you lose it, you can never update the app.
- [ ] **Build Release Bundle:** Run `./gradlew bundleRelease` to generate an `.aab` file.

## 2. Play Console Setup
- [ ] **Create App:** Set the name, default language, and type (App/Free).
- [ ] **Policy & Permissions:**
    - [ ] **MANAGE_EXTERNAL_STORAGE Declaration:** Google is strict about this. You will need to provide a video and written justification explaining why your app *needs* this permission (bypassing Scoped Storage for sync is a valid reason, but you must be clear).
    - [ ] **Privacy Policy:** You need a hosted URL for your privacy policy.
- [ ] **Content Rating:** Complete the questionnaire.
- [ ] **App Content:** Declare that your app doesn't have ads and its target age group.

## 3. Store Listing
- [ ] **Short Description:** (~80 characters) "Sync protected Android folders to public storage using Shizuku."
- [ ] **Full Description:** Explain Shizuku, Scoped Storage, and how to use the app.
- [ ] **Screenshots:** At least 2-4 screenshots of the main screen, settings, and the folder browser.

## 4. Testing & Launch
- [ ] **Internal Testing:** Upload the AAB to the "Internal Testing" track to test on your own device.
- [ ] **Closed Testing:** (Required for new accounts) Invite 20 testers for 14 days before you can go to Production.
- [ ] **Production:** Once testing is complete, promote the release to Production for review.

---
**Note on Shizuku:** Since Shizuku is a third-party tool, you might want to include a link to its official documentation in your app's "About" section or store description to help users get it set up.
