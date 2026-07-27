/* ============================================================
   보증금 챌린지 — 공통 JS (가이드 §5)
   - API 래퍼(X-AUTH-TOKEN, {result,data} 언랩, ErrorResponse 토스트)
   - 세션/가드, 헤더 렌더, 포맷 유틸, 프린트 애니메이션
   전역 네임스페이스: window.App
   ============================================================ */
(function () {
  'use strict';

  // ---------- 테마 (라이트 기본 + 수동 토글) ----------
  const THEME_KEY = 'theme';
  function currentTheme() { return localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'; }
  function applyTheme(t) { document.documentElement.setAttribute('data-theme', t === 'dark' ? 'dark' : 'light'); }
  function toggleTheme() {
    const next = currentTheme() === 'dark' ? 'light' : 'dark';
    localStorage.setItem(THEME_KEY, next);
    applyTheme(next);
    return next;
  }
  applyTheme(currentTheme()); // 스크립트 로드 즉시 적용

  const TOKEN_KEY = 'accessToken';
  const REFRESH_KEY = 'refreshToken';
  const NAME_KEY = 'userName';
  const ROLE_KEY = 'userRole';
  const UID_KEY = 'userId';

  // ---------- 세션 ----------
  const session = {
    get token() { return localStorage.getItem(TOKEN_KEY); },
    get refresh() { return localStorage.getItem(REFRESH_KEY); },
    get name() { return localStorage.getItem(NAME_KEY); },
    get role() { return localStorage.getItem(ROLE_KEY); },
    get userId() { const v = localStorage.getItem(UID_KEY); return v == null ? null : Number(v); },
    get isAuthed() { return !!localStorage.getItem(TOKEN_KEY); },
    save(loginResponse) {
      localStorage.setItem(TOKEN_KEY, loginResponse.accessToken);
      localStorage.setItem(REFRESH_KEY, loginResponse.refreshToken);
      if (loginResponse.name != null) localStorage.setItem(NAME_KEY, loginResponse.name);
      if (loginResponse.role != null) localStorage.setItem(ROLE_KEY, loginResponse.role);
      if (loginResponse.userId != null) localStorage.setItem(UID_KEY, loginResponse.userId);
    },
    clear() {
      [TOKEN_KEY, REFRESH_KEY, NAME_KEY, ROLE_KEY, UID_KEY].forEach((k) => localStorage.removeItem(k));
    },
  };

  // ---------- API 래퍼 ----------
  let refreshing = null; // 동시 401 중복 refresh 방지

  async function rawRequest(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (session.token) headers['X-AUTH-TOKEN'] = session.token; // Bearer 없음, raw
    const res = await fetch(path, {
      method,
      headers,
      body: body != null ? JSON.stringify(body) : undefined,
    });
    return res;
  }

  async function tryRefresh() {
    if (!session.refresh) return false;
    if (!refreshing) {
      refreshing = (async () => {
        try {
          const res = await rawRequest('POST', '/api/auth/refresh', { refreshToken: session.refresh });
          if (!res.ok) return false;
          const json = await res.json();
          if (json.result !== 'success' || !json.data) return false;
          localStorage.setItem(TOKEN_KEY, json.data.accessToken);
          localStorage.setItem(REFRESH_KEY, json.data.refreshToken);
          return true;
        } catch (_) {
          return false;
        }
      })();
    }
    const ok = await refreshing;
    refreshing = null;
    return ok;
  }

  async function request(method, path, body, _retried) {
    let res;
    try {
      res = await rawRequest(method, path, body);
    } catch (netErr) {
      throw new ApiError(0, '서버에 연결할 수 없습니다.');
    }

    // 401 → refresh 1회 재시도
    if (res.status === 401 && !_retried && session.refresh) {
      if (await tryRefresh()) return request(method, path, body, true);
    }

    // 응답 파싱
    const text = await res.text();
    let json = null;
    if (text) { try { json = JSON.parse(text); } catch (_) { /* non-json */ } }

    if (res.ok && json && json.result === 'success') {
      return json.data; // 성공 → data 언랩
    }
    // 실패 → ErrorResponse {message,status} 사용
    const msg = (json && json.message) || `요청 실패 (${res.status})`;
    if (res.status === 401) {
      // 만료/미인증 → 세션 정리 + 로그인 필요 모달(토스트 대신)
      session.clear();
      requireLoginModal();
    }
    throw new ApiError(res.status, msg);
  }

  class ApiError extends Error {
    constructor(status, message) { super(message); this.status = status; }
  }

  const api = {
    get: (p) => request('GET', p),
    post: (p, b) => request('POST', p, b),
    put: (p, b) => request('PUT', p, b),
    del: (p) => request('DELETE', p),
  };

  // ---------- 포맷 유틸 ----------
  function won(n) {
    if (n == null || isNaN(n)) return '-';
    return Number(n).toLocaleString('ko-KR') + ' P';
  }
  function signed(n) {
    const v = Number(n);
    const s = (v > 0 ? '+' : '') + v.toLocaleString('ko-KR') + ' P';
    return s;
  }
  function signClass(n) { return Number(n) > 0 ? 'pos' : (Number(n) < 0 ? 'neg' : ''); }

  function fmtDate(d) {
    if (!d) return '-';
    const s = String(d);
    return s.length >= 10 ? s.slice(0, 10) : s;
  }
  function fmtDateTime(d) {
    if (!d) return '-';
    return String(d).replace('T', ' ').slice(0, 16);
  }
  function agoText(endDate) {
    if (!endDate) return '';
    const end = new Date(String(endDate).slice(0, 10) + 'T00:00:00');
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const diff = Math.round((end - today) / 86400000);
    if (diff > 0) return `${diff}일 남음`;
    if (diff === 0) return '오늘 종료';
    return `${-diff}일 전 종료`;
  }
  function todayISO() {
    const t = new Date();
    return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`;
  }

  // ---------- 상수/라벨 맵 ----------
  const CHALLENGE_STATUS = {
    RECRUITING: { label: '모집중', cls: 'stamp--recruiting' },
    ONGOING: { label: '진행중', cls: 'stamp--ongoing' },
    CLOSED: { label: '종료', cls: 'stamp--closed' },
    DELETED: { label: '삭제됨', cls: 'stamp--cancled' },
  };
  const PARTICIPATION_STATUS = {
    JOINED: { label: '참여중', cls: 'stamp--joined' },
    SUCCESS: { label: '성공 ✔', cls: 'stamp--success' },
    FAILED: { label: '실패', cls: 'stamp--failed' },
    CANCELED: { label: '취소', cls: 'stamp--cancled' },
    JOINED_PROGRESS: { label: '진행중', cls: 'stamp--joined' }, // 프론트 전용(정산 프리뷰 미종료 표시)
  };
  const POINT_TYPE = {
    CHARGE: { label: '충전', sign: 1 },
    WITHDRAW: { label: '출금', sign: -1 },
    DEPOSIT_LOCK: { label: '보증금잠금', sign: -1 },
    DEPOSIT_REFUND: { label: '환불', sign: 1 },
    PENALTY: { label: '몰수', sign: -1 },
    REWARD: { label: '보상', sign: 1 },
  };

  function statusStamp(enumVal, map) {
    const m = (map || CHALLENGE_STATUS)[enumVal] || { label: enumVal, cls: 'stamp--closed' };
    return `<span class="stamp ${m.cls}">${m.label}</span>`;
  }

  // ---------- DOM 헬퍼 ----------
  function el(html) {
    const t = document.createElement('template');
    t.innerHTML = html.trim();
    return t.content.firstElementChild;
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, (c) => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
  }

  // ---------- 토스트 ----------
  function toast(msg, type) {
    let host = document.querySelector('.toast-host');
    if (!host) { host = el('<div class="toast-host"></div>'); document.body.appendChild(host); }
    const t = el(`<div class="toast ${type === 'err' ? 'toast--err' : type === 'ok' ? 'toast--ok' : ''}">${esc(msg)}</div>`);
    host.appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; t.style.transition = 'opacity .3s'; setTimeout(() => t.remove(), 300); }, 2600);
  }

  // ---------- 로그인 필요 모달 ----------
  let loginModalOpen = false;
  function requireLoginModal(redirectPath) {
    if (loginModalOpen) return;
    loginModalOpen = true;
    const to = encodeURIComponent(redirectPath || (location.pathname + location.search));
    const overlay = el(`
      <div class="modal-overlay">
        <div class="receipt modal-card" role="dialog" aria-modal="true">
          <div class="receipt__head">로그인 필요<span class="receipt__no">LOGIN REQUIRED</span></div>
          <div class="perf">· · · · · · · · · · · · ·</div>
          <hr class="rule" />
          <p class="muted" style="text-align:center">이 기능은 로그인이 필요합니다.</p>
          <div class="actions">
            <a class="btn" href="/auth/login.html?redirect=${to}">로그인하러 가기</a>
            <button class="btn btn--ghost" id="modal-close">닫기</button>
          </div>
        </div>
      </div>`);
    overlay.querySelector('#modal-close').onclick = () => { overlay.remove(); loginModalOpen = false; };
    overlay.addEventListener('click', (e) => { if (e.target === overlay) { overlay.remove(); loginModalOpen = false; } });
    document.body.appendChild(overlay);
  }

  // ---------- 프린트 애니메이션 ----------
  function printIn(node) {
    if (!node) return;
    node.classList.add('printing');
    node.addEventListener('animationend', () => node.classList.remove('printing'), { once: true });
  }

  // ---------- 헤더 렌더 ----------
  async function mountHeader(active) {
    const host = document.getElementById('appbar');
    if (!host) return;
    const authed = session.isAuthed;
    const nav = [
      ['/', '챌린지'],
      authed ? ['/challenge/my.html', '내 챌린지'] : null,
      authed ? ['/point/wallet.html', '지갑'] : null,
    ].filter(Boolean);

    host.innerHTML = `
      <div class="appbar__logo"><a href="/">※ 청산위원회</a></div>
      <nav class="appbar__nav">
        ${nav.map(([href, label]) => `<a href="${href}"${active === href ? ' style="color:var(--ink);font-weight:700"' : ''}>${label}</a>`).join('')}
      </nav>
      <div class="appbar__spacer"></div>
      <div id="appbar-right"></div>`;

    const right = host.querySelector('#appbar-right');
    const themeBtnHtml = `<button class="btn btn--ghost btn--sm" id="theme-btn" title="테마 전환">${currentTheme() === 'dark' ? '☀ 라이트' : '☾ 다크'}</button>`;
    if (authed) {
      right.innerHTML = `<div class="appbar__actions">
        ${session.name ? `<span class="appbar__user">${esc(session.name)} 님</span>` : ''}
        <a class="wallet-badge" href="/point/wallet.html" id="wallet-badge" title="내 지갑">…</a>
        ${themeBtnHtml}
        <button class="btn btn--ghost btn--sm" id="logout-btn">로그아웃</button>
      </div>`;
      right.querySelector('#logout-btn').onclick = () => { session.clear(); location.href = '/'; };
      // 잔액 배지
      try {
        const b = await api.get('/api/points/balance');
        right.querySelector('#wallet-badge').textContent = won(b && b.balance);
      } catch (_) {
        const badge = right.querySelector('#wallet-badge');
        if (badge) badge.textContent = '지갑';
      }
    } else {
      right.innerHTML = `<div class="appbar__actions">
        ${themeBtnHtml}
        <a class="btn btn--ghost btn--sm" href="/auth/login.html">로그인</a>
      </div>`;
    }
    const tbtn = right.querySelector('#theme-btn');
    if (tbtn) tbtn.onclick = () => { const t = toggleTheme(); tbtn.textContent = t === 'dark' ? '☀ 라이트' : '☾ 다크'; };
  }

  function requireAuth() {
    if (!session.isAuthed) {
      const back = encodeURIComponent(location.pathname + location.search);
      location.href = `/auth/login.html?redirect=${back}`;
      return false;
    }
    return true;
  }

  // ---------- 쿼리스트링 ----------
  function qs(key) { return new URLSearchParams(location.search).get(key); }

  // ---------- export ----------
  window.App = {
    api, ApiError, session,
    won, signed, signClass, fmtDate, fmtDateTime, agoText, todayISO,
    CHALLENGE_STATUS, PARTICIPATION_STATUS, POINT_TYPE, statusStamp,
    el, esc, toast, printIn, mountHeader, requireAuth, qs,
    currentTheme, toggleTheme, applyTheme, requireLoginModal,
  };
})();
