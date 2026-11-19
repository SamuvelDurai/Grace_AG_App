const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read user profile or prayers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("user123").get());
  await assertFails(unauthDb.collection("prayerRequests").where("uid", "==", "user123").get());
  await assertFails(unauthDb.collection("offerings").where("uid", "==", "user123").get());
});

test("Authenticated user can read own profile and query own prayers", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("user123").set({
      uid: "user123",
      displayName: "John Doe",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("prayerRequests").doc("pr1").set({
      requestId: "pr1",
      uid: "user123",
      churchId: "church_main",
      title: "Peace",
      description: "Pray for family",
      privacy: "PASTOR_ONLY",
      status: "NEW"
    });
  });

  const authDb = testEnv.authenticatedContext("user123").firestore();
  await assertSucceeds(authDb.collection("users").doc("user123").get());
  await assertSucceeds(authDb.collection("prayerRequests").where("uid", "==", "user123").get());
});

test("Authenticated user cannot read another user's private prayer or offering", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("prayerRequests").doc("bob_pr").set({
      requestId: "bob_pr",
      uid: "bob_456",
      churchId: "church_main",
      title: "Private",
      description: "Private prayer",
      privacy: "PASTOR_ONLY",
      status: "NEW"
    });
    await db.collection("offerings").doc("bob_tx").set({
      transactionId: "bob_tx",
      uid: "bob_456",
      churchId: "church_main",
      category: "TITHE",
      amount: 1000,
      receiptNumber: "REC-TITHE-1001"
    });
  });

  const aliceDb = testEnv.authenticatedContext("alice_123").firestore();
  await assertFails(aliceDb.collection("prayerRequests").doc("bob_pr").get());
  await assertFails(aliceDb.collection("offerings").doc("bob_tx").get());
});

test("Authenticated user cannot escalate role during update", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("user123").set({
      uid: "user123",
      displayName: "John Doe",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const authDb = testEnv.authenticatedContext("user123").firestore();
  await assertFails(
    authDb.collection("users").doc("user123").update({
      role: "SUPER_ADMIN"
    })
  );
});

test("Believer can submit PENDING offering, cannot self-approve SUCCESS, and Pastor can verify", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("pastor_off").set({
      uid: "pastor_off",
      displayName: "Pastor Paul",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const aliceDb = testEnv.authenticatedContext("alice_123").firestore();
  const pastorDb = testEnv.authenticatedContext("pastor_off").firestore();

  // Believer can submit a PENDING offering claim
  await assertSucceeds(
    aliceDb.collection("offerings").doc("tx_valid").set({
      transactionId: "tx_valid",
      uid: "alice_123",
      churchId: "church_main",
      category: "TITHE",
      amount: 500,
      status: "PENDING",
      transactionReference: "UTR11223344",
      receiptNumber: "REQ-TITH-202611-0001"
    })
  );

  // Believer CANNOT create an already-SUCCESS offering or self-verify
  await assertFails(
    aliceDb.collection("offerings").doc("tx_self_paid").set({
      transactionId: "tx_self_paid",
      uid: "alice_123",
      churchId: "church_main",
      category: "TITHE",
      amount: 500,
      status: "SUCCESS",
      receiptNumber: "REC-TITH-202611-0001"
    })
  );
  await assertFails(
    aliceDb.collection("offerings").doc("tx_valid").update({
      status: "SUCCESS",
      receiptNumber: "REC-TITH-202611-0001"
    })
  );

  // Negative amount and uid spoofing remain blocked
  await assertFails(
    aliceDb.collection("offerings").doc("tx_negative").set({
      transactionId: "tx_negative",
      uid: "alice_123",
      churchId: "church_main",
      category: "TITHE",
      amount: -50,
      status: "PENDING",
      receiptNumber: "REQ-TITH-202611-0002"
    })
  );
  await assertFails(
    aliceDb.collection("offerings").doc("tx_spoof").set({
      transactionId: "tx_spoof",
      uid: "bob_456",
      churchId: "church_main",
      category: "TITHE",
      amount: 500,
      status: "PENDING",
      receiptNumber: "REQ-TITH-202611-0003"
    })
  );

  // Pastor can verify and issue official receipt number
  await assertSucceeds(
    pastorDb.collection("offerings").doc("tx_valid").update({
      status: "SUCCESS",
      receiptNumber: "REC-TITH-202611-0001"
    })
  );
});

test("Approved Pastor can publish announcements and read church prayer requests", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_1").set({
      uid: "pastor_1",
      displayName: "Pastor Stephen",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("prayerRequests").doc("pr_member").set({
      requestId: "pr_member",
      uid: "alice_123",
      churchId: "church_main",
      title: "Guidance",
      description: "Pray",
      privacy: "PASTOR_ONLY",
      status: "NEW"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_1").firestore();
  await assertSucceeds(
    pastorDb.collection("prayerRequests").where("churchId", "==", "church_main").get()
  );
  await assertSucceeds(
    pastorDb.collection("announcements").doc("ann_new").set({
      announcementId: "ann_new",
      churchId: "church_main",
      titleEnglish: "Fasting Prayer",
      titleTamil: "உபவாச ஜெபம்"
    })
  );
});

test("Fellowship groups allow authenticated read, pastor CRUD, and block believer/unauthenticated writes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_fg").set({
      uid: "pastor_fg",
      displayName: "Pastor Daniel",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_fg").set({
      uid: "believer_fg",
      displayName: "Mary",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_fg").firestore();
  const believerDb = testEnv.authenticatedContext("believer_fg").firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const validGroup = {
    groupId: "fg_youth_new",
    churchId: "church_main",
    nameEnglish: "Zion Youth Fellowship",
    nameTamil: "சீயோன் வாலிபர் ஐக்கியம்",
    areaZone: "North Zone",
    leaderName: "Bro. Caleb",
    leaderPhone: "+91 98765 43210",
    meetingSchedule: "Saturdays 5:00 PM",
    meetingLocation: "Youth Hall",
    memberCount: 12,
    memberIds: ["mem_1", "mem_2"],
    category: "YOUTH_MINISTRY"
  };

  // Unauthenticated cannot read or write
  await assertFails(unauthDb.collection("fellowshipGroups").doc("fg_youth_new").get());
  await assertFails(unauthDb.collection("fellowshipGroups").doc("fg_youth_new").set(validGroup));

  // Believer cannot create
  await assertFails(believerDb.collection("fellowshipGroups").doc("fg_youth_new").set(validGroup));

  // Approved Pastor can create
  await assertSucceeds(pastorDb.collection("fellowshipGroups").doc("fg_youth_new").set(validGroup));

  // Believer can read created group, but cannot update or delete
  await assertSucceeds(believerDb.collection("fellowshipGroups").doc("fg_youth_new").get());
  await assertFails(
    believerDb.collection("fellowshipGroups").doc("fg_youth_new").update({
      memberIds: ["mem_1", "mem_2", "mem_3"],
      memberCount: 13
    })
  );
  await assertFails(believerDb.collection("fellowshipGroups").doc("fg_youth_new").delete());

  // Pastor can update members and category (including OTHER_GROUP)
  await assertSucceeds(
    pastorDb.collection("fellowshipGroups").doc("fg_youth_new").update({
      memberIds: ["mem_1", "mem_2", "mem_3"],
      memberCount: 13,
      category: "OTHER_GROUP"
    })
  );

  // Pastor fails invalid category or negative memberCount
  await assertFails(
    pastorDb.collection("fellowshipGroups").doc("fg_youth_new").update({
      memberCount: -1
    })
  );

  // Pastor can delete group
  await assertSucceeds(pastorDb.collection("fellowshipGroups").doc("fg_youth_new").delete());
});

test("Church events allow authenticated read, pastor CRUD and attendee management, and block believer/unauthenticated writes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_ev").set({
      uid: "pastor_ev",
      displayName: "Pastor Thomas",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_ev").set({
      uid: "believer_ev",
      displayName: "Esther",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_ev").firestore();
  const believerDb = testEnv.authenticatedContext("believer_ev").firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const validEvent = {
    eventId: "ev_good_friday",
    churchId: "church_main",
    titleEnglish: "Good Friday Service",
    titleTamil: "புனித வெள்ளி ஆராதனை",
    eventDate: "Friday, 03 Apr 2026",
    startTime: "09:00 AM",
    endTime: "12:00 PM",
    venue: "Main Sanctuary",
    capacity: 300,
    registeredCount: 2,
    attendeeIds: ["mem_1", "mem_2"],
    category: "SPECIAL_FESTIVAL"
  };

  // Unauthenticated denied
  await assertFails(unauthDb.collection("events").doc("ev_good_friday").get());
  await assertFails(unauthDb.collection("events").doc("ev_good_friday").set(validEvent));

  // Believer cannot create
  await assertFails(believerDb.collection("events").doc("ev_good_friday").set(validEvent));

  // Pastor can create
  await assertSucceeds(pastorDb.collection("events").doc("ev_good_friday").set(validEvent));

  // Believer can read, cannot spoof other members, and can toggle own UID RSVP
  await assertSucceeds(believerDb.collection("events").doc("ev_good_friday").get());
  await assertFails(
    believerDb.collection("events").doc("ev_good_friday").update({
      attendeeIds: ["mem_1", "mem_2", "mem_3"],
      registeredCount: 3
    })
  );
  await assertSucceeds(
    believerDb.collection("events").doc("ev_good_friday").update({
      attendeeIds: ["mem_1", "mem_2", "believer_ev"],
      registeredCount: 3
    })
  );
  await assertFails(believerDb.collection("events").doc("ev_good_friday").delete());

  // Pastor can update attendees & category
  await assertSucceeds(
    pastorDb.collection("events").doc("ev_good_friday").update({
      attendeeIds: ["mem_1", "mem_2", "mem_3"],
      registeredCount: 3,
      category: "SUNDAY_SERVICE"
    })
  );

  // Pastor fails invalid capacity
  await assertFails(
    pastorDb.collection("events").doc("ev_good_friday").update({
      capacity: 0
    })
  );

  // Pastor can delete event
  await assertSucceeds(pastorDb.collection("events").doc("ev_good_friday").delete());
});

test("Pastors can publish/delete announcements and sermons, Super Admin manages church config and audit logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("admin_cfg").set({
      uid: "admin_cfg",
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("pastor_cfg").set({
      uid: "pastor_cfg",
      displayName: "Pastor John",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_cfg").set({
      uid: "believer_cfg",
      displayName: "Believer Luke",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_cfg").firestore();
  const pastorDb = testEnv.authenticatedContext("pastor_cfg").firestore();
  const believerDb = testEnv.authenticatedContext("believer_cfg").firestore();

  // Pastor can publish & delete announcement and sermon
  await assertSucceeds(
    pastorDb.collection("announcements").doc("ann_del").set({
      announcementId: "ann_del",
      churchId: "church_main",
      titleEnglish: "Midweek Bible Study"
    })
  );
  await assertSucceeds(pastorDb.collection("announcements").doc("ann_del").delete());

  await assertSucceeds(
    pastorDb.collection("sermons").doc("serm_del").set({
      sermonId: "serm_del",
      churchId: "church_main",
      titleEnglish: "Grace Abounds",
      speakerName: "Pastor John"
    })
  );
  await assertFails(believerDb.collection("sermons").doc("serm_del").delete());
  await assertSucceeds(pastorDb.collection("sermons").doc("serm_del").delete());

  // Only Super Admin can update church config, and schema must be valid
  const validChurch = {
    churchId: "church_main",
    name: "Grace Baptist Church",
    defaultLanguage: "en",
    monthlyDuesAmount: 500
  };
  await assertFails(pastorDb.collection("churches").doc("church_main").set(validChurch));
  await assertFails(believerDb.collection("churches").doc("church_main").set(validChurch));
  await assertSucceeds(adminDb.collection("churches").doc("church_main").set(validChurch));
  await assertFails(
    adminDb.collection("churches").doc("church_main").set({
      ...validChurch,
      monthlyDuesAmount: -10
    })
  );

  // Audit logs: actorUid must match caller, immutable after creation, readable only by Super Admin
  await assertFails(
    pastorDb.collection("auditLogs").doc("log_spoof").set({
      logId: "log_spoof",
      churchId: "church_main",
      action: "Spoofed log",
      actorUid: "someone_else"
    })
  );
  await assertSucceeds(
    adminDb.collection("auditLogs").doc("log_ok").set({
      logId: "log_ok",
      churchId: "church_main",
      action: "Saved church config",
      actorUid: "admin_cfg",
      status: "SUCCESS"
    })
  );
  await assertSucceeds(adminDb.collection("auditLogs").where("churchId", "==", "church_main").get());
  await assertFails(believerDb.collection("auditLogs").doc("log_ok").get());
  await assertFails(adminDb.collection("auditLogs").doc("log_ok").delete());
});

test("Pastoral care alerts allow believer own request, pastor read/status update, and block cross-user tampering", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_pc").set({
      uid: "pastor_pc",
      displayName: "Pastor Paul",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_pc").set({
      uid: "believer_pc",
      displayName: "Mary",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_pc").firestore();
  const believerDb = testEnv.authenticatedContext("believer_pc").firestore();
  const otherDb = testEnv.authenticatedContext("other_pc").firestore();

  const validAlert = {
    alertId: "pca_1",
    churchId: "church_main",
    memberUid: "believer_pc",
    memberName: "Mary",
    careType: "HOSPITAL_VISIT",
    status: "PENDING"
  };

  await assertSucceeds(believerDb.collection("pastoralCareAlerts").doc("pca_1").set(validAlert));
  await assertFails(otherDb.collection("pastoralCareAlerts").doc("pca_1").get());
  await assertSucceeds(pastorDb.collection("pastoralCareAlerts").doc("pca_1").get());
  await assertFails(believerDb.collection("pastoralCareAlerts").doc("pca_1").update({ status: "VISITED" }));
  await assertSucceeds(pastorDb.collection("pastoralCareAlerts").doc("pca_1").update({ status: "VISITED" }));
});

test("First user cannot hijack existing claimed church admin as SUPER_ADMIN", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("churches").doc("church_main").set({
      churchId: "church_main",
      name: "Grace Baptist Church",
      defaultLanguage: "en",
      adminUid: "real_owner_uid"
    });
  });

  const intruderDb = testEnv.authenticatedContext("intruder_uid", { email: "intruder@example.com" }).firestore();
  await assertFails(
    intruderDb.collection("users").doc("intruder_uid").set({
      uid: "intruder_uid",
      displayName: "Intruder",
      role: "SUPER_ADMIN",
      churchId: "church_main",
      accountStatus: "APPROVED"
    })
  );
  await assertSucceeds(
    intruderDb.collection("users").doc("intruder_uid").set({
      uid: "intruder_uid",
      displayName: "Regular Member",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "PENDING"
    })
  );
});

test("Super Admin can create Pastor accounts and Pastor invites while Believers cannot forge Pastor invites", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("admin_pastor_mgr").set({
      uid: "admin_pastor_mgr",
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_pastor_test").set({
      uid: "believer_pastor_test",
      displayName: "Believer",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_pastor_mgr").firestore();
  const believerDb = testEnv.authenticatedContext("believer_pastor_test").firestore();
  const invitedPastorDb = testEnv.authenticatedContext("invited_pastor_uid", { email: "pastor.john@church.org" }).firestore();

  // Believer cannot create a Pastor account or Pastor invite
  await assertFails(
    believerDb.collection("users").doc("new_pastor_uid").set({
      uid: "new_pastor_uid",
      displayName: "Fake Pastor",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    })
  );
  await assertFails(
    believerDb.collection("pastorInvites").doc("email_fake@church.org").set({
      emailLower: "fake@church.org",
      role: "PASTOR"
    })
  );

  // Super Admin can directly create an approved Pastor user profile and invite
  await assertSucceeds(
    adminDb.collection("users").doc("new_pastor_uid").set({
      uid: "new_pastor_uid",
      displayName: "Pastor Samuel",
      email: "pastor.samuel@church.org",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED",
      profileCompleted: true
    })
  );
  await assertSucceeds(
    adminDb.collection("pastorInvites").doc("email_pastor.john@church.org").set({
      inviteId: "email_pastor.john@church.org",
      churchId: "church_main",
      displayName: "Pastor John",
      emailLower: "pastor.john@church.org",
      role: "PASTOR",
      accountStatus: "APPROVED"
    })
  );

  // Invited Pastor can read their invite and create their own PASTOR profile
  await assertSucceeds(invitedPastorDb.collection("pastorInvites").doc("email_pastor.john@church.org").get());
  await assertSucceeds(
    invitedPastorDb.collection("users").doc("invited_pastor_uid").set({
      uid: "invited_pastor_uid",
      displayName: "Pastor John",
      email: "pastor.john@church.org",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED",
      profileCompleted: true
    })
  );
});

test("Family-shared subscriptions allow spouse with matching familyId to read and pay once, block non-family users and familyId spoofing", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_fam").set({
      uid: "pastor_fam",
      displayName: "Pastor Daniel",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("head_uid").set({
      uid: "head_uid",
      displayName: "Head Samuel",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_head_uid"
    });
    await db.collection("users").doc("spouse_uid").set({
      uid: "spouse_uid",
      displayName: "Spouse Martha",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_head_uid"
    });
    await db.collection("users").doc("stranger_uid").set({
      uid: "stranger_uid",
      displayName: "Other Believer",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_stranger_uid"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_fam").firestore();
  const spouseDb = testEnv.authenticatedContext("spouse_uid").firestore();
  const headDb = testEnv.authenticatedContext("head_uid").firestore();
  const strangerDb = testEnv.authenticatedContext("stranger_uid").firestore();

  // Stranger cannot spoof head's familyId on own /users profile
  await assertFails(
    strangerDb.collection("users").doc("stranger_uid").update({
      familyId: "fam_head_uid"
    })
  );

  // Checking a nonexistent monthly bill returns cleanly (exists=false) for family generation / self-pay
  await assertSucceeds(pastorDb.collection("subscriptions").doc("sub_fam_head_uid_2099_01").get());
  await assertSucceeds(spouseDb.collection("subscriptions").doc("sub_fam_head_uid_2099_01").get());

  // Pastor generates 1 monthly subscription bill for the family
  const subId = "sub_fam_head_uid_2026_11";
  await assertSucceeds(
    pastorDb.collection("subscriptions").doc(subId).set({
      subscriptionId: subId,
      churchId: "church_main",
      uid: "head_uid",
      familyId: "fam_head_uid",
      familyName: "Head Samuel Family",
      familyMemberCount: 3,
      memberName: "Head Samuel",
      monthYear: "November 2026",
      amountDue: 500,
      amountPaid: 0,
      status: "PENDING",
      dueDate: "10 Nov 2026"
    })
  );

  // Non-family believer cannot read or pay the family's subscription
  await assertFails(strangerDb.collection("subscriptions").doc(subId).get());
  await assertFails(
    strangerDb.collection("subscriptions").doc(subId).update({
      status: "PAID",
      amountPaid: 500
    })
  );

  // Linked spouse CANNOT directly self-mark PAID without Pastor/Admin verification
  await assertSucceeds(spouseDb.collection("subscriptions").doc(subId).get());
  await assertFails(
    spouseDb.collection("subscriptions").doc(subId).update({
      status: "PAID",
      amountPaid: 500,
      paidByUid: "spouse_uid",
      receiptNumber: "REC-SUB-202611-0001"
    })
  );

  // Linked spouse CAN submit payment proof (PENDING_VERIFICATION, amountPaid: 0)
  await assertSucceeds(
    spouseDb.collection("subscriptions").doc(subId).update({
      status: "PENDING_VERIFICATION",
      amountPaid: 0,
      paidDate: "05 Nov 2026",
      paidByUid: "spouse_uid",
      paidByName: "Spouse Martha",
      paymentMethod: "UPI_ONLINE",
      transactionReference: "UTR99887766",
      receiptNumber: "REQ-SUB-202611-0001"
    })
  );

  // Pastor verifies and marks the household subscription officially PAID
  await assertSucceeds(
    pastorDb.collection("subscriptions").doc(subId).update({
      status: "PAID",
      amountPaid: 500,
      paidDate: "05 Nov 2026",
      paidByUid: "spouse_uid",
      paidByName: "Spouse Martha",
      paymentMethod: "UPI_ONLINE",
      receiptNumber: "REC-SUB-202611-0001"
    })
  );

  // Head of family can read and query the PAID household subscription (both by uid and by familyId)
  await assertSucceeds(headDb.collection("subscriptions").doc(subId).get());
  await assertSucceeds(headDb.collection("subscriptions").where("uid", "==", "head_uid").get());
  await assertSucceeds(headDb.collection("subscriptions").where("familyId", "==", "fam_head_uid").get());
  await assertSucceeds(spouseDb.collection("subscriptions").where("familyId", "==", "fam_head_uid").get());

  // Spouse cannot reassign familyId to hijack another family's bill
  await assertFails(
    spouseDb.collection("subscriptions").doc(subId).update({
      familyId: "fam_stranger_uid"
    })
  );
});

test("Families collection protects household head, dependent members, and shared subscription status", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_family").set({
      uid: "pastor_family",
      displayName: "Pastor Paul",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("fam_head_u1").set({
      uid: "fam_head_u1",
      displayName: "David Jacob",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_fam_head_u1"
    });
    await db.collection("users").doc("fam_spouse_u2").set({
      uid: "fam_spouse_u2",
      displayName: "Mary Jacob",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_fam_head_u1"
    });
    await db.collection("users").doc("outsider_u3").set({
      uid: "outsider_u3",
      displayName: "Outsider",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_outsider_u3"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_family").firestore();
  const headDb = testEnv.authenticatedContext("fam_head_u1").firestore();
  const spouseDb = testEnv.authenticatedContext("fam_spouse_u2").firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_u3").firestore();

  const famId = "fam_fam_head_u1";
  const familyDoc = {
    familyId: famId,
    churchId: "church_main",
    familyName: "Jacob Household",
    headUid: "fam_head_u1",
    headMemberId: "mem_fam_head_u1",
    headName: "David Jacob",
    dependentMembers: [
      { id: "dep_1", name: "Luke Jacob", relationship: "Son", baptismStatus: "NOT_BAPTIZED" }
    ],
    linkedMemberUids: ["fam_head_u1", "fam_spouse_u2"],
    sharedSubscriptionStatus: "PENDING"
  };

  // Outsider cannot spoof creating another user's family
  await assertFails(outsiderDb.collection("families").doc(famId).set(familyDoc));

  // Household head can create their own Family entity
  await assertSucceeds(headDb.collection("families").doc(famId).set(familyDoc));

  // Linked spouse and Pastor can read the Family entity; outsider is denied
  await assertSucceeds(spouseDb.collection("families").doc(famId).get());
  await assertSucceeds(pastorDb.collection("families").doc(famId).get());
  await assertFails(outsiderDb.collection("families").doc(famId).get());

  // Linked spouse can add a dependent and set PENDING_VERIFICATION, but CANNOT self-mark PAID
  await assertFails(
    spouseDb.collection("families").doc(famId).update({
      sharedSubscriptionStatus: "PAID"
    })
  );
  await assertSucceeds(
    spouseDb.collection("families").doc(famId).update({
      dependentMembers: [
        { id: "dep_1", name: "Luke Jacob", relationship: "Son", baptismStatus: "NOT_BAPTIZED" },
        { id: "dep_2", name: "Hannah Jacob", relationship: "Daughter", baptismStatus: "NOT_BAPTIZED" }
      ],
      sharedSubscriptionStatus: "PENDING_VERIFICATION",
      lastPaidByUid: "fam_spouse_u2",
      lastPaidByName: "Mary Jacob"
    })
  );

  // Pastor verifies and marks sharedSubscriptionStatus as PAID
  await assertSucceeds(
    pastorDb.collection("families").doc(famId).update({
      sharedSubscriptionStatus: "PAID"
    })
  );

  // Outsider cannot modify or delete the family entity
  await assertFails(outsiderDb.collection("families").doc(famId).update({ sharedSubscriptionStatus: "WAIVED" }));
  await assertFails(headDb.collection("families").doc(famId).delete());
});

test("MemberLookup protects privacy against listing/spoofing and enforces photo size limits", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("pastor_lk").set({
      uid: "pastor_lk",
      displayName: "Pastor Daniel",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("alice_lk").set({
      uid: "alice_lk",
      displayName: "Alice",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_alice_lk"
    });
    await db.collection("users").doc("bob_lk").set({
      uid: "bob_lk",
      displayName: "Bob",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_bob_lk"
    });
  });

  const pastorDb = testEnv.authenticatedContext("pastor_lk").firestore();
  const aliceDb = testEnv.authenticatedContext("alice_lk").firestore();
  const bobDb = testEnv.authenticatedContext("bob_lk").firestore();

  const tinyPhoto = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/";
  const oversizedPhoto = "data:image/jpeg;base64," + "A".repeat(200500);

  // Alice creates her own lookup index & believer profile with valid compressed photo
  await assertSucceeds(
    aliceDb.collection("memberLookup").doc("id_GAG-2026-0101").set({
      uid: "alice_lk",
      memberId: "mem_alice_lk",
      membershipId: "GAG-2026-0101",
      churchId: "church_main",
      fullName: "Alice",
      phoneNumber: "9876543210",
      profilePhotoUrl: tinyPhoto
    })
  );
  await assertSucceeds(
    aliceDb.collection("believers").doc("mem_alice_lk").set({
      memberId: "mem_alice_lk",
      uid: "alice_lk",
      churchId: "church_main",
      membershipId: "GAG-2026-0101",
      fullName: "Alice",
      profilePhotoUrl: tinyPhoto
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc("alice_lk").update({
      photoUrl: tinyPhoto
    })
  );

  // Oversized (>200,000 chars) photos are rejected across users, believers, and memberLookup
  await assertFails(
    aliceDb.collection("users").doc("alice_lk").update({
      photoUrl: oversizedPhoto
    })
  );
  await assertFails(
    aliceDb.collection("believers").doc("mem_alice_lk").update({
      profilePhotoUrl: oversizedPhoto
    })
  );
  await assertFails(
    aliceDb.collection("memberLookup").doc("phone_9876543210").set({
      uid: "alice_lk",
      churchId: "church_main",
      profilePhotoUrl: oversizedPhoto
    })
  );

  // Bob can exact-get Alice's lookup doc, CANNOT list/scrape memberLookup, and CANNOT overwrite Alice's lookup doc
  await assertSucceeds(bobDb.collection("memberLookup").doc("id_GAG-2026-0101").get());
  await assertFails(bobDb.collection("memberLookup").where("churchId", "==", "church_main").get());
  await assertFails(
    bobDb.collection("memberLookup").doc("id_GAG-2026-0101").set({
      uid: "alice_lk",
      churchId: "church_main",
      fullName: "Hijacked"
    })
  );

  // Pastor CAN list memberLookup and update a lookup doc
  await assertSucceeds(pastorDb.collection("memberLookup").where("churchId", "==", "church_main").get());
  await assertSucceeds(
    pastorDb.collection("memberLookup").doc("id_GAG-2026-0101").update({
      fullName: "Alice Verified"
    })
  );
});

test("Family head can connect another believer, linked believer can adopt familyId, and head transfer is restricted", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("head_con").set({
      uid: "head_con",
      displayName: "Father Samuel",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_head_con"
    });
    await db.collection("users").doc("rel_con").set({
      uid: "rel_con",
      displayName: "Mother Ruth",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_rel_con"
    });
    await db.collection("users").doc("stranger_con").set({
      uid: "stranger_con",
      displayName: "Stranger",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED",
      familyId: "fam_stranger_con"
    });
  });

  const headDb = testEnv.authenticatedContext("head_con").firestore();
  const relDb = testEnv.authenticatedContext("rel_con").firestore();
  const strangerDb = testEnv.authenticatedContext("stranger_con").firestore();
  const famId = "fam_head_con";

  await assertSucceeds(
    headDb.collection("families").doc(famId).set({
      familyId: famId,
      churchId: "church_main",
      familyName: "Samuel Family",
      headUid: "head_con",
      headMemberId: "mem_head_con",
      headName: "Father Samuel",
      dependentMembers: [
        {
          id: "linked_rel_con",
          name: "Mother Ruth",
          relationship: "Mother",
          linkedUid: "rel_con",
          membershipId: "GAG-2026-0202"
        }
      ],
      linkedMemberUids: ["head_con", "rel_con"],
      sharedSubscriptionStatus: "PENDING"
    })
  );

  // Linked believer can read /families/{famId} and adopt familyId onto their own /users/{uid}
  await assertSucceeds(relDb.collection("families").doc(famId).get());
  await assertSucceeds(
    relDb.collection("users").doc("rel_con").update({
      familyId: famId
    })
  );

  // Stranger cannot adopt famId since they are not in linkedMemberUids
  await assertFails(
    strangerDb.collection("users").doc("stranger_con").update({
      familyId: famId
    })
  );

  // Non-head linked member CANNOT hijack headUid
  await assertFails(
    relDb.collection("families").doc(famId).update({
      headUid: "rel_con",
      headName: "Mother Ruth"
    })
  );

  // Current Head CAN transfer headUid to rel_con (already in linkedMemberUids)
  await assertSucceeds(
    headDb.collection("families").doc(famId).update({
      headUid: "rel_con",
      headName: "Mother Ruth",
      dependentMembers: [
        {
          id: "linked_head_con",
          name: "Father Samuel",
          relationship: "Father",
          linkedUid: "head_con",
          membershipId: "GAG-2026-0201"
        }
      ],
      linkedMemberUids: ["head_con", "rel_con"]
    })
  );
});

test("Daily verses, auto daily verse config, church update metadata, and fcmToken follow role & validation rules", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc("admin_dv").set({
      uid: "admin_dv",
      displayName: "Super Admin",
      role: "SUPER_ADMIN",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("pastor_dv").set({
      uid: "pastor_dv",
      displayName: "Pastor Thomas",
      role: "PASTOR",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
    await db.collection("users").doc("believer_dv").set({
      uid: "believer_dv",
      displayName: "Believer Sarah",
      role: "BELIEVER",
      churchId: "church_main",
      accountStatus: "APPROVED"
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_dv").firestore();
  const pastorDb = testEnv.authenticatedContext("pastor_dv").firestore();
  const believerDb = testEnv.authenticatedContext("believer_dv").firestore();

  // Pastor can configure Auto Verse Daily; Believer cannot
  await assertFails(
    believerDb.collection("dailyVerseConfig").doc("church_main").set({
      churchId: "church_main",
      autoDailyEnabled: false,
      updatedByUid: "believer_dv"
    })
  );
  await assertSucceeds(
    pastorDb.collection("dailyVerseConfig").doc("church_main").set({
      churchId: "church_main",
      autoDailyEnabled: true,
      rotationHour: 6,
      updatedByName: "Pastor Thomas",
      updatedByUid: "pastor_dv"
    })
  );
  await assertSucceeds(believerDb.collection("dailyVerseConfig").doc("church_main").get());

  // Pastor can publish & delete custom Bible verse; Believer can read but cannot write/delete
  const versePayload = {
    verseId: "verse_custom_1",
    churchId: "church_main",
    bookReference: "Psalm 23:1",
    textEnglish: "The Lord is my shepherd; I shall not want.",
    textTamil: "கர்த்தர் என் மேய்ப்பராயிருக்கிறார்.",
    pinDateKey: "2026-11-18",
    publishedByPastor: "Pastor Thomas"
  };
  await assertFails(believerDb.collection("bibleVerses").doc("verse_custom_1").set(versePayload));
  await assertSucceeds(pastorDb.collection("bibleVerses").doc("verse_custom_1").set(versePayload));
  await assertSucceeds(believerDb.collection("bibleVerses").doc("verse_custom_1").get());
  await assertFails(believerDb.collection("bibleVerses").doc("verse_custom_1").delete());
  await assertSucceeds(pastorDb.collection("bibleVerses").doc("verse_custom_1").delete());

  // Super Admin can save APK release updater metadata in ChurchConfig; invalid versionCode rejected
  await assertSucceeds(
    adminDb.collection("churches").doc("church_main").set({
      churchId: "church_main",
      name: "GRACE AG",
      defaultLanguage: "en",
      latestVersionCode: 2,
      latestVersionName: "1.1",
      updateDownloadUrl: "https://github.com/graceag/app/releases/download/v1.1/grace-ag.apk",
      updateReleaseNotes: "Added daily auto Bible verse and push notifications.",
      forceUpdate: false
    })
  );
  await assertFails(
    adminDb.collection("churches").doc("church_main").set({
      churchId: "church_main",
      name: "GRACE AG",
      defaultLanguage: "en",
      latestVersionCode: 0
    })
  );

  // User can save valid fcmToken, oversized token rejected
  await assertSucceeds(
    believerDb.collection("users").doc("believer_dv").update({
      fcmToken: "fcm_token_sample_123456"
    })
  );
  await assertFails(
    believerDb.collection("users").doc("believer_dv").update({
      fcmToken: "T".repeat(600)
    })
  );
});




