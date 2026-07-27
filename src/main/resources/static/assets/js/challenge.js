/* ===== 챌린지 ===== */

const Challenge = {
  currentFilter: null,
  searchQuery: '',
  searchTimeout: null,

  async renderList(main) {
    main.innerHTML = `
      <div class="container animate-fadeIn">
        <div class="page-header">
          <h1 class="section-title">챌린지 목록</h1>
          <div class="flex gap-3 items-center flex-wrap">
            <div class="search-bar">
              <span class="search-bar-icon">${Icon.svg('search', 16)}</span>
              <input type="text" id="challenge-search" placeholder="챌린지 검색..." value="${Utils.escapeHtml(this.searchQuery)}">
            </div>
            <button class="btn btn-primary" onclick="Challenge.showFormModal()">${Icon.svg('plus', 16)} 개설</button>
          </div>
        </div>

        <div class="challenge-filters">
          <div class="tabs" id="challenge-tabs">
            <button class="tab ${!this.currentFilter ? 'active' : ''}" data-status="">전체</button>
            <button class="tab ${this.currentFilter === 'RECRUITING' ? 'active' : ''}" data-status="RECRUITING">모집중</button>
            <button class="tab ${this.currentFilter === 'ONGOING' ? 'active' : ''}" data-status="ONGOING">진행중</button>
            <button class="tab ${this.currentFilter === 'CLOSED' ? 'active' : ''}" data-status="CLOSED">종료</button>
          </div>
        </div>

        <div id="challenge-list" class="challenge-grid"></div>
      </div>

      <div id="challenge-modal"></div>
    `;

    document.getElementById('challenge-search').addEventListener('input', (e) => {
      clearTimeout(this.searchTimeout);
      this.searchTimeout = setTimeout(() => {
        this.searchQuery = e.target.value.trim();
        this.loadChallenges();
      }, 300);
    });

    document.querySelectorAll('#challenge-tabs .tab').forEach(tab => {
      tab.addEventListener('click', () => {
        document.querySelectorAll('#challenge-tabs .tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        this.currentFilter = tab.dataset.status || null;
        this.loadChallenges();
      });
    });

    this.loadChallenges();
  },

  async loadChallenges() {
    const list = document.getElementById('challenge-list');
    if (!list) return;

    list.innerHTML = Array.from({ length: 3 }).map(() =>
      `<div style="grid-column:span 1;"><div class="skeleton" style="height:220px;border-radius:var(--radius-xl);"></div></div>`
    ).join('');

    try {
      let url = '/challenges?page=0&size=100';
      if (this.currentFilter) url += `&status=${this.currentFilter}`;
      if (this.searchQuery) url += `&title=${encodeURIComponent(this.searchQuery)}`;
      const res = await API.get(url);
      const challenges = res.data || [];

      if (challenges.length === 0) {
        list.innerHTML = `
          <div class="empty-state" style="grid-column:1/-1;">
            <div class="empty-state-icon">${Icon.svg('trophy', 24)}</div>
            <p class="empty-state-text">${this.searchQuery ? '검색 결과가 없습니다' : '등록된 챌린지가 없습니다'}</p>
            ${!this.searchQuery ? '<p class="empty-state-sub">새로운 챌린지를 개설해보세요</p>' : ''}
          </div>
        `;
        return;
      }

      list.innerHTML = challenges.map((c, i) => `
        <div class="challenge-card animate-slideUp" style="animation-delay:${Math.min(i * 0.04, 0.3)}s" onclick="window.location.hash='#/challenges/${c.id}'">
          <div class="challenge-card-top">
            <span class="badge ${Utils.statusBadgeClass(c.status)}">${Utils.challengeStatusLabel(c.status)}</span>
          </div>
          <div class="challenge-card-body">
            <div class="challenge-card-title">${Utils.escapeHtml(c.title)}</div>
            <div class="challenge-card-desc">${Utils.escapeHtml(c.description || '')}</div>
            <div class="challenge-card-meta">
              <span>${Icon.svg('calendar', 14)} ${Utils.formatDate(c.startDate)} ~ ${Utils.formatDate(c.endDate)}</span>
            </div>
          </div>
          <div class="challenge-card-footer">
            <span class="challenge-deposit">${Utils.formatCurrency(c.depositAmount)}P</span>
            <span style="font-size:0.78rem;color:var(--gray-500);">인정 ${c.requiredCount}회</span>
          </div>
        </div>
      `).join('');
    } catch (e) {
      list.innerHTML = `<div class="empty-state" style="grid-column:1/-1;"><p class="text-danger">챌린지를 불러오지 못했습니다</p></div>`;
    }
  },

  async renderDetail(main, id) {
    main.innerHTML = `<div class="container"><div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div></div>`;

    try {
      const [challengeRes, participantsRes] = await Promise.all([
        API.get(`/challenges/${id}`),
        API.get(`/challenges/${id}/participations`).catch(() => ({ data: [] })),
      ]);

      const c = challengeRes.data;
      const participants = participantsRes.data || [];
      const canJoin = c.status === 'RECRUITING' && !c.participationId;
      const canCancel = c.status === 'RECRUITING' && !!c.participationId;
      const canEdit = c.status === 'RECRUITING';
      const canDelete = c.status !== 'ONGOING';

      main.innerHTML = `
        <div class="container animate-fadeIn">
          <a href="#/challenges" class="back-link">${Icon.svg('arrowLeft', 15)} 목록으로</a>

          <div class="challenge-detail-header">
            <div class="flex justify-between items-start mb-4">
              <span class="badge ${Utils.statusBadgeClass(c.status)}" style="font-size:0.8125rem;padding:5px 14px;">${Utils.challengeStatusLabel(c.status)}</span>
              <div style="position:relative;">
                <button class="icon-btn" onclick="Challenge.toggleMenu(${c.id})" id="challenge-menu-btn">${Icon.svg('more', 18)}</button>
                <div id="challenge-menu-mount"></div>
              </div>
            </div>
            <h1 class="challenge-detail-title">${Utils.escapeHtml(c.title)}</h1>
            <p class="challenge-detail-desc">${Utils.escapeHtml(c.description || '설명이 없습니다.')}</p>

            <div class="challenge-detail-info">
              <div class="challenge-info-item"><div class="label">보증금</div><div class="value text-primary">${Utils.formatCurrency(c.depositAmount)}P</div></div>
              <div class="challenge-info-item"><div class="label">인정 횟수</div><div class="value">${c.requiredCount}회</div></div>
              <div class="challenge-info-item"><div class="label">시작일</div><div class="value">${Utils.formatDate(c.startDate)}</div></div>
              <div class="challenge-info-item"><div class="label">종료일</div><div class="value">${Utils.formatDate(c.endDate)}</div></div>
            </div>
          </div>

          <div class="challenge-actions">
            ${canJoin ? `<button class="btn btn-primary btn-lg" onclick="Challenge.join(${c.id})">${Icon.svg('plus', 16)} 참여하기</button>` : ''}
            ${canCancel ? `<button class="btn btn-outline btn-lg" onclick="Challenge.cancel(${c.participationId}, ${c.id})">${Icon.svg('x', 16)} 참여 취소</button>` : ''}
            <div class="challenge-actions-right">
              <button class="btn btn-success" onclick="Settlement.showPreviewModal(${c.id})">${Icon.svg('check', 15)} 정산 미리보기</button>
              <button class="btn btn-secondary" onclick="window.location.hash='#/settlement/${c.id}'">${Icon.svg('chart', 15)} 정산 결과</button>
            </div>
          </div>

          <div class="card mt-8">
            <div class="card-header">
              <h2 class="card-title">참여자 (${participants.length}명)</h2>
            </div>
            <div class="card-body">
              ${participants.length > 0 ? `
                <div class="participant-list">
                  ${participants.map(p => `
                    <div class="participant-item">
                      <div class="avatar" style="width:38px;height:38px;font-size:0.8rem;">${Utils.initial(p.userName)}</div>
                      <div class="participant-info">
                        <div class="participant-name">${Utils.escapeHtml(p.userName || '알 수 없음')}</div>
                        <div class="participant-meta">인정 ${p.successCount || 0}회</div>
                      </div>
                      <span class="badge ${Utils.statusBadgeClass(p.status)}">${Utils.participationStatusLabel(p.status)}</span>
                    </div>
                  `).join('')}
                </div>
              ` : `<p class="text-muted" style="font-size:0.875rem;">아직 참여자가 없습니다.</p>`}
            </div>
          </div>

          <div id="verification-section"></div>
        </div>

        <div id="challenge-modal"></div>
        <div id="settlement-modal"></div>
      `;

      this._current = c;
      this._canEdit = canEdit;
      this._canDelete = canDelete;

      Verification.renderFeed(c.id);
    } catch (e) {
      main.innerHTML = `
        <div class="container animate-fadeIn">
          <a href="#/challenges" class="back-link">${Icon.svg('arrowLeft', 15)} 목록으로</a>
          <div class="empty-state">
            <div class="empty-state-icon">${Icon.svg('alert', 24)}</div>
            <p class="empty-state-text">챌린지를 불러오지 못했습니다</p>
            <p class="empty-state-sub">${Utils.escapeHtml(e.message || '')}</p>
          </div>
        </div>
      `;
    }
  },

  toggleMenu(challengeId) {
    const mount = document.getElementById('challenge-menu-mount');
    if (!mount) return;

    if (mount.innerHTML.trim()) {
      mount.innerHTML = '';
      return;
    }

    mount.innerHTML = `
      <div class="dropdown-menu">
        ${this._canEdit ? `<button class="dropdown-item" onclick="Challenge.showFormModal(${challengeId})">${Icon.svg('edit', 15)} 수정하기</button>` : ''}
        ${this._canDelete ? `<button class="dropdown-item danger" onclick="Challenge.remove(${challengeId})">${Icon.svg('trash', 15)} 삭제하기</button>` : ''}
        ${!this._canEdit && !this._canDelete ? `<div style="padding:9px 12px;font-size:0.8rem;color:var(--gray-400);">진행 중인 챌린지입니다</div>` : ''}
      </div>
    `;

    const closeOnce = (e) => {
      if (!mount.contains(e.target) && e.target.id !== 'challenge-menu-btn') {
        mount.innerHTML = '';
        document.removeEventListener('click', closeOnce, true);
      }
    };
    setTimeout(() => document.addEventListener('click', closeOnce, true), 0);
  },

  showFormModal(challengeId) {
    const editing = !!challengeId;
    const c = editing ? this._current : null;
    const modal = document.getElementById('challenge-modal');
    const today = new Date().toISOString().split('T')[0];

    modal.innerHTML = `
      <div class="modal-overlay" onclick="Challenge.closeModal(event)">
        <div class="modal" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">${editing ? '챌린지 수정' : '챌린지 개설'}</h2>
            <button class="modal-close" onclick="Challenge.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            <form id="challenge-form">
              <div class="form-group">
                <label class="form-label">챌린지 제목 *</label>
                <input type="text" class="form-input" id="ch-title" placeholder="예: 30일 운동 챌린지" value="${editing ? Utils.escapeHtml(c.title) : ''}" required>
              </div>
              <div class="form-group">
                <label class="form-label">설명</label>
                <textarea class="form-input" id="ch-desc" placeholder="챌린지 설명을 입력하세요" rows="3" style="resize:vertical;">${editing ? Utils.escapeHtml(c.description || '') : ''}</textarea>
              </div>
              <div class="flex gap-4">
                <div class="form-group flex-1">
                  <label class="form-label">보증금 (P) *</label>
                  <input type="number" class="form-input" id="ch-deposit" placeholder="5000" min="100" value="${editing ? c.depositAmount : ''}" required>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">인정 횟수 *</label>
                  <input type="number" class="form-input" id="ch-required" placeholder="20" min="1" value="${editing ? c.requiredCount : ''}" required>
                </div>
              </div>
              <div class="flex gap-4">
                <div class="form-group flex-1">
                  <label class="form-label">시작일 *</label>
                  <input type="date" class="form-input" id="ch-start" min="${today}" value="${editing ? c.startDate : ''}" required>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">종료일 *</label>
                  <input type="date" class="form-input" id="ch-end" min="${today}" value="${editing ? c.endDate : ''}" required>
                </div>
              </div>
              <div class="modal-footer" style="padding:0;margin-top:var(--space-4);">
                <button type="button" class="btn btn-secondary" onclick="Challenge.closeModal()">취소</button>
                <button type="submit" class="btn btn-primary" id="ch-submit-btn">${editing ? '수정하기' : '개설하기'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;

    document.getElementById('challenge-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleSubmitForm(editing ? challengeId : null);
    });
  },

  closeModal(e) {
    if (e && e.target !== e.currentTarget) return;
    document.getElementById('challenge-modal').innerHTML = '';
  },

  async handleSubmitForm(challengeId) {
    const btn = document.getElementById('ch-submit-btn');
    const editing = !!challengeId;
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span>';

    const body = {
      title: document.getElementById('ch-title').value.trim(),
      description: document.getElementById('ch-desc').value.trim(),
      depositAmount: parseInt(document.getElementById('ch-deposit').value, 10),
      requiredCount: parseInt(document.getElementById('ch-required').value, 10),
      startDate: document.getElementById('ch-start').value,
      endDate: document.getElementById('ch-end').value,
    };
    if (editing) body.id = challengeId;

    if (body.startDate > body.endDate) {
      Toast.show('종료일은 시작일 이후여야 합니다.', 'error');
      btn.disabled = false;
      btn.textContent = editing ? '수정하기' : '개설하기';
      return;
    }

    try {
      if (editing) {
        await API.put('/challenges', body);
        Toast.show('챌린지가 수정되었습니다.', 'success');
        this.closeModal();
        this.renderDetail(document.getElementById('main-content'), challengeId);
      } else {
        const res = await API.post('/challenges', body);
        Toast.show('챌린지가 개설되었습니다!', 'success');
        this.closeModal();
        window.location.hash = `#/challenges/${res.data}`;
      }
    } catch (error) {
      Toast.show(error.message || '요청에 실패했습니다.', 'error');
      btn.disabled = false;
      btn.textContent = editing ? '수정하기' : '개설하기';
    }
  },

  async remove(challengeId) {
    if (!confirm('정말 이 챌린지를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;
    try {
      await API.delete(`/challenges/${challengeId}`);
      Toast.show('챌린지가 삭제되었습니다.', 'success');
      window.location.hash = '#/challenges';
    } catch (error) {
      Toast.show(error.message || '삭제에 실패했습니다.', 'error');
    }
  },

  async join(challengeId) {
    if (!confirm('이 챌린지에 참여하시겠습니까?\n보증금이 예치됩니다.')) return;
    try {
      await API.post(`/challenges/${challengeId}/participations`, {});
      Toast.show('챌린지에 참여했습니다!', 'success');
      this.renderDetail(document.getElementById('main-content'), challengeId);
    } catch (error) {
      let msg = error.message || '참여에 실패했습니다.';
      if (error.status === 400 && !error.message) msg = '잔액이 부족합니다.';
      Toast.show(msg, 'error');
    }
  },

  async cancel(participationId, challengeId) {
    if (!confirm('참여를 취소하시겠습니까?\n예치했던 보증금은 환불됩니다.')) return;
    try {
      const res = await API.delete(`/participations/${participationId}`);
      Toast.show(`참여가 취소되었습니다. ${Utils.formatCurrency(res.data)}P가 환불되었습니다.`, 'success');
      this.renderDetail(document.getElementById('main-content'), challengeId);
    } catch (error) {
      Toast.show(error.message || '참여 취소에 실패했습니다.', 'error');
    }
  },
};
