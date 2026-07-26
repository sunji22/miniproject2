/* ===== 메인 라우터 & 레이아웃 ===== */

const App = {
  app: null,
  currentRoute: null,

  init() {
    this.app = document.getElementById('app');
    window.addEventListener('hashchange', () => this.route());
    this.route();
  },

  route() {
    const hash = window.location.hash || '#/';
    const path = hash.slice(1);

    const publicPages = ['/login', '/register'];
    const isPublic = publicPages.some(p => path.startsWith(p));

    if (!isPublic && !API.isLoggedIn()) {
      window.location.hash = '#/login';
      return;
    }

    this.currentRoute = path;

    if (path === '/login') {
      Auth.renderLogin();
    } else if (path === '/register') {
      Auth.renderRegister();
    } else if (path === '/' || path === '') {
      this.renderShell('/', main => this.renderDashboard(main));
    } else if (path === '/challenges') {
      this.renderShell(path, main => Challenge.renderList(main));
    } else if (path.startsWith('/challenges/')) {
      const id = path.split('/')[2];
      this.renderShell(path, main => Challenge.renderDetail(main, id));
    } else if (path === '/points') {
      this.renderShell(path, main => Point.renderPage(main));
    } else if (path.startsWith('/settlement/')) {
      const id = path.split('/')[2];
      this.renderShell(path, main => Settlement.renderResult(main, id));
    } else {
      this.renderShell('/', main => this.renderDashboard(main));
    }
  },

  navItems(path) {
    const isChallenge = path === '/challenges' || path.startsWith('/challenges/');
    const isPoints = path === '/points';
    const isHome = path === '/';
    return [
      { href: '#/', label: '대시보드', icon: 'home', active: isHome, section: '메인' },
      { href: '#/challenges', label: '챌린지 목록', icon: 'trophy', active: isChallenge, section: '챌린지' },
      { href: '#/points', label: '포인트 관리', icon: 'wallet', active: isPoints, section: '포인트' },
    ];
  },

  // 사이드바 + 헤더가 있는 레이아웃을 그리고, main-content 엘리먼트를 콜백에 넘겨 페이지를 채우게 한다.
  renderShell(path, fillMain) {
    this.app.innerHTML = '';

    const userInfo = API.getUserInfo();
    const email = userInfo?.email || 'user';
    const displayName = email.split('@')[0];
    const initial = Utils.initial(displayName);

    const mobileHeader = document.createElement('div');
    mobileHeader.className = 'mobile-header';
    mobileHeader.innerHTML = `
      <div class="mobile-logo">
        <span style="width:28px;height:28px;border-radius:8px;background:linear-gradient(135deg,var(--primary),var(--primary-dark));display:flex;align-items:center;justify-content:center;color:#fff;">${Icon.svg('trophy', 15)}</span>
        <span>챌린지</span>
      </div>
      <button class="mobile-menu-btn" onclick="App.toggleSidebar()">${Icon.svg('menu', 18)}</button>
    `;
    this.app.appendChild(mobileHeader);

    const overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    overlay.id = 'sidebar-overlay';
    overlay.onclick = () => this.closeSidebar();
    this.app.appendChild(overlay);

    const items = this.navItems(path);
    let navHtml = '';
    let lastSection = null;
    items.forEach(item => {
      if (item.section !== lastSection) {
        navHtml += `<div class="sidebar-section">${item.section}</div>`;
        lastSection = item.section;
      }
      navHtml += `
        <a href="${item.href}" class="sidebar-link ${item.active ? 'active' : ''}">
          ${Icon.svg(item.icon, 18)}
          <span>${item.label}</span>
        </a>
      `;
    });

    const sidebar = document.createElement('aside');
    sidebar.className = 'sidebar';
    sidebar.id = 'sidebar';
    sidebar.innerHTML = `
      <div class="sidebar-header">
        <a href="#/" class="sidebar-logo">
          <span class="sidebar-logo-icon">${Icon.svg('trophy', 18)}</span>
          <span>챌린지</span>
        </a>
      </div>
      <nav class="sidebar-nav">${navHtml}</nav>
      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-user-avatar">${initial}</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">${Utils.escapeHtml(displayName)}</div>
            <div class="sidebar-user-email">${Utils.escapeHtml(email)}</div>
          </div>
          <button class="sidebar-logout-btn" onclick="Auth.logout()" title="로그아웃">${Icon.svg('logout', 17)}</button>
        </div>
      </div>
    `;
    this.app.appendChild(sidebar);

    const mainArea = document.createElement('div');
    mainArea.className = 'main-area';
    this.app.appendChild(mainArea);

    const mainContent = document.createElement('main');
    mainContent.className = 'main-content';
    mainContent.id = 'main-content';
    mainArea.appendChild(mainContent);

    fillMain(mainContent);
  },

  toggleSidebar() {
    document.getElementById('sidebar')?.classList.toggle('open');
    document.getElementById('sidebar-overlay')?.classList.toggle('open');
  },

  closeSidebar() {
    document.getElementById('sidebar')?.classList.remove('open');
    document.getElementById('sidebar-overlay')?.classList.remove('open');
  },

  async renderDashboard(main) {
    const userInfo = API.getUserInfo();
    const displayName = (userInfo?.email || 'user').split('@')[0];

    main.innerHTML = `
      <div class="container">
        <div class="dashboard-welcome animate-fadeIn">
          <h2>${Utils.escapeHtml(displayName)}님, 환영합니다</h2>
          <p>오늘도 목표를 향해 도전해보세요</p>
        </div>

        <div class="dashboard-balance-row animate-slideUp">
          <div class="dashboard-balance-card">
            <div class="dashboard-balance-label">현재 포인트 잔액</div>
            <div class="dashboard-balance-value" id="dash-balance">
              <div class="skeleton" style="width:100px;height:2.2rem;"></div>
            </div>
          </div>
          <div class="dashboard-quick-links">
            <a href="#/challenges" class="dashboard-card">
              <div class="dashboard-card-icon blue">${Icon.svg('trophy', 20)}</div>
              <div class="dashboard-card-info"><h3>챌린지 둘러보기</h3><p>새로운 챌린지에 참여해보세요</p></div>
            </a>
            <a href="#/points" class="dashboard-card">
              <div class="dashboard-card-icon green">${Icon.svg('wallet', 20)}</div>
              <div class="dashboard-card-info"><h3>포인트 관리</h3><p>충전, 출금, 이력 조회</p></div>
            </a>
          </div>
        </div>

        <div class="card animate-slideUp" style="animation-delay:0.08s">
          <div class="card-header flex justify-between items-center">
            <h2 class="card-title">내가 참여 중인 챌린지</h2>
            <a href="#/challenges" class="btn btn-ghost btn-sm">전체 챌린지 ${Icon.svg('arrowLeft', 14)}</a>
          </div>
          <div class="card-body" style="padding:var(--space-3) var(--space-4);">
            <div id="dash-my-challenges">
              <div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div>
            </div>
          </div>
        </div>
      </div>
    `;

    try {
      const res = await API.get('/points/balance');
      const el = document.getElementById('dash-balance');
      if (el) el.innerHTML = `${Utils.formatCurrency(res.data.balance)}<span class="unit">P</span>`;
    } catch (e) {
      const el = document.getElementById('dash-balance');
      if (el) el.textContent = '0';
    }

    try {
      const res = await API.get('/challenges/my/participations');
      const list = (res.data || []).filter(p => p.myStatus !== 'CANCLED');
      const box = document.getElementById('dash-my-challenges');
      if (!box) return;

      if (list.length === 0) {
        box.innerHTML = `
          <div class="empty-state" style="padding:var(--space-8);">
            <div class="empty-state-icon">${Icon.svg('trophy', 24)}</div>
            <p class="empty-state-text">아직 참여 중인 챌린지가 없습니다</p>
            <p class="empty-state-sub">챌린지에 참여하고 목표를 달성해보세요</p>
          </div>
        `;
        return;
      }

      box.innerHTML = list.slice(0, 6).map(p => `
        <a class="my-challenge-row" href="#/challenges/${p.challengeId}">
          <div class="dashboard-card-icon blue" style="width:38px;height:38px;">${Icon.svg('trophy', 16)}</div>
          <div class="flex-1">
            <div class="my-challenge-title">${Utils.escapeHtml(p.challengeTitle)}</div>
            <div class="my-challenge-meta">인정 ${p.successCount}회 &middot; ${Utils.challengeStatusLabel(p.challengeStatus)}</div>
          </div>
          <span class="badge ${Utils.statusBadgeClass(p.myStatus)}">${Utils.participationStatusLabel(p.myStatus)}</span>
        </a>
      `).join('');
    } catch (e) {
      const box = document.getElementById('dash-my-challenges');
      if (box) box.innerHTML = `<p class="text-danger" style="padding:var(--space-4);">참여 목록을 불러오지 못했습니다.</p>`;
    }
  },
};

document.addEventListener('DOMContentLoaded', () => App.init());
