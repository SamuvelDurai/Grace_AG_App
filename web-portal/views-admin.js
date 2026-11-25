import { esc, formatTimestamp, renderAvatar, resolvePhotoUrl, statusBadge } from "./utils.js";

export function renderUpdaterView(container, state, actions) {
  const cfg = state.churchConfig || {};
  const currentCode = Number(cfg.latestVersionCode || 1);
  const currentName = cfg.latestVersionName || "1.0";

  container.innerHTML = `
    <div class="grid-2">
      <section class="card">
        <h3 class="card-title">In-App Android APK Pop-Up Updater</h3>
        <p class="muted" style="margin:0 0 16px;">
          Publish a new release here and every GRACE AG Android phone on an older Version Code will automatically see an update pop-up on launch.
        </p>

        <form id="updater-form" class="stack-sm">
          <div class="grid-2">
            <label class="field">
              <span>Latest Version Code *</span>
              <input id="upd-code" name="latestVersionCode" type="number" min="1" max="1000000" required value="${esc(currentCode)}" />
            </label>
            <label class="field">
              <span>Latest Version Name *</span>
              <input id="upd-name" name="latestVersionName" required placeholder="e.g. 1.1" maxlength="40" value="${esc(currentName)}" />
            </label>
          </div>

          <label class="field">
            <span>Direct APK Download URL *</span>
            <input id="upd-url" name="updateDownloadUrl" type="url" placeholder="https://github.com/SamuvelDurai/Grace_AG_App/releases/download/v1.1/grace-ag-v1.1.apk" maxlength="500" value="${esc(cfg.updateDownloadUrl || "")}" />
          </label>

          <div class="row-gap">
            <button id="btn-autofill-github-url" class="btn btn-outline btn-sm" type="button">
              Auto-fill GitHub Release URL for Version Name
            </button>
          </div>

          <label class="field">
            <span>What's New / Release Notes</span>
            <textarea name="updateReleaseNotes" maxlength="1000" placeholder="• Daily Bible Verse & Auto-Verse&#10;• Notification Center &amp; 6 AM Reminders&#10;• Family connections &amp; synced profile photos">${esc(cfg.updateReleaseNotes || "")}</textarea>
          </label>

          <label class="checkbox-row">
            <input type="checkbox" name="forceUpdate" ${cfg.forceUpdate ? "checked" : ""} />
            <span>Force Update (require members to update before continuing)</span>
          </label>

          <button class="btn btn-primary btn-lg" type="submit">Publish App Update Pop-Up</button>
        </form>
      </section>

      <section class="card stack-sm">
        <h3 class="card-title">Currently Broadcast Release</h3>
        <div class="switch-row">
          <div>
            <strong>Version ${esc(currentName)} (Code ${esc(currentCode)})</strong>
            <div class="muted">${cfg.forceUpdate ? "Mandatory Update Enabled" : "Optional Update Prompt"}</div>
          </div>
          ${cfg.updateDownloadUrl ? `<span class="badge badge-green">Live Pop-Up Active</span>` : `<span class="badge badge-gray">No URL Set</span>`}
        </div>
        <div class="field">
          <span>Active Download Link</span>
          <input readonly value="${esc(cfg.updateDownloadUrl || "Not configured yet")}" />
        </div>
        <div class="alert alert-info" style="margin:0;">
          <strong>Workflow Tip:</strong> Whenever you push a new version from AI Studio to GitHub, grab the link from GitHub Actions → <strong>Releases</strong>, paste it here, increment the Version Code, and click <strong>Publish</strong>.
        </div>
      </section>
    </div>
  `;

  container.querySelector("#btn-autofill-github-url")?.addEventListener("click", () => {
    const version = (container.querySelector("#upd-name")?.value || "1.1").trim();
    const urlInput = container.querySelector("#upd-url");
    if (urlInput && version) {
      urlInput.value = `https://github.com/SamuvelDurai/Grace_AG_App/releases/download/v${version}/grace-ag-v${version}.apk`;
    }
  });

  container.querySelector("#updater-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    await actions.saveChurchConfig({
      ...cfg,
      latestVersionCode: Number(fd.get("latestVersionCode") || 1),
      latestVersionName: String(fd.get("latestVersionName") || "1.0").trim(),
      updateDownloadUrl: String(fd.get("updateDownloadUrl") || "").trim(),
      updateReleaseNotes: String(fd.get("updateReleaseNotes") || "").trim(),
      forceUpdate: fd.get("forceUpdate") === "on"
    });
  });
}

export function renderChurchAndPastorsView(container, state, actions) {
  const cfg = state.churchConfig || {};
  const pastors = state.users.filter((u) => u.role === "PASTOR" || u.role === "SUPER_ADMIN");

  const believerByUid = new Map((state.believers || []).map((b) => [b.uid, b]));
  const pastorRows = pastors
    .map((p) => {
      const photo = resolvePhotoUrl(believerByUid.get(p.uid)?.profilePhotoUrl, p.photoUrl);
      return `
      <tr>
        <td>
          <div class="member-cell">
            ${renderAvatar(p.displayName || p.email, photo)}
            <div>
              <strong>${esc(p.displayName || p.email)}</strong>
              <div class="muted">${esc(p.email || "")}</div>
            </div>
          </div>
        </td>
        <td>${esc(p.phoneNumber || "—")}</td>
        <td><code>${esc(p.membershipId || "—")}</code></td>
        <td>${statusBadge(p.role)}</td>
        <td>${statusBadge(p.accountStatus)}</td>
      </tr>
    `;
    })
    .join("");

  container.innerHTML = `
    <div class="grid-2" style="margin-bottom:20px;">
      <section class="card">
        <h3 class="card-title" style="margin-bottom:14px;">Church Profile &amp; Payment Settings</h3>
        <form id="church-config-form" class="stack-sm">
          <label class="field">
            <span>Church Name *</span>
            <input name="name" required maxlength="200" value="${esc(cfg.name || "GRACE AG")}" />
          </label>
          <label class="field">
            <span>Address</span>
            <input name="address" value="${esc(cfg.address || "")}" />
          </label>
          <div class="grid-2">
            <label class="field">
              <span>Phone</span>
              <input name="phone" value="${esc(cfg.phone || "")}" />
            </label>
            <label class="field">
              <span>Email</span>
              <input name="email" type="email" value="${esc(cfg.email || "")}" />
            </label>
          </div>
          <label class="field">
            <span>Service Timings</span>
            <input name="serviceTimings" placeholder="Sunday 09:00 AM • Friday 06:30 PM" value="${esc(cfg.serviceTimings || "")}" />
          </label>
          <div class="grid-2">
            <label class="field">
              <span>Monthly Family Dues (₹)</span>
              <input name="monthlyDuesAmount" type="number" min="0" max="1000000" value="${esc(cfg.monthlyDuesAmount ?? 500)}" />
            </label>
            <label class="field">
              <span>Default App Language</span>
              <select name="defaultLanguage">
                <option value="en" ${cfg.defaultLanguage !== "ta" ? "selected" : ""}>English (en)</option>
                <option value="ta" ${cfg.defaultLanguage === "ta" ? "selected" : ""}>Tamil (ta)</option>
              </select>
            </label>
          </div>
          <label class="field">
            <span>Church UPI ID (for Giving screen)</span>
            <input name="upiId" placeholder="graceag@upi" value="${esc(cfg.upiId || "")}" />
          </label>
          <label class="field">
            <span>Bank Account Details</span>
            <textarea name="bankAccountDetails" placeholder="Account Name, Number, IFSC, Branch">${esc(cfg.bankAccountDetails || "")}</textarea>
          </label>
          <button class="btn btn-primary btn-lg" type="submit">Save Church Settings</button>
        </form>
      </section>

      <section class="card stack-sm">
        <h3 class="card-title">Pre-Approve / Invite a Pastor</h3>
        <p class="muted" style="margin:0;">
          Create a verified Pastor invite below. When that pastor signs into either the Android app or this Web Portal using Google Sign-In, Email, or Phone OTP, their account is automatically promoted to <strong>PASTOR (APPROVED)</strong>.
        </p>
        <form id="pastor-invite-form" class="stack-sm">
          <label class="field">
            <span>Pastor Full Name *</span>
            <input name="displayName" required placeholder="e.g. Ps. John Wesley" />
          </label>
          <label class="field">
            <span>Pastor Email Address *</span>
            <input name="email" type="email" required placeholder="pastor@gmail.com" />
          </label>
          <label class="field">
            <span>Pastor Mobile Number (10 digits or +91...) *</span>
            <input name="phoneNumber" required placeholder="+919876543210" />
          </label>
          <button class="btn btn-gold btn-lg" type="submit">Create Pastor Invite</button>
        </form>
      </section>
    </div>

    <section class="table-wrap">
      <table>
        <thead>
          <tr><th>Pastor / Admin</th><th>Phone</th><th>ID</th><th>Role</th><th>Status</th></tr>
        </thead>
        <tbody>
          ${pastorRows || `<tr><td colspan="5" class="empty-state">No pastor accounts found.</td></tr>`}
        </tbody>
      </table>
    </section>
  `;

  container.querySelector("#church-config-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    await actions.saveChurchConfig({
      ...cfg,
      name: String(fd.get("name") || "GRACE AG").trim(),
      address: String(fd.get("address") || "").trim(),
      phone: String(fd.get("phone") || "").trim(),
      email: String(fd.get("email") || "").trim(),
      serviceTimings: String(fd.get("serviceTimings") || "").trim(),
      monthlyDuesAmount: Number(fd.get("monthlyDuesAmount") || 0),
      defaultLanguage: fd.get("defaultLanguage") === "ta" ? "ta" : "en",
      upiId: String(fd.get("upiId") || "").trim(),
      bankAccountDetails: String(fd.get("bankAccountDetails") || "").trim()
    });
  });

  container.querySelector("#pastor-invite-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const ok = await actions.createPastorInvite({
      displayName: fd.get("displayName"),
      email: fd.get("email"),
      phoneNumber: fd.get("phoneNumber")
    });
    if (ok) e.target.reset();
  });
}

export function renderAuditLogsView(container, state) {
  const rows = state.auditLogs
    .map(
      (log) => `
      <tr>
        <td>${formatTimestamp(log.createdAt, log.timestampText)}</td>
        <td><strong>${esc(log.actorName || "Admin")}</strong></td>
        <td>${esc(log.action)}</td>
        <td>${statusBadge(log.status || "SUCCESS")}</td>
      </tr>
    `
    )
    .join("");

  container.innerHTML = `
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>Timestamp</th><th>Actor</th><th>Action</th><th>Status</th></tr>
        </thead>
        <tbody>
          ${rows || `<tr><td colspan="4" class="empty-state">No audit logs recorded yet.</td></tr>`}
        </tbody>
      </table>
    </div>
  `;
}
