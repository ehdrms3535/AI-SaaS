const API_BASE = `${window.location.protocol}//${window.location.hostname}:8081`;
const state = {
  token: localStorage.getItem("saas.accessToken") || "",
  refreshToken: localStorage.getItem("saas.refreshToken") || "",
  user: JSON.parse(localStorage.getItem("saas.user") || "null"),
  customers: [],
  orgs: [],
  services: [],
  reservations: [],
  dmMessages: [],
};

const el = {
  sessionState: document.getElementById("sessionState"),
  sessionUser: document.getElementById("sessionUser"),
  sessionOrg: document.getElementById("sessionOrg"),
  apiBaseLabel: document.getElementById("apiBaseLabel"),
  logView: document.getElementById("logView"),
  orgList: document.getElementById("orgList"),
  customerList: document.getElementById("customerList"),
  serviceList: document.getElementById("serviceList"),
  reservationList: document.getElementById("reservationList"),
  reservationCustomer: document.getElementById("reservationCustomer"),
  reservationService: document.getElementById("reservationService"),
  includeCanceled: document.getElementById("includeCanceled"),
  reservationStartFilter: document.getElementById("reservationStartFilter"),
  reservationEndFilter: document.getElementById("reservationEndFilter"),
  reservationDialog: document.getElementById("reservationDialog"),
  reservationEditForm: document.getElementById("reservationEditForm"),
  editReservationId: document.getElementById("editReservationId"),
  editReservationCustomer: document.getElementById("editReservationCustomer"),
  editReservationService: document.getElementById("editReservationService"),
  editReservationStartAt: document.getElementById("editReservationStartAt"),
  editReservationEndAt: document.getElementById("editReservationEndAt"),
  editReservationNotes: document.getElementById("editReservationNotes"),
  closeReservationDialogBtn: document.getElementById("closeReservationDialogBtn"),
  cancelReservationBtn: document.getElementById("cancelReservationBtn"),
  restoreReservationBtn: document.getElementById("restoreReservationBtn"),
  customerDialog: document.getElementById("customerDialog"),
  customerEditForm: document.getElementById("customerEditForm"),
  editCustomerId: document.getElementById("editCustomerId"),
  editCustomerName: document.getElementById("editCustomerName"),
  editCustomerPhone: document.getElementById("editCustomerPhone"),
  editCustomerEmail: document.getElementById("editCustomerEmail"),
  editCustomerMemo: document.getElementById("editCustomerMemo"),
  closeCustomerDialogBtn: document.getElementById("closeCustomerDialogBtn"),
  serviceDialog: document.getElementById("serviceDialog"),
  serviceEditForm: document.getElementById("serviceEditForm"),
  editServiceId: document.getElementById("editServiceId"),
  editServiceName: document.getElementById("editServiceName"),
  editServiceDurationMinutes: document.getElementById("editServiceDurationMinutes"),
  editServicePrice: document.getElementById("editServicePrice"),
  editServiceActive: document.getElementById("editServiceActive"),
  closeServiceDialogBtn: document.getElementById("closeServiceDialogBtn"),
  refreshOrgsBtn: document.getElementById("refreshOrgsBtn"),
  orgForm: document.getElementById("orgForm"),
  dmForm: document.getElementById("dmForm"),
  dmResult: document.getElementById("dmResult"),
  dmMessageList: document.getElementById("dmMessageList"),
  refreshDmMessagesBtn: document.getElementById("refreshDmMessagesBtn"),
};

function log(message, data) {
  const timestamp = new Date().toLocaleTimeString("ko-KR", { hour12: false });
  const payload = data ? `\n${JSON.stringify(data, null, 2)}` : "";
  el.logView.textContent = `[${timestamp}] ${message}${payload}\n\n${el.logView.textContent}`;
}

function renderSession() {
  el.apiBaseLabel.textContent = API_BASE;
  if (!state.token || !state.user) {
    el.sessionState.textContent = "로그아웃";
    el.sessionUser.textContent = "-";
    el.sessionOrg.textContent = "-";
    return;
  }
  el.sessionState.textContent = "로그인";
  el.sessionUser.textContent = `${state.user.name} / ${state.user.email}`;
  el.sessionOrg.textContent = parseJwt(state.token).org || "-";
}

function parseJwt(token) {
  try {
    const [, payload] = token.split(".");
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return {};
  }
}

async function api(path, options = {}) {
  return apiInternal(path, options, true);
}

async function apiInternal(path, options = {}, allowRefresh = true) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (response.status === 401 && allowRefresh && state.refreshToken && !path.startsWith("/api/auth/")) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiInternal(path, options, false);
    }
  }

  if (!response.ok) {
    throw data || { error: "HTTP_ERROR", message: response.statusText };
  }
  return data;
}

function wireTabs() {
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".tab, .tab-content").forEach((node) => node.classList.remove("active"));
      tab.classList.add("active");
      document.querySelector(`.tab-content[data-content="${tab.dataset.tab}"]`).classList.add("active");
    });
  });
}

function formatLimit(count, limit) {
  const safeCount = typeof count === "number" ? count : 0;
  if (limit === null || limit === undefined || limit > 9000000000000000) {
    return `${safeCount} / 무제한`;
  }
  return `${safeCount} / ${limit}`;
}

async function refreshOrgs() {
  const orgs = await api("/api/orgs");
  state.orgs = orgs;
  const currentOrgId = parseJwt(state.token).org;
  el.orgList.innerHTML = orgs.map((org) => `
    <article class="list-item">
      <strong>${org.name}</strong>
      <p>${org.slug}</p>
      <p>플랜: ${org.plan || "FREE"}</p>
      <p>${org.timezone}</p>
      <p>영업시간: ${org.schedule?.businessOpenTime || "09:00"} ~ ${org.schedule?.businessCloseTime || "21:00"}</p>
      <p>휴무: ${(org.schedule?.closedWeekdays || []).join(", ") || "-"}</p>
      <div class="plan-metrics">
        <p>고객: ${formatLimit(org.usage?.customerCount, org.usage?.customerLimit)}</p>
        <p>서비스: ${formatLimit(org.usage?.serviceCount, org.usage?.serviceLimit)}</p>
        <p>활성 예약: ${formatLimit(org.usage?.activeReservationCount, org.usage?.activeReservationLimit)}</p>
      </div>
      <div class="schedule-editor">
        <label class="inline-field">
          <span>영업 시작</span>
          <input type="time" value="${org.schedule?.businessOpenTime || "09:00"}" data-open-time="${org.id}">
        </label>
        <label class="inline-field">
          <span>영업 종료</span>
          <input type="time" value="${org.schedule?.businessCloseTime || "21:00"}" data-close-time="${org.id}">
        </label>
        <label class="inline-field">
          <span>휴무일</span>
          <input type="text" value="${(org.schedule?.closedWeekdays || []).join(",")}" data-closed-days="${org.id}" placeholder="예: SUNDAY,MONDAY">
        </label>
        <p class="field-help">휴무일은 영문 대문자 요일을 콤마로 구분해서 입력합니다.</p>
      </div>
      <div class="list-item-actions">
        <select data-plan-select="${org.id}">
          <option value="FREE" ${(org.plan || "FREE") === "FREE" ? "selected" : ""}>FREE</option>
          <option value="PRO" ${org.plan === "PRO" ? "selected" : ""}>PRO</option>
          <option value="ENTERPRISE" ${org.plan === "ENTERPRISE" ? "selected" : ""}>ENTERPRISE</option>
        </select>
        <button type="button" class="ghost-btn" data-action="update-plan" data-id="${org.id}">플랜 변경</button>
        <button type="button" class="ghost-btn" data-action="update-schedule" data-id="${org.id}">영업시간 저장</button>
        <button type="button" data-action="switch-org" data-id="${org.id}" ${org.id === currentOrgId ? "disabled" : ""}>
          ${org.id === currentOrgId ? "현재 조직" : "전환"}
        </button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>조직이 없습니다.</p></article>`;
}

async function refreshCustomers() {
  const customers = await api("/api/customers");
  state.customers = customers;
  el.customerList.innerHTML = customers.map((customer) => `
    <article class="list-item">
      <strong>${customer.name}</strong>
      <p>${customer.phone || "-"}</p>
      <p>${customer.email || "-"}</p>
      <p>${customer.memo || "-"}</p>
      <p><code>${customer.id}</code></p>
      <div class="list-item-actions">
        <button type="button" data-action="edit-customer" data-id="${customer.id}">수정</button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>고객이 없습니다.</p></article>`;

  const customerOptions = customers.map((customer) =>
    `<option value="${customer.id}">${customer.name} (${customer.phone || "전화없음"})</option>`
  ).join("");
  el.reservationCustomer.innerHTML = customerOptions;
  el.editReservationCustomer.innerHTML = customerOptions;
}

async function refreshServices() {
  const services = await api("/api/services");
  state.services = services;
  el.serviceList.innerHTML = services.map((service) => `
    <article class="list-item">
      <strong>${service.name}</strong>
      <p>${service.durationMinutes}분 / ${service.price.toLocaleString()}원</p>
      <p>${service.active ? "활성" : "비활성"}</p>
      <p><code>${service.id}</code></p>
      <div class="list-item-actions">
        <button type="button" data-action="edit-service" data-id="${service.id}">수정</button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>서비스가 없습니다.</p></article>`;

  const serviceOptions = services.map((service) =>
    `<option value="${service.id}">${service.name} / ${service.durationMinutes}분</option>`
  ).join("");
  el.reservationService.innerHTML = `<option value="">선택 안 함</option>${serviceOptions}`;
  el.editReservationService.innerHTML = `<option value="">선택 안 함</option>${serviceOptions}`;
}

async function refreshReservations() {
  const params = new URLSearchParams();
  if (el.includeCanceled.checked) {
    params.set("includeCanceled", "true");
  }
  if (el.reservationStartFilter.value || el.reservationEndFilter.value) {
    if (!el.reservationStartFilter.value || !el.reservationEndFilter.value) {
      throw { error: "INVALID_TIME_RANGE", message: "시작/종료 필터를 함께 입력해야 합니다." };
    }
    params.set("startAt", toIsoLocal(el.reservationStartFilter.value));
    params.set("endAt", toIsoLocal(el.reservationEndFilter.value));
  }
  const query = params.toString() ? `?${params.toString()}` : "";
  const reservations = await api(`/api/reservations${query}`);
  state.reservations = reservations;
  el.reservationList.innerHTML = reservations.map((reservation) => `
    <article class="list-item">
      <strong>${reservation.startAt} ~ ${reservation.endAt}</strong>
      <p>상태: ${reservation.status}</p>
      <p>고객: ${findCustomerName(reservation.customerId)}</p>
      <p>서비스: ${findServiceName(reservation.serviceId)}</p>
      <p>메모: ${reservation.notes || "-"}</p>
      <p>취소시각: ${reservation.canceledAt || "-"}</p>
      <p><code>${reservation.id}</code></p>
      <div class="list-item-actions">
        <button type="button" data-action="edit-reservation" data-id="${reservation.id}">상세/수정</button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>예약이 없습니다.</p></article>`;
}

async function refreshDmMessages() {
  const messages = await api("/api/dm/messages");
  state.dmMessages = messages;
  el.dmMessageList.innerHTML = messages.map((message) => `
    <article class="list-item">
      <strong>${message.status}</strong>
      <p>채널: ${message.channel}</p>
      <p>발신자: ${message.senderName || "-"} / ${message.senderPhone || "-"}</p>
      <p>고객ID: ${message.customerId || "-"}</p>
      <p>예약ID: ${message.reservationId || "-"}</p>
      <p>메시지: ${message.messageText}</p>
      <p>처리결과: ${message.failureReason || "예약 완료"}</p>
      <p>수신시각: ${message.receivedAt || "-"}</p>
    </article>
  `).join("") || `<article class="list-item"><p>DM 메시지가 없습니다.</p></article>`;
}

async function bootstrapData() {
  if (!state.token) {
    return;
  }
  await Promise.all([refreshOrgs(), refreshCustomers(), refreshServices(), refreshReservations(), refreshDmMessages()]);
}

function saveSession(authResponse) {
  state.token = authResponse.accessToken;
  if (authResponse.refreshToken) {
    state.refreshToken = authResponse.refreshToken;
    localStorage.setItem("saas.refreshToken", state.refreshToken);
  }
  state.user = authResponse.user;
  localStorage.setItem("saas.accessToken", state.token);
  localStorage.setItem("saas.user", JSON.stringify(state.user));
  renderSession();
}

function clearSession() {
  state.token = "";
  state.refreshToken = "";
  state.user = null;
  localStorage.removeItem("saas.accessToken");
  localStorage.removeItem("saas.refreshToken");
  localStorage.removeItem("saas.user");
  renderSession();
  el.orgList.innerHTML = "";
}

async function refreshAccessToken() {
  if (!state.refreshToken) {
    clearSession();
    return false;
  }

  try {
    const result = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken: state.refreshToken }),
    });

    const text = await result.text();
    const data = text ? JSON.parse(text) : null;

    if (!result.ok) {
      clearSession();
      log("세션 갱신 실패", data);
      return false;
    }

    saveSession(data);
    log("세션 갱신 성공");
    return true;
  } catch (error) {
    clearSession();
    log("세션 갱신 실패", error);
    return false;
  }
}

function formToJson(form) {
  const data = new FormData(form);
  const json = {};
  for (const [key, value] of data.entries()) {
    json[key] = value;
  }
  return json;
}

function toLocalInputValue(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60000);
  return local.toISOString().slice(0, 16);
}

function toIsoLocal(value) {
  if (!value) {
    return value;
  }
  return new Date(value).toISOString();
}

function findCustomerName(customerId) {
  const customer = state.customers.find((item) => item.id === customerId);
  return customer ? `${customer.name} (${customer.phone || "전화없음"})` : customerId;
}

function findServiceName(serviceId) {
  if (!serviceId) {
    return "-";
  }
  const service = state.services.find((item) => item.id === serviceId);
  return service ? `${service.name} / ${service.durationMinutes}분` : serviceId;
}

function openReservationDialog(reservationId) {
  const reservation = state.reservations.find((item) => item.id === reservationId);
  if (!reservation) {
    log("예약 상세 열기 실패", { reservationId });
    return;
  }
  el.editReservationId.value = reservation.id;
  el.editReservationCustomer.value = reservation.customerId;
  el.editReservationService.value = reservation.serviceId || "";
  el.editReservationStartAt.value = toLocalInputValue(reservation.startAt);
  el.editReservationEndAt.value = toLocalInputValue(reservation.endAt);
  el.editReservationNotes.value = reservation.notes || "";
  el.cancelReservationBtn.disabled = Boolean(reservation.canceledAt);
  el.restoreReservationBtn.hidden = !reservation.canceledAt;
  el.restoreReservationBtn.disabled = !reservation.canceledAt;
  el.reservationDialog.showModal();
}

function openCustomerDialog(customerId) {
  const customer = state.customers.find((item) => item.id === customerId);
  if (!customer) {
    log("고객 상세 열기 실패", { customerId });
    return;
  }
  el.editCustomerId.value = customer.id;
  el.editCustomerName.value = customer.name || "";
  el.editCustomerPhone.value = customer.phone || "";
  el.editCustomerEmail.value = customer.email || "";
  el.editCustomerMemo.value = customer.memo || "";
  el.customerDialog.showModal();
}

function openServiceDialog(serviceId) {
  const service = state.services.find((item) => item.id === serviceId);
  if (!service) {
    log("서비스 상세 열기 실패", { serviceId });
    return;
  }
  el.editServiceId.value = service.id;
  el.editServiceName.value = service.name || "";
  el.editServiceDurationMinutes.value = service.durationMinutes ?? "";
  el.editServicePrice.value = service.price ?? "";
  el.editServiceActive.checked = Boolean(service.active);
  el.serviceDialog.showModal();
}

async function submitReservationEdit(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const reservationId = payload.id;
    delete payload.id;
    payload.startAt = toIsoLocal(payload.startAt);
    payload.endAt = toIsoLocal(payload.endAt);
    payload.serviceId = payload.serviceId || null;
    const result = await api(`/api/reservations/${reservationId}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
    el.reservationDialog.close();
    await refreshReservations();
    log("예약 수정 성공", result);
  } catch (error) {
    log("예약 수정 실패", error);
  }
}

async function cancelReservation() {
  const reservationId = el.editReservationId.value;
  if (!reservationId) {
    return;
  }
  try {
    const result = await api(`/api/reservations/${reservationId}`, { method: "DELETE" });
    el.reservationDialog.close();
    await refreshReservations();
    log("예약 취소 성공", result);
  } catch (error) {
    log("예약 취소 실패", error);
  }
}

async function restoreReservation() {
  const reservationId = el.editReservationId.value;
  if (!reservationId) {
    return;
  }
  try {
    const result = await api(`/api/reservations/${reservationId}/restore`, { method: "POST" });
    el.reservationDialog.close();
    await refreshReservations();
    log("예약 복구 성공", result);
  } catch (error) {
    log("예약 복구 실패", error);
  }
}

async function submitCustomerEdit(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const customerId = payload.id;
    delete payload.id;
    const result = await api(`/api/customers/${customerId}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
    el.customerDialog.close();
    await refreshCustomers();
    log("고객 수정 성공", result);
  } catch (error) {
    log("고객 수정 실패", error);
  }
}

async function submitServiceEdit(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const serviceId = payload.id;
    delete payload.id;
    payload.durationMinutes = Number(payload.durationMinutes);
    payload.price = Number(payload.price);
    payload.active = el.editServiceActive.checked;
    const result = await api(`/api/services/${serviceId}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
    el.serviceDialog.close();
    await refreshServices();
    log("서비스 수정 성공", result);
  } catch (error) {
    log("서비스 수정 실패", error);
  }
}

async function createOrganization(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/orgs", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    event.target.reset();
    await refreshOrgs();
    log("조직 생성 성공", result);
  } catch (error) {
    log("조직 생성 실패", error);
  }
}

async function switchOrganization(orgId) {
  try {
    const result = await api("/api/auth/switch-org", {
      method: "POST",
      body: JSON.stringify({ orgId }),
    });
    saveSession({
      accessToken: result.accessToken,
      user: result.user || state.user,
    });
    await bootstrapData();
    log("조직 전환 성공", { orgId });
  } catch (error) {
    log("조직 전환 실패", error);
  }
}

async function updateOrganizationPlan(orgId) {
  const select = el.orgList.querySelector(`[data-plan-select="${orgId}"]`);
  if (!select) {
    return;
  }
  try {
    const result = await api(`/api/orgs/${orgId}/plan`, {
      method: "PATCH",
      body: JSON.stringify({ plan: select.value }),
    });
    await refreshOrgs();
    log("플랜 변경 성공", result);
  } catch (error) {
    log("플랜 변경 실패", error);
  }
}

async function updateOrganizationSchedule(orgId) {
  const openInput = el.orgList.querySelector(`[data-open-time="${orgId}"]`);
  const closeInput = el.orgList.querySelector(`[data-close-time="${orgId}"]`);
  const closedInput = el.orgList.querySelector(`[data-closed-days="${orgId}"]`);
  if (!openInput || !closeInput || !closedInput) {
    return;
  }
  try {
    const closedWeekdays = closedInput.value
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean);
    const result = await api(`/api/orgs/${orgId}/schedule`, {
      method: "PATCH",
      body: JSON.stringify({
        businessOpenTime: openInput.value,
        businessCloseTime: closeInput.value,
        closedWeekdays,
      }),
    });
    await refreshOrgs();
    log("영업시간 저장 성공", result);
  } catch (error) {
    log("영업시간 저장 실패", error);
  }
}

async function submitDmReservation(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    payload.durationMinutes = payload.durationMinutes ? Number(payload.durationMinutes) : null;
    const result = await api("/api/dm/auto-reserve", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    el.dmResult.textContent = JSON.stringify(result, null, 2);
    await Promise.all([refreshReservations(), refreshDmMessages()]);
    log("DM 자동예약 처리 완료", result);
  } catch (error) {
    el.dmResult.textContent = JSON.stringify(error, null, 2);
    log("DM 자동예약 처리 실패", error);
  }
}

document.getElementById("registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    saveSession(result);
    await bootstrapData();
    log("회원가입 성공", result.user);
  } catch (error) {
    log("회원가입 실패", error);
  }
});

document.getElementById("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    saveSession(result);
    await bootstrapData();
    log("로그인 성공", result.user);
  } catch (error) {
    log("로그인 실패", error);
  }
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    if (state.refreshToken) {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken: state.refreshToken }),
      });
    }
  } finally {
    clearSession();
    el.customerList.innerHTML = "";
    el.serviceList.innerHTML = "";
    el.reservationList.innerHTML = "";
    el.reservationCustomer.innerHTML = "";
    el.reservationService.innerHTML = `<option value="">선택 안 함</option>`;
    log("로그아웃");
  }
});

document.getElementById("customerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/customers", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    event.target.reset();
    await refreshCustomers();
    log("고객 생성 성공", result);
  } catch (error) {
    log("고객 생성 실패", error);
  }
});

document.getElementById("serviceForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    payload.durationMinutes = Number(payload.durationMinutes);
    payload.price = Number(payload.price);
    payload.active = event.target.active.checked;
    const result = await api("/api/services", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    event.target.reset();
    event.target.active.checked = true;
    event.target.durationMinutes.value = 60;
    event.target.price.value = 30000;
    await refreshServices();
    log("서비스 생성 성공", result);
  } catch (error) {
    log("서비스 생성 실패", error);
  }
});

document.getElementById("reservationForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    payload.startAt = toIsoLocal(payload.startAt);
    payload.endAt = toIsoLocal(payload.endAt);
    payload.serviceId = payload.serviceId || null;
    const result = await api("/api/reservations", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    event.target.reset();
    await refreshReservations();
    log("예약 생성 성공", result);
  } catch (error) {
    log("예약 생성 실패", error);
  }
});

document.getElementById("refreshCustomersBtn").addEventListener("click", () => refreshCustomers().catch((error) => log("고객 새로고침 실패", error)));
document.getElementById("refreshServicesBtn").addEventListener("click", () => refreshServices().catch((error) => log("서비스 새로고침 실패", error)));
document.getElementById("refreshReservationsBtn").addEventListener("click", () => refreshReservations().catch((error) => log("예약 새로고침 실패", error)));
el.refreshDmMessagesBtn.addEventListener("click", () => refreshDmMessages().catch((error) => log("DM 새로고침 실패", error)));
el.refreshOrgsBtn.addEventListener("click", () => refreshOrgs().catch((error) => log("조직 새로고침 실패", error)));
el.orgForm.addEventListener("submit", createOrganization);
el.dmForm.addEventListener("submit", submitDmReservation);
el.orgList.addEventListener("click", (event) => {
  const updateButton = event.target.closest("[data-action='update-plan']");
  if (updateButton) {
    updateOrganizationPlan(updateButton.dataset.id);
    return;
  }
  const scheduleButton = event.target.closest("[data-action='update-schedule']");
  if (scheduleButton) {
    updateOrganizationSchedule(scheduleButton.dataset.id);
    return;
  }
  const switchButton = event.target.closest("[data-action='switch-org']");
  if (switchButton) {
    switchOrganization(switchButton.dataset.id);
  }
});
el.includeCanceled.addEventListener("change", () => refreshReservations().catch((error) => log("예약 새로고침 실패", error)));
el.reservationStartFilter.addEventListener("change", () => refreshReservations().catch((error) => log("예약 새로고침 실패", error)));
el.reservationEndFilter.addEventListener("change", () => refreshReservations().catch((error) => log("예약 새로고침 실패", error)));
el.closeReservationDialogBtn.addEventListener("click", () => el.reservationDialog.close());
el.cancelReservationBtn.addEventListener("click", () => cancelReservation());
el.restoreReservationBtn.addEventListener("click", () => restoreReservation());
el.reservationEditForm.addEventListener("submit", submitReservationEdit);
el.closeCustomerDialogBtn.addEventListener("click", () => el.customerDialog.close());
el.customerEditForm.addEventListener("submit", submitCustomerEdit);
el.closeServiceDialogBtn.addEventListener("click", () => el.serviceDialog.close());
el.serviceEditForm.addEventListener("submit", submitServiceEdit);
el.customerList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='edit-customer']");
  if (!button) {
    return;
  }
  openCustomerDialog(button.dataset.id);
});
el.serviceList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='edit-service']");
  if (!button) {
    return;
  }
  openServiceDialog(button.dataset.id);
});
el.reservationList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='edit-reservation']");
  if (!button) {
    return;
  }
  openReservationDialog(button.dataset.id);
});

renderSession();
wireTabs();
bootstrapData().catch((error) => log("초기 데이터 로드 실패", error));
