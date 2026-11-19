export function esc(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function initials(name) {
  const parts = String(name || "GA").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "GA";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function renderAvatar(name, photoUrl) {
  const cleanUrl = String(photoUrl || "").trim();
  const isSafeUrl =
    cleanUrl.startsWith("https://") ||
    cleanUrl.startsWith("http://") ||
    cleanUrl.startsWith("data:image/");
  if (isSafeUrl) {
    return `<div class="avatar-circle"><img src="${esc(cleanUrl)}" alt="${esc(name)}" /></div>`;
  }
  return `<div class="avatar-circle">${esc(initials(name))}</div>`;
}

export function formatCurrency(amount) {
  const num = Number(amount || 0);
  return `₹${num.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

export function formatTimestamp(ts, fallback = "") {
  if (!ts) return fallback || "—";
  try {
    const date = typeof ts.toDate === "function" ? ts.toDate() : new Date(ts);
    if (Number.isNaN(date.getTime())) return fallback || "—";
    return date.toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  } catch {
    return fallback || "—";
  }
}

export function statusBadge(status) {
  const s = String(status || "").toUpperCase();
  let cls = "badge-gray";
  if (["APPROVED", "ACTIVE", "PAID", "SUCCESS", "PRAYED_FOR", "VISITED"].includes(s)) cls = "badge-green";
  else if (["PENDING", "PENDING_VERIFICATION", "IN_REVIEW", "NEW"].includes(s)) cls = "badge-amber";
  else if (["REJECTED", "SUSPENDED", "FAILED", "OVERDUE"].includes(s)) cls = "badge-red";
  else if (["SUPER_ADMIN"].includes(s)) cls = "badge-gold";
  else if (["PASTOR", "BELIEVER", "WAIVED", "CLOSED"].includes(s)) cls = "badge-blue";
  return `<span class="badge ${cls}">${esc(s.replace(/_/g, " "))}</span>`;
}

export function randomId(prefix, len = 10) {
  const chars = "abcdefghijklmnopqrstuvwxyz0123456789";
  let out = "";
  const arr = new Uint8Array(len);
  crypto.getRandomValues(arr);
  for (const byte of arr) out += chars[byte % chars.length];
  return `${prefix}_${out}`;
}

export function downloadCsv(filename, headers, rows) {
  const escapeCell = (val) => {
    const str = String(val ?? "");
    return /[",\n]/.test(str) ? `"${str.replace(/"/g, '""')}"` : str;
  };
  const lines = [
    headers.map(escapeCell).join(","),
    ...rows.map((r) => r.map(escapeCell).join(","))
  ];
  const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function showToast(message, type = "info") {
  const root = document.getElementById("toast-root");
  if (!root) return;
  const el = document.createElement("div");
  el.className = `toast ${type === "error" ? "toast-error" : type === "success" ? "toast-success" : ""}`;
  el.textContent = message;
  root.appendChild(el);
  setTimeout(() => {
    el.remove();
  }, 4200);
}

export function openModal(title, htmlContent, onBind) {
  const root = document.getElementById("modal-root");
  const titleEl = document.getElementById("modal-title");
  const bodyEl = document.getElementById("modal-body");
  titleEl.textContent = title;
  bodyEl.innerHTML = htmlContent;
  root.classList.remove("hidden");
  if (typeof onBind === "function") onBind(bodyEl);
}

export function closeModal() {
  document.getElementById("modal-root")?.classList.add("hidden");
}
