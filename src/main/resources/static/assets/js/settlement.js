/* ===== 정산 ===== */

const Settlement = {
  async executeSettle(challengeId) {
    if (!confirm('정산을 실행하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;

    try {
      await API.post(`/settlements/settle/${challengeId}`, {});
      Toast.show('정산이 완료되었습니다!', 'success');
      window.location.hash = `#/settlement/${challengeId}`;
    } catch (error) {
      Toast.show(error.message || '정산에 실패했습니다.', 'error');
    }
  },

  async renderResult(main, challengeId) {
    main.innerHTML = `<div class="container"><div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div></div>`;

    try {
      const res = await API.get(`/settlements/result/${challengeId}`);
      const data = res.data;

      main.innerHTML = `
        <div class="container animate-fadeIn">
          <a href="#/challenges/${challengeId}" class="back-link">${Icon.svg('arrowLeft', 15)} 챌린지로 돌아가기</a>

          <h1 class="section-title mb-6">정산 결과</h1>

          <div class="settlement-result-card animate-slideUp">
            <div class="settlement-icon">${Icon.svg('check', 28)}</div>
            <h2 style="font-size:1.25rem;font-weight:800;margin-bottom:var(--space-2);color:var(--gray-900);">정산이 완료되었습니다</h2>
            <p class="text-muted" style="font-size:0.9rem;">챌린지 #${challengeId}의 정산 결과입니다</p>

            <div class="settlement-stats">
              <div class="settlement-stat">
                <div class="settlement-stat-value text-primary">${Utils.formatCurrency(data.amount)}</div>
                <div class="settlement-stat-label">정산 금액</div>
              </div>
              <div class="settlement-stat">
                <div class="settlement-stat-value text-success">${Utils.formatCurrency(data.balanceAfter)}</div>
                <div class="settlement-stat-label">현재 잔액</div>
              </div>
            </div>

            <div class="divider"></div>

            <div>
              <div class="settlement-detail-row"><span class="text-muted">챌린지 ID</span><span class="font-bold">#${data.challengeId}</span></div>
              <div class="settlement-detail-row"><span class="text-muted">정산 금액</span><span class="font-bold text-primary">${Utils.formatCurrency(data.amount)}P</span></div>
              <div class="settlement-detail-row"><span class="text-muted">현재 잔액</span><span class="font-bold text-success">${Utils.formatCurrency(data.balanceAfter)}P</span></div>
            </div>
          </div>

          <div class="flex gap-4 mt-6 justify-center">
            <a href="#/points" class="btn btn-primary btn-lg">포인트 이력 확인</a>
            <a href="#/challenges" class="btn btn-secondary btn-lg">챌린지 목록</a>
          </div>
        </div>
      `;
    } catch (error) {
      main.innerHTML = `
        <div class="container animate-fadeIn">
          <a href="#/challenges" class="back-link">${Icon.svg('arrowLeft', 15)} 챌린지 목록</a>
          <div class="empty-state">
            <div class="empty-state-icon">${Icon.svg('alert', 24)}</div>
            <p class="empty-state-text">정산 결과를 불러오지 못했습니다</p>
            <p class="empty-state-sub">${Utils.escapeHtml(error.message || '')}</p>
            <a href="#/challenges" class="btn btn-primary mt-4">챌린지 목록으로</a>
          </div>
        </div>
      `;
    }
  },
};
