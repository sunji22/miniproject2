/* ===== 포인트 관리 ===== */

const Point = {
  async renderPage(main) {
    main.innerHTML = `
      <div class="container animate-fadeIn">
        <h1 class="section-title mb-6">포인트 관리</h1>

        <div class="point-balance-card animate-slideUp">
          <div class="point-balance-label">현재 포인트 잔액</div>
          <div class="point-balance-value" id="point-balance">
            <div class="skeleton" style="width:140px;height:2.4rem;margin:0 auto;background:rgba(255,255,255,0.18);"></div>
          </div>
        </div>

        <div class="point-actions animate-slideUp" style="animation-delay:0.08s;">
          <div class="point-action-card" onclick="Point.showChargeModal()">
            <div class="point-action-icon charge">${Icon.svg('coin', 22)}</div>
            <h3>포인트 충전</h3>
            <p>계좌에서 포인트를 충전합니다</p>
          </div>
          <div class="point-action-card" onclick="Point.showWithdrawModal()">
            <div class="point-action-icon withdraw">${Icon.svg('wallet', 22)}</div>
            <h3>포인트 출금</h3>
            <p>포인트를 계좌로 출금합니다</p>
          </div>
        </div>

        <div class="card animate-slideUp" style="animation-delay:0.14s;">
          <div class="card-header"><h2 class="card-title">포인트 이력</h2></div>
          <div class="card-body" style="padding:var(--space-3) var(--space-4);">
            <div id="point-history" class="history-list">
              <div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div>
            </div>
          </div>
        </div>
      </div>

      <div id="point-modal"></div>
    `;

    this.loadBalance();
    this.loadHistory();
  },

  async loadBalance() {
    const el = document.getElementById('point-balance');
    try {
      const res = await API.get('/points/balance');
      if (el) el.innerHTML = `${Utils.formatCurrency(res.data.balance)}<span class="unit">P</span>`;
    } catch (e) {
      if (el) el.textContent = '0';
    }
  },

  async loadHistory() {
    const container = document.getElementById('point-history');
    if (!container) return;
    try {
      const res = await API.get('/points/history');
      const history = res.data || [];

      if (history.length === 0) {
        container.innerHTML = `
          <div class="empty-state" style="padding:var(--space-8);">
            <div class="empty-state-icon">${Icon.svg('wallet', 22)}</div>
            <p class="empty-state-text">포인트 이력이 없습니다</p>
            <p class="empty-state-sub">포인트를 충전하고 챌린지에 참여해보세요</p>
          </div>
        `;
        return;
      }

      container.innerHTML = history.map((h, i) => {
        const typeClass = h.type.toLowerCase();
        const isPositive = ['CHARGE', 'DEPOSIT_REFUND', 'REWARD'].includes(h.type);
        return `
          <div class="history-item animate-slideUp" style="animation-delay:${Math.min(i * 0.03, 0.3)}s">
            <div class="history-icon ${typeClass}">${Icon.svg(Utils.pointTypeIcon(h.type), 17)}</div>
            <div class="history-info">
              <div class="history-type">${Utils.pointTypeLabel(h.type)}</div>
              <div class="history-date">${Utils.formatDateTime(h.createdAt)}</div>
            </div>
            <div>
              <div class="history-amount ${isPositive ? 'positive' : 'negative'}">${isPositive ? '+' : '-'}${Utils.formatCurrency(h.amount)}P</div>
              <div class="history-balance">잔액 ${Utils.formatCurrency(h.balanceAfter)}P</div>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) {
      container.innerHTML = `<div class="empty-state"><p class="text-danger">이력을 불러오지 못했습니다</p></div>`;
    }
  },

  showChargeModal() {
    const modal = document.getElementById('point-modal');
    modal.innerHTML = `
      <div class="modal-overlay" onclick="Point.closeModal(event)">
        <div class="modal" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">포인트 충전</h2>
            <button class="modal-close" onclick="Point.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            <form id="charge-form">
              <div class="form-group">
                <label class="form-label">충전 금액</label>
                <input type="number" class="form-input" id="charge-amount" placeholder="충전할 금액을 입력하세요" min="100" required>
                <p class="form-hint">최소 100P 이상 충전 가능합니다</p>
              </div>
              <div class="chip-row">
                <button type="button" class="chip" onclick="document.getElementById('charge-amount').value=10000">10,000P</button>
                <button type="button" class="chip" onclick="document.getElementById('charge-amount').value=30000">30,000P</button>
                <button type="button" class="chip" onclick="document.getElementById('charge-amount').value=50000">50,000P</button>
                <button type="button" class="chip" onclick="document.getElementById('charge-amount').value=100000">100,000P</button>
              </div>
              <div class="modal-footer" style="padding:0;">
                <button type="button" class="btn btn-secondary" onclick="Point.closeModal()">취소</button>
                <button type="submit" class="btn btn-primary" id="charge-btn">충전하기</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;

    document.getElementById('charge-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleCharge();
    });
  },

  async handleCharge() {
    const amount = parseInt(document.getElementById('charge-amount').value, 10);
    const btn = document.getElementById('charge-btn');

    if (!amount || amount < 100) {
      Toast.show('최소 충전 금액은 100P입니다.', 'error');
      return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 충전 중...';

    try {
      await API.post('/points/charge', { amount });
      Toast.show(`${Utils.formatCurrency(amount)}P가 충전되었습니다!`, 'success');
      this.closeModal();
      this.loadBalance();
      this.loadHistory();
    } catch (error) {
      Toast.show(error.message || '충전에 실패했습니다.', 'error');
      btn.disabled = false;
      btn.textContent = '충전하기';
    }
  },

  showWithdrawModal() {
    const modal = document.getElementById('point-modal');
    modal.innerHTML = `
      <div class="modal-overlay" onclick="Point.closeModal(event)">
        <div class="modal" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">포인트 출금</h2>
            <button class="modal-close" onclick="Point.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            <form id="withdraw-form">
              <div class="form-group">
                <label class="form-label">출금 금액</label>
                <input type="number" class="form-input" id="withdraw-amount" placeholder="출금할 금액을 입력하세요" min="100" required>
                <p class="form-hint">보유 잔액 내에서 출금 가능합니다</p>
              </div>
              <div class="modal-footer" style="padding:0;">
                <button type="button" class="btn btn-secondary" onclick="Point.closeModal()">취소</button>
                <button type="submit" class="btn btn-danger" id="withdraw-btn">출금하기</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;

    document.getElementById('withdraw-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleWithdraw();
    });
  },

  async handleWithdraw() {
    const amount = parseInt(document.getElementById('withdraw-amount').value, 10);
    const btn = document.getElementById('withdraw-btn');

    if (!amount || amount < 100) {
      Toast.show('최소 출금 금액은 100P입니다.', 'error');
      return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 출금 중...';

    try {
      await API.post('/points/withdraw', { amount });
      Toast.show(`${Utils.formatCurrency(amount)}P가 출금되었습니다!`, 'success');
      this.closeModal();
      this.loadBalance();
      this.loadHistory();
    } catch (error) {
      Toast.show(error.message || '잔액이 부족합니다.', 'error');
      btn.disabled = false;
      btn.textContent = '출금하기';
    }
  },

  closeModal(e) {
    if (e && e.target !== e.currentTarget) return;
    document.getElementById('point-modal').innerHTML = '';
  },
};
