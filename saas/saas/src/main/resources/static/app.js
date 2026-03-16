const API_BASE = window.location.origin;
const REMEMBERED_EMAIL_KEY = "saas.rememberedEmail";
const state = {
  token: localStorage.getItem("saas.accessToken") || "",
  refreshToken: localStorage.getItem("saas.refreshToken") || "",
  user: JSON.parse(localStorage.getItem("saas.user") || "null"),
  customers: [],
  orgs: [],
  channels: [],
  services: [],
  reservations: [],
  dmMessages: [],
  customerPage: 1,
  servicePage: 1,
  reservationPage: 1,
  reservationCalendarMonth: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
  selectedReservationDateKey: "",
  dmPage: 1,
  listPageSize: 5,
  dmPageSize: 5,
  instagramConnectPoller: null,
};

const el = {
  authShell: document.getElementById("authShell"),
  appShell: document.getElementById("appShell"),
  registerForm: document.getElementById("registerForm"),
  loginForm: document.getElementById("loginForm"),
  registerFeedback: document.getElementById("registerFeedback"),
  loginFeedback: document.getElementById("loginFeedback"),
  rememberEmail: document.getElementById("rememberEmail"),
  openPasswordResetBtn: document.getElementById("openPasswordResetBtn"),
  passwordResetDialog: document.getElementById("passwordResetDialog"),
  passwordResetForm: document.getElementById("passwordResetForm"),
  passwordResetEmail: document.getElementById("passwordResetEmail"),
  passwordResetFeedback: document.getElementById("passwordResetFeedback"),
  passwordResetSubmitBtn: document.getElementById("passwordResetSubmitBtn"),
  closePasswordResetDialogBtn: document.getElementById("closePasswordResetDialogBtn"),
  sessionState: document.getElementById("sessionState"),
  sessionUser: document.getElementById("sessionUser"),
  sessionOrg: document.getElementById("sessionOrg"),
  heroBadge: document.getElementById("heroBadge"),
  logView: document.getElementById("logView"),
  orgList: document.getElementById("orgList"),
  channelList: document.getElementById("channelList"),
  dashboardStats: document.getElementById("dashboardStats"),
  dashboardRecentReservations: document.getElementById("dashboardRecentReservations"),
  dashboardTodayReservations: document.getElementById("dashboardTodayReservations"),
  dashboardDmSummary: document.getElementById("dashboardDmSummary"),
  dashboardChannelStatus: document.getElementById("dashboardChannelStatus"),
  customerList: document.getElementById("customerList"),
  customerPagination: document.getElementById("customerPagination"),
  customerCountLabel: document.getElementById("customerCountLabel"),
  serviceList: document.getElementById("serviceList"),
  servicePagination: document.getElementById("servicePagination"),
  serviceCountLabel: document.getElementById("serviceCountLabel"),
  reservationList: document.getElementById("reservationList"),
  reservationPagination: document.getElementById("reservationPagination"),
  reservationCountLabel: document.getElementById("reservationCountLabel"),
  reservationCalendarPrevBtn: document.getElementById("reservationCalendarPrevBtn"),
  reservationCalendarNextBtn: document.getElementById("reservationCalendarNextBtn"),
  reservationCalendarLabel: document.getElementById("reservationCalendarLabel"),
  reservationCalendarGrid: document.getElementById("reservationCalendarGrid"),
  reservationCalendarDetailTitle: document.getElementById("reservationCalendarDetailTitle"),
  reservationCalendarDayList: document.getElementById("reservationCalendarDayList"),
  reservationFormDateHint: document.getElementById("reservationFormDateHint"),
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
  refreshChannelsBtn: document.getElementById("refreshChannelsBtn"),
  startInstagramConnectBtn: document.getElementById("startInstagramConnectBtn"),
  orgForm: document.getElementById("orgForm"),
  dmForm: document.getElementById("dmForm"),
  dmResult: document.getElementById("dmResult"),
  dmSummaryCards: document.getElementById("dmSummaryCards"),
  dmMessageList: document.getElementById("dmMessageList"),
  dmPagination: document.getElementById("dmPagination"),
  dmCountLabel: document.getElementById("dmCountLabel"),
  refreshDmMessagesBtn: document.getElementById("refreshDmMessagesBtn"),
  dmStatusFilter: document.getElementById("dmStatusFilter"),
  dmMessageDialog: document.getElementById("dmMessageDialog"),
  dmMessageConfirmForm: document.getElementById("dmMessageConfirmForm"),
  dmMessageId: document.getElementById("dmMessageId"),
  dmMessageText: document.getElementById("dmMessageText"),
  dmMessageIntent: document.getElementById("dmMessageIntent"),
  dmMessageReservation: document.getElementById("dmMessageReservation"),
  dmMessageCustomer: document.getElementById("dmMessageCustomer"),
  dmMessageService: document.getElementById("dmMessageService"),
  dmMessageStartAt: document.getElementById("dmMessageStartAt"),
  dmMessageEndAt: document.getElementById("dmMessageEndAt"),
  dmSuggestedTimes: document.getElementById("dmSuggestedTimes"),
  dmMessageNotes: document.getElementById("dmMessageNotes"),
  dmConfirmBookBtn: document.getElementById("dmConfirmBookBtn"),
  dmConfirmUpdateBtn: document.getElementById("dmConfirmUpdateBtn"),
  dmConfirmCancelBtn: document.getElementById("dmConfirmCancelBtn"),
  closeDmMessageDialogBtn: document.getElementById("closeDmMessageDialogBtn"),
};

function log(message, data) {
  const timestamp = new Date().toLocaleTimeString("ko-KR", { hour12: false });
  const payload = data ? `\n${JSON.stringify(data, null, 2)}` : "";
  el.logView.textContent = `[${timestamp}] ${message}${payload}\n\n${el.logView.textContent}`;
}

function setFormFeedback(target, message = "", tone = "") {
  if (!target) {
    return;
  }
  target.textContent = message;
  target.classList.remove("success", "error");
  if (tone) {
    target.classList.add(tone);
  }
}

function setFormPending(form, pending, label) {
  const submitButton = form?.querySelector("button[type='submit']");
  if (!submitButton) {
    return;
  }
  if (!submitButton.dataset.defaultLabel) {
    submitButton.dataset.defaultLabel = submitButton.textContent;
  }
  submitButton.disabled = pending;
  submitButton.textContent = pending ? label : submitButton.dataset.defaultLabel;
}

function hydrateRememberedEmail() {
  const rememberedEmail = localStorage.getItem(REMEMBERED_EMAIL_KEY) || "";
  const loginEmailInput = document.querySelector("#loginForm input[name='email']");
  if (!loginEmailInput) {
    return;
  }
  if (rememberedEmail) {
    loginEmailInput.value = rememberedEmail;
    el.rememberEmail.checked = true;
  }
  loginEmailInput.focus();
}

function syncRememberedEmail(email) {
  if (el.rememberEmail.checked && email) {
    localStorage.setItem(REMEMBERED_EMAIL_KEY, email);
  } else {
    localStorage.removeItem(REMEMBERED_EMAIL_KEY);
  }
}

function openPasswordResetDialog() {
  const loginEmailInput = el.loginForm?.querySelector("input[name='email']");
  const initialEmail = loginEmailInput?.value || localStorage.getItem(REMEMBERED_EMAIL_KEY) || "";
  el.passwordResetEmail.value = initialEmail;
  el.passwordResetSubmitBtn.textContent = "재설정 링크 보내기";
  el.passwordResetSubmitBtn.dataset.defaultLabel = "재설정 링크 보내기";
  setFormFeedback(el.passwordResetFeedback);
  el.passwordResetDialog.showModal();
  el.passwordResetEmail.focus();
}

function closePasswordResetDialog() {
  el.passwordResetDialog.close();
}

async function submitPasswordResetRequest(event) {
  event.preventDefault();
  const email = el.passwordResetEmail.value.trim();
  if (!email) {
    setFormFeedback(el.passwordResetFeedback, "재설정 안내를 받을 이메일을 입력해 주세요.", "error");
    el.passwordResetEmail.focus();
    return;
  }

  setFormPending(el.passwordResetForm, true, "전송 중...");
  try {
    const result = await api("/api/auth/password-reset/request", {
      method: "POST",
      body: JSON.stringify({
        email,
        baseUrl: window.location.origin,
      }),
    });
    setFormFeedback(el.passwordResetFeedback, result.message || "재설정 링크를 메일로 보냈습니다.", "success");
    window.setTimeout(() => {
      closePasswordResetDialog();
    }, 900);
  } catch (error) {
    setFormFeedback(el.passwordResetFeedback, error.message || "비밀번호 재설정 요청에 실패했습니다.", "error");
    el.passwordResetEmail.focus();
  } finally {
    setFormPending(el.passwordResetForm, false);
  }
}

function toggleShells() {
  const loggedIn = Boolean(state.token && state.user);
  el.authShell.classList.toggle("hidden", loggedIn);
  el.appShell.classList.toggle("hidden", !loggedIn);
}

function renderSession() {
  if (!state.token || !state.user) {
    toggleShells();
    el.sessionState.textContent = "로그아웃";
    el.sessionUser.textContent = "-";
    el.sessionOrg.textContent = "-";
    return;
  }
  toggleShells();
  el.sessionState.textContent = "로그인";
  el.sessionUser.textContent = `${state.user.name} / ${state.user.email}`;
  el.sessionOrg.textContent = parseJwt(state.token).org || "-";
}

function getCurrentOrganization() {
  const currentOrgId = parseJwt(state.token).org;
  return state.orgs.find((org) => org.id === currentOrgId) || null;
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

function wirePasswordToggles() {
  document.querySelectorAll("[data-password-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
      const field = button.closest(".password-field")?.querySelector("input");
      if (!field) {
        return;
      }
      const nextType = field.type === "password" ? "text" : "password";
      field.type = nextType;
      button.textContent = nextType === "password" ? "보기" : "숨기기";
    });
  });
}

function wireAuthFieldFlow() {
  [el.registerForm, el.loginForm].forEach((form) => {
    if (!form) {
      return;
    }
    const fields = Array.from(form.querySelectorAll("input"))
      .filter((field) => field.type !== "checkbox" && field.type !== "hidden");
    fields.forEach((field, index) => {
      field.addEventListener("keydown", (event) => {
        if (event.key !== "Enter" || event.shiftKey) {
          return;
        }
        const isLastField = index === fields.length - 1;
        if (!isLastField) {
          event.preventDefault();
          fields[index + 1].focus();
          fields[index + 1].select?.();
        }
      });
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
      <p>웹훅: ${org.webhook?.enabled ? "활성" : "비활성"}</p>
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
        <label class="inline-field">
          <span>Webhook Secret</span>
          <input type="text" value="${org.webhook?.secret || ""}" data-webhook-secret="${org.id}" placeholder="secret">
        </label>
        <label class="checkbox compact">
          <input type="checkbox" ${org.webhook?.enabled ? "checked" : ""} data-webhook-enabled="${org.id}">
          <span>Webhook 활성</span>
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
        <button type="button" class="ghost-btn" data-action="update-webhook" data-id="${org.id}">Webhook 저장</button>
        <button type="button" data-action="switch-org" data-id="${org.id}" ${org.id === currentOrgId ? "disabled" : ""}>
          ${org.id === currentOrgId ? "현재 조직" : "전환"}
        </button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>조직이 없습니다.</p></article>`;
  renderDashboard();
}

async function refreshCustomers() {
  const customers = await api("/api/customers");
  state.customers = customers;
  renderCustomerPage();

  const customerOptions = customers.map((customer) =>
    `<option value="${customer.id}">${customer.name} (${customer.phone || "전화없음"})</option>`
  ).join("");
  el.reservationCustomer.innerHTML = customerOptions;
  el.editReservationCustomer.innerHTML = customerOptions;
  el.dmMessageCustomer.innerHTML = customerOptions;
  renderDashboard();
}

async function refreshChannels() {
  const channels = await api("/api/channels");
  state.channels = channels;
  el.channelList.innerHTML = channels.map((channel) => `
    <article class="list-item channel-item">
      <div class="channel-head">
        <strong>${channel.provider === "INSTAGRAM" ? "Instagram" : channel.provider}</strong>
        <span class="status-chip ${getChannelModeTone(channel.sendMode)}">${getChannelModeLabel(channel.sendMode)}</span>
      </div>
      <div class="channel-meta">
        <p><span>상태</span><strong>${channel.status}</strong></p>
        <p><span>계정명</span><strong>${escapeHtml(channel.accountName || "-")}</strong></p>
        <p><span>유저명</span><strong>${escapeHtml(channel.username || "-")}</strong></p>
        <p><span>외부 계정 ID</span><strong>${escapeHtml(channel.externalAccountId || "-")}</strong></p>
        <p><span>Webhook</span><strong>${channel.webhookSubscribed ? "연결됨" : "미연결"}</strong></p>
        <p><span>연결 시각</span><strong>${escapeHtml(formatDateTime(channel.connectedAt))}</strong></p>
      </div>
      <div class="list-item-actions">
        ${channel.provider === "INSTAGRAM" && channel.status !== "DISCONNECTED"
          ? `<button type="button" class="ghost-btn" data-action="sync-instagram-channel" data-id="${channel.id}">Instagram 동기화</button>`
          : ""}
        <button type="button" class="ghost-btn" data-action="disconnect-channel" data-id="${channel.id}" ${channel.status === "DISCONNECTED" ? "disabled" : ""}>연결 해제</button>
      </div>
    </article>
  `).join("") || `<article class="list-item"><p>연결된 채널이 없습니다.</p></article>`;
  renderDashboard();
}

function getChannelModeLabel(sendMode) {
  switch (sendMode) {
    case "LIVE":
      return "실제 발송";
    case "DRY_RUN":
      return "테스트 모드";
    default:
      return "발송 꺼짐";
  }
}

function getChannelModeTone(sendMode) {
  switch (sendMode) {
    case "LIVE":
      return "success";
    case "DRY_RUN":
      return "warning";
    default:
      return "neutral";
  }
}

function getDmStatusLabel(status) {
  switch (status) {
    case "RECEIVED":
      return "신규 수신";
    case "NEEDS_REVIEW":
      return "검토 필요";
    case "RESERVED":
      return "예약 완료";
    default:
      return status || "-";
  }
}

async function refreshServices() {
  const services = await api("/api/services");
  state.services = services;
  renderServicePage();

  const serviceOptions = services.map((service) =>
    `<option value="${service.id}">${service.name} / ${service.durationMinutes}분</option>`
  ).join("");
  el.reservationService.innerHTML = serviceOptions;
  el.editReservationService.innerHTML = serviceOptions;
  el.dmMessageService.innerHTML = serviceOptions;
  renderDashboard();
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
  renderReservationPage();
  renderDashboard();
}

async function refreshDmMessages() {
  const query = el.dmStatusFilter.value ? `?status=${encodeURIComponent(el.dmStatusFilter.value)}` : "";
  const messages = await api(`/api/dm/messages${query}`);
  state.dmMessages = messages;
  renderDmSummary(messages);
  renderDmMessagePage();
  renderDashboard();
}

function renderCustomerPage() {
  const totalPages = Math.max(1, Math.ceil(state.customers.length / state.listPageSize));
  state.customerPage = Math.min(state.customerPage, totalPages);
  state.customerPage = Math.max(state.customerPage, 1);

  const start = (state.customerPage - 1) * state.listPageSize;
  const pagedCustomers = state.customers.slice(start, start + state.listPageSize);

  el.customerList.innerHTML = pagedCustomers.map((customer) => `
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

  if (el.customerCountLabel) {
    el.customerCountLabel.textContent = `총 ${state.customers.length}명`;
  }
  renderNumberPagination(el.customerPagination, totalPages, state.customerPage, "customer-page");
}

function renderServicePage() {
  const totalPages = Math.max(1, Math.ceil(state.services.length / state.listPageSize));
  state.servicePage = Math.min(state.servicePage, totalPages);
  state.servicePage = Math.max(state.servicePage, 1);

  const start = (state.servicePage - 1) * state.listPageSize;
  const pagedServices = state.services.slice(start, start + state.listPageSize);

  el.serviceList.innerHTML = pagedServices.map((service) => `
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

  if (el.serviceCountLabel) {
    el.serviceCountLabel.textContent = `총 ${state.services.length}개`;
  }
  renderNumberPagination(el.servicePagination, totalPages, state.servicePage, "service-page");
}

function renderReservationPage() {
  const visibleReservations = getVisibleReservations();
  const totalPages = Math.max(1, Math.ceil(visibleReservations.length / state.listPageSize));
  state.reservationPage = Math.min(state.reservationPage, totalPages);
  state.reservationPage = Math.max(state.reservationPage, 1);

  const start = (state.reservationPage - 1) * state.listPageSize;
  const pagedReservations = visibleReservations.slice(start, start + state.listPageSize);

  el.reservationList.innerHTML = pagedReservations.map((reservation) => `
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
  `).join("") || `<article class="list-item"><p>${state.selectedReservationDateKey ? "선택한 날짜에 예약이 없습니다." : "예약이 없습니다."}</p></article>`;

  if (el.reservationCountLabel) {
    el.reservationCountLabel.textContent = state.selectedReservationDateKey
      ? `${formatDateKeyLabel(state.selectedReservationDateKey)} · 총 ${visibleReservations.length}건`
      : `총 ${visibleReservations.length}건`;
  }
  renderNumberPagination(el.reservationPagination, totalPages, state.reservationPage, "reservation-page");
  renderReservationCalendar();
}

function renderReservationCalendar() {
  if (!el.reservationCalendarGrid) {
    return;
  }

  const monthStart = new Date(
    state.reservationCalendarMonth.getFullYear(),
    state.reservationCalendarMonth.getMonth(),
    1
  );
  const monthEnd = new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0);
  const firstWeekday = monthStart.getDay();
  const totalDays = monthEnd.getDate();
  const reservationDateKeys = new Set(
    state.reservations
      .filter((reservation) => !reservation.canceledAt)
      .map((reservation) => getDateKey(reservation.startAt))
  );
  const reservationCountByDate = state.reservations
    .filter((reservation) => !reservation.canceledAt)
    .reduce((accumulator, reservation) => {
      const dateKey = getDateKey(reservation.startAt);
      accumulator[dateKey] = (accumulator[dateKey] || 0) + 1;
      return accumulator;
    }, {});

  if (!state.selectedReservationDateKey) {
    const todayKey = getDateKey(new Date());
    state.selectedReservationDateKey = reservationDateKeys.has(todayKey) ? todayKey : "";
  }
  if (state.selectedReservationDateKey) {
    syncReservationFiltersFromCalendar();
  } else {
    clearReservationDateFilters();
  }

  el.reservationCalendarLabel.textContent = `${monthStart.getFullYear()}년 ${monthStart.getMonth() + 1}월`;

  const cells = [];
  for (let index = 0; index < firstWeekday; index += 1) {
    cells.push(`<span class="calendar-cell empty" aria-hidden="true"></span>`);
  }

  for (let day = 1; day <= totalDays; day += 1) {
    const current = new Date(monthStart.getFullYear(), monthStart.getMonth(), day);
    const dateKey = getDateKey(current);
    const hasReservation = reservationDateKeys.has(dateKey);
    const reservationCount = reservationCountByDate[dateKey] || 0;
    const isSelected = state.selectedReservationDateKey === dateKey;
    const isCurrentDay = getDateKey(new Date()) === dateKey;
    cells.push(`
      <button
        type="button"
        class="calendar-cell ${hasReservation ? "has-reservation" : ""} ${isSelected ? "selected" : ""} ${isCurrentDay ? "today" : ""}"
        data-action="select-calendar-day"
        data-date-key="${dateKey}">
        <span class="calendar-day-number">${day}</span>
        ${hasReservation ? `<span class="calendar-count">${reservationCount}건</span><span class="calendar-dot" aria-hidden="true"></span>` : ""}
      </button>
    `);
  }

  el.reservationCalendarGrid.innerHTML = cells.join("");

  const selectedReservations = state.selectedReservationDateKey
    ? getReservationsForDateKey(state.selectedReservationDateKey)
    : [];
  el.reservationCalendarDetailTitle.textContent = state.selectedReservationDateKey
    ? `${formatDateKeyLabel(state.selectedReservationDateKey)} 예약`
    : "날짜를 선택하면 예약이 표시됩니다";
  el.reservationCalendarDayList.innerHTML = selectedReservations.length
    ? selectedReservations.map((reservation) => `
      <article class="list-item compact-list-item">
        <div class="calendar-reservation-head">
          <strong>${escapeHtml(findCustomerName(reservation.customerId))}</strong>
          <span class="status-chip ${getReservationStatusTone(reservation.status)}">${escapeHtml(reservation.status || "PENDING")}</span>
        </div>
        <p><span class="service-tag ${getServiceToneClass(reservation.serviceId)}">${escapeHtml(findServiceName(reservation.serviceId))}</span></p>
        <p>${escapeHtml(formatShortDateTime(reservation.startAt))} ~ ${escapeHtml(formatShortDateTime(reservation.endAt))}</p>
        <p>${escapeHtml(reservation.notes || reservation.status || "-")}</p>
      </article>
    `).join("")
    : createEmptyListItem(state.selectedReservationDateKey ? "이 날짜에는 예약이 없습니다." : "예약이 있는 날짜를 선택해 주세요.");
}

function renderDmMessagePage() {
  const totalPages = Math.max(1, Math.ceil(state.dmMessages.length / state.dmPageSize));
  state.dmPage = Math.min(state.dmPage, totalPages);
  state.dmPage = Math.max(state.dmPage, 1);

  const start = (state.dmPage - 1) * state.dmPageSize;
  const pagedMessages = state.dmMessages.slice(start, start + state.dmPageSize);

  el.dmMessageList.innerHTML = pagedMessages.map(renderDmMessageCard).join("")
    || `<article class="list-item"><p>DM 메시지가 없습니다.</p></article>`;

  if (el.dmCountLabel) {
    el.dmCountLabel.textContent = `현재 ${state.dmMessages.length}건`;
  }
  renderNumberPagination(el.dmPagination, totalPages, state.dmPage, "dm-page");
}

function renderNumberPagination(container, totalPages, currentPage, action) {
  if (!container) {
    return;
  }
  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  const buttons = Array.from({ length: totalPages }, (_, index) => {
    const page = index + 1;
    const active = page === currentPage ? "active" : "";
    return `<button type="button" class="pagination-btn ${active}" data-action="${action}" data-page="${page}">${page}</button>`;
  }).join("");

  container.innerHTML = buttons;
}

function renderDmSummary(messages) {
  const total = messages.length;
  const received = messages.filter((message) => message.status === "RECEIVED").length;
  const review = messages.filter((message) => message.status === "NEEDS_REVIEW").length;
  const reserved = messages.filter((message) => message.status === "RESERVED").length;
  const latest = messages[0];

  el.dmSummaryCards.innerHTML = `
    <article class="summary-card">
      <span class="summary-label">전체 메시지</span>
      <strong class="summary-value">${total}</strong>
      <p class="summary-help">현재 필터 기준으로 보이는 전체 문의 수</p>
    </article>
    <article class="summary-card warning">
      <span class="summary-label">검토 필요</span>
      <strong class="summary-value">${review}</strong>
      <p class="summary-help">운영 확인이 필요한 문의</p>
    </article>
    <article class="summary-card">
      <span class="summary-label">신규 수신</span>
      <strong class="summary-value">${received}</strong>
      <p class="summary-help">아직 처리 전인 문의</p>
    </article>
    <article class="summary-card success">
      <span class="summary-label">예약 완료</span>
      <strong class="summary-value">${reserved}</strong>
      <p class="summary-help">자동 또는 수동 확정 완료</p>
    </article>
    <article class="summary-card latest">
      <span class="summary-label">최근 수신</span>
      <strong class="summary-value">${escapeHtml(latest ? (latest.senderName || latest.senderPhone || latest.channel) : "-")}</strong>
      <p class="summary-help">${escapeHtml(latest ? `${formatDateTime(latest.receivedAt)} · ${latest.messageText || "-"}` : "최근 수신 메시지가 없습니다.")}</p>
    </article>
  `;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  try {
    return new Date(value).toLocaleString("ko-KR");
  } catch {
    return value;
  }
}

function formatShortDateTime(value) {
  if (!value) {
    return "-";
  }
  try {
    return new Date(value).toLocaleString("ko-KR", {
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    });
  } catch {
    return value;
  }
}

function isToday(value) {
  if (!value) {
    return false;
  }
  const today = new Date();
  const target = new Date(value);
  return today.getFullYear() === target.getFullYear()
    && today.getMonth() === target.getMonth()
    && today.getDate() === target.getDate();
}

function getDateKey(value) {
  const date = value instanceof Date ? value : new Date(value);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDateKeyLabel(dateKey) {
  if (!dateKey) {
    return "날짜를 선택해 주세요";
  }
  try {
    return new Date(`${dateKey}T00:00:00`).toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
      weekday: "long",
    });
  } catch {
    return dateKey;
  }
}

function getReservationsForDateKey(dateKey) {
  return state.reservations
    .filter((reservation) => !reservation.canceledAt && getDateKey(reservation.startAt) === dateKey)
    .sort((left, right) => new Date(left.startAt) - new Date(right.startAt));
}

function getServiceToneClass(serviceId) {
  if (!serviceId) {
    return "service-tone-1";
  }
  const serviceIndex = state.services.findIndex((service) => service.id === serviceId);
  const normalizedIndex = serviceIndex >= 0 ? serviceIndex : 0;
  return `service-tone-${(normalizedIndex % 4) + 1}`;
}

function toLocalDateTimeInput(date) {
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60000);
  return local.toISOString().slice(0, 16);
}

function getReservationStatusTone(status) {
  switch (status) {
    case "CONFIRMED":
    case "RESERVED":
      return "success";
    case "CANCELED":
      return "danger";
    default:
      return "neutral";
  }
}

function syncReservationFormFromCalendar() {
  if (!state.selectedReservationDateKey) {
    if (el.reservationFormDateHint) {
      el.reservationFormDateHint.textContent = "날짜를 선택하면 기본 시간이 자동으로 채워집니다.";
    }
    return;
  }
  const reservationForm = document.getElementById("reservationForm");
  if (!reservationForm) {
    return;
  }

  const startInput = reservationForm.querySelector("input[name='startAt']");
  const endInput = reservationForm.querySelector("input[name='endAt']");
  const serviceSelect = reservationForm.querySelector("select[name='serviceId']");
  if (!startInput || !endInput) {
    return;
  }

  const startDate = new Date(`${state.selectedReservationDateKey}T09:00:00`);
  const selectedServiceId = serviceSelect?.value || "";
  const durationMinutes = getServiceDuration(selectedServiceId) || 60;
  const endDate = new Date(startDate.getTime() + durationMinutes * 60000);

  startInput.value = toLocalDateTimeInput(startDate);
  endInput.value = toLocalDateTimeInput(endDate);
  if (el.reservationFormDateHint) {
    el.reservationFormDateHint.textContent = `${formatDateKeyLabel(state.selectedReservationDateKey)} 기준으로 기본 예약 시간이 채워졌습니다.`;
  }
}

function syncReservationFiltersFromCalendar() {
  if (!state.selectedReservationDateKey) {
    return;
  }
  el.reservationStartFilter.value = `${state.selectedReservationDateKey}T00:00`;
  el.reservationEndFilter.value = `${state.selectedReservationDateKey}T23:59`;
}

function clearReservationDateFilters() {
  el.reservationStartFilter.value = "";
  el.reservationEndFilter.value = "";
}

function getVisibleReservations() {
  if (!state.selectedReservationDateKey) {
    return state.reservations;
  }
  return state.reservations.filter((reservation) => getDateKey(reservation.startAt) === state.selectedReservationDateKey);
}

function createEmptyListItem(message) {
  return `<article class="list-item compact-list-item"><p>${escapeHtml(message)}</p></article>`;
}

function renderDashboardReservationItem(reservation, label) {
  return `
    <article class="list-item compact-list-item">
      <strong>${escapeHtml(findCustomerName(reservation.customerId))}</strong>
      <p>${escapeHtml(findServiceName(reservation.serviceId))}</p>
      <p>${escapeHtml(formatShortDateTime(reservation.startAt))} ~ ${escapeHtml(formatShortDateTime(reservation.endAt))}</p>
      <p>${escapeHtml(label)}</p>
    </article>
  `;
}

function renderDashboard() {
  if (!el.dashboardStats) {
    return;
  }

  const currentOrg = getCurrentOrganization();
  const activeReservations = state.reservations.filter((reservation) => !reservation.canceledAt);
  const todayReservations = activeReservations
    .filter((reservation) => isToday(reservation.startAt))
    .sort((left, right) => new Date(left.startAt) - new Date(right.startAt));
  const recentReservations = [...activeReservations]
    .sort((left, right) => new Date(right.startAt) - new Date(left.startAt))
    .slice(0, 4);
  const reviewDmCount = state.dmMessages.filter((message) => message.status === "NEEDS_REVIEW").length;
  const receivedDmCount = state.dmMessages.filter((message) => message.status === "RECEIVED").length;
  const activeChannels = state.channels.filter((channel) => channel.status === "ACTIVE").length;
  const latestDm = state.dmMessages[0];

  if (el.heroBadge) {
    el.heroBadge.innerHTML = `
      <p class="eyebrow">오늘 요약</p>
      <strong>${escapeHtml(currentOrg?.name || state.user?.name || "워크스페이스")}</strong>
      <p>오늘 예약 ${todayReservations.length}건</p>
      <p>검토 필요 DM ${reviewDmCount}건</p>
    `;
  }

  el.dashboardStats.innerHTML = `
    <article class="dashboard-stat-card">
      <span class="summary-label">오늘 예약</span>
      <strong class="summary-value">${todayReservations.length}</strong>
      <p class="summary-help">오늘 일정에 잡힌 예약 수</p>
    </article>
    <article class="dashboard-stat-card">
      <span class="summary-label">고객</span>
      <strong class="summary-value">${state.customers.length}</strong>
      <p class="summary-help">현재 워크스페이스 고객 수</p>
    </article>
    <article class="dashboard-stat-card warning">
      <span class="summary-label">검토 필요 DM</span>
      <strong class="summary-value">${reviewDmCount}</strong>
      <p class="summary-help">운영 확인이 필요한 문의</p>
    </article>
    <article class="dashboard-stat-card success">
      <span class="summary-label">연결 채널</span>
      <strong class="summary-value">${activeChannels}</strong>
      <p class="summary-help">현재 연결된 채널 수</p>
    </article>
  `;

  el.dashboardRecentReservations.innerHTML = recentReservations.length
    ? recentReservations.map((reservation) =>
      renderDashboardReservationItem(reservation, reservation.notes || "가장 최근에 등록된 예약")
    ).join("")
    : createEmptyListItem("최근 예약이 없습니다.");

  el.dashboardTodayReservations.innerHTML = todayReservations.length
    ? todayReservations.slice(0, 5).map((reservation) =>
      renderDashboardReservationItem(reservation, reservation.status === "CONFIRMED" ? "확정됨" : reservation.status || "진행 예정")
    ).join("")
    : createEmptyListItem("오늘 일정이 없습니다.");

  el.dashboardDmSummary.innerHTML = `
    <article class="summary-card ${reviewDmCount ? "warning" : ""}">
      <span class="summary-label">검토 필요</span>
      <strong class="summary-value">${reviewDmCount}</strong>
      <p class="summary-help">운영 확인이 필요한 문의</p>
    </article>
    <article class="summary-card">
      <span class="summary-label">신규 수신</span>
      <strong class="summary-value">${receivedDmCount}</strong>
      <p class="summary-help">아직 처리 전인 문의</p>
    </article>
    <article class="summary-card latest">
      <span class="summary-label">최근 문의</span>
      <strong class="summary-value">${escapeHtml(latestDm ? (latestDm.senderName || latestDm.senderPhone || "채널 사용자") : "-")}</strong>
      <p class="summary-help">${escapeHtml(latestDm ? `${formatShortDateTime(latestDm.receivedAt)} · ${latestDm.messageText || "-"}` : "최근 문의가 없습니다.")}</p>
    </article>
  `;

  el.dashboardChannelStatus.innerHTML = state.channels.length
    ? state.channels.map((channel) => `
      <article class="list-item compact-list-item">
        <div class="channel-head">
          <strong>${escapeHtml(channel.accountName || channel.provider)}</strong>
          <span class="status-chip ${getChannelModeTone(channel.sendMode)}">${getChannelModeLabel(channel.sendMode)}</span>
        </div>
        <p>${escapeHtml(channel.provider)} · ${escapeHtml(channel.status)}</p>
        <p>${escapeHtml(channel.username || channel.externalAccountId || "-")}</p>
      </article>
    `).join("")
    : createEmptyListItem("연결된 채널이 없습니다.");
}

function getDmStatusTone(status) {
  switch (status) {
    case "RESERVED":
      return "success";
    case "NEEDS_REVIEW":
      return "warning";
    default:
      return "neutral";
  }
}

function getDmOutcomeText(message) {
  if (message.status === "RESERVED") {
    return "예약 완료 또는 운영 처리 완료";
  }
  if (message.failureReason) {
    return message.failureReason;
  }
  return "검토 필요";
}

function renderDmMessageCard(message) {
  const senderSummary = [message.senderName, message.senderPhone].filter(Boolean).join(" / ") || "채널 사용자";
  const parseSummary = message.parsedStartAt || message.parsedEndAt
    ? `${formatDateTime(message.parsedStartAt)} ~ ${formatDateTime(message.parsedEndAt)}`
    : "-";
  return `
    <article class="list-item dm-message-item">
      <div class="dm-message-head">
        <span class="status-chip ${getDmStatusTone(message.status)}">${escapeHtml(getDmStatusLabel(message.status))}</span>
        <span class="dm-message-channel">${escapeHtml(message.channel)} · ${escapeHtml(message.intent || "BOOK")}</span>
      </div>
      <strong class="dm-message-sender">${escapeHtml(senderSummary)}</strong>
      <p class="dm-message-body">${escapeHtml(message.messageText || "-")}</p>
      <div class="dm-message-meta">
        <p><span>처리결과</span><strong>${escapeHtml(getDmOutcomeText(message))}</strong></p>
        <p><span>자동응답</span><strong>${escapeHtml(message.replyText || "-")}</strong></p>
        <p><span>고객ID</span><strong>${escapeHtml(message.customerId || "-")}</strong></p>
        <p><span>예약ID</span><strong>${escapeHtml(message.reservationId || "-")}</strong></p>
        <p><span>파싱 시간</span><strong>${escapeHtml(parseSummary)}</strong></p>
        <p><span>수신시각</span><strong>${escapeHtml(formatDateTime(message.receivedAt))}</strong></p>
        <p><span>처리시각</span><strong>${escapeHtml(formatDateTime(message.processedAt))}</strong></p>
      </div>
      <div class="list-item-actions">
        <button type="button" data-action="open-dm-message" data-id="${message.id}">상세</button>
      </div>
    </article>
  `;
}

async function bootstrapData() {
  if (!state.token) {
    return;
  }
  await Promise.all([refreshOrgs(), refreshChannels(), refreshCustomers(), refreshServices(), refreshReservations(), refreshDmMessages()]);
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
  el.channelList.innerHTML = "";
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

function normalizeText(value) {
  if (!value) {
    return "";
  }
  return value.toLowerCase().replace(/\s+/g, "").replace(/[^0-9a-z가-힣]/g, "");
}

function findServiceIdByHint(serviceHint) {
  const normalizedHint = normalizeText(serviceHint);
  if (!normalizedHint) {
    return "";
  }
  const match = state.services
    .filter((service) => service.active)
    .sort((left, right) => normalizeText(right.name).length - normalizeText(left.name).length)
    .find((service) => {
      const normalizedName = normalizeText(service.name);
      return normalizedHint.includes(normalizedName) || normalizedName.includes(normalizedHint);
    });
  return match ? match.id : "";
}

function getServiceDuration(serviceId) {
  const service = state.services.find((item) => item.id === serviceId);
  return service ? Number(service.durationMinutes || 0) : 0;
}

function syncDmEndAtFromService() {
  if (!el.dmMessageStartAt.value || !el.dmMessageService.value) {
    return;
  }
  const durationMinutes = getServiceDuration(el.dmMessageService.value);
  if (!durationMinutes) {
    return;
  }
  const startDate = new Date(el.dmMessageStartAt.value);
  const endDate = new Date(startDate.getTime() + durationMinutes * 60000);
  const offset = endDate.getTimezoneOffset();
  const local = new Date(endDate.getTime() - offset * 60000);
  el.dmMessageEndAt.value = local.toISOString().slice(0, 16);
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

function openDmMessageDialog(messageId) {
  const message = state.dmMessages.find((item) => item.id === messageId);
  if (!message) {
    log("DM 상세 열기 실패", { messageId });
    return;
  }
  el.dmMessageId.value = message.id;
  el.dmMessageText.value = message.messageText || "";
  el.dmMessageIntent.value = message.intent || "BOOK";
  el.dmMessageCustomer.value = message.customerId || "";
  el.dmMessageService.value = findServiceIdByHint(message.serviceHint || message.messageText);
  el.dmMessageStartAt.value = toLocalInputValue(message.parsedStartAt);
  el.dmMessageEndAt.value = toLocalInputValue(message.parsedEndAt);
  if (!el.dmMessageEndAt.value) {
    syncDmEndAtFromService();
  }
  el.dmMessageNotes.value = message.replyText || message.failureReason || "";
  el.dmSuggestedTimes.innerHTML = "";
  const reservationOptions = state.reservations
    .filter((reservation) => !message.customerId || reservation.customerId === message.customerId)
    .map((reservation) => `<option value="${reservation.id}">${reservation.startAt} / ${findServiceName(reservation.serviceId)}</option>`)
    .join("");
  el.dmMessageReservation.innerHTML = `<option value="">예약 선택</option>${reservationOptions}`;
  const locked = message.status === "RESERVED";
  el.dmConfirmBookBtn.disabled = locked;
  el.dmConfirmUpdateBtn.disabled = locked;
  el.dmConfirmCancelBtn.disabled = locked;
  el.dmConfirmBookBtn.hidden = (message.intent || "BOOK") !== "BOOK";
  el.dmConfirmUpdateBtn.hidden = (message.intent || "BOOK") !== "UPDATE";
  el.dmConfirmCancelBtn.hidden = (message.intent || "BOOK") !== "CANCEL";
  el.dmMessageDialog.showModal();
}

async function submitReservationEdit(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const reservationId = payload.id;
    delete payload.id;
    payload.startAt = toIsoLocal(payload.startAt);
    payload.endAt = toIsoLocal(payload.endAt);
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

async function submitDmMessageConfirm(event) {
  event.preventDefault();
  try {
    const payload = formToJson(event.target);
    const messageId = payload.id;
    delete payload.id;
    payload.startAt = toIsoLocal(payload.startAt);
    payload.endAt = toIsoLocal(payload.endAt);
    const result = await api(`/api/dm/messages/${messageId}/confirm`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    el.dmMessageDialog.close();
    await Promise.all([refreshReservations(), refreshDmMessages()]);
    log("DM 수동 예약 확정 성공", result);
  } catch (error) {
    log("DM 수동 예약 확정 실패", error);
  }
}

async function confirmDmUpdate() {
  try {
    const payload = formToJson(el.dmMessageConfirmForm);
    const reservationId = el.dmMessageReservation.value;
    if (!reservationId) {
      throw { error: "RESERVATION_REQUIRED", message: "변경할 예약을 선택해야 합니다." };
    }
    const messageId = payload.id;
    payload.reservationId = reservationId;
    delete payload.id;
    payload.startAt = toIsoLocal(payload.startAt);
    payload.endAt = toIsoLocal(payload.endAt);
    const result = await api(`/api/dm/messages/${messageId}/update`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    el.dmMessageDialog.close();
    await Promise.all([refreshReservations(), refreshDmMessages()]);
    log("DM 예약 변경 확정 성공", result);
  } catch (error) {
    log("DM 예약 변경 확정 실패", error);
  }
}

async function confirmDmCancel() {
  try {
    const messageId = el.dmMessageId.value;
    const reservationId = el.dmMessageReservation.value;
    if (!reservationId) {
      throw { error: "RESERVATION_REQUIRED", message: "취소할 예약을 선택해야 합니다." };
    }
    const result = await api(`/api/dm/messages/${messageId}/cancel`, {
      method: "POST",
      body: JSON.stringify({ reservationId }),
    });
    el.dmMessageDialog.close();
    await Promise.all([refreshReservations(), refreshDmMessages()]);
    log("DM 예약 취소 확정 성공", result);
  } catch (error) {
    log("DM 예약 취소 확정 실패", error);
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

async function updateOrganizationWebhook(orgId) {
  const secretInput = el.orgList.querySelector(`[data-webhook-secret="${orgId}"]`);
  const enabledInput = el.orgList.querySelector(`[data-webhook-enabled="${orgId}"]`);
  if (!secretInput || !enabledInput) {
    return;
  }
  try {
    const result = await api(`/api/orgs/${orgId}/webhook`, {
      method: "PATCH",
      body: JSON.stringify({
        enabled: enabledInput.checked,
        secret: secretInput.value,
      }),
    });
    await refreshOrgs();
    log("Webhook 설정 저장 성공", result);
  } catch (error) {
    log("Webhook 설정 저장 실패", error);
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

async function startInstagramConnect() {
  try {
    const result = await api("/api/channels/instagram/connect/start", {
      method: "POST",
    });
    await refreshChannels();
    log("Instagram 연결 시작", result);
    if (result.authorizationUrl) {
      window.open(result.authorizationUrl, "_blank", "noopener,noreferrer");
      beginInstagramConnectPolling();
    }
  } catch (error) {
    log("Instagram 연결 시작 실패", error);
  }
}

async function disconnectChannel(channelId) {
  try {
    const result = await api(`/api/channels/${channelId}`, {
      method: "DELETE",
    });
    await refreshChannels();
    log("채널 연결 해제 성공", result);
  } catch (error) {
    log("채널 연결 해제 실패", error);
  }
}

function beginInstagramConnectPolling() {
  if (state.instagramConnectPoller) {
    clearInterval(state.instagramConnectPoller);
  }

  let attempts = 0;
  state.instagramConnectPoller = setInterval(async () => {
    attempts += 1;
    try {
      await refreshChannels();
      const activeInstagram = state.channels.find((channel) => channel.provider === "INSTAGRAM" && channel.status === "ACTIVE");
      if (activeInstagram || attempts >= 20) {
        clearInterval(state.instagramConnectPoller);
        state.instagramConnectPoller = null;
      }
    } catch (error) {
      if (attempts >= 20) {
        clearInterval(state.instagramConnectPoller);
        state.instagramConnectPoller = null;
      }
    }
  }, 3000);
}

async function syncInstagramChannel(channelId) {
  try {
    const result = await api(`/api/channels/${channelId}/sync-instagram`, {
      method: "POST",
    });
    await refreshChannels();
    log("Instagram 채널 동기화 성공", result);
  } catch (error) {
    log("Instagram 채널 동기화 실패", error);
  }
}

el.registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setFormFeedback(el.registerFeedback);
  setFormPending(el.registerForm, true, "계정 만드는 중...");
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    saveSession(result);
    setFormFeedback(el.registerFeedback, "계정이 만들어졌습니다. 워크스페이스로 이동합니다.", "success");
    await bootstrapData();
    log("회원가입 성공", result.user);
  } catch (error) {
    setFormFeedback(el.registerFeedback, error.message || "회원가입에 실패했습니다. 입력 정보를 다시 확인해 주세요.", "error");
    event.target.querySelector("input[name='email']")?.focus();
    log("회원가입 실패", error);
  } finally {
    setFormPending(el.registerForm, false);
  }
});

el.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setFormFeedback(el.loginFeedback);
  setFormPending(el.loginForm, true, "로그인 중...");
  try {
    const payload = formToJson(event.target);
    const result = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    saveSession(result);
    syncRememberedEmail(payload.email);
    setFormFeedback(el.loginFeedback, "로그인되었습니다. 워크스페이스로 이동합니다.", "success");
    await bootstrapData();
    log("로그인 성공", result.user);
  } catch (error) {
    setFormFeedback(el.loginFeedback, error.message || "로그인에 실패했습니다. 이메일과 비밀번호를 확인해 주세요.", "error");
    event.target.querySelector("input[name='password']")?.focus();
    log("로그인 실패", error);
  } finally {
    setFormPending(el.loginForm, false);
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
    setFormFeedback(el.registerFeedback);
    setFormFeedback(el.loginFeedback);
    el.customerList.innerHTML = "";
    el.serviceList.innerHTML = "";
    el.reservationList.innerHTML = "";
    el.reservationCustomer.innerHTML = "";
    el.reservationService.innerHTML = "";
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
el.refreshChannelsBtn.addEventListener("click", () => refreshChannels().catch((error) => log("채널 새로고침 실패", error)));
el.startInstagramConnectBtn.addEventListener("click", startInstagramConnect);
el.refreshDmMessagesBtn.addEventListener("click", () => refreshDmMessages().catch((error) => log("DM 새로고침 실패", error)));
el.dmStatusFilter.addEventListener("change", () => {
  state.dmPage = 1;
  refreshDmMessages().catch((error) => log("DM 새로고침 실패", error));
});
el.customerPagination.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='customer-page']");
  if (!button) {
    return;
  }
  state.customerPage = Number(button.dataset.page);
  renderCustomerPage();
});
el.servicePagination.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='service-page']");
  if (!button) {
    return;
  }
  state.servicePage = Number(button.dataset.page);
  renderServicePage();
});
el.reservationPagination.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='reservation-page']");
  if (!button) {
    return;
  }
  state.reservationPage = Number(button.dataset.page);
  renderReservationPage();
});
el.reservationCalendarPrevBtn.addEventListener("click", () => {
  state.reservationCalendarMonth = new Date(
    state.reservationCalendarMonth.getFullYear(),
    state.reservationCalendarMonth.getMonth() - 1,
    1
  );
  renderReservationCalendar();
});
el.reservationCalendarNextBtn.addEventListener("click", () => {
  state.reservationCalendarMonth = new Date(
    state.reservationCalendarMonth.getFullYear(),
    state.reservationCalendarMonth.getMonth() + 1,
    1
  );
  renderReservationCalendar();
});
el.reservationCalendarGrid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='select-calendar-day']");
  if (!button) {
    return;
  }
  state.selectedReservationDateKey = state.selectedReservationDateKey === button.dataset.dateKey ? "" : button.dataset.dateKey;
  state.reservationPage = 1;
  if (state.selectedReservationDateKey) {
    syncReservationFiltersFromCalendar();
    syncReservationFormFromCalendar();
  } else {
    clearReservationDateFilters();
  }
  renderReservationPage();
  renderReservationCalendar();
});
el.dmPagination.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='dm-page']");
  if (!button) {
    return;
  }
  state.dmPage = Number(button.dataset.page);
  renderDmMessagePage();
});
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
  const webhookButton = event.target.closest("[data-action='update-webhook']");
  if (webhookButton) {
    updateOrganizationWebhook(webhookButton.dataset.id);
    return;
  }
  const switchButton = event.target.closest("[data-action='switch-org']");
  if (switchButton) {
    switchOrganization(switchButton.dataset.id);
  }
});
el.includeCanceled.addEventListener("change", () => refreshReservations().catch((error) => log("예약 새로고침 실패", error)));
el.reservationStartFilter.addEventListener("change", () => {
  state.selectedReservationDateKey = "";
  state.reservationPage = 1;
  refreshReservations().catch((error) => log("예약 새로고침 실패", error));
});
el.reservationEndFilter.addEventListener("change", () => {
  state.selectedReservationDateKey = "";
  state.reservationPage = 1;
  refreshReservations().catch((error) => log("예약 새로고침 실패", error));
});
el.closeReservationDialogBtn.addEventListener("click", () => el.reservationDialog.close());
el.cancelReservationBtn.addEventListener("click", () => cancelReservation());
el.restoreReservationBtn.addEventListener("click", () => restoreReservation());
el.reservationEditForm.addEventListener("submit", submitReservationEdit);
el.closeCustomerDialogBtn.addEventListener("click", () => el.customerDialog.close());
el.customerEditForm.addEventListener("submit", submitCustomerEdit);
el.closeServiceDialogBtn.addEventListener("click", () => el.serviceDialog.close());
el.serviceEditForm.addEventListener("submit", submitServiceEdit);
el.closeDmMessageDialogBtn.addEventListener("click", () => el.dmMessageDialog.close());
el.dmMessageConfirmForm.addEventListener("submit", submitDmMessageConfirm);
el.dmConfirmUpdateBtn.addEventListener("click", confirmDmUpdate);
el.dmConfirmCancelBtn.addEventListener("click", confirmDmCancel);
el.dmMessageService.addEventListener("change", syncDmEndAtFromService);
el.dmMessageStartAt.addEventListener("change", syncDmEndAtFromService);
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
el.dmMessageList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='open-dm-message']");
  if (!button) {
    return;
  }
  openDmMessageDialog(button.dataset.id);
});
el.channelList.addEventListener("click", (event) => {
  const syncButton = event.target.closest("[data-action='sync-instagram-channel']");
  if (syncButton) {
    syncInstagramChannel(syncButton.dataset.id);
    return;
  }
  const button = event.target.closest("[data-action='disconnect-channel']");
  if (!button) {
    return;
  }
  disconnectChannel(button.dataset.id);
});
el.openPasswordResetBtn.addEventListener("click", openPasswordResetDialog);
el.closePasswordResetDialogBtn.addEventListener("click", closePasswordResetDialog);
el.passwordResetForm.addEventListener("submit", submitPasswordResetRequest);

renderSession();
wireTabs();
wirePasswordToggles();
wireAuthFieldFlow();
hydrateRememberedEmail();
bootstrapData().catch((error) => log("초기 데이터 로드 실패", error));
window.addEventListener("focus", () => {
  if (state.token) {
    refreshChannels().catch((error) => log("채널 새로고침 실패", error));
    refreshDmMessages().catch((error) => log("DM 새로고침 실패", error));
  }
});
setInterval(() => {
  if (!state.token) {
    return;
  }
  refreshDmMessages().catch((error) => log("DM 자동 새로고침 실패", error));
}, 15000);
