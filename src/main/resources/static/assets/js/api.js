/* ===== API 통신 래퍼 ===== */

const API = {
  BASE_URL: '/api',

  getToken() {
    return localStorage.getItem('accessToken');
  },

  setToken(token) {
    localStorage.setItem('accessToken', token);
  },

  removeToken() {
    localStorage.removeItem('accessToken');
  },

  isLoggedIn() {
    return !!this.getToken();
  },

  getUserInfo() {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return { email: payload.sub, roles: payload.roles };
    } catch {
      return null;
    }
  },

  async request(method, path, body = null) {
    const url = `${this.BASE_URL}${path}`;
    const headers = { 'Content-Type': 'application/json' };

    const token = this.getToken();
    if (token) {
      headers['X-AUTH-TOKEN'] = token;
    }

    const options = { method, headers };
    if (body && method !== 'GET') {
      options.body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(url, options);
    } catch (networkError) {
      const error = new Error('서버에 연결할 수 없습니다.');
      error.status = 0;
      throw error;
    }

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      const error = new Error(data.message || '요청에 실패했습니다.');
      error.status = response.status;
      error.data = data;

      if (error.status === 401) {
        const currentHash = window.location.hash;
        if (!currentHash.includes('/login') && !currentHash.includes('/register')) {
          this.removeToken();
          window.location.hash = '#/login';
          Toast.show('로그인이 만료되었습니다.', 'error');
        }
      }
      throw error;
    }

    return data;
  },

  async get(path) { return this.request('GET', path); },
  async post(path, body) { return this.request('POST', path, body); },
  async put(path, body) { return this.request('PUT', path, body); },
  async delete(path) { return this.request('DELETE', path); },
};

/* ===== 아이콘 (인라인 SVG) ===== */
const Icon = {
  paths: {
    home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9"/>',
    trophy: '<path d="M7 4h10v3a5 5 0 0 1-10 0V4Z"/><path d="M7 5H4.5a1.5 1.5 0 0 0 0 3H7"/><path d="M17 5h2.5a1.5 1.5 0 0 1 0 3H17"/><path d="M12 12v4"/><path d="M9 20h6"/><path d="M10.5 16h3v1.5a1.5 1.5 0 0 1-3 0V16Z"/>',
    wallet: '<path d="M3 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/><path d="M16.2 12h2.3"/>',
    image: '<path d="M4 8a2 2 0 0 1 2-2h1l1.5-2h6.5L17 6h1a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8Z"/><circle cx="12" cy="13" r="3.3"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>',
    plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/>',
    calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18"/><path d="M8 3v4"/><path d="M16 3v4"/>',
    check: '<path d="M5 13l4 4L19 7"/>',
    x: '<path d="M6 6l12 12"/><path d="M18 6L6 18"/>',
    edit: '<path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z"/><path d="M13.2 6.8l3 3"/>',
    trash: '<path d="M4 7h16"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/><path d="M9 7V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v3"/>',
    arrowLeft: '<path d="M19 12H5"/><path d="M11 6l-6 6 6 6"/>',
    menu: '<path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/>',
    lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    alert: '<path d="M12 4 2 20h20L12 4Z"/><path d="M12 10v4"/><circle cx="12" cy="17" r="0.9" fill="currentColor" stroke="none"/>',
    users: '<circle cx="9" cy="8" r="3.2"/><path d="M3.5 20c0-3.3 2.6-5.5 5.5-5.5s5.5 2.2 5.5 5.5"/><circle cx="17" cy="9" r="2.6"/><path d="M15.8 14.6c2.4.4 4.2 2.3 4.2 5.4"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    coin: '<circle cx="12" cy="12" r="8.5"/><path d="M8.2 10h7.6M8.2 13.5h7.6"/>',
    link: '<path d="M9 15l6-6"/><path d="M13 5.5 15 3.5a3.5 3.5 0 0 1 5 5L18 10.5"/><path d="M11 18.5 9 20.5a3.5 3.5 0 0 1-5-5L6 13.5"/>',
    chart: '<path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-7"/><path d="M3 20h18"/>',
    more: '<circle cx="12" cy="5" r="1.1" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.1" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.1" fill="currentColor" stroke="none"/>',
  },

  svg(name, size = 20) {
    const inner = this.paths[name] || '';
    return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">${inner}</svg>`;
  },
};

/* ===== Toast 알림 ===== */
const Toast = {
  container: null,

  init() {
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.className = 'toast-container';
      document.body.appendChild(this.container);
    }
  },

  show(message, type = 'success', duration = 3200) {
    this.init();
    const iconName = { success: 'check', error: 'x', warning: 'alert' }[type] || 'check';
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <span class="toast-icon">${Icon.svg(iconName, 13)}</span>
      <span class="toast-message">${Utils.escapeHtml(message)}</span>
    `;
    this.container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(24px)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 250);
    }, duration);
  },
};

/* ===== 유틸리티 함수 ===== */
const Utils = {
  formatCurrency(amount) {
    return Number(amount || 0).toLocaleString('ko-KR');
  },

  formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric', month: '2-digit', day: '2-digit',
    });
  },

  formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit',
    });
  },

  pointTypeLabel(type) {
    const labels = {
      CHARGE: '충전', DEPOSIT_LOCK: '보증금 예치',
      DEPOSIT_REFUND: '보증금 환불', PENALTY: '보증금 몰수',
      REWARD: '보상', WITHDRAW: '출금',
    };
    return labels[type] || type;
  },

  challengeStatusLabel(status) {
    const labels = { RECRUITING: '모집중', ONGOING: '진행중', CLOSED: '종료' };
    return labels[status] || status;
  },

  participationStatusLabel(status) {
    const labels = { JOINED: '참여중', SUCCESS: '성공', FAILED: '실패', CANCLED: '취소됨' };
    return labels[status] || status;
  },

  statusBadgeClass(status) {
    const classes = {
      RECRUITING: 'badge-primary', ONGOING: 'badge-success', CLOSED: 'badge-gray',
      SUCCESS: 'badge-success', FAILED: 'badge-danger', JOINED: 'badge-primary',
      CANCLED: 'badge-gray',
    };
    return classes[status] || 'badge-gray';
  },

  pointTypeIcon(type) {
    const icons = {
      CHARGE: 'coin', DEPOSIT_LOCK: 'lock', DEPOSIT_REFUND: 'coin',
      PENALTY: 'alert', REWARD: 'trophy', WITHDRAW: 'wallet',
    };
    return icons[type] || 'coin';
  },

  escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  },

  initial(name) {
    return (name || 'U').trim().charAt(0).toUpperCase();
  },
};
