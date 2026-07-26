/* ===== 인증 게시글 (Verification) ===== */

const Verification = {
  currentChallengeId: null,
  dateFilter: '',
  itemsById: {},

  async renderFeed(challengeId) {
    this.currentChallengeId = challengeId;
    this.dateFilter = '';
    const container = document.getElementById('verification-section');
    if (!container) return;

    container.innerHTML = `
      <div class="card mt-8">
        <div class="card-header flex justify-between items-center flex-wrap gap-3">
          <h2 class="card-title">인증 게시글</h2>
          <div class="verification-toolbar">
            <input type="date" class="form-input" id="verification-date-filter" style="width:160px;">
            <button class="btn btn-outline btn-sm" onclick="Verification.clearDateFilter()">전체보기</button>
            <button class="btn btn-primary btn-sm" onclick="Verification.showCreateModal()">${Icon.svg('plus', 14)} 오늘 인증하기</button>
          </div>
        </div>
        <div class="card-body" style="padding:var(--space-4);">
          <div id="verification-list" class="verification-list">
            <div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div>
          </div>
        </div>
      </div>

      <div id="verification-modal"></div>
    `;

    document.getElementById('verification-date-filter').addEventListener('change', (e) => {
      this.dateFilter = e.target.value;
      this.loadList();
    });

    this.loadList();
  },

  clearDateFilter() {
    this.dateFilter = '';
    const input = document.getElementById('verification-date-filter');
    if (input) input.value = '';
    this.loadList();
  },

  async loadList() {
    const list = document.getElementById('verification-list');
    if (!list) return;
    list.innerHTML = `<div class="loading-overlay"><span class="loading-spinner"></span> 불러오는 중...</div>`;

    try {
      let url = `/verifications?challengeId=${this.currentChallengeId}`;
      if (this.dateFilter) url += `&date=${this.dateFilter}`;
      const res = await API.get(url);
      const items = (res.data || []).slice().sort((a, b) => {
        if (a.verifiedDate !== b.verifiedDate) return a.verifiedDate < b.verifiedDate ? 1 : -1;
        return b.id - a.id;
      });

      this.itemsById = {};
      items.forEach(v => { this.itemsById[v.id] = v; });

      if (items.length === 0) {
        list.innerHTML = `
          <div class="empty-state">
            <div class="empty-state-icon">${Icon.svg('image', 22)}</div>
            <p class="empty-state-text">${this.dateFilter ? '해당 날짜의 인증글이 없습니다' : '등록된 인증글이 없습니다'}</p>
            <p class="empty-state-sub">인증글을 작성해보세요</p>
          </div>
        `;
        return;
      }

      list.innerHTML = items.map((v, i) => this.renderCard(v, i)).join('');
    } catch (e) {
      list.innerHTML = `<div class="empty-state"><p class="text-danger">인증글을 불러오지 못했습니다</p></div>`;
    }
  },

  renderCard(v, i) {
    const imgSafe = Utils.escapeHtml(v.imageUrl);
    const contentSafe = v.content ? Utils.escapeHtml(v.content) : '';
    const progressLabel = v.succeeded ? '인증완료' : `${v.checkCount}/${v.requiredChecks} 검증`;

    return `
      <div class="verification-card animate-slideUp" style="animation-delay:${Math.min(i * 0.03, 0.25)}s">
        <div class="verification-card-header">
          <div class="avatar" style="width:38px;height:38px;font-size:0.8rem;">${Utils.initial(v.userName)}</div>
          <div class="verification-user-info">
            <div class="verification-user-name">
              ${Utils.escapeHtml(v.userName || '알 수 없음')}
              ${v.mine ? '<span class="badge badge-primary">내 게시글</span>' : ''}
            </div>
            <div class="verification-date">${Utils.formatDate(v.verifiedDate)}</div>
          </div>
          <span class="badge ${v.succeeded ? 'badge-success' : 'badge-gray'}">${progressLabel}</span>
        </div>
        ${contentSafe ? `<p class="verification-content">${contentSafe}</p>` : ''}
        <a href="${imgSafe}" target="_blank" rel="noopener noreferrer">
          <img src="${imgSafe}" class="verification-image" alt="인증 이미지" onerror="this.style.display='none'">
        </a>
        <div class="verification-card-footer">
          <span class="verification-time">${Utils.formatDateTime(v.createdAt)}</span>
          <div class="verification-actions">
            ${v.mine ? `<button class="btn btn-outline btn-sm" onclick="Verification.showEditModal(${v.id})">${Icon.svg('edit', 14)} 수정</button>` : ''}
            ${!v.mine && !v.checkedByMe ? `<button class="btn btn-success btn-sm" onclick="Verification.check(${v.id})">${Icon.svg('check', 14)} 교차 검증</button>` : ''}
            ${!v.mine && v.checkedByMe ? `<span class="badge badge-success">${Icon.svg('check', 11)} 검증완료</span>` : ''}
          </div>
        </div>
      </div>
    `;
  },

  showCreateModal() {
    const modal = document.getElementById('verification-modal');
    modal.innerHTML = `
      <div class="modal-overlay" onclick="Verification.closeModal(event)">
        <div class="modal" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">오늘 인증하기</h2>
            <button class="modal-close" onclick="Verification.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            <form id="verification-create-form">
              <div class="form-group">
                <label class="form-label">인증 이미지 URL *</label>
                <input type="url" class="form-input" id="verification-image-url" placeholder="https://..." required>
                <p class="form-hint">인증 사진이 업로드된 이미지 주소를 입력하세요</p>
              </div>
              <div class="form-group">
                <label class="form-label">내용</label>
                <textarea class="form-input" id="verification-content" placeholder="인증 내용을 입력하세요" rows="3" style="resize:vertical;"></textarea>
              </div>
              <div class="modal-footer" style="padding:0;">
                <button type="button" class="btn btn-secondary" onclick="Verification.closeModal()">취소</button>
                <button type="submit" class="btn btn-primary" id="verification-create-btn">인증하기</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;

    document.getElementById('verification-create-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleCreate();
    });
  },

  async handleCreate() {
    const imageUrl = document.getElementById('verification-image-url').value.trim();
    const content = document.getElementById('verification-content').value.trim();
    const btn = document.getElementById('verification-create-btn');

    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 등록 중...';

    try {
      await API.post('/verifications', { challengeId: this.currentChallengeId, imageUrl, content });
      Toast.show('인증글이 등록되었습니다!', 'success');
      this.closeModal();
      this.loadList();
    } catch (error) {
      Toast.show(error.message || '인증글 등록에 실패했습니다.', 'error');
      btn.disabled = false;
      btn.textContent = '인증하기';
    }
  },

  showEditModal(id) {
    const v = this.itemsById[id];
    if (!v) return;

    const modal = document.getElementById('verification-modal');
    modal.innerHTML = `
      <div class="modal-overlay" onclick="Verification.closeModal(event)">
        <div class="modal" onclick="event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">인증글 수정</h2>
            <button class="modal-close" onclick="Verification.closeModal()">${Icon.svg('x', 16)}</button>
          </div>
          <div class="modal-body">
            <form id="verification-edit-form">
              <div class="form-group">
                <label class="form-label">인증 이미지 URL *</label>
                <input type="url" class="form-input" id="verification-edit-image-url" value="${Utils.escapeHtml(v.imageUrl)}" required>
              </div>
              <div class="form-group">
                <label class="form-label">내용</label>
                <textarea class="form-input" id="verification-edit-content" rows="3" style="resize:vertical;">${Utils.escapeHtml(v.content || '')}</textarea>
              </div>
              <div class="modal-footer" style="padding:0;">
                <button type="button" class="btn btn-secondary" onclick="Verification.closeModal()">취소</button>
                <button type="submit" class="btn btn-primary" id="verification-edit-btn">수정하기</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    `;

    document.getElementById('verification-edit-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleUpdate(id);
    });
  },

  async handleUpdate(id) {
    const imageUrl = document.getElementById('verification-edit-image-url').value.trim();
    const content = document.getElementById('verification-edit-content').value.trim();
    const btn = document.getElementById('verification-edit-btn');

    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> 수정 중...';

    try {
      await API.put(`/verifications/${id}`, { imageUrl, content });
      Toast.show('인증글이 수정되었습니다!', 'success');
      this.closeModal();
      this.loadList();
    } catch (error) {
      Toast.show(error.message || '인증글 수정에 실패했습니다.', 'error');
      btn.disabled = false;
      btn.textContent = '수정하기';
    }
  },

  async check(id) {
    try {
      await API.post(`/verifications/${id}/check`, {});
      Toast.show('교차 검증이 완료되었습니다!', 'success');
      this.loadList();
    } catch (error) {
      Toast.show(error.message || '검증에 실패했습니다.', 'error');
    }
  },

  closeModal(e) {
    if (e && e.target !== e.currentTarget) return;
    document.getElementById('verification-modal').innerHTML = '';
  },
};
