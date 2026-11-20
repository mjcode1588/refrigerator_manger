const API_BASE = "/api/v1";

const state = {
  accessToken: localStorage.getItem("rm_access_token") || "",
  refreshToken: localStorage.getItem("rm_refresh_token") || "",
  user: null,
  households: [],
  currentHouseholdId: Number(localStorage.getItem("rm_household_id")) || null,
  locationsTree: [],
  locationsFlat: [],
  selectedLocationId: null,
  items: [],
};

const $ = (id) => document.getElementById(id);

function showToast(msg) {
  const el = $("toast");
  el.textContent = msg;
  el.classList.remove("hidden");
  setTimeout(() => el.classList.add("hidden"), 2600);
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.accessToken) {
    headers.Authorization = `Bearer ${state.accessToken}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg = data?.error?.message || data?.detail || `요청 실패 (${res.status})`;
    throw new Error(msg);
  }
  return data;
}

async function checkHealth() {
  try {
    const data = await api("/health");
    $("dbStatus").textContent = `DB: ${data.db_provider} (${data.status})`;
  } catch {
    $("dbStatus").textContent = "서버 연결 실패";
  }
}

async function doDevLogin(nickname, email = null) {
  const data = await api("/auth/dev-login", {
    method: "POST",
    body: JSON.stringify({ nickname, email: email || null }),
  });
  state.accessToken = data.access_token;
  state.refreshToken = data.refresh_token;
  localStorage.setItem("rm_access_token", data.access_token);
  localStorage.setItem("rm_refresh_token", data.refresh_token);
  await bootstrapSession();
  showToast(`${data.user.nickname}님으로 로그인했습니다.`);
}

function logout() {
  state.accessToken = "";
  state.refreshToken = "";
  state.user = null;
  localStorage.removeItem("rm_access_token");
  localStorage.removeItem("rm_refresh_token");
  renderAuth();
}

function renderAuth() {
  if (!state.user) {
    $("authSection").classList.remove("hidden");
    $("appWorkspace").classList.add("hidden");
    $("userArea").innerHTML = "";
  } else {
    $("authSection").classList.add("hidden");
    $("appWorkspace").classList.remove("hidden");
    $("userArea").innerHTML = `
      <span>👤 <strong>${state.user.nickname}</strong></span>
      <button id="logoutBtn" class="btn small">로그아웃</button>
    `;
    $("logoutBtn").onclick = logout;
  }
}

async function bootstrapSession() {
  if (!state.accessToken) {
    renderAuth();
    return;
  }
  try {
    const me = await api("/users/me");
    state.user = me;
    state.households = me.households || [];
    if (
      !state.currentHouseholdId ||
      !state.households.some((h) => h.id === state.currentHouseholdId)
    ) {
      state.currentHouseholdId = state.households[0]?.id || null;
    }
    renderAuth();
    renderHouseholds();
    if (state.currentHouseholdId) {
      await loadHouseholdData();
    }
  } catch {
    logout();
  }
}

function renderHouseholds() {
  const sel = $("householdSelect");
  sel.innerHTML = "";
  if (state.households.length === 0) {
    const opt = document.createElement("option");
    opt.textContent = "소속 가구 없음 (새 우리집을 만드세요)";
    sel.appendChild(opt);
    return;
  }
  for (const h of state.households) {
    const opt = document.createElement("option");
    opt.value = h.id;
    opt.textContent = `${h.name} (${h.role})`;
    if (h.id === state.currentHouseholdId) opt.selected = true;
    sel.appendChild(opt);
  }
}

async function loadHouseholdData() {
  const hhId = state.currentHouseholdId;
  if (!hhId) return;
  localStorage.setItem("rm_household_id", String(hhId));

  const [tree, flat] = await Promise.all([
    api(`/households/${hhId}/locations?format=tree`),
    api(`/households/${hhId}/locations?format=flat`),
  ]);
  state.locationsTree = tree;
  state.locationsFlat = flat;

  renderLocationTree();
  renderLocationSelect();
  await Promise.all([loadItems(), loadActivities()]);
}

function renderTreeNodes(nodes) {
  return nodes
    .map((n) => {
      const activeClass = state.selectedLocationId === n.id ? "active" : "";
      const childrenHtml = n.children?.length
        ? `<div class="tree-children">${renderTreeNodes(n.children)}</div>`
        : "";
      return `
        <div class="tree-node">
          <div class="tree-row ${activeClass}" data-loc-id="${n.id}">
            <span>${n.icon || "📁"} ${n.name}</span>
            <span class="tree-actions">
              <button class="btn small add-child-btn" data-parent-id="${n.id}" title="하위 장소 추가">+</button>
              <button class="btn small danger del-loc-btn" data-del-id="${n.id}" title="삭제">×</button>
            </span>
          </div>
          ${childrenHtml}
        </div>
      `;
    })
    .join("");
}

function renderLocationTree() {
  const allBtn = $("selectAllLocationsBtn");
  allBtn.classList.toggle("active", state.selectedLocationId === null);

  const container = $("locationTree");
  container.innerHTML = renderTreeNodes(state.locationsTree);

  container.querySelectorAll(".tree-row").forEach((row) => {
    row.addEventListener("click", async (e) => {
      if (e.target.closest("button")) return;
      state.selectedLocationId = Number(row.dataset.locId);
      renderLocationTree();
      await loadItems();
    });
  });

  container.querySelectorAll(".add-child-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const parentId = Number(btn.dataset.parentId);
      const name = prompt("하위 보관 장소 이름을 입력하세요 (예: 문쪽 선반)");
      if (!name) return;
      await api(`/households/${state.currentHouseholdId}/locations`, {
        method: "POST",
        body: JSON.stringify({ parent_id: parentId, name, storage_type: "CUSTOM" }),
      });
      await loadHouseholdData();
      showToast("하위 보관 장소를 추가했습니다.");
    });
  });

  container.querySelectorAll(".del-loc-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const locId = Number(btn.dataset.delId);
      if (!confirm("이 보관 장소를 삭제할까요? (하위 장소/품목 포함 cascade 삭제)")) return;
      await api(`/households/${state.currentHouseholdId}/locations/${locId}?strategy=cascade`, {
        method: "DELETE",
      });
      if (state.selectedLocationId === locId) state.selectedLocationId = null;
      await loadHouseholdData();
      showToast("보관 장소를 삭제했습니다.");
    });
  });
}

function renderLocationSelect() {
  const sel = $("itemLocationSelect");
  sel.innerHTML = state.locationsFlat
    .map((l) => `<option value="${l.id}">${l.full_path}</option>`)
    .join("");
}

function formatDDayBadge(item) {
  if (item.expiry_state === "NONE") return `<span class="badge">기한 없음</span>`;
  if (item.d_day < 0) return `<span class="badge EXPIRED">D+${Math.abs(item.d_day)} 만료</span>`;
  if (item.d_day === 0) return `<span class="badge TODAY">D-Day</span>`;
  return `<span class="badge ${item.expiry_state}">D-${item.d_day}</span>`;
}

async function loadItems() {
  const hhId = state.currentHouseholdId;
  if (!hhId) return;
  const params = new URLSearchParams({
    status: $("statusFilter").value,
    include_sublocations: "true",
    sort: "expiry",
  });
  if (state.selectedLocationId !== null) {
    params.set("location_id", String(state.selectedLocationId));
  }
  const q = $("searchInput").value.trim();
  if (q) params.set("q", q);

  const [items, activeAll] = await Promise.all([
    api(`/households/${hhId}/items?${params.toString()}`),
    api(`/households/${hhId}/items?status=ACTIVE`),
  ]);
  state.items = items;

  const selectedLoc = state.locationsFlat.find((l) => l.id === state.selectedLocationId);
  $("currentLocationLabel").textContent = selectedLoc ? `(${selectedLoc.full_path})` : "(전체)";

  $("statActive").textContent = activeAll.length;
  $("statExpiring").textContent = activeAll.filter(
    (i) => i.expiry_state === "EXPIRING_SOON" || i.expiry_state === "TODAY"
  ).length;
  $("statExpired").textContent = activeAll.filter((i) => i.expiry_state === "EXPIRED").length;

  renderItems();
}

function renderItems() {
  const container = $("itemsList");
  if (state.items.length === 0) {
    container.innerHTML = `<p class="muted">표시할 품목이 없습니다.</p>`;
    return;
  }
  container.innerHTML = state.items
    .map(
      (it) => `
      <div class="item-card">
        <div>
          <div class="item-title-row">
            <strong>${it.name}</strong>
            ${formatDDayBadge(it)}
            <span class="badge">${it.category}</span>
          </div>
          <div class="muted">${it.location_path}${it.memo ? ` · ${it.memo}` : ""}</div>
        </div>
        <div class="item-actions">
          ${
            it.status === "ACTIVE"
              ? `
            <button class="btn small qty-minus-btn" data-id="${it.id}" data-qty="${it.quantity}">-</button>
            <span class="qty-box">${it.quantity} ${it.unit}</span>
            <button class="btn small qty-plus-btn" data-id="${it.id}" data-qty="${it.quantity}">+</button>
            <button class="btn small primary consume-btn" data-id="${it.id}">소비완료</button>
            <button class="btn small danger discard-btn" data-id="${it.id}">폐기</button>
          `
              : `<span class="muted">${it.status} (${it.quantity} ${it.unit})</span>`
          }
        </div>
      </div>
    `
    )
    .join("");

  container.querySelectorAll(".qty-minus-btn").forEach((btn) => {
    btn.onclick = async () => {
      const id = Number(btn.dataset.id);
      const cur = Number(btn.dataset.qty);
      if (cur <= 1) {
        await api(`/households/${state.currentHouseholdId}/items/${id}/consume`, { method: "POST" });
      } else {
        await api(`/households/${state.currentHouseholdId}/items/${id}/consume`, {
          method: "POST",
          body: JSON.stringify({ amount: 1 }),
        });
      }
      await Promise.all([loadItems(), loadActivities()]);
    };
  });

  container.querySelectorAll(".qty-plus-btn").forEach((btn) => {
    btn.onclick = async () => {
      const id = Number(btn.dataset.id);
      const cur = Number(btn.dataset.qty);
      await api(`/households/${state.currentHouseholdId}/items/${id}`, {
        method: "PATCH",
        body: JSON.stringify({ quantity: cur + 1 }),
      });
      await Promise.all([loadItems(), loadActivities()]);
    };
  });

  container.querySelectorAll(".consume-btn").forEach((btn) => {
    btn.onclick = async () => {
      await api(`/households/${state.currentHouseholdId}/items/${btn.dataset.id}/consume`, {
        method: "POST",
      });
      await Promise.all([loadItems(), loadActivities()]);
      showToast("소비 완료 처리했습니다.");
    };
  });

  container.querySelectorAll(".discard-btn").forEach((btn) => {
    btn.onclick = async () => {
      await api(`/households/${state.currentHouseholdId}/items/${btn.dataset.id}/discard`, {
        method: "POST",
      });
      await Promise.all([loadItems(), loadActivities()]);
      showToast("폐기 처리했습니다.");
    };
  });
}

async function loadActivities() {
  const hhId = state.currentHouseholdId;
  if (!hhId) return;
  const data = await api(`/households/${hhId}/activities?limit=15`);
  const list = $("activitiesList");
  if (data.items.length === 0) {
    list.innerHTML = `<li class="muted">아직 기록된 변경 이력이 없습니다.</li>`;
    return;
  }
  list.innerHTML = data.items
    .map((a) => {
      const detailName = a.detail?.name || a.detail?.nickname || "";
      const time = new Date(a.created_at).toLocaleString("ko-KR");
      return `<li><strong>${a.actor_nickname}</strong> · [${a.entity_type}/${a.action_type}] ${detailName} <span class="muted">(${time})</span></li>`;
    })
    .join("");
}

// Event Listeners
$("loginForm").onsubmit = async (e) => {
  e.preventDefault();
  try {
    await doDevLogin($("loginNickname").value, $("loginEmail").value);
  } catch (err) {
    showToast(err.message);
  }
};

document.querySelectorAll(".quick-login-btn").forEach((btn) => {
  btn.onclick = () => doDevLogin(btn.dataset.name).catch((err) => showToast(err.message));
});

$("householdSelect").onchange = async (e) => {
  state.currentHouseholdId = Number(e.target.value);
  state.selectedLocationId = null;
  await loadHouseholdData();
};

$("createHouseholdBtn").onclick = async () => {
  const name = prompt("새 가구(우리집) 이름을 입력하세요", "우리집");
  if (!name) return;
  try {
    const hh = await api("/households", {
      method: "POST",
      body: JSON.stringify({ name, create_default_locations: true }),
    });
    state.currentHouseholdId = hh.id;
    await bootstrapSession();
    showToast(`'${hh.name}' 가구를 생성했습니다.`);
  } catch (err) {
    showToast(err.message);
  }
};

$("createInviteBtn").onclick = async () => {
  if (!state.currentHouseholdId) return;
  try {
    const inv = await api(`/households/${state.currentHouseholdId}/invites`, {
      method: "POST",
      body: JSON.stringify({ expires_in_hours: 24, max_uses: 5 }),
    });
    $("inviteResult").textContent = `초대 코드: ${inv.code}`;
    $("inviteResult").classList.remove("hidden");
    showToast(`초대 코드 ${inv.code} 발급됨`);
  } catch (err) {
    showToast(err.message);
  }
};

$("joinHouseholdBtn").onclick = async () => {
  const code = prompt("가족에게 받은 초대 코드를 입력하세요");
  if (!code) return;
  try {
    const hh = await api("/households/join", {
      method: "POST",
      body: JSON.stringify({ code }),
    });
    state.currentHouseholdId = hh.id;
    await bootstrapSession();
    showToast(`'${hh.name}' 가구에 참여했습니다.`);
  } catch (err) {
    showToast(err.message);
  }
};

$("addRootLocationBtn").onclick = async () => {
  if (!state.currentHouseholdId) return;
  const name = prompt("최상위 보관 장소 이름 (예: 김치냉장고, 베란다 선반)");
  if (!name) return;
  await api(`/households/${state.currentHouseholdId}/locations`, {
    method: "POST",
    body: JSON.stringify({ parent_id: null, name, storage_type: "CUSTOM", icon: "🗄️" }),
  });
  await loadHouseholdData();
};

$("selectAllLocationsBtn").onclick = async () => {
  state.selectedLocationId = null;
  renderLocationTree();
  await loadItems();
};

$("createItemForm").onsubmit = async (e) => {
  e.preventDefault();
  try {
    await api(`/households/${state.currentHouseholdId}/items`, {
      method: "POST",
      body: JSON.stringify({
        location_id: Number($("itemLocationSelect").value),
        name: $("itemName").value,
        category: $("itemCategory").value,
        quantity: Number($("itemQty").value),
        unit: $("itemUnit").value,
        expiry_date: $("itemExpiry").value || null,
        expiry_type: $("itemExpiryType").value,
        memo: $("itemMemo").value || null,
      }),
    });
    $("itemName").value = "";
    $("itemMemo").value = "";
    await Promise.all([loadItems(), loadActivities()]);
    showToast("품목을 등록했습니다.");
  } catch (err) {
    showToast(err.message);
  }
};

$("searchInput").oninput = () => loadItems();
$("statusFilter").onchange = () => loadItems();
$("refreshActivitiesBtn").onclick = () => loadActivities();

checkHealth();
bootstrapSession();
