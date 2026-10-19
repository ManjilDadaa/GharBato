# GharBato server functions

The Android app calls these Firebase Functions for Cloudinary upload signatures, Gemini chat, and short-lived Zego call tokens. The app sends Firebase Auth and App Check tokens through the Firebase callable SDK. The API secrets stay in Firebase Secret Manager.

## One-time setup

1. Install Firebase CLI, sign in with an account that can deploy to `gharbatodb`, and make sure the project is on the Blaze plan. Cloud Functions deployment and Secret Manager require billing to be enabled.
2. In Firebase Console, register the Android app with App Check and configure the Play Integrity provider. Link the Play Console app if required for production verdicts. Debug builds use the Firebase App Check debug provider; register its logged debug token in Firebase Console for local development.
3. Rotate the Cloudinary API secret and Gemini API key that were previously present in Android source. Replace the exposed Zego AppSign with server generated Token04 credentials and create or rotate the 32-character Zego Server Secret. Do not reuse credentials that were committed to source control.
4. From the repository root, set each secret using the interactive prompt so values do not enter shell history:

   ```powershell
   firebase functions:secrets:set CLOUDINARY_API_SECRET
   firebase functions:secrets:set GEMINI_API_KEY
   firebase functions:secrets:set ZEGO_SERVER_SECRET
   ```

5. Copy `functions/.env.example` to `functions/.env.gharbatodb` and fill in its three non-secret values. That project-specific file is ignored by Git. Then deploy with `firebase deploy --only functions`. These values identify the provider accounts; the signing/API secrets above are stored separately in Secret Manager.

The functions are deployed in `asia-south1` and require both a signed-in Firebase user and a valid App Check token. The upload function accepts only the fixed image categories in `index.js` and derives the Cloudinary folder and public ID on the server.

Set Firebase budget alerts/spend caps and provider-side API quotas before broad release. The Functions deployment caps each function at 20 instances; adjust that limit deliberately as traffic grows.

## Google Maps Android key

The Maps SDK requires an Android client key in the app package. It is not a server secret, so secure it with Google Cloud API and Android app restrictions. Rotate the key that was committed in the manifest. Add the replacement locally to `local.properties`:

```properties
MAPS_API_KEY=your_restricted_android_maps_key
```

For CI, pass `MAPS_API_KEY` as an environment variable. Restrict the key to package `com.example.gharbato`, the release certificate used by Play App Signing, and the Maps SDK for Android API. Add the debug certificate for local development or use a separate development key.

## Project changes

If deploying to a different Firebase project, update `.firebaserc`, the Realtime Database URL in `index.js`, and the Android `google-services.json`. Keep `.env.*`, local properties, and provider secrets out of source control.
