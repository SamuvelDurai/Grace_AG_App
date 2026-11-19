import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  limit,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import {
  auth,
  BOOTSTRAP_SUPER_ADMIN_EMAIL,
  db,
  DEFAULT_CHURCH_ID,
  googleProvider,
  isFirebaseConfigured
} from "./firebase-config.js";
import { closeModal, initials, randomId, showToast } from "./utils.js";
import { todayDateKey } from "./verses-catalog.js";
import {
  renderAnnouncementsView,
  renderDailyVerseView,
  renderEventsView,
  renderFamiliesView,
  renderGivingView,
  renderMembersView,
  renderOverviewView,
  renderPrayersView
} from "./views-pastor.js";
import {
  renderAuditLogsView,
  renderChurchAndPastorsView,
  renderUpdaterView
} from "./views-admin.js";

const ROUTE_META = {
  overview: { title: "Overview", subtitle: "Live church summary & action queue" },
  members: { title: "Members & Approvals", subtitle: "Directory, profile photos & membership approvals" },
  families: { title: "Church Families", subtitle: "Household heads, roles & linked believer accounts" },
  verses: { title: "Daily Bible Verse", subtitle: "Pastor daily verse composer & Auto-Verse automation" },
  announcements: { title: "Announcements", subtitle: "Bilingual church broadcasts synced to all phones" },
  prayers: { title: "Prayer Requests", subtitle: "Pastoral intercession queue & encouragement notes" },
  giving: { title: "Giving & Monthly Dues", subtitle: "Verify family monthly subscriptions & offerings" },
  events: { title: "Church Events", subtitle: "Schedule services, meetings & track RSVPs" },
  updater: { title: "App Update Pop-Up", subtitle: "Broadcast new Android APK releases to all members" },
  church: { title: "Church & Pastors", subtitle: "Church profile, UPI / bank details & Pastor invites" },
  audit: { title: "Audit Logs", subtitle: "Chronological security trail of administrative actions" }
};

const state = {
  route: "overview",
  currentUserProfile: null,
  churchId: DEFAULT_CHURCH_ID,
  churchConfig: { name: "GRACE AG", defaultLanguage: "en", monthlyDuesAmount: 500, latestVersionCode: 1, latestVersionName: "1.0" },
  dailyVerseConfig: { churchId: DEFAULT_CHURCH_ID, autoDailyEnabled: true, rotationHour: 6 },
  users: [],
  believers: [],
  applications: [],
  families: [],
  verses: [],
  announcements: [],
  prayers: [],
  subscriptions: [],
  offerings: [],
  events: [],
  auditLogs: [],
  ui: {
    memberSearch: "",
    memberFilter: "ALL",
    givingTab: "subscriptions"
  }
};

let unsubscribers = [];

function clearListeners() {
  unsubscribers.forEach((fn) => {
    try { fn(); } catch { /* ignore */ }
  });
  unsubscribers = [];
}

function showAuthError(msg) {
  const el = document.getElementById("auth-error");
  if (!el) return;
  if (!msg) {
    el.classList.add("hidden");
    el.textContent = "";
  } else {
    el.textContent = msg;
    el.classList.remove("hidden");
  }
}

async function recordAuditLog(action) {
  if (!auth?.currentUser) return;
  try {
    const logId = randomId("log", 12);
    const nowText = new Date().toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
    await setDoc(doc(db, "auditLogs", logId), {
      logId,
      churchId: state.churchId,
      action: String(action).slice(0, 480),
      actorName: state.currentUserProfile?.displayName || auth.currentUser.email || "Admin",
      actorUid: auth.currentUser.uid,
      timestampText: nowText,
      status: "SUCCESS",
      createdAt: serverTimestamp()
    });
  } catch (err) {
    console.warn("Audit log skipped:", err);
  }
}

async function resolveOrClaimPortalProfile(firebaseUser) {
  const userRef = doc(db, "users", firebaseUser.uid);
  const snap = await getDoc(userRef);
  const emailLower = (firebaseUser.email || "").trim().toLowerCase();

  if (snap.exists()) {
    const data = { uid: firebaseUser.uid, ...snap.data() };
    if ((data.role === "PASTOR" || data.role === "SUPER_ADMIN") && data.accountStatus === "APPROVED") {
      return data;
    }
    if (emailLower === BOOTSTRAP_SUPER_ADMIN_EMAIL) {
      await updateDoc(userRef, {
        role: "SUPER_ADMIN",
        accountStatus: "APPROVED",
        updatedAt: serverTimestamp()
      });
      return { ...data, role: "SUPER_ADMIN", accountStatus: "APPROVED" };
    }
    if (emailLower) {
      const inviteSnap = await getDoc(doc(db, "pastorInvites", `email_${emailLower}`));
      if (inviteSnap.exists()) {
        await updateDoc(userRef, {
          role: "PASTOR",
          accountStatus: "APPROVED",
          updatedAt: serverTimestamp()
        });
        return { ...data, role: "PASTOR", accountStatus: "APPROVED" };
      }
    }
    return data;
  }

  // No user document yet: allow bootstrap Super Admin or matching Pastor invite
  if (emailLower === BOOTSTRAP_SUPER_ADMIN_EMAIL) {
    const payload = {
      uid: firebaseUser.uid,
      displayName: firebaseUser.displayName || "Super Admin",
      email: firebaseUser.email || "",
      phoneNumber: firebaseUser.phoneNumber || "",
      role: "SUPER_ADMIN",
      churchId: DEFAULT_CHURCH_ID,
      accountStatus: "APPROVED",
      photoUrl: firebaseUser.photoURL || "",
      profileCompleted: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    };
    await setDoc(userRef, payload);
    return payload;
  }

  if (emailLower) {
    const inviteSnap = await getDoc(doc(db, "pastorInvites", `email_${emailLower}`));
    if (inviteSnap.exists()) {
      const inv = inviteSnap.data();
      const payload = {
        uid: firebaseUser.uid,
        displayName: inv.displayName || firebaseUser.displayName || "Pastor",
        email: firebaseUser.email || inv.email || "",
        phoneNumber: inv.phoneNormalized || firebaseUser.phoneNumber || "",
        role: "PASTOR",
        churchId: inv.churchId || DEFAULT_CHURCH_ID,
        accountStatus: "APPROVED",
        membershipId: inv.membershipId || "",
        photoUrl: firebaseUser.photoURL || "",
        profileCompleted: true,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      };
      await setDoc(userRef, payload);
      return payload;
    }
  }

  return null;
}

function subscribeChurchData() {
  clearListeners();
  const churchId = state.churchId;
  const isSuperAdmin = state.currentUserProfile?.role === "SUPER_ADMIN";

  const watch = (qOrRef, onNext, label) => {
    const unsub = onSnapshot(
      qOrRef,
      (snap) => {
        onNext(snap);
        updateNavBadges();
        const activeEl = document.activeElement;
        const isEditingForm =
          activeEl &&
          ["INPUT", "TEXTAREA", "SELECT"].includes(activeEl.tagName) &&
          activeEl.id !== "member-search";
        const modalOpen = !document.getElementById("modal-root")?.classList.contains("hidden");
        if (!isEditingForm && !modalOpen) {
          renderCurrentRoute();
        }
      },
      (err) => {
        console.warn(`Firestore listener (${label}):`, err.message);
      }
    );
    unsubscribers.push(unsub);
  };

  watch(doc(db, "churches", churchId), (snap) => {
    if (snap.exists()) {
      state.churchConfig = { churchId, ...snap.data() };
      const titleEl = document.getElementById("sidebar-church-name");
      if (titleEl && state.churchConfig.name) titleEl.textContent = state.churchConfig.name;
    }
  }, "churchConfig");

  watch(doc(db, "dailyVerseConfig", churchId), (snap) => {
    state.dailyVerseConfig = snap.exists()
      ? { churchId, ...snap.data() }
      : { churchId, autoDailyEnabled: true, rotationHour: 6 };
  }, "dailyVerseConfig");

  watch(query(collection(db, "users"), where("churchId", "==", churchId), limit(250)), (snap) => {
    state.users = snap.docs.map((d) => ({ uid: d.id, ...d.data() }))
      .sort((a, b) => (a.accountStatus === "PENDING" ? -1 : 1) - (b.accountStatus === "PENDING" ? -1 : 1));
  }, "users");

  watch(query(collection(db, "believers"), where("churchId", "==", churchId), limit(250)), (snap) => {
    state.believers = snap.docs.map((d) => ({ memberId: d.id, ...d.data() }));
  }, "believers");

  watch(query(collection(db, "membershipApplications"), where("churchId", "==", churchId), limit(100)), (snap) => {
    state.applications = snap.docs.map((d) => ({ applicationId: d.id, ...d.data() }));
  }, "applications");

  watch(query(collection(db, "families"), where("churchId", "==", churchId), limit(250)), (snap) => {
    state.families = snap.docs.map((d) => ({ familyId: d.id, ...d.data() }));
  }, "families");

  watch(query(collection(db, "bibleVerses"), where("churchId", "==", churchId), limit(60)), (snap) => {
    state.verses = snap.docs
      .map((d) => ({ verseId: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  }, "bibleVerses");

  watch(query(collection(db, "announcements"), where("churchId", "==", churchId), limit(50)), (snap) => {
    state.announcements = snap.docs
      .map((d) => ({ announcementId: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  }, "announcements");

  watch(query(collection(db, "prayerRequests"), where("churchId", "==", churchId), limit(100)), (snap) => {
    state.prayers = snap.docs
      .map((d) => ({ requestId: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  }, "prayerRequests");

  watch(query(collection(db, "subscriptions"), where("churchId", "==", churchId), limit(200)), (snap) => {
    state.subscriptions = snap.docs
      .map((d) => ({ subscriptionId: d.id, ...d.data() }))
      .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
  }, "subscriptions");

  watch(query(collection(db, "offerings"), where("churchId", "==", churchId), limit(200)), (snap) => {
    state.offerings = snap.docs
      .map((d) => ({ transactionId: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
  }, "offerings");

  watch(query(collection(db, "events"), where("churchId", "==", churchId), limit(80)), (snap) => {
    state.events = snap.docs
      .map((d) => ({ eventId: d.id, ...d.data() }))
      .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
  }, "events");

  if (isSuperAdmin) {
    watch(query(collection(db, "auditLogs"), where("churchId", "==", churchId), limit(100)), (snap) => {
      state.auditLogs = snap.docs
        .map((d) => ({ logId: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
    }, "auditLogs");
  }
}

function updateNavBadges() {
  const setBadge = (id, count) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = String(count);
    el.classList.toggle("hidden", count <= 0);
  };
  setBadge("badge-pending-members", state.users.filter((u) => u.accountStatus === "PENDING").length);
  setBadge("badge-open-prayers", state.prayers.filter((p) => p.status === "NEW" || p.status === "IN_REVIEW").length);
  setBadge(
    "badge-pending-payments",
    state.subscriptions.filter((s) => s.status === "PENDING_VERIFICATION").length +
      state.offerings.filter((o) => o.status === "PENDING").length
  );
}

const actions = {
  navigate(route) {
    if (["updater", "church", "audit"].includes(route) && state.currentUserProfile?.role !== "SUPER_ADMIN") {
      return;
    }
    state.route = route;
    document.querySelectorAll(".nav-item").forEach((el) => {
      el.classList.toggle("active", el.dataset.route === route);
    });
    const meta = ROUTE_META[route] || ROUTE_META.overview;
    document.getElementById("page-title").textContent = meta.title;
    document.getElementById("page-subtitle").textContent = meta.subtitle;
    toggleMobileSidebar(false);
    renderCurrentRoute();
  },

  rerender() {
    renderCurrentRoute();
  },

  async updateUserRoleStatus(uid, role, accountStatus) {
    try {
      await updateDoc(doc(db, "users", uid), {
        role,
        accountStatus,
        profileCompleted: true,
        updatedAt: serverTimestamp()
      });

      if (accountStatus === "APPROVED") {
        const bRef = doc(db, "believers", `mem_${uid}`);
        const bSnap = await getDoc(bRef);
        if (bSnap.exists()) {
          await updateDoc(bRef, { membershipStatus: "ACTIVE", updatedAt: serverTimestamp() });
        }
        const aRef = doc(db, "membershipApplications", uid);
        const aSnap = await getDoc(aRef);
        if (aSnap.exists()) {
          await updateDoc(aRef, {
            status: "APPROVED",
            reviewedBy: auth.currentUser.uid,
            reviewedAt: serverTimestamp()
          });
        }
      }
      await recordAuditLog(`Web Portal: Updated user ${uid} -> ${role} / ${accountStatus}`);
      showToast(`Member updated to ${role} (${accountStatus})`, "success");
    } catch (err) {
      showToast(`Update failed: ${err.message}`, "error");
    }
  },

  async connectBelieverToFamily(familyId, memberUid, role) {
    try {
      const fam = state.families.find((f) => f.familyId === familyId);
      const user = state.users.find((u) => u.uid === memberUid);
      const believer = state.believers.find((b) => b.uid === memberUid);
      if (!fam || !user) throw new Error("Family or member record not found.");

      const linkedSet = new Set(Array.isArray(fam.linkedMemberUids) ? fam.linkedMemberUids : []);
      linkedSet.add(memberUid);

      const deps = Array.isArray(fam.dependentMembers) ? [...fam.dependentMembers] : [];
      const existingIdx = deps.findIndex((d) => d.linkedUid === memberUid);
      const entry = {
        id: existingIdx >= 0 ? deps[existingIdx].id : randomId("dep", 8),
        name: user.displayName || believer?.fullName || user.email || "Believer",
        relationship: role,
        gender: believer?.gender || "",
        baptismStatus: believer?.baptismStatus || "",
        linkedUid: memberUid,
        linkedMembershipId: user.membershipId || believer?.membershipId || "",
        phoneNumber: user.phoneNumber || believer?.phoneNumber || "",
        profilePhotoUrl: believer?.profilePhotoUrl || user.photoUrl || ""
      };
      if (existingIdx >= 0) deps[existingIdx] = entry;
      else deps.push(entry);

      const famUpdate = {
        familyId: fam.familyId,
        churchId: fam.churchId || state.churchId,
        headUid: role === "Head" ? memberUid : fam.headUid,
        headName: role === "Head" ? entry.name : fam.headName || "",
        dependentMembers: deps,
        linkedMemberUids: Array.from(linkedSet),
        sharedSubscriptionStatus: fam.sharedSubscriptionStatus || "PENDING",
        updatedAt: serverTimestamp()
      };

      await setDoc(doc(db, "families", familyId), famUpdate, { merge: true });
      await updateDoc(doc(db, "users", memberUid), { familyId, updatedAt: serverTimestamp() });
      await setDoc(
        doc(db, "believers", `mem_${memberUid}`),
        {
          uid: memberUid,
          churchId: state.churchId,
          familyId,
          familyRole: role,
          updatedAt: serverTimestamp()
        },
        { merge: true }
      );
      await recordAuditLog(`Web Portal: Connected ${entry.name} as ${role} in family ${fam.familyName || familyId}`);
      showToast(`${entry.name} connected as ${role}`, "success");
    } catch (err) {
      showToast(`Family update failed: ${err.message}`, "error");
    }
  },

  async setAutoDailyVerse(enabled) {
    try {
      await setDoc(
        doc(db, "dailyVerseConfig", state.churchId),
        {
          churchId: state.churchId,
          autoDailyEnabled: Boolean(enabled),
          rotationHour: 6,
          updatedByName: state.currentUserProfile?.displayName || "Pastor",
          updatedByUid: auth.currentUser.uid,
          updatedAt: serverTimestamp()
        },
        { merge: true }
      );
      await recordAuditLog(`Web Portal: ${enabled ? "Enabled" : "Paused"} Auto Daily Bible Verse`);
      showToast(`Auto Daily Verse ${enabled ? "enabled" : "paused"}`, "success");
    } catch (err) {
      showToast(`Could not update Auto-Verse: ${err.message}`, "error");
    }
  },

  async publishVerse({ bookReference, textEnglish, textTamil, devotionalEnglish, pinForToday }) {
    try {
      const cleanRef = String(bookReference || "").trim();
      const cleanEn = String(textEnglish || "").trim();
      const cleanTa = String(textTamil || "").trim() || cleanEn;
      const verseId = randomId("verse", 12);
      const pinKey = pinForToday ? todayDateKey() : "";
      await setDoc(doc(db, "bibleVerses", verseId), {
        verseId,
        churchId: state.churchId,
        bookReference: cleanRef,
        textEnglish: cleanEn,
        textTamil: cleanTa,
        devotionalEnglish: String(devotionalEnglish || "").trim(),
        devotionalTamil: String(devotionalEnglish || "").trim(),
        translationSource: "NKJV / Tamil BSI",
        scheduledDate: pinForToday ? `Pinned Today (${pinKey})` : "Pastor Collection",
        pinDateKey: pinKey,
        publishedByPastor: state.currentUserProfile?.displayName || "Senior Pastor",
        createdAt: serverTimestamp()
      });
      await recordAuditLog(`Web Portal: Published Daily Verse ${cleanRef}`);
      showToast(`Daily verse "${cleanRef}" published!`, "success");
      return true;
    } catch (err) {
      showToast(`Failed to publish verse: ${err.message}`, "error");
      return false;
    }
  },

  async deleteVerse(verseId) {
    try {
      await deleteDoc(doc(db, "bibleVerses", verseId));
      showToast("Bible verse deleted.", "success");
    } catch (err) {
      showToast(`Delete failed: ${err.message}`, "error");
    }
  },

  async publishAnnouncement({ category, titleEnglish, titleTamil, bodyEnglish, bodyTamil }) {
    try {
      const announcementId = randomId("ann", 8);
      const titleEn = String(titleEnglish || "").trim();
      const bodyEn = String(bodyEnglish || "").trim();
      await setDoc(doc(db, "announcements", announcementId), {
        announcementId,
        churchId: state.churchId,
        titleEnglish: titleEn,
        titleTamil: String(titleTamil || "").trim() || titleEn,
        bodyEnglish: bodyEn,
        bodyTamil: String(bodyTamil || "").trim() || bodyEn,
        category: category || "GENERAL",
        publishDate: "Just now",
        expiryDate: "",
        isEmergency: category === "EMERGENCY",
        authorName: state.currentUserProfile?.displayName || "Pastor",
        targetGroup: "ALL",
        createdBy: auth.currentUser.uid,
        createdAt: serverTimestamp()
      });
      await recordAuditLog(`Web Portal: Published announcement "${titleEn}"`);
      showToast("Announcement broadcast to all members!", "success");
      return true;
    } catch (err) {
      showToast(`Publish failed: ${err.message}`, "error");
      return false;
    }
  },

  async deleteAnnouncement(announcementId) {
    try {
      await deleteDoc(doc(db, "announcements", announcementId));
      showToast("Announcement removed.", "success");
    } catch (err) {
      showToast(`Delete failed: ${err.message}`, "error");
    }
  },

  async updatePrayerRequest(requestId, status, pastoralNote) {
    try {
      const todayLabel = new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
      await updateDoc(doc(db, "prayerRequests", requestId), {
        status,
        pastoralNote: String(pastoralNote || "").trim(),
        pastoralResponseDate: todayLabel,
        updatedAt: serverTimestamp()
      });
      showToast("Prayer request response saved.", "success");
    } catch (err) {
      showToast(`Update failed: ${err.message}`, "error");
    }
  },

  async verifySubscription(subscriptionId, approve) {
    try {
      const sub = state.subscriptions.find((s) => s.subscriptionId === subscriptionId);
      if (!sub) return;
      const todayLabel = new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
      if (approve) {
        const receipt = sub.receiptNumber || `GAG-SUB-${Date.now().toString().slice(-6)}`;
        await updateDoc(doc(db, "subscriptions", subscriptionId), {
          status: "PAID",
          amountPaid: Number(sub.amountDue || 0),
          paidDate: sub.paidDate || todayLabel,
          paymentMethod: sub.paymentMethod || "CASH_VERIFIED",
          receiptNumber: receipt,
          updatedAt: serverTimestamp()
        });
        if (sub.familyId) {
          await setDoc(
            doc(db, "families", sub.familyId),
            { sharedSubscriptionStatus: "PAID", lastPaidDate: todayLabel, lastPaidMonthYear: sub.monthYear || "", updatedAt: serverTimestamp() },
            { merge: true }
          );
        }
        await recordAuditLog(`Web Portal: Verified monthly dues PAID for ${sub.familyName || sub.memberName}`);
        showToast("Monthly family subscription marked PAID!", "success");
      } else {
        await updateDoc(doc(db, "subscriptions", subscriptionId), {
          status: "PENDING",
          amountPaid: 0,
          paidDate: "",
          transactionReference: "",
          updatedAt: serverTimestamp()
        });
        if (sub.familyId) {
          await setDoc(
            doc(db, "families", sub.familyId),
            { sharedSubscriptionStatus: "PENDING", updatedAt: serverTimestamp() },
            { merge: true }
          );
        }
        showToast("Payment claim returned to PENDING.", "info");
      }
    } catch (err) {
      showToast(`Verification failed: ${err.message}`, "error");
    }
  },

  async verifyOffering(transactionId, nextStatus) {
    try {
      const off = state.offerings.find((o) => o.transactionId === transactionId);
      if (!off) return;
      await updateDoc(doc(db, "offerings", transactionId), {
        status: nextStatus,
        receiptNumber: off.receiptNumber || `GAG-OFF-${Date.now().toString().slice(-6)}`,
        updatedAt: serverTimestamp()
      });
      await recordAuditLog(`Web Portal: Marked offering ${transactionId} as ${nextStatus}`);
      showToast(`Offering marked ${nextStatus}`, "success");
    } catch (err) {
      showToast(`Offering update failed: ${err.message}`, "error");
    }
  },

  async createEvent(payload) {
    try {
      const eventId = randomId("ev", 8);
      const titleEn = String(payload.titleEnglish || "").trim();
      await setDoc(doc(db, "events", eventId), {
        eventId,
        churchId: state.churchId,
        titleEnglish: titleEn,
        titleTamil: String(payload.titleTamil || "").trim() || titleEn,
        descriptionEnglish: String(payload.descriptionEnglish || "").trim(),
        descriptionTamil: String(payload.descriptionEnglish || "").trim(),
        eventDate: String(payload.eventDate || "").trim(),
        startTime: String(payload.startTime || "").trim(),
        endTime: "",
        venue: String(payload.venue || "").trim(),
        capacity: Math.max(1, Math.min(10000, Math.round(Number(payload.capacity || 300)))),
        registeredCount: 0,
        attendeeIds: [],
        organizer: state.currentUserProfile?.displayName || "Church Leadership",
        category: payload.category || "SUNDAY_SERVICE",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      await recordAuditLog(`Web Portal: Created event "${titleEn}"`);
      showToast(`Event "${titleEn}" published!`, "success");
      return true;
    } catch (err) {
      showToast(`Event creation failed: ${err.message}`, "error");
      return false;
    }
  },

  async deleteEvent(eventId) {
    try {
      await deleteDoc(doc(db, "events", eventId));
      showToast("Event deleted.", "success");
    } catch (err) {
      showToast(`Delete failed: ${err.message}`, "error");
    }
  },

  async saveChurchConfig(config) {
    try {
      const normalized = {
        churchId: state.churchId,
        name: String(config.name || "GRACE AG").trim(),
        address: String(config.address || "").trim(),
        phone: String(config.phone || "").trim(),
        email: String(config.email || "").trim(),
        defaultLanguage: config.defaultLanguage === "ta" ? "ta" : "en",
        serviceTimings: String(config.serviceTimings || "").trim(),
        monthlyDuesAmount: Math.max(0, Number(config.monthlyDuesAmount || 0)),
        upiId: String(config.upiId || "").trim(),
        bankAccountDetails: String(config.bankAccountDetails || "").trim(),
        latestVersionCode: Math.max(1, Math.round(Number(config.latestVersionCode || 1))),
        latestVersionName: String(config.latestVersionName || "1.0").trim(),
        updateDownloadUrl: String(config.updateDownloadUrl || "").trim(),
        updateReleaseNotes: String(config.updateReleaseNotes || "").trim(),
        forceUpdate: Boolean(config.forceUpdate),
        updatedAt: serverTimestamp()
      };
      await setDoc(doc(db, "churches", state.churchId), normalized, { merge: true });
      await recordAuditLog(
        `Web Portal: Saved church settings (${normalized.name}) & APK release v${normalized.latestVersionName} (${normalized.latestVersionCode})`
      );
      showToast("Church configuration & App Updater saved!", "success");
    } catch (err) {
      showToast(`Save failed: ${err.message}`, "error");
    }
  },

  async createPastorInvite({ displayName, email, phoneNumber }) {
    try {
      const cleanName = String(displayName || "").trim();
      const cleanEmail = String(email || "").trim();
      const emailLower = cleanEmail.toLowerCase();
      const digits = String(phoneNumber || "").replace(/\D/g, "");
      const phoneNormalized = String(phoneNumber || "").trim().startsWith("+")
        ? `+${digits}`
        : digits.length >= 10
        ? `+91${digits.slice(-10)}`
        : String(phoneNumber || "").trim();

      const payload = {
        inviteId: `email_${emailLower}`,
        churchId: state.churchId,
        displayName: cleanName,
        email: cleanEmail,
        emailLower,
        phoneNormalized,
        role: "PASTOR",
        accountStatus: "APPROVED",
        membershipId: `GAG-P-${new Date().getFullYear()}-${String(Math.floor(100 + Math.random() * 900))}`,
        createdBy: auth.currentUser.uid,
        createdAt: serverTimestamp()
      };

      await setDoc(doc(db, "pastorInvites", `email_${emailLower}`), payload);
      if (phoneNormalized.startsWith("+")) {
        await setDoc(doc(db, "pastorInvites", `phone_${phoneNormalized}`), {
          ...payload,
          inviteId: `phone_${phoneNormalized}`
        });
      }

      const existing = state.users.find((u) => (u.email || "").toLowerCase() === emailLower);
      if (existing) {
        await updateDoc(doc(db, "users", existing.uid), {
          role: "PASTOR",
          accountStatus: "APPROVED",
          displayName: cleanName,
          phoneNumber: phoneNormalized,
          updatedAt: serverTimestamp()
        });
      }

      await recordAuditLog(`Web Portal: Created Pastor invite for ${cleanName} (${cleanEmail})`);
      showToast(`Pastor invite created for ${cleanName}!`, "success");
      return true;
    } catch (err) {
      showToast(`Invite failed: ${err.message}`, "error");
      return false;
    }
  }
};

function renderCurrentRoute() {
  const container = document.getElementById("page-content");
  if (!container || !state.currentUserProfile) return;

  switch (state.route) {
    case "overview": return renderOverviewView(container, state, actions);
    case "members": return renderMembersView(container, state, actions);
    case "families": return renderFamiliesView(container, state, actions);
    case "verses": return renderDailyVerseView(container, state, actions);
    case "announcements": return renderAnnouncementsView(container, state, actions);
    case "prayers": return renderPrayersView(container, state, actions);
    case "giving": return renderGivingView(container, state, actions);
    case "events": return renderEventsView(container, state, actions);
    case "updater": return renderUpdaterView(container, state, actions);
    case "church": return renderChurchAndPastorsView(container, state, actions);
    case "audit": return renderAuditLogsView(container, state);
    default: return renderOverviewView(container, state, actions);
  }
}

function toggleMobileSidebar(open) {
  const sidebar = document.getElementById("sidebar");
  const backdrop = document.getElementById("sidebar-backdrop");
  sidebar?.classList.toggle("open", open);
  backdrop?.classList.toggle("hidden", !open);
}

function bindShellControls() {
  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.addEventListener("click", () => actions.navigate(btn.dataset.route));
  });

  document.getElementById("btn-open-sidebar")?.addEventListener("click", () => toggleMobileSidebar(true));
  document.getElementById("btn-close-sidebar")?.addEventListener("click", () => toggleMobileSidebar(false));
  document.getElementById("sidebar-backdrop")?.addEventListener("click", () => toggleMobileSidebar(false));
  document.getElementById("btn-close-modal")?.addEventListener("click", closeModal);
  document.getElementById("modal-root")?.addEventListener("click", (e) => {
    if (e.target.id === "modal-root") closeModal();
  });

  document.getElementById("btn-signout")?.addEventListener("click", async () => {
    clearListeners();
    await signOut(auth);
  });

  document.getElementById("btn-google-signin")?.addEventListener("click", async () => {
    showAuthError("");
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err) {
      if (err.code === "auth/unauthorized-domain") {
        showAuthError(
          `This domain (${window.location.hostname}) is not authorized in Firebase Auth yet. Add "${window.location.hostname}" under Firebase Console -> Authentication -> Settings -> Authorized domains.`
        );
      } else {
        showAuthError(err.message);
      }
    }
  });

  document.getElementById("email-login-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    showAuthError("");
    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value;
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err) {
      showAuthError(err.message);
    }
  });
}

function boot() {
  bindShellControls();

  if (!isFirebaseConfigured) {
    const warn = document.getElementById("config-warning");
    warn.innerHTML =
      "<strong>Firebase web config not injected yet.</strong> Make sure <code>GOOGLE_SERVICES_JSON</code> is added in GitHub Repository Secrets and deploy via the <strong>Deploy Web Portal</strong> GitHub Action.";
    warn.classList.remove("hidden");
    document.getElementById("btn-google-signin").disabled = true;
    document.getElementById("btn-email-signin").disabled = true;
    return;
  }

  onAuthStateChanged(auth, async (firebaseUser) => {
    const authScreen = document.getElementById("auth-screen");
    const appShell = document.getElementById("app-shell");

    if (!firebaseUser) {
      clearListeners();
      state.currentUserProfile = null;
      appShell.classList.add("hidden");
      authScreen.classList.remove("hidden");
      return;
    }

    try {
      const profile = await resolveOrClaimPortalProfile(firebaseUser);
      const allowed =
        profile &&
        (profile.role === "PASTOR" || profile.role === "SUPER_ADMIN") &&
        profile.accountStatus === "APPROVED";

      if (!allowed) {
        await signOut(auth);
        showAuthError(
          "Access denied. Only approved Pastors and Super Admins can sign into this Web Portal. Believers should use the GRACE AG mobile app."
        );
        return;
      }

      state.currentUserProfile = profile;
      state.churchId = profile.churchId || DEFAULT_CHURCH_ID;

      document.getElementById("sidebar-role-badge").textContent = profile.role.replace(/_/g, " ");
      document.getElementById("user-name").textContent = profile.displayName || firebaseUser.displayName || "Pastor";
      document.getElementById("user-email").textContent = profile.email || firebaseUser.email || "";
      document.getElementById("user-avatar").textContent = initials(profile.displayName || firebaseUser.email);
      document.getElementById("topbar-church-id").textContent = state.churchId;
      document
        .getElementById("super-admin-nav-group")
        .classList.toggle("hidden", profile.role !== "SUPER_ADMIN");

      authScreen.classList.add("hidden");
      appShell.classList.remove("hidden");
      subscribeChurchData();
      actions.navigate("overview");
    } catch (err) {
      console.error(err);
      showAuthError(`Sign-in verification failed: ${err.message}`);
      await signOut(auth);
    }
  });
}

boot();
