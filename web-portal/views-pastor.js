import {
  downloadCsv,
  esc,
  formatCurrency,
  formatTimestamp,
  openModal,
  closeModal,
  renderAvatar,
  statusBadge
} from "./utils.js";
import { resolveActiveDailyVerse, todayDateKey } from "./verses-catalog.js";

const FAMILY_ROLES = [
  "Head",
  "Father",
  "Mother",
  "Spouse",
  "Son",
  "Daughter",
  "Brother",
  "Sister",
  "Grandparent",
  "Relative"
];

export function renderOverviewView(container, state, actions) {
  const approvedMembers = state.users.filter((u) => u.accountStatus === "APPROVED").length;
  const pendingUsers = state.users.filter((u) => u.accountStatus === "PENDING");
  const openPrayers = state.prayers.filter((p) => p.status === "NEW" || p.status === "IN_REVIEW");
  const pendingSubs = state.subscriptions.filter((s) => s.status === "PENDING_VERIFICATION");
  const pendingOfferings = state.offerings.filter((o) => o.status === "PENDING");
  const totalVerifiedGiving = state.offerings
    .filter((o) => o.status === "SUCCESS")
    .reduce((sum, o) => sum + Number(o.amount || 0), 0);

  const activeVerseRes = resolveActiveDailyVerse(state.verses, state.dailyVerseConfig);
  const v = activeVerseRes.verse;
  const sourceLabel =
    activeVerseRes.source === "PASTOR_PINNED_TODAY"
      ? "Pastor Pinned Today"
      : activeVerseRes.source === "AUTO_DAILY"
      ? "Auto Daily Rotation"
      : "Pastor Featured";

  container.innerHTML = `
    <div class="grid-kpi">
      <div class="kpi-card">
        <div class="kpi-label">Approved Members</div>
        <div class="kpi-value">${approvedMembers}</div>
        <div class="kpi-sub">${state.users.length} total accounts</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Pending Approvals</div>
        <div class="kpi-value">${pendingUsers.length}</div>
        <div class="kpi-sub">${state.applications.filter((a) => a.status === "PENDING").length} applications</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Church Families</div>
        <div class="kpi-value">${state.families.length}</div>
        <div class="kpi-sub">${state.families.filter((f) => f.sharedSubscriptionStatus === "PAID").length} paid this cycle</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Open Prayer Requests</div>
        <div class="kpi-value">${openPrayers.length}</div>
        <div class="kpi-sub">${state.prayers.length} total submitted</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">Pending Payment Checks</div>
        <div class="kpi-value">${pendingSubs.length + pendingOfferings.length}</div>
        <div class="kpi-sub">${formatCurrency(totalVerifiedGiving)} verified offerings</div>
      </div>
    </div>

    <div class="grid-2">
      <section class="card card-hero">
        <div class="row-between">
          <h3 class="card-title">Today's Active Daily Verse</h3>
          <span class="badge badge-gold">${esc(sourceLabel)}</span>
        </div>
        <p class="verse-quote">“${esc(v.textEnglish)}”</p>
        <p class="verse-tamil">${esc(v.textTamil)}</p>
        <div class="row-between">
          <span class="muted">— ${esc(v.bookReference)}</span>
          <button class="btn btn-gold btn-sm" id="ov-manage-verse" type="button">Manage Daily Verse</button>
        </div>
      </section>

      <section class="card">
        <div class="row-between" style="margin-bottom: 12px;">
          <div>
            <h3 class="card-title">Action Queue</h3>
            <p class="muted" style="margin:0;">Items needing Pastor or Admin attention right now</p>
          </div>
        </div>
        <div class="stack-sm">
          <div class="switch-row">
            <div>
              <strong>${pendingUsers.length} Pending Member Approval(s)</strong>
              <div class="muted">Approve new believers to unlock full app access</div>
            </div>
            <button class="btn btn-primary btn-sm" data-goto="members" type="button">Review</button>
          </div>
          <div class="switch-row">
            <div>
              <strong>${pendingSubs.length + pendingOfferings.length} Payment Verification(s)</strong>
              <div class="muted">Confirm UPI / cash monthly dues and offerings</div>
            </div>
            <button class="btn btn-primary btn-sm" data-goto="giving" type="button">Verify</button>
          </div>
          <div class="switch-row">
            <div>
              <strong>${openPrayers.length} Unanswered Prayer Request(s)</strong>
              <div class="muted">Mark prayed and share pastoral encouragement</div>
            </div>
            <button class="btn btn-primary btn-sm" data-goto="prayers" type="button">Respond</button>
          </div>
        </div>
      </section>
    </div>
  `;

  container.querySelector("#ov-manage-verse")?.addEventListener("click", () => actions.navigate("verses"));
  container.querySelectorAll("[data-goto]").forEach((btn) => {
    btn.addEventListener("click", () => actions.navigate(btn.dataset.goto));
  });
}

export function renderMembersView(container, state, actions) {
  const query = (state.ui.memberSearch || "").trim().toLowerCase();
  const statusFilter = state.ui.memberFilter || "ALL";
  const believerByUid = new Map(state.believers.map((b) => [b.uid, b]));

  const filtered = state.users.filter((u) => {
    if (statusFilter !== "ALL" && u.accountStatus !== statusFilter) return false;
    if (!query) return true;
    const b = believerByUid.get(u.uid);
    const haystack = [u.displayName, u.email, u.phoneNumber, u.membershipId, u.role, b?.familyRole]
      .join(" ")
      .toLowerCase();
    return haystack.includes(query);
  });

  const rows = filtered
    .map((u) => {
      const believer = believerByUid.get(u.uid);
      const photo = believer?.profilePhotoUrl || u.photoUrl || "";
      const canManageRole = state.currentUserProfile?.role === "SUPER_ADMIN" && u.uid !== state.currentUserProfile.uid;
      return `
        <tr>
          <td>
            <div class="member-cell">
              ${renderAvatar(u.displayName || u.email, photo)}
              <div>
                <strong>${esc(u.displayName || "Unnamed Member")}</strong>
                <div class="muted">${esc(u.email || u.phoneNumber || u.uid)}</div>
              </div>
            </div>
          </td>
          <td><code>${esc(u.membershipId || believer?.membershipId || "—")}</code></td>
          <td>${esc(u.phoneNumber || believer?.phoneNumber || "—")}</td>
          <td>${statusBadge(u.role)}</td>
          <td>${statusBadge(u.accountStatus)}</td>
          <td>
            <div class="row-gap">
              ${
                u.accountStatus !== "APPROVED"
                  ? `<button class="btn btn-success btn-sm" data-approve="${esc(u.uid)}" type="button">Approve</button>`
                  : ""
              }
              ${
                u.accountStatus === "PENDING"
                  ? `<button class="btn btn-danger btn-sm" data-reject="${esc(u.uid)}" type="button">Reject</button>`
                  : ""
              }
              ${
                canManageRole
                  ? `<button class="btn btn-outline btn-sm" data-role-modal="${esc(u.uid)}" type="button">Role / Status</button>`
                  : ""
              }
            </div>
          </td>
        </tr>
      `;
    })
    .join("");

  container.innerHTML = `
    <div class="toolbar">
      <input id="member-search" class="search-input" type="search" placeholder="Search by name, ID, phone, or email..." value="${esc(state.ui.memberSearch || "")}" />
      <div class="row-gap">
        <div class="tabs">
          ${["ALL", "PENDING", "APPROVED", "SUSPENDED"]
            .map(
              (f) =>
                `<button class="tab-btn ${statusFilter === f ? "active" : ""}" data-filter="${f}" type="button">${f}</button>`
            )
            .join("")}
        </div>
        <button id="btn-export-members" class="btn btn-outline btn-sm" type="button">Export CSV</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Member</th>
            <th>Membership ID</th>
            <th>Phone</th>
            <th>Role</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${rows || `<tr><td colspan="6" class="empty-state">No members match your filter.</td></tr>`}
        </tbody>
      </table>
    </div>
  `;

  container.querySelector("#member-search")?.addEventListener("input", (e) => {
    const pos = e.target.selectionStart ?? e.target.value.length;
    state.ui.memberSearch = e.target.value;
    actions.rerender();
    const nextInput = document.getElementById("member-search");
    if (nextInput) {
      nextInput.focus();
      nextInput.setSelectionRange(pos, pos);
    }
  });

  container.querySelectorAll("[data-filter]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.ui.memberFilter = btn.dataset.filter;
      actions.rerender();
    });
  });

  container.querySelector("#btn-export-members")?.addEventListener("click", () => {
    downloadCsv(
      `grace-ag-members-${todayDateKey()}.csv`,
      ["Name", "Membership ID", "Phone", "Email", "Role", "Status", "Family ID"],
      filtered.map((u) => [
        u.displayName || "",
        u.membershipId || "",
        u.phoneNumber || "",
        u.email || "",
        u.role || "",
        u.accountStatus || "",
        u.familyId || ""
      ])
    );
  });

  container.querySelectorAll("[data-approve]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = state.users.find((u) => u.uid === btn.dataset.approve);
      if (target) actions.updateUserRoleStatus(target.uid, target.role || "BELIEVER", "APPROVED");
    });
  });

  container.querySelectorAll("[data-reject]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = state.users.find((u) => u.uid === btn.dataset.reject);
      if (target) actions.updateUserRoleStatus(target.uid, target.role || "BELIEVER", "REJECTED");
    });
  });

  container.querySelectorAll("[data-role-modal]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = state.users.find((u) => u.uid === btn.dataset.roleModal);
      if (!target) return;
      openModal(
        `Manage ${target.displayName || target.email}`,
        `
        <form id="modal-role-form" class="stack-sm">
          <label class="field">
            <span>Role</span>
            <select name="role">
              <option value="BELIEVER" ${target.role === "BELIEVER" ? "selected" : ""}>BELIEVER</option>
              <option value="PASTOR" ${target.role === "PASTOR" ? "selected" : ""}>PASTOR</option>
              <option value="SUPER_ADMIN" ${target.role === "SUPER_ADMIN" ? "selected" : ""}>SUPER ADMIN</option>
            </select>
          </label>
          <label class="field">
            <span>Account Status</span>
            <select name="status">
              <option value="APPROVED" ${target.accountStatus === "APPROVED" ? "selected" : ""}>APPROVED</option>
              <option value="PENDING" ${target.accountStatus === "PENDING" ? "selected" : ""}>PENDING</option>
              <option value="SUSPENDED" ${target.accountStatus === "SUSPENDED" ? "selected" : ""}>SUSPENDED</option>
              <option value="REJECTED" ${target.accountStatus === "REJECTED" ? "selected" : ""}>REJECTED</option>
            </select>
          </label>
          <div class="row-gap" style="justify-content:flex-end;margin-top:8px;">
            <button class="btn btn-outline" type="button" id="cancel-role-modal">Cancel</button>
            <button class="btn btn-primary" type="submit">Save Changes</button>
          </div>
        </form>
        `,
        (body) => {
          body.querySelector("#cancel-role-modal")?.addEventListener("click", closeModal);
          body.querySelector("#modal-role-form")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            await actions.updateUserRoleStatus(target.uid, fd.get("role"), fd.get("status"));
            closeModal();
          });
        }
      );
    });
  });
}

export function renderFamiliesView(container, state, actions) {
  const believerByUid = new Map(state.believers.map((b) => [b.uid, b]));
  const userByUid = new Map(state.users.map((u) => [u.uid, u]));

  const cards = state.families
    .map((fam) => {
      const deps = Array.isArray(fam.dependentMembers) ? fam.dependentMembers : [];
      const linked = Array.isArray(fam.linkedMemberUids) ? fam.linkedMemberUids : [];
      const memberTags = deps
        .map((d) => {
          const linkedBeliever = d.linkedUid ? believerByUid.get(d.linkedUid) : null;
          const photo = linkedBeliever?.profilePhotoUrl || (d.linkedUid ? userByUid.get(d.linkedUid)?.photoUrl : "") || "";
          return `
            <div class="switch-row" style="padding:8px 12px;">
              <div class="member-cell">
                ${renderAvatar(d.name, photo)}
                <div>
                  <strong>${esc(d.name)}</strong>
                  <div class="muted">${esc(d.relationship || "Member")}${d.linkedUid ? " • Connected App Account" : ""}</div>
                </div>
              </div>
              <span class="badge badge-blue">${esc(d.relationship || "Member")}</span>
            </div>
          `;
        })
        .join("");

      return `
        <div class="card stack-sm">
          <div class="row-between">
            <div>
              <h3 class="card-title">${esc(fam.familyName || `${fam.headName || "Household"} Family`)}</h3>
              <div class="muted">Head: <strong>${esc(fam.headName || "—")}</strong> • ${esc(fam.headPhone || "")} • ${linked.length} linked account(s)</div>
            </div>
            <div class="row-gap">
              ${statusBadge(fam.sharedSubscriptionStatus || "PENDING")}
              <button class="btn btn-primary btn-sm" data-connect-family="${esc(fam.familyId)}" type="button">+ Connect Believer</button>
            </div>
          </div>
          <div class="stack-sm">
            ${memberTags || `<div class="muted">No additional family members connected yet.</div>`}
          </div>
        </div>
      `;
    })
    .join("");

  container.innerHTML = `
    <div class="toolbar">
      <p class="muted" style="margin:0;">Manage church households, assign family roles (Father, Mother, Spouse, Son, Daughter), and connect believers.</p>
      <span class="chip">${state.families.length} Households</span>
    </div>
    <div class="stack-md">
      ${cards || `<div class="card empty-state">No families created yet.</div>`}
    </div>
  `;

  container.querySelectorAll("[data-connect-family]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const fam = state.families.find((f) => f.familyId === btn.dataset.connectFamily);
      if (!fam) return;
      const options = state.users
        .map((u) => `<option value="${esc(u.uid)}">${esc(u.displayName || u.email)} (${esc(u.membershipId || u.phoneNumber || "No ID")})</option>`)
        .join("");
      openModal(
        `Connect Believer to ${fam.familyName || fam.headName}`,
        `
        <form id="connect-member-form" class="stack-sm">
          <label class="field">
            <span>Select Registered Believer</span>
            <select name="memberUid" required>${options}</select>
          </label>
          <label class="field">
            <span>Family Role / Relationship</span>
            <select name="role" required>
              ${FAMILY_ROLES.map((r) => `<option value="${r}">${r}</option>`).join("")}
            </select>
          </label>
          <div class="row-gap" style="justify-content:flex-end;margin-top:8px;">
            <button class="btn btn-outline" type="button" id="cancel-connect">Cancel</button>
            <button class="btn btn-primary" type="submit">Connect to Family</button>
          </div>
        </form>
        `,
        (body) => {
          body.querySelector("#cancel-connect")?.addEventListener("click", closeModal);
          body.querySelector("#connect-member-form")?.addEventListener("submit", async (e) => {
            e.preventDefault();
            const fd = new FormData(e.target);
            await actions.connectBelieverToFamily(fam.familyId, fd.get("memberUid"), fd.get("role"));
            closeModal();
          });
        }
      );
    });
  });
}

export function renderDailyVerseView(container, state, actions) {
  const autoEnabled = state.dailyVerseConfig?.autoDailyEnabled !== false;
  const active = resolveActiveDailyVerse(state.verses, state.dailyVerseConfig);
  const v = active.verse;

  const verseItems = state.verses
    .map((item) => {
      const isPinned = item.pinDateKey === todayDateKey();
      return `
        <div class="card stack-sm">
          <div class="row-between">
            <div class="row-gap">
              <strong>${esc(item.bookReference)}</strong>
              ${isPinned ? `<span class="badge badge-gold">Pinned Today</span>` : ""}
            </div>
            <button class="btn btn-danger btn-sm" data-delete-verse="${esc(item.verseId)}" type="button">Delete</button>
          </div>
          <p style="margin:0;">${esc(item.textEnglish)}</p>
          ${item.textTamil && item.textTamil !== item.textEnglish ? `<p class="muted" style="margin:0;">${esc(item.textTamil)}</p>` : ""}
          ${item.devotionalEnglish ? `<div class="muted">Devotional: ${esc(item.devotionalEnglish)}</div>` : ""}
          <div class="muted">Published by ${esc(item.publishedByPastor || "Pastor")} • ${formatTimestamp(item.createdAt, item.scheduledDate)}</div>
        </div>
      `;
    })
    .join("");

  container.innerHTML = `
    <div class="grid-2" style="margin-bottom:20px;">
      <section class="card card-hero">
        <div class="row-between">
          <h3 class="card-title">Live on All Believer Phones Today</h3>
          <span class="badge badge-gold">${esc(active.source.replace(/_/g, " "))}</span>
        </div>
        <p class="verse-quote">“${esc(v.textEnglish)}”</p>
        <p class="verse-tamil">${esc(v.textTamil)}</p>
        <div class="muted">— ${esc(v.bookReference)} (Pool: ${active.poolSize} verses)</div>
      </section>

      <section class="card stack-sm">
        <h3 class="card-title">Auto Daily Bible Verse Automation</h3>
        <p class="muted" style="margin:0;">
          When enabled, GRACE AG automatically serves a different English + Tamil Bible verse to every member each day at 6:00 AM. Any verse you pin for today overrides the rotation for that day.
        </p>
        <div class="switch-row">
          <div>
            <strong>Auto Verse Daily</strong>
            <div class="muted">${autoEnabled ? "Active — rotates automatically every day" : "Paused — shows your latest published verse"}</div>
          </div>
          <label class="switch">
            <input id="toggle-auto-verse" type="checkbox" ${autoEnabled ? "checked" : ""} />
            <span class="switch-slider"></span>
          </label>
        </div>
        <div class="muted">Last updated by: ${esc(state.dailyVerseConfig?.updatedByName || "Default")}</div>
      </section>
    </div>

    <div class="grid-2">
      <section class="card">
        <h3 class="card-title" style="margin-bottom:14px;">Create &amp; Publish Daily Bible Verse</h3>
        <form id="verse-form" class="stack-sm">
          <label class="field">
            <span>Bible Reference *</span>
            <input name="bookReference" required placeholder="e.g. Psalm 23:1 / சங்கீதம் 23:1" maxlength="140" />
          </label>
          <label class="field">
            <span>Verse Text (English) *</span>
            <textarea name="textEnglish" required placeholder="Enter the scripture verse in English..."></textarea>
          </label>
          <label class="field">
            <span>Verse Text (Tamil — optional)</span>
            <textarea name="textTamil" placeholder="தமிழ் வேத வசனம் (விருப்பத்திற்குரியது)..."></textarea>
          </label>
          <label class="field">
            <span>Short Pastoral Reflection (English — optional)</span>
            <textarea name="devotionalEnglish" placeholder="Encouragement note for the church..."></textarea>
          </label>
          <label class="checkbox-row">
            <input type="checkbox" name="pinForToday" checked />
            <span>Pin as Today's Verse immediately (overrides Auto-Verse for today)</span>
          </label>
          <button class="btn btn-primary btn-lg" type="submit">Publish Daily Verse</button>
        </form>
      </section>

      <section class="stack-sm">
        <h3 class="card-title">Pastor Published Verses (${state.verses.length})</h3>
        ${verseItems || `<div class="card empty-state">No custom pastor verses published yet. Built-in 31-day catalog is active.</div>`}
      </section>
    </div>
  `;

  container.querySelector("#toggle-auto-verse")?.addEventListener("change", (e) => {
    actions.setAutoDailyVerse(e.target.checked);
  });

  container.querySelector("#verse-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const ok = await actions.publishVerse({
      bookReference: fd.get("bookReference"),
      textEnglish: fd.get("textEnglish"),
      textTamil: fd.get("textTamil"),
      devotionalEnglish: fd.get("devotionalEnglish"),
      pinForToday: fd.get("pinForToday") === "on"
    });
    if (ok) e.target.reset();
  });

  container.querySelectorAll("[data-delete-verse]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (confirm("Delete this published Bible verse?")) {
        actions.deleteVerse(btn.dataset.deleteVerse);
      }
    });
  });
}

export function renderAnnouncementsView(container, state, actions) {
  const list = state.announcements
    .map(
      (a) => `
      <div class="card stack-sm">
        <div class="row-between">
          <div class="row-gap">
            <strong>${esc(a.titleEnglish)}</strong>
            <span class="badge badge-blue">${esc((a.category || "GENERAL").replace(/_/g, " "))}</span>
          </div>
          <button class="btn btn-danger btn-sm" data-delete-ann="${esc(a.announcementId)}" type="button">Delete</button>
        </div>
        ${a.titleTamil && a.titleTamil !== a.titleEnglish ? `<div class="muted"><strong>${esc(a.titleTamil)}</strong></div>` : ""}
        <p style="margin:0;">${esc(a.bodyEnglish)}</p>
        ${a.bodyTamil && a.bodyTamil !== a.bodyEnglish ? `<p class="muted" style="margin:0;">${esc(a.bodyTamil)}</p>` : ""}
        <div class="muted">By ${esc(a.authorName || "Pastor")} • ${formatTimestamp(a.createdAt, a.publishDate)}</div>
      </div>
    `
    )
    .join("");

  container.innerHTML = `
    <div class="grid-2">
      <section class="card">
        <h3 class="card-title" style="margin-bottom:14px;">Publish Church Announcement</h3>
        <form id="ann-form" class="stack-sm">
          <label class="field">
            <span>Category</span>
            <select name="category">
              <option value="SUNDAY_SERVICE">Sunday Service</option>
              <option value="SPECIAL_MEETING">Special Meeting</option>
              <option value="FASTING_PRAYER">Fasting Prayer</option>
              <option value="YOUTH_FELLOWSHIP">Youth Fellowship</option>
              <option value="EMERGENCY">Urgent / Emergency</option>
              <option value="GENERAL">General Notice</option>
            </select>
          </label>
          <label class="field">
            <span>Title (English) *</span>
            <input name="titleEnglish" required placeholder="e.g. Friday Fasting & Prayer Meeting" />
          </label>
          <label class="field">
            <span>Title (Tamil — optional)</span>
            <input name="titleTamil" placeholder="வெள்ளி உபவாச ஜெபக் கூட்டம்" />
          </label>
          <label class="field">
            <span>Message (English) *</span>
            <textarea name="bodyEnglish" required placeholder="Details, timings, and venue..."></textarea>
          </label>
          <label class="field">
            <span>Message (Tamil — optional)</span>
            <textarea name="bodyTamil" placeholder="தமிழ் அறிவிப்பு விவரங்கள்..."></textarea>
          </label>
          <button class="btn btn-primary btn-lg" type="submit">Broadcast Announcement</button>
        </form>
      </section>

      <section class="stack-sm">
        <h3 class="card-title">Published Announcements (${state.announcements.length})</h3>
        ${list || `<div class="card empty-state">No announcements published yet.</div>`}
      </section>
    </div>
  `;

  container.querySelector("#ann-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const ok = await actions.publishAnnouncement({
      category: fd.get("category"),
      titleEnglish: fd.get("titleEnglish"),
      titleTamil: fd.get("titleTamil"),
      bodyEnglish: fd.get("bodyEnglish"),
      bodyTamil: fd.get("bodyTamil")
    });
    if (ok) e.target.reset();
  });

  container.querySelectorAll("[data-delete-ann]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (confirm("Delete this announcement?")) actions.deleteAnnouncement(btn.dataset.deleteAnn);
    });
  });
}

export function renderPrayersView(container, state, actions) {
  const cards = state.prayers
    .map(
      (p) => `
      <div class="card stack-sm">
        <div class="row-between">
          <div>
            <strong>${esc(p.title)}</strong>
            <div class="muted">From: ${esc(p.isAnonymous ? "Anonymous Believer" : p.requesterName || "Member")} • Privacy: ${esc((p.privacy || "PASTOR_ONLY").replace(/_/g, " "))} • ${formatTimestamp(p.createdAt, p.dateSubmitted)}</div>
          </div>
          ${statusBadge(p.status)}
        </div>
        <p style="margin:0;">${esc(p.description)}</p>
        <form class="row-gap prayer-reply-form" data-prayer-id="${esc(p.requestId)}">
          <select name="status" style="padding:8px 10px;border-radius:8px;border:1px solid #d5d0c6;">
            ${["NEW", "IN_REVIEW", "PRAYED_FOR", "CLOSED"]
              .map((s) => `<option value="${s}" ${p.status === s ? "selected" : ""}>${s.replace(/_/g, " ")}</option>`)
              .join("")}
          </select>
          <input name="pastoralNote" style="flex:1;min-width:220px;padding:8px 10px;border-radius:8px;border:1px solid #d5d0c6;" placeholder="Write a pastoral prayer note..." value="${esc(p.pastoralNote || "")}" />
          <button class="btn btn-primary btn-sm" type="submit">Save Response</button>
        </form>
      </div>
    `
    )
    .join("");

  container.innerHTML = `
    <div class="stack-md">
      ${cards || `<div class="card empty-state">No prayer requests received yet.</div>`}
    </div>
  `;

  container.querySelectorAll(".prayer-reply-form").forEach((form) => {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      actions.updatePrayerRequest(form.dataset.prayerId, fd.get("status"), fd.get("pastoralNote"));
    });
  });
}

export function renderGivingView(container, state, actions) {
  const tab = state.ui.givingTab || "subscriptions";
  const subRows = state.subscriptions
    .map(
      (s) => `
      <tr>
        <td>
          <strong>${esc(s.familyName || s.memberName || "Family")}</strong>
          <div class="muted">${esc(s.membershipId || "")}</div>
        </td>
        <td>${esc(s.monthYear || "—")}</td>
        <td><strong>${formatCurrency(s.amountDue)}</strong></td>
        <td>${esc(s.paymentMethod || "—")}<div class="muted">${esc(s.transactionReference || "")}</div></td>
        <td>${statusBadge(s.status)}</td>
        <td>
          <div class="row-gap">
            ${
              s.status !== "PAID"
                ? `<button class="btn btn-success btn-sm" data-verify-sub="${esc(s.subscriptionId)}" type="button">Mark Paid</button>`
                : ""
            }
            ${
              s.status === "PENDING_VERIFICATION"
                ? `<button class="btn btn-danger btn-sm" data-reject-sub="${esc(s.subscriptionId)}" type="button">Reject</button>`
                : ""
            }
          </div>
        </td>
      </tr>
    `
    )
    .join("");

  const offRows = state.offerings
    .map(
      (o) => `
      <tr>
        <td>
          <strong>${esc(o.donorName || "Believer")}</strong>
          <div class="muted">${esc(o.membershipId || "")}</div>
        </td>
        <td>${esc((o.category || "OFFERING").replace(/_/g, " "))}</td>
        <td><strong>${formatCurrency(o.amount)}</strong></td>
        <td>${esc(o.paymentMethod || "—")}<div class="muted">${esc(o.transactionReference || o.receiptNumber || "")}</div></td>
        <td>${statusBadge(o.status)}</td>
        <td>
          <div class="row-gap">
            ${
              o.status !== "SUCCESS"
                ? `<button class="btn btn-success btn-sm" data-verify-off="${esc(o.transactionId)}" data-status="SUCCESS" type="button">Verify</button>`
                : ""
            }
            ${
              o.status === "PENDING"
                ? `<button class="btn btn-danger btn-sm" data-verify-off="${esc(o.transactionId)}" data-status="FAILED" type="button">Mark Failed</button>`
                : ""
            }
          </div>
        </td>
      </tr>
    `
    )
    .join("");

  container.innerHTML = `
    <div class="toolbar">
      <div class="tabs">
        <button class="tab-btn ${tab === "subscriptions" ? "active" : ""}" data-gtab="subscriptions" type="button">Monthly Family Dues (${state.subscriptions.length})</button>
        <button class="tab-btn ${tab === "offerings" ? "active" : ""}" data-gtab="offerings" type="button">Voluntary Offerings (${state.offerings.length})</button>
      </div>
      <button id="btn-export-giving" class="btn btn-outline btn-sm" type="button">Export Ledger CSV</button>
    </div>

    <div class="table-wrap">
      ${
        tab === "subscriptions"
          ? `
        <table>
          <thead><tr><th>Family / Member</th><th>Month</th><th>Amount Due</th><th>Method / Ref</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>${subRows || `<tr><td colspan="6" class="empty-state">No monthly subscriptions recorded yet.</td></tr>`}</tbody>
        </table>`
          : `
        <table>
          <thead><tr><th>Donor</th><th>Category</th><th>Amount</th><th>Method / Ref</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>${offRows || `<tr><td colspan="6" class="empty-state">No offerings recorded yet.</td></tr>`}</tbody>
        </table>`
      }
    </div>
  `;

  container.querySelectorAll("[data-gtab]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.ui.givingTab = btn.dataset.gtab;
      actions.rerender();
    });
  });

  container.querySelector("#btn-export-giving")?.addEventListener("click", () => {
    if (tab === "subscriptions") {
      downloadCsv(
        `grace-ag-monthly-dues-${todayDateKey()}.csv`,
        ["Family", "Member ID", "Month", "Amount Due", "Amount Paid", "Status", "Receipt"],
        state.subscriptions.map((s) => [s.familyName || s.memberName, s.membershipId, s.monthYear, s.amountDue, s.amountPaid, s.status, s.receiptNumber])
      );
    } else {
      downloadCsv(
        `grace-ag-offerings-${todayDateKey()}.csv`,
        ["Donor", "Member ID", "Category", "Amount", "Method", "Status", "Receipt"],
        state.offerings.map((o) => [o.donorName, o.membershipId, o.category, o.amount, o.paymentMethod, o.status, o.receiptNumber])
      );
    }
  });

  container.querySelectorAll("[data-verify-sub]").forEach((btn) => {
    btn.addEventListener("click", () => actions.verifySubscription(btn.dataset.verifySub, true));
  });
  container.querySelectorAll("[data-reject-sub]").forEach((btn) => {
    btn.addEventListener("click", () => actions.verifySubscription(btn.dataset.rejectSub, false));
  });
  container.querySelectorAll("[data-verify-off]").forEach((btn) => {
    btn.addEventListener("click", () => actions.verifyOffering(btn.dataset.verifyOff, btn.dataset.status));
  });
}

export function renderEventsView(container, state, actions) {
  const list = state.events
    .map(
      (ev) => `
      <div class="card stack-sm">
        <div class="row-between">
          <div>
            <strong>${esc(ev.titleEnglish)}</strong>
            <div class="muted">${esc(ev.eventDate)} • ${esc(ev.startTime || "")}${ev.venue ? ` • ${esc(ev.venue)}` : ""}</div>
          </div>
          <div class="row-gap">
            <span class="badge badge-green">${Number(ev.registeredCount || 0)} / ${Number(ev.capacity || 200)} RSVPs</span>
            <button class="btn btn-danger btn-sm" data-delete-event="${esc(ev.eventId)}" type="button">Delete</button>
          </div>
        </div>
        ${ev.descriptionEnglish ? `<p style="margin:0;">${esc(ev.descriptionEnglish)}</p>` : ""}
      </div>
    `
    )
    .join("");

  container.innerHTML = `
    <div class="grid-2">
      <section class="card">
        <h3 class="card-title" style="margin-bottom:14px;">Create Church Event</h3>
        <form id="event-form" class="stack-sm">
          <label class="field">
            <span>Event Title (English) *</span>
            <input name="titleEnglish" required placeholder="e.g. Watchnight Service 2026" />
          </label>
          <label class="field">
            <span>Event Title (Tamil — optional)</span>
            <input name="titleTamil" placeholder="புத்தாண்டு ஆராதனை" />
          </label>
          <label class="field">
            <span>Category</span>
            <select name="category">
              <option value="SUNDAY_SERVICE">Sunday Service</option>
              <option value="FASTING_PRAYER">Fasting Prayer</option>
              <option value="YOUTH_EVENT">Youth Event</option>
              <option value="OUTREACH_MISSION">Outreach &amp; Mission</option>
              <option value="SPECIAL_FESTIVAL">Special Festival</option>
              <option value="OTHER_EVENT">Other Event</option>
            </select>
          </label>
          <div class="grid-2">
            <label class="field">
              <span>Event Date *</span>
              <input name="eventDate" required placeholder="e.g. Sunday, 30 Nov 2026" />
            </label>
            <label class="field">
              <span>Time</span>
              <input name="startTime" placeholder="09:00 AM" />
            </label>
          </div>
          <div class="grid-2">
            <label class="field">
              <span>Venue</span>
              <input name="venue" placeholder="Main Sanctuary" />
            </label>
            <label class="field">
              <span>Capacity</span>
              <input name="capacity" type="number" min="1" max="10000" value="300" />
            </label>
          </div>
          <label class="field">
            <span>Description</span>
            <textarea name="descriptionEnglish" placeholder="Event schedule and notes..."></textarea>
          </label>
          <button class="btn btn-primary btn-lg" type="submit">Publish Event</button>
        </form>
      </section>

      <section class="stack-sm">
        <h3 class="card-title">Upcoming &amp; Scheduled Events (${state.events.length})</h3>
        ${list || `<div class="card empty-state">No church events created yet.</div>`}
      </section>
    </div>
  `;

  container.querySelector("#event-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const ok = await actions.createEvent({
      titleEnglish: fd.get("titleEnglish"),
      titleTamil: fd.get("titleTamil"),
      category: fd.get("category"),
      eventDate: fd.get("eventDate"),
      startTime: fd.get("startTime"),
      venue: fd.get("venue"),
      capacity: Number(fd.get("capacity") || 300),
      descriptionEnglish: fd.get("descriptionEnglish")
    });
    if (ok) e.target.reset();
  });

  container.querySelectorAll("[data-delete-event]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (confirm("Delete this event?")) actions.deleteEvent(btn.dataset.deleteEvent);
    });
  });
}
