/* ===== 인증 (로그인 / 회원가입) ===== */

const Auth = {
  renderLogin() {
    const app = document.getElementById('app');
    app.innerHTML = `
      <div class="auth-page">
        <div class="auth-card animate-scaleIn">
          <div class="auth-header">
            <div class="auth-logo">${Icon.svg('trophy', 24)}</div>
            <div class="auth-title">다시 오셨군요</div>
            <p class="auth-subtitle">로그인하여 챌린지를 이어가세요</p>
          </div>
          <div class="auth-error" id="login-error"></div>
          <form id="login-form">
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input type="email" class="form-input" id="login-email" placeholder="email@example.com" required autofocus>
            </div>
            <div class="form-group">
              <label class="form-label">비밀번호</label>
              <input type="password" class="form-input" id="login-password" placeholder="비밀번호를 입력하세요" required>
            </div>
            <button type="submit" class="btn btn-primary btn-block btn-lg" id="login-btn" style="margin-top:var(--space-2);">로그인</button>
          </form>
          <div class="auth-footer">계정이 없으신가요? <a href="#/register">회원가입</a></div>
        </div>
      </div>
    `;

    document.getElementById('login-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleLogin();
    });
  },

  async handleLogin() {
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('login-btn');
    const errorEl = document.getElementById('login-error');

    errorEl.classList.remove('visible');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 로그인 중...';

    try {
      const res = await API.post('/auth/login', { email, password });
      API.setToken(res.data.accessToken);
      Toast.show('로그인되었습니다.', 'success');
      window.location.hash = '#/';
      setTimeout(() => App.route(), 30);
    } catch (error) {
      let msg = error.message || '로그인에 실패했습니다.';
      if (error.status === 401) msg = '이메일 또는 비밀번호가 올바르지 않습니다.';
      if (error.status === 404) msg = '존재하지 않는 계정입니다.';
      errorEl.textContent = msg;
      errorEl.classList.add('visible');
      btn.disabled = false;
      btn.textContent = '로그인';
    }
  },

  renderRegister() {
    const app = document.getElementById('app');
    app.innerHTML = `
      <div class="auth-page">
        <div class="auth-card animate-scaleIn">
          <div class="auth-header">
            <div class="auth-logo">${Icon.svg('trophy', 24)}</div>
            <div class="auth-title">계정 만들기</div>
            <p class="auth-subtitle">보증금 챌린지를 시작해보세요</p>
          </div>
          <div class="auth-error" id="register-error"></div>
          <form id="register-form">
            <div class="form-group">
              <label class="form-label">이름</label>
              <input type="text" class="form-input" id="register-name" placeholder="이름을 입력하세요" required autofocus>
            </div>
            <div class="form-group">
              <label class="form-label">이메일</label>
              <input type="email" class="form-input" id="register-email" placeholder="email@example.com" required>
            </div>
            <div class="form-group">
              <label class="form-label">비밀번호</label>
              <input type="password" class="form-input" id="register-password" placeholder="8자 이상 입력하세요" minlength="8" required>
            </div>
            <button type="submit" class="btn btn-primary btn-block btn-lg" id="register-btn" style="margin-top:var(--space-2);">회원가입</button>
          </form>
          <div class="auth-footer">이미 계정이 있으신가요? <a href="#/login">로그인</a></div>
        </div>
      </div>
    `;

    document.getElementById('register-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleRegister();
    });
  },

  async handleRegister() {
    const name = document.getElementById('register-name').value.trim();
    const email = document.getElementById('register-email').value.trim();
    const password = document.getElementById('register-password').value;
    const btn = document.getElementById('register-btn');
    const errorEl = document.getElementById('register-error');

    errorEl.classList.remove('visible');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 처리 중...';

    try {
      await API.post('/users', { email, password, name });
      Toast.show('회원가입이 완료되었습니다. 로그인해주세요.', 'success');
      window.location.hash = '#/login';
    } catch (error) {
      let msg = error.message || '회원가입에 실패했습니다.';
      if (error.status === 409 || error.status === 400) msg = error.message || '이미 사용 중인 이메일입니다.';
      errorEl.textContent = msg;
      errorEl.classList.add('visible');
      btn.disabled = false;
      btn.textContent = '회원가입';
    }
  },

  logout() {
    API.removeToken();
    Toast.show('로그아웃되었습니다.', 'success');
    window.location.hash = '#/login';
  },
};
