const state = {
  user: JSON.parse(localStorage.getItem("cm_user") || "null"),
  accessToken: localStorage.getItem("cm_accessToken"),
  refreshToken: localStorage.getItem("cm_refreshToken"),
  challenges: [],
  myParticipations: [],
  currentView: "dashboard",
  authMode: "login",
  challengePage: 1,
  challengePageSize: 10,
  challengePageData: {
    totalPages: 1,
    totalElements: 0,
    number: 0,
  },
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const statusText = {
  RECRUITING: "모집 중",
  ONGOING: "진행 중",
  CLOSED: "종료",
};

const myStatusText = {
  JOINED: "참여 중",
  SUCCESS: "성공",
  FAILED: "실패",
  CANCLED: "취소",
};

function formatPoint(value) {
  if (value === undefined || value === null || value === "") return "-";
  return `${Number(value).toLocaleString("ko-KR")}P`;
}

function formatDate(value) {
  return value ? new Intl.DateTimeFormat("ko-KR").format(new Date(value)) : "-";
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function saveSession(data) {
  state.user = { email: data.email, name: data.name, role: data.role };
  state.accessToken = data.accessToken;
  state.refreshToken = data.refreshToken;
  localStorage.setItem("cm_user", JSON.stringify(state.user));
  localStorage.setItem("cm_accessToken", state.accessToken);
  localStorage.setItem("cm_refreshToken", state.refreshToken);
  renderAccount();
}

function clearSession() {
  state.user = null;
  state.accessToken = null;
  state.refreshToken = null;
  localStorage.removeItem("cm_user");
  localStorage.removeItem("cm_accessToken");
  localStorage.removeItem("cm_refreshToken");
  renderAccount();
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (!(options.body instanceof FormData)) headers["Content-Type"] = "application/json";
  if (state.accessToken) headers.Authorization = `Bearer ${state.accessToken}`;

  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(payload?.message || payload?.error || "요청을 처리하지 못했습니다.");
  }

  return payload?.data ?? payload;
}

function toast(message) {
  const toastEl = $("#toast");
  toastEl.textContent = message;
  toastEl.classList.add("show");
  window.clearTimeout(toastEl.dataset.timer);
  toastEl.dataset.timer = window.setTimeout(() => toastEl.classList.remove("show"), 2600);
}

function requireLogin() {
  if (state.accessToken) return true;
  $("#authDialog").showModal();
  toast("로그인 후 이용할 수 있습니다.");
  return false;
}

function renderAccount() {
  $("#accountName").textContent = state.user?.name || "방문자";
  $("#accountEmail").textContent = state.user?.email || "로그인이 필요합니다";
  $("#logoutButton").classList.toggle("hidden", !state.accessToken);
  $("[data-open-auth]").textContent = state.accessToken ? "계정 전환" : "로그인";
}

function switchView(view) {
  state.currentView = view;
  $$(".view").forEach((el) => el.classList.toggle("active", el.id === `${view}View`));
  $$(".nav-item").forEach((el) => el.classList.toggle("active", el.dataset.view === view));
  $("#pageTitle").textContent = {
    dashboard: "대시보드",
    explore: "챌린지 찾기",
    mine: "내 챌린지",
    points: "포인트",
  }[view];

  if (view === "mine") loadMine();
  if (view === "points") loadPoints();
}

function challengeCard(challenge) {
  const status = challenge.status || "RECRUITING";
  const desc = challenge.description || "상세 설명이 아직 없습니다.";
  return `
    <article class="challenge-card">
      <div class="status-row">
        <span class="badge ${status.toLowerCase()}">${statusText[status] || status}</span>
        <span class="badge">${formatPoint(challenge.depositAmount)}</span>
      </div>
      <div>
        <h3>${escapeHtml(challenge.title)}</h3>
        <p>${escapeHtml(desc)}</p>
      </div>
      <div class="card-meta">
        <span class="badge">${challenge.requiredCount}회 인증</span>
        <span class="badge">${formatDate(challenge.startDate)} - ${formatDate(challenge.endDate)}</span>
      </div>
      <button class="ghost-button full" data-detail-id="${challenge.id}" type="button">상세 보기</button>
    </article>
  `;
}

function renderChallenges() {
  const keyword = $("#challengeSearch")?.value.trim().toLowerCase() || "";
  const filtered = state.challenges.filter((item) => {
    const haystack = `${item.title || ""} ${item.description || ""}`.toLowerCase();
    return !keyword || haystack.includes(keyword);
  });
  const totalPages = Math.max(1, state.challengePageData.totalPages || 1);
  const totalItems = state.challengePageData.totalElements || 0;

  $("#challengeList").innerHTML = filtered.length
    ? filtered.map(challengeCard).join("")
    : `<div class="empty">조건에 맞는 챌린지가 없습니다.</div>`;
  renderChallengePagination(totalPages, totalItems);

  const featured = state.challenges.filter((item) => item.status === "RECRUITING").slice(0, 3);
  $("#featuredChallenges").innerHTML = featured.length
    ? featured.map(challengeCard).join("")
    : `<div class="empty">모집 중인 챌린지가 아직 없습니다.</div>`;
}

function renderChallengePagination(totalPages, totalItems) {
  const pagination = $("#challengePagination");
  if (!pagination) return;

  if (totalItems <= state.challengePageSize) {
    pagination.innerHTML = "";
    return;
  }

  const pageButtons = Array.from({ length: totalPages }, (_, index) => {
    const page = index + 1;
    return `<button class="page-button ${page === state.challengePage ? "active" : ""}" data-page="${page}" type="button" aria-label="${page}페이지">${page}</button>`;
  }).join("");

  pagination.innerHTML = `
    <button class="page-button" data-page-prev type="button" ${state.challengePage === 1 ? "disabled" : ""} aria-label="이전 페이지">&lt;</button>
    ${pageButtons}
    <button class="page-button" data-page-next type="button" ${state.challengePage === totalPages ? "disabled" : ""} aria-label="다음 페이지">&gt;</button>
  `;
}

function renderMine() {
  const list = state.myParticipations;
  $("#joinedCount").textContent = list.filter((item) => item.challengeStatus === "ONGOING" || item.myStatus === "JOINED").length;
  $("#successCount").textContent = list.reduce((sum, item) => sum + (item.successCount || 0), 0);

  $("#myChallengeList").innerHTML = list.length
    ? list.map((item) => `
      <article class="my-card">
        <div>
          <div class="status-row">
            <span class="badge ${(item.challengeStatus || "").toLowerCase()}">${statusText[item.challengeStatus] || item.challengeStatus}</span>
            <span class="badge">${myStatusText[item.myStatus] || item.myStatus}</span>
          </div>
          <h3>${escapeHtml(item.challengeTitle)}</h3>
          <p>${item.successCount || 0}회 인증 완료 · ${formatDate(item.joinedAt)} 참여</p>
        </div>
        <div class="button-row">
          <button class="ghost-button" data-detail-id="${item.challengeId}" type="button">상세</button>
          <button class="primary-button" data-open-verification="${item.challengeId}" type="button">인증하기</button>
        </div>
      </article>
    `).join("")
    : `<div class="empty">참여한 챌린지가 없습니다.</div>`;
}

async function loadChallenges() {
  try {
    const status = $("#statusFilter")?.value || "";
    const params = new URLSearchParams({
      page: String(state.challengePage - 1),
      size: String(state.challengePageSize),
    });
    if (status) params.set("status", status);
    const keyword = $("#challengeSearch")?.value.trim();
    if (keyword) params.set("title", keyword);

    const page = normalizeChallengePage(await request(`/api/challenges?${params.toString()}`));
    state.challengePageData = page;
    state.challengePage = page.number + 1;
    state.challenges = page.content;
    renderChallenges();
  } catch (error) {
    toast(error.message);
  }
}

function normalizeChallengePage(data) {
  if (Array.isArray(data)) {
    return {
      content: data,
      totalPages: Math.max(1, Math.ceil(data.length / state.challengePageSize)),
      totalElements: data.length,
      number: state.challengePage - 1,
    };
  }

  return {
    content: data?.content || [],
    totalPages: data?.totalPages ?? 1,
    totalElements: data?.totalElements ?? data?.content?.length ?? 0,
    number: data?.number ?? 0,
  };
}

async function loadMine() {
  if (!state.accessToken) {
    renderMine();
    return;
  }
  try {
    state.myParticipations = await request("/api/challenges/my/participations");
    renderMine();
  } catch (error) {
    toast(error.message);
  }
}

async function loadPoints() {
  if (!state.accessToken) return;
  try {
    const [balance, history] = await Promise.all([
      request("/api/points/balance"),
      request("/api/points/history"),
    ]);
    $("#pointBalance").textContent = formatPoint(balance?.balance);
    $("#pointHistory").innerHTML = history.length
      ? history.map((item) => `
        <article class="history-item">
          <div>
            <strong>${pointTypeText(item.type)}</strong>
            <p class="muted">${formatDate(item.createdAt)}</p>
          </div>
          <div>
            <strong>${formatPoint(item.amount)}</strong>
            <p class="muted">잔액 ${formatPoint(item.balanceAfter)}</p>
          </div>
        </article>
      `).join("")
      : `<div class="empty">포인트 내역이 없습니다.</div>`;
  } catch (error) {
    toast(error.message);
  }
}

function pointTypeText(type) {
  return {
    CHARGE: "충전",
    WITHDRAW: "차감",
    DEPOSIT: "보증금",
    REFUND: "환급",
    REWARD: "리워드",
    PENALTY: "패널티",
  }[type] || type || "거래";
}

async function openDetail(id, showVerificationForm = false) {
  try {
    const detail = await request(`/api/challenges/${id}`);
    const canParticipate = detail.status === "RECRUITING" && !detail.participationId;
    $("#challengeDetail").innerHTML = `
      <section class="detail-layout">
        <div class="detail-hero">
          <div class="status-row">
            <span class="badge ${(detail.status || "").toLowerCase()}">${statusText[detail.status] || detail.status}</span>
            <span class="badge">${formatPoint(detail.depositAmount)}</span>
            <span class="badge">${detail.requiredCount}회 인증</span>
          </div>
          <h2>${escapeHtml(detail.title)}</h2>
          <p class="muted">${escapeHtml(detail.description || "상세 설명이 없습니다.")}</p>
          <p>${formatDate(detail.startDate)}부터 ${formatDate(detail.endDate)}까지 진행됩니다.</p>
        </div>
        <div class="detail-actions">
          <button class="primary-button" data-participate="${detail.id}" ${canParticipate ? "" : "disabled"} type="button">
            ${detail.participationId ? "참여 중" : "참여하기"}
          </button>
          <button class="ghost-button" data-load-verifications="${detail.id}" type="button">인증글 보기</button>
        </div>
        <form class="verification-form ${showVerificationForm ? "" : "hidden"}" id="verificationForm">
          <h3>오늘 인증 남기기</h3>
          <label>
            <span>이미지 URL</span>
            <input name="imageUrl" type="url" placeholder="https://example.com/photo.jpg" required>
          </label>
          <label>
            <span>내용</span>
            <textarea name="content" rows="3" placeholder="오늘의 달성 내용을 적어주세요."></textarea>
          </label>
          <button class="primary-button" type="submit">인증 등록</button>
        </form>
        <div id="verificationList" class="verification-grid"></div>
      </section>
    `;
    $("#detailDialog").showModal();
  } catch (error) {
    toast(error.message);
  }
}

async function loadVerifications(challengeId) {
  if (!requireLogin()) return;
  try {
    const list = await request(`/api/verifications?challengeId=${challengeId}`);
    $("#verificationList").innerHTML = list.length
      ? list.map((item) => `
        <article class="verification-card">
          <img src="${escapeAttribute(item.imageUrl)}" alt="${escapeAttribute(item.userName || "인증 이미지")}" loading="lazy">
          <div>
            <div class="status-row">
              <span class="badge">${escapeHtml(item.userName || "참여자")}</span>
              <span class="badge">${item.checkCount}/${item.requiredChecks} 체크</span>
            </div>
            <p>${escapeHtml(item.content || "인증 내용을 남기지 않았습니다.")}</p>
            <button class="ghost-button full" data-check-verification="${item.id}" ${item.mine || item.checkedByMe ? "disabled" : ""} type="button">
              ${item.checkedByMe ? "체크 완료" : "상호 체크"}
            </button>
          </div>
        </article>
      `).join("")
      : `<div class="empty">아직 인증글이 없습니다.</div>`;
  } catch (error) {
    toast(error.message);
  }
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  }[char]));
}

function escapeAttribute(value) {
  return escapeHtml(value).replace(/`/g, "&#096;");
}

function bindEvents() {
  $$(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });

  $$("[data-view-link]").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.viewLink));
  });

  $("[data-open-auth]").addEventListener("click", () => $("#authDialog").showModal());
  $("[data-open-create]").addEventListener("click", () => {
    if (requireLogin()) $("#createDialog").showModal();
  });
  $("#logoutButton").addEventListener("click", () => {
    clearSession();
    state.myParticipations = [];
    renderMine();
    toast("로그아웃했습니다.");
  });

  $$("#authDialog [data-auth-mode]").forEach((button) => {
    button.addEventListener("click", () => {
      state.authMode = button.dataset.authMode;
      $$("#authDialog [data-auth-mode]").forEach((item) => item.classList.toggle("active", item === button));
      $("#nameField").classList.toggle("hidden", state.authMode === "login");
      $("#authSubmit").textContent = state.authMode === "login" ? "로그인" : "회원가입";
    });
  });

  $("#authForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = Object.fromEntries(form.entries());
    try {
      if (state.authMode === "register") {
        await request("/api/users", { method: "POST", body: JSON.stringify(body) });
        toast("회원가입이 완료되었습니다. 바로 로그인할게요.");
      }
      const login = await request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: body.email, password: body.password }),
      });
      saveSession(login);
      $("#authDialog").close();
      event.currentTarget.reset();
      await Promise.all([loadMine(), loadPoints()]);
      toast(`${login.name}님, 반갑습니다.`);
    } catch (error) {
      toast(error.message);
    }
  });

  $("#challengeForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const body = Object.fromEntries(new FormData(event.currentTarget).entries());
    body.depositAmount = Number(body.depositAmount);
    body.requiredCount = Number(body.requiredCount);
    try {
      await request("/api/challenges", { method: "POST", body: JSON.stringify(body) });
      $("#createDialog").close();
      event.currentTarget.reset();
      setDefaultDates();
      await Promise.all([loadChallenges(), loadMine(), loadPoints()]);
      toast("챌린지를 개설했습니다.");
    } catch (error) {
      toast(error.message);
    }
  });

  $("#pointForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!requireLogin()) return;
    const submitter = event.submitter;
    const action = submitter.dataset.pointAction;
    const amount = Number(new FormData(event.currentTarget).get("amount"));
    try {
      await request(`/api/points/${action}`, { method: "POST", body: JSON.stringify({ amount }) });
      await loadPoints();
      toast(action === "charge" ? "포인트를 충전했습니다." : "포인트를 차감했습니다.");
    } catch (error) {
      toast(error.message);
    }
  });

  $("#refreshChallenges").addEventListener("click", loadChallenges);
  $("#refreshMine").addEventListener("click", loadMine);
  $("#refreshPoints").addEventListener("click", loadPoints);
  $("#challengeSearch").addEventListener("input", () => {
    state.challengePage = 1;
    loadChallenges();
  });
  $("#statusFilter").addEventListener("change", () => {
    state.challengePage = 1;
    loadChallenges();
  });
  $("[data-close-detail]").addEventListener("click", () => $("#detailDialog").close());
  $$("[data-close-dialog]").forEach((button) => {
    button.addEventListener("click", () => button.closest("dialog").close());
  });

  document.addEventListener("click", async (event) => {
    const detailButton = event.target.closest("[data-detail-id]");
    const participateButton = event.target.closest("[data-participate]");
    const loadVerificationButton = event.target.closest("[data-load-verifications]");
    const openVerificationButton = event.target.closest("[data-open-verification]");
    const checkButton = event.target.closest("[data-check-verification]");
    const pageButton = event.target.closest("[data-page]");
    const pagePrevButton = event.target.closest("[data-page-prev]");
    const pageNextButton = event.target.closest("[data-page-next]");

    if (pageButton) {
      state.challengePage = Number(pageButton.dataset.page);
      await loadChallenges();
    }
    if (pagePrevButton) {
      state.challengePage = Math.max(1, state.challengePage - 1);
      await loadChallenges();
    }
    if (pageNextButton) {
      state.challengePage += 1;
      await loadChallenges();
    }
    if (detailButton) openDetail(detailButton.dataset.detailId);
    if (openVerificationButton) openDetail(openVerificationButton.dataset.openVerification, true);
    if (loadVerificationButton) loadVerifications(loadVerificationButton.dataset.loadVerifications);
    if (participateButton) {
      if (!requireLogin()) return;
      try {
        await request(`/api/challenges/${participateButton.dataset.participate}/participations`, { method: "POST" });
        await Promise.all([loadMine(), loadPoints()]);
        await openDetail(participateButton.dataset.participate);
        toast("챌린지에 참여했습니다.");
      } catch (error) {
        toast(error.message);
      }
    }
    if (checkButton) {
      try {
        await request(`/api/verifications/${checkButton.dataset.checkVerification}/check`, { method: "POST" });
        checkButton.disabled = true;
        checkButton.textContent = "체크 완료";
        toast("상호 체크를 완료했습니다.");
      } catch (error) {
        toast(error.message);
      }
    }
  });

  document.addEventListener("submit", async (event) => {
    if (event.target.id !== "verificationForm") return;
    event.preventDefault();
    const challengeId = $("#challengeDetail [data-load-verifications]")?.dataset.loadVerifications;
    const body = Object.fromEntries(new FormData(event.target).entries());
    body.challengeId = Number(challengeId);
    try {
      await request("/api/verifications", { method: "POST", body: JSON.stringify(body) });
      event.target.reset();
      await Promise.all([loadVerifications(challengeId), loadMine()]);
      toast("인증글을 등록했습니다.");
    } catch (error) {
      toast(error.message);
    }
  });
}

function setDefaultDates() {
  const start = $("#challengeForm [name='startDate']");
  const end = $("#challengeForm [name='endDate']");
  start.value = today();
  const date = new Date();
  date.setDate(date.getDate() + 7);
  end.value = date.toISOString().slice(0, 10);
}

async function init() {
  bindEvents();
  renderAccount();
  setDefaultDates();
  await loadChallenges();
  if (state.accessToken) {
    await Promise.all([loadMine(), loadPoints()]);
  } else {
    renderMine();
  }
}

init();
