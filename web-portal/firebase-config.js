import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import { getAuth, GoogleAuthProvider } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";

export const FIRESTORE_DATABASE_ID =
  "ai-studio-android-churchco-b7034718-041b-4fe3-915a-dda6afd1e6a0";

export const DEFAULT_CHURCH_ID = "church_main";
export const BOOTSTRAP_SUPER_ADMIN_EMAIL = "samuveldurai6@gmail.com";

const runtimeConfig = window.__FIREBASE_CONFIG__ || null;

export const isFirebaseConfigured = Boolean(
  runtimeConfig && runtimeConfig.apiKey && runtimeConfig.projectId
);

export let firebaseApp = null;
export let auth = null;
export let db = null;
export let googleProvider = null;

if (isFirebaseConfigured) {
  firebaseApp = initializeApp(runtimeConfig);
  auth = getAuth(firebaseApp);
  db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);
  googleProvider = new GoogleAuthProvider();
  googleProvider.setCustomParameters({ prompt: "select_account" });
}
