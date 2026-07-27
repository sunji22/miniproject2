/* ===== 정산 ===== */

const Settlement = {
  async showPreviewModal(challengeId) {
    let data;
    try {
      const res = await API.get(`/settlements/preview/${challengeId}`);
      data = res.data;
    } catch (error) {
      if (error.status === 409) {
        Toast.show('이미 정산이 완료된 챌린지입니다.', 'warning');
        window.location.hash = `#/settlement/${challengeId}`;
      } else {
        Toast.show(error.message || '정산 미리보기를 불러오지 못했습니다.', 'error');
      }
      return;
    }

    const mount = document.getElementById('settlement-modal');
    if (!mount) return;

    const statusBanner = data.settleable
      ? `<div class="badge badge-success" style="font-size:0.8125rem;padding:6px 14px;">${Icon.svg('check', 13)} 지금 정산할 수 있습니다</div>`
      : `<div class="badge badge-warning" style="font-size:0.8125rem;padding:6px 14px;">${Icon.svg('clock', 13)} 아직 정산할 수 없습니다 (종료일 ${Utils.formatDate(data.endDate)})</div>`;

    mount.innerHTML = `
      <div class="modal-overlay" onclick="Settlement.closeModal(event)">
        <div class="modal" style="max-width:600px;" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">정산 미리보기</h2>
            <button class="modal-close" onclick="Settlement.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            ${statusBanner}
            <div class="settlement-stats" style="grid-template-columns:repeat(4, 1fr); margin-top:var(--space-4);">
              <div class="settlement-stat">
                <div class="settlement-stat-value">${data.totalParticipants}</div>
                <div class="settlement-stat-label">총 참여자</div>
              </div>
              <div class="settlement-stat">
                <div class="settlement-stat-value text-success">${data.successCount}</div>
                <div class="settlement-stat-label">성공 예상</div>
              </div>
              <div class="settlement-stat">
                <div class="settlement-stat-value text-danger">${data.failCount}</div>
                <div class="settlement-stat-label">실패 예상</div>
              </div>
              <div class="settlement-stat">
                <div class="settlement-stat-value text-primary">${Utils.formatCurrency(data.rewardPerPerson)}</div>
                <div class="settlement-stat-label">인당 보상(P)</div>
              </div>
            </div>

            <div class="divider"></div>

            <div>
              ${(data.participants || []).map(p => `
                <div class="settlement-detail-row">
                  <span>
                    <span class="font-bold">${Utils.escapeHtml(p.userName)}</span>
                    <span class="text-muted" style="font-size:0.8rem;"> · 인정 ${p.currentSuccessCount}회</span>
                  </span>
                  <span class="flex items-center gap-2">
                    <span class="badge ${p.success ? 'badge-success' : 'badge-danger'}">${p.success ? '성공' : '실패'}</span>
                    <span class="font-bold ${p.success ? 'text-success' : 'text-danger'}">
                      ${p.success ? '+' + Utils.formatCurrency(p.refundAmount + p.rewardAmount) : '-' + Utils.formatCurrency(p.penaltyAmount)}P
                    </span>
                  </span>
                </div>
              `).join('')}
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" onclick="Settlement.closeModal()">닫기</button>
            ${data.host ? `<button type="button" class="btn btn-success" id="settlement-execute-btn" ${data.settleable ? '' : 'disabled'} onclick="Settlement.confirmExecute(${challengeId})">${Icon.svg('check', 15)} 지금 정산하기</button>` : ''}
          </div>
        </div>
      </div>
    `;
  },

  async confirmExecute(challengeId) {
    if (!confirm('정산을 실행하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;

    const btn = document.getElementById('settlement-execute-btn');
    if (btn) { btn.disabled = true; btn.innerHTML = '<span class="loading-spinner"></span> 처리 중...'; }

    try {
      await API.post(`/settlements/settle/${challengeId}`, {});
      Toast.show('정산이 완료되었습니다!', 'success');
      this.closeModal();
      window.location.hash = `#/settlement/${challengeId}`;
    } catch (error) {
      Toast.show(error.message || '정산에 실패했습니다.', 'error');
      if (btn) { btn.disabled = false; btn.innerHTML = `${Icon.svg('check', 15)} 지금 정산하기`; }
    }
  },

  closeModal(e) {
    if (e && e.target !== e.currentTarget) return;
    const mount = document.getElementById('settlement-modal');
    if (mount) mount.innerHTML = '';
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
