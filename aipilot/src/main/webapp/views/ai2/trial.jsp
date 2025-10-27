<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    :root {
        --color-background: #f3f4f8;
        --color-panel: #ffffff;
        --color-border: #e2e8f0;
        --color-ink: #1f2937;
        --color-muted: #6b7280;
        --color-highlight: #b89c6d;
        --shadow-panel: 0 18px 45px rgba(15, 23, 42, 0.12);
        font-family: 'Pretendard', 'Noto Sans KR', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        background-color: var(--color-background);
    }

    .trial-shell {
        width: 100%;
        padding: 28px 16px 40px;
        background: linear-gradient(180deg, rgba(223, 215, 196, 0.45) 0%, rgba(240, 245, 255, 0.9) 60%, rgba(243, 244, 248, 1) 100%);
        border-radius: 28px;
        position: relative;
        overflow: hidden;
    }

    .trial-shell::before {
        content: '';
        position: absolute;
        inset: 0;
        pointer-events: none;
        background: radial-gradient(circle at 16% 8%, rgba(255, 255, 255, 0.9), transparent 55%),
        radial-gradient(circle at 82% 0%, rgba(232, 223, 209, 0.7), transparent 65%);
    }

    .trial-layout {
        position: relative;
        z-index: 1;
        display: grid;
        grid-template-columns: 320px 1fr;
        gap: 24px;
        min-height: 640px;
    }

    .trial-panel {
        background: var(--color-panel);
        border-radius: 22px;
        box-shadow: var(--shadow-panel);
        border: 1px solid var(--color-border);
        overflow: hidden;
    }

    .trial-panel__header {
        padding: 28px 30px 24px;
        background: linear-gradient(145deg, rgba(27, 40, 69, 0.92) 0%, rgba(41, 57, 90, 0.95) 100%);
        color: #fff;
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    }

    .trial-panel__header h2 {
        margin: 0 0 10px;
        font-size: 1.65rem;
        font-weight: 700;
        letter-spacing: -0.01em;
    }

    .trial-panel__header p {
        margin: 0;
        color: rgba(255, 255, 255, 0.78);
        font-size: 0.95rem;
        line-height: 1.5;
    }

    .stage-indicator {
        margin-top: 18px;
        padding: 12px 14px;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.12);
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 0.86rem;
        letter-spacing: 0.04em;
        text-transform: uppercase;
    }

    .stage-indicator::before {
        content: '\2696';
        font-size: 1.1rem;
    }

    .roles-panel {
        display: flex;
        flex-direction: column;
        padding: 20px 22px 26px;
        gap: 18px;
    }

    .roles-panel__intro {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 0.95rem;
        color: var(--color-muted);
        line-height: 1.6;
    }

    .roles-panel__intro strong {
        color: var(--color-ink);
        font-weight: 600;
    }

    .role-grid {
        display: grid;
        gap: 14px;
    }

    .role-card {
        position: relative;
        padding: 16px 18px 18px;
        border-radius: 18px;
        border: 1px solid var(--color-border);
        background: rgba(255, 255, 255, 0.86);
        box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
        cursor: pointer;
        transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
    }

    .role-card::before {
        content: attr(data-icon);
        font-size: 1.4rem;
        display: inline-flex;
        width: 38px;
        height: 38px;
        border-radius: 50%;
        align-items: center;
        justify-content: center;
        background: rgba(27, 40, 69, 0.08);
        margin-bottom: 12px;
    }

    .role-card h3 {
        margin: 0 0 6px;
        font-size: 1.05rem;
        color: var(--color-ink);
    }

    .role-card p {
        margin: 0;
        font-size: 0.85rem;
        color: var(--color-muted);
        line-height: 1.5;
    }

    .role-card.active {
        transform: translateY(-2px);
        border-color: rgba(184, 156, 109, 0.8);
        box-shadow: 0 18px 36px rgba(184, 156, 109, 0.25);
        background: linear-gradient(155deg, rgba(255, 255, 255, 0.95) 0%, rgba(249, 244, 235, 0.9) 100%);
    }

    .role-card.active::after {
        content: '현재 발언자';
        position: absolute;
        top: 16px;
        right: 18px;
        font-size: 0.72rem;
        letter-spacing: 0.04em;
        padding: 4px 10px;
        border-radius: 999px;
        background: rgba(184, 156, 109, 0.14);
        color: #7b5b2b;
    }

    .chat-panel {
        display: flex;
        flex-direction: column;
        height: 100%;
    }

    .chat-panel__header {
        padding: 26px 32px 20px;
        border-bottom: 1px solid var(--color-border);
        background: var(--color-panel);
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .chat-panel__header-top {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
    }

    .chat-title {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 700;
        color: var(--color-ink);
    }

    .case-chip {
        padding: 8px 14px;
        border-radius: 999px;
        border: 1px solid rgba(27, 40, 69, 0.15);
        background: rgba(27, 40, 69, 0.04);
        font-size: 0.82rem;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: #3f4a63;
    }

    .chat-panel__header p {
        margin: 0;
        font-size: 0.92rem;
        color: var(--color-muted);
        line-height: 1.6;
    }

    .chat-area {
        flex: 1;
        padding: 28px 32px 24px;
        background: linear-gradient(180deg, rgba(249, 247, 243, 0.6) 0%, rgba(255, 255, 255, 0.8) 100%);
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 22px;
    }

    .chat-area::-webkit-scrollbar {
        width: 8px;
    }

    .chat-area::-webkit-scrollbar-thumb {
        background: rgba(27, 40, 69, 0.2);
        border-radius: 10px;
    }

    .message {
        display: flex;
        gap: 16px;
        align-items: flex-start;
        --accent: rgba(27, 40, 69, 0.76);
    }

    .message-avatar {
        width: 46px;
        height: 46px;
        border-radius: 14px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        color: #fff;
        background: var(--accent);
        box-shadow: 0 12px 24px rgba(27, 40, 69, 0.25);
    }

    .message-content {
        position: relative;
        max-width: min(72%, 620px);
        padding: 18px 20px 20px;
        border-radius: 18px;
        background: #fff;
        border: 1px solid rgba(31, 41, 55, 0.08);
        box-shadow: 0 18px 32px rgba(15, 23, 42, 0.1);
        transition: transform 0.18s ease, box-shadow 0.18s ease;
        word-wrap: break-word;      /* ← 추가 */
        overflow-wrap: break-word;  /* ← 추가 */
        box-sizing: border-box;     /* ← 추가: 패딩 포함 너비 계산 */
    }

    .message.human .message-content {
        background: #fffdf6;
        border: 1px solid rgba(184, 156, 109, 0.35);
        box-shadow: 0 18px 36px rgba(184, 156, 109, 0.22);
    }

    .message-content::before {
        content: '';
        position: absolute;
        top: 18px;
        left: -12px;
        width: 12px;
        height: 12px;
        background: inherit;
        border-left: inherit;
        border-bottom: inherit;
        transform: rotate(45deg);
    }

    .message-label {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        font-size: 0.8rem;
        font-weight: 600;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        color: var(--accent);
        margin-bottom: 10px;
    }

    .message-label::before {
        content: '';
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: 999px;
        background: var(--accent);
    }

    .message-text {
        margin: 0;
        font-size: 0.98rem;
        line-height: 1.68;
        color: var(--color-ink);
        white-space: pre-wrap;
        word-break: keep-all;
        overflow-wrap: break-word;
        max-width: 100%;
    }

    .message.human .message-text {
        color: #3f321f;
    }

    .message-meta {
        display: block;
        margin-top: 12px;
        font-size: 0.78rem;
        color: var(--color-muted);
    }

    .message.role-ai {
        --accent: #334155;
    }

    .message.role-judge {
        --accent: #7a5c2a;
    }

    .message.role-prosecutor {
        --accent: #be4d2d;
    }

    .message.role-defender {
        --accent: #2d6fb8;
    }

    .message.role-defendant {
        --accent: #0f766e;
    }

    .message.role-witness {
        --accent: #a855f7;
    }

    .message.role-jury {
        --accent: #6366f1;
    }

    .input-panel {
        padding: 22px 28px 26px;
        border-top: 1px solid var(--color-border);
        background: var(--color-panel);
        display: flex;
        flex-direction: column;
        gap: 14px;
    }

    .active-role-chip {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        align-self: flex-start;
        padding: 6px 12px;
        border-radius: 999px;
        border: 1px solid rgba(184, 156, 109, 0.6);
        background: rgba(249, 244, 235, 0.7);
        font-size: 0.8rem;
        letter-spacing: 0.04em;
        text-transform: uppercase;
        color: #7b5b2b;
    }

    .active-role-chip::before {
        content: '\25B6';
        font-size: 0.7rem;
    }

    .input-group {
        display: flex;
        align-items: flex-end;
        gap: 14px;
    }

    .input-field-wrapper {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    label[for="trialInput"] {
        font-size: 0.82rem;
        color: var(--color-muted);
        letter-spacing: 0.02em;
    }

    #trialInput {
        width: 100%;
        min-height: 88px;
        padding: 14px 16px;
        border-radius: 16px;
        border: 1px solid var(--color-border);
        background: rgba(255, 255, 255, 0.95);
        font-size: 1rem;
        line-height: 1.65;
        color: var(--color-ink);
        resize: vertical;
        transition: border-color 0.18s ease, box-shadow 0.18s ease;
    }

    #trialInput:focus {
        outline: none;
        border-color: rgba(184, 156, 109, 0.7);
        box-shadow: 0 0 0 4px rgba(184, 156, 109, 0.16);
    }

    #trialSendBtn {
        padding: 16px 24px;
        border-radius: 16px;
        border: none;
        background: linear-gradient(155deg, #b89c6d 0%, #8a6f3c 100%);
        color: #fff;
        font-weight: 600;
        letter-spacing: 0.02em;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 10px;
        box-shadow: 0 18px 28px rgba(138, 111, 60, 0.28);
        transition: transform 0.15s ease, box-shadow 0.18s ease;
    }

    #trialSendBtn:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 22px 32px rgba(138, 111, 60, 0.32);
    }

    #trialSendBtn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        box-shadow: none;
        transform: none;
    }

    .btn-icon {
        font-size: 1rem;
        transform: translateX(0);
        transition: transform 0.18s ease;
    }

    #trialSendBtn:hover:not(:disabled) .btn-icon {
        transform: translateX(2px);
    }

    .loading {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        color: var(--color-muted);
        font-size: 0.9rem;
    }

    .loading-spinner {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        border: 2px solid rgba(27, 40, 69, 0.18);
        border-top-color: rgba(27, 40, 69, 0.8);
        animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
        to {
            transform: rotate(360deg);
        }
    }

    @media (max-width: 1100px) {
        .trial-layout {
            grid-template-columns: 280px 1fr;
        }
    }

    @media (max-width: 880px) {
        .trial-layout {
            grid-template-columns: 1fr;
        }

        .roles-panel {
            flex-direction: row;
            overflow-x: auto;
        }

        .role-grid {
            display: flex;
            gap: 12px;
        }

        .role-card {
            min-width: 200px;
        }
    }

    @media (max-width: 640px) {
        .trial-shell {
            padding: 22px 12px 28px;
        }

        .chat-panel__header,
        .chat-area,
        .input-panel {
            padding-left: 18px;
            padding-right: 18px;
        }

        .message-content {
            max-width: 100%;
        }

        .input-group {
            flex-direction: column;
            align-items: stretch;
        }

        #trialSendBtn {
            justify-content: center;
            width: 100%;
        }
    }
</style>

<script>
    const API_BASE_URL = '<c:url value="/ai2/api"/>';

    let trial = {
        sending: false,
        sessionId: null,
        currentRoleId: 'defendant',
        isTrialCompleted: false,
        currentCase: null, // ⭐ 신규: 현재 사건 정보

        roles: {
            judge: {
                id: 'judge',
                label: '판사',
                icon: '⚖️',
                summary: '절차 진행과 판결을 담당합니다.',
                promptPrefix: '[재판장]'
            },
            prosecutor: {
                id: 'prosecutor',
                label: '검사',
                icon: '🧾',
                summary: '공소 사실 입증과 증거 제시 역할을 맡습니다.',
                promptPrefix: '[검사]'
            },
            defender: {
                id: 'defender',
                label: '변호인',
                icon: '🛡️',
                summary: '피고인의 주장을 정리하고 반박 전략을 세웁니다.',
                promptPrefix: '[변호인]'
            },
            defendant: {
                id: 'defendant',
                label: '피고인',
                icon: '👤',
                summary: '사건에 대한 입장을 직접 진술합니다.',
                promptPrefix: '[피고인]'
            },
            witness: {
                id: 'witness',
                label: '증인',
                icon: '🗣️',
                summary: '사건과 관련된 사실을 진술합니다.',
                promptPrefix: '[증인]'
            },
            jury: {
                id: 'jury',
                label: '참심위원',
                icon: '👥',
                summary: '배심원 또는 시민 참여자의 의견을 공유합니다.',
                promptPrefix: '[참심위원]'
            },
            ai: {
                id: 'ai',
                label: 'AI 재판부',
                icon: 'AI',
                summary: '',
                promptPrefix: ''
            }
        },

        init: function() {
            this.sessionId = sessionStorage.getItem('trial-session-id');
            if (!this.sessionId) {
                this.sessionId = 'trial-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
                sessionStorage.setItem('trial-session-id', this.sessionId);
            }

            this.bindInputInteractions();
            this.enableRoleSelection(); // this.disableRoleSelection();
            this.updateSendButtonState();
            this.updateRoleChip();

            // ⭐ 세션 정보 조회
            this.loadSessionInfo();
        },

        // ⭐ 신규: 세션 정보 로드
        loadSessionInfo: function() {
            fetch(API_BASE_URL + '/trial-session-info')
                    .then(res => res.json())
                    .then(data => {
                        if (data.hasCase) {
                            // 이미 진행 중인 재판이 있음
                            this.currentCase = {
                                caseNumber: data.caseNumber,
                                defendant: data.defendant,
                                charge: data.charge
                            };
                            this.currentRoleId = data.currentRole || 'defendant';
                            this.enableRoleSelection();
                            this.updateRoleChip();
                            this.showCaseInfo();
                        } else {
                            // 사건 선택 필요
                            this.showCaseSelectionPrompt();
                        }
                    })
                    .catch(err => {
                        console.error('세션 정보 조회 실패:', err);
                        this.showCaseSelectionPrompt();
                    });
        },

        // ⭐ 신규: 사건 선택 안내 메시지
        showCaseSelectionPrompt: function() {
            const chatArea = document.getElementById('trialChatArea');
            const promptDiv = document.createElement('div');
            promptDiv.className = 'alert alert-info text-center';
            promptDiv.style.margin = '20px';
            promptDiv.innerHTML = `
            <h5><i class="fas fa-gavel"></i> 모의 법정에 오신 것을 환영합니다</h5>
            <p>재판을 시작하려면 먼저 사건을 선택해주세요.</p>
            <button class="btn btn-primary" onclick="trial.showCaseSelectionModal()">
                <i class="fas fa-folder-open"></i> 사건 선택하기
            </button>
        `;
            chatArea.appendChild(promptDiv);
        },

        // ⭐ 신규: 사건 선택 모달 표시
        showCaseSelectionModal: function() {
            fetch(API_BASE_URL + '/trial-cases')
                    .then(res => res.json())
                    .then(cases => {
                        if (cases.length === 0) {
                            alert('등록된 사건이 없습니다. Trial_Admin에서 사건을 먼저 등록해주세요.');
                            return;
                        }
                        this.renderCaseSelectionModal(cases);
                    })
                    .catch(err => {
                        console.error('사건 목록 조회 실패:', err);
                        alert('사건 목록을 불러오는 데 실패했습니다.');
                    });
        },

        // ⭐ 신규: 사건 선택 모달 렌더링
        renderCaseSelectionModal: function(cases) {
            // 기존 모달 제거
            const existingModal = document.getElementById('caseSelectionModal');
            if (existingModal) {
                existingModal.remove();
            }

            const modal = document.createElement('div');
            modal.id = 'caseSelectionModal';
            modal.className = 'modal fade show';
            modal.style.display = 'block';
            modal.style.backgroundColor = 'rgba(0,0,0,0.5)';

            let caseListHtml = cases.map(c => {
                const typeLabel = c.caseType === 'criminal'
                        ? '<span class="badge badge-danger">형사</span>'
                        : '<span class="badge badge-primary">민사</span>';

                return '' +
                        '<div class="case-item" onclick="trial.selectCase(' + c.caseId + ')" ' +
                        '     style="padding: 15px; border: 1px solid #ddd; margin-bottom: 10px; cursor: pointer; border-radius: 5px;">' +
                        '    <div style="display: flex; justify-content: space-between; align-items: center;">' +
                        '        <div>' +
                        '            <strong>' + c.caseNumber + '</strong> ' + typeLabel +
                        '            <br><small>피고인: ' + c.defendant + ' | 혐의: ' + c.charge + '</small>' +
                        '        </div>' +
                        '        <button class="btn btn-sm btn-success">' +
                        '            <i class="fas fa-play"></i> 시작' +
                        '        </button>' +
                        '    </div>' +
                        '</div>';
            }).join('');

            modal.innerHTML = '' +
                    '<div class="modal-dialog modal-lg">' +
                    '    <div class="modal-content">' +
                    '        <div class="modal-header">' +
                    '            <h5 class="modal-title"><i class="fas fa-gavel"></i> 재판할 사건 선택</h5>' +
                    '            <button type="button" class="close" onclick="trial.closeCaseSelectionModal()">' +
                    '                <span>&times;</span>' +
                    '            </button>' +
                    '        </div>' +
                    '        <div class="modal-body" style="max-height: 500px; overflow-y: auto;">' +
                    '            ' + caseListHtml +
                    '        </div>' +
                    '        <div class="modal-footer">' +
                    '            <button class="btn btn-secondary" onclick="trial.closeCaseSelectionModal()">' +
                    '                취소' +
                    '            </button>' +
                    '        </div>' +
                    '    </div>' +
                    '</div>';

            document.body.appendChild(modal);
        },

        // ⭐ 신규: 사건 선택
        selectCase: function(caseId) {
            this.closeCaseSelectionModal();

            // 채팅 영역 초기화
            const chatArea = document.getElementById('trialChatArea');
            chatArea.innerHTML = '';

            // 로딩 표시
            const loadingId = this.addLoadingMessage();

            // 재판 시작 요청
            const url = API_BASE_URL + '/trial-start?caseId=' + encodeURIComponent(caseId);
            const eventSource = new EventSource(url);
            let openingMessage = '';

            eventSource.onmessage = (event) => {
                if (event.data === '[DONE]') {
                    eventSource.close();
                    this.removeLoadingMessage(loadingId);

                    // 사건 정보 표시
                    this.loadSessionInfo();
                    this.enableRoleSelection();
                    return;
                }

                openingMessage += event.data;

                if (loadingId) {
                    this.removeLoadingMessage(loadingId);
                    this.addAIMessage(openingMessage);
                }
            };

            eventSource.onerror = () => {
                eventSource.close();
                this.removeLoadingMessage(loadingId);
                this.addAIMessage('재판 시작 중 오류가 발생했습니다.');
            };
        },

        closeCaseSelectionModal: function() {
            const modal = document.getElementById('caseSelectionModal');
            if (modal) {
                modal.remove();
            }
        },

        // ⭐ 신규: 사건 정보 표시
        showCaseInfo: function() {
            if (!this.currentCase) return;

            const infoDiv = document.getElementById('caseInfoDisplay');
            if (infoDiv) {
                infoDiv.innerHTML = '<div class="alert alert-secondary">' +
                        '<strong>현재 재판 중:</strong> ' + this.currentCase.caseNumber + '<br>' +
                        '<small>피고인: ' + this.currentCase.defendant + ' | ' + this.currentCase.charge + '</small>' +
                        '</div>';
            }
        },

        bindInputInteractions: function() {
            const input = document.getElementById('trialInput');
            if (!input) {
                return;
            }

            input.addEventListener('input', () => {
                this.updateSendButtonState();
            });

            input.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault();
                    this.send();
                }
            });
        },

        setupRoleSelection: function() {
            const cards = document.querySelectorAll('.role-card');
            cards.forEach(card => {
                card.addEventListener('click', () => {
                    const roleId = card.getAttribute('data-role');
                    this.setRole(roleId);
                });
            });

            this.setRole(this.currentRoleId);
        },

        // ⭐ 역할 선택 비활성화
        disableRoleSelection: function() {
            const cards = document.querySelectorAll('.role-card');
            cards.forEach(card => {
                card.style.opacity = '0.5';
                card.style.pointerEvents = 'none';
                card.style.cursor = 'not-allowed';
            });

            // 피고인만 활성화 표시
            const defCard = document.querySelector('.role-card[data-role="defendant"]');
            if (defCard) {
                defCard.style.opacity = '1';
                defCard.classList.add('active');
            }
        },

        // ⭐ 역할 선택 활성화 (재판 종료 후)
        enableRoleSelection: function() {
            const cards = document.querySelectorAll('.role-card');
            cards.forEach(card => {
                card.style.opacity = '1';
                card.style.pointerEvents = 'auto';
                card.style.cursor = 'pointer';

                card.addEventListener('click', () => {
                    const roleId = card.getAttribute('data-role');
                    this.setRole(roleId);
                });
            });
        },

        // ⭐ 개선: 역할 전환
        setRole: function(roleId) {
            if (!this.roles[roleId]) return;

            const oldRole = this.currentRoleId;
            this.currentRoleId = roleId;

            // 서버에 역할 전환 알림
            fetch(API_BASE_URL + '/trial-switch-role', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'role=' + roleId
            })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            // UI 업데이트
                            this.updateRoleCards(roleId);
                            this.updateRoleChip();
                            this.syncPlaceholder();

                            console.log('역할 전환 완료:', oldRole, '->', roleId);
                        } else {
                            alert('역할 전환에 실패했습니다: ' + data.message);
                            this.currentRoleId = oldRole; // 롤백
                        }
                    })
                    .catch(err => {
                        console.error('역할 전환 오류:', err);
                        alert('역할 전환 중 오류가 발생했습니다.');
                        this.currentRoleId = oldRole; // 롤백
                    });
        },

        updateRoleCards: function(activeRoleId) {
            document.querySelectorAll('.role-card').forEach(card => {
                const isActive = card.getAttribute('data-role') === activeRoleId;
                card.classList.toggle('active', isActive);
            });
        },

        addSystemMessage: function(text) {
            const chatArea = document.getElementById('trialChatArea');
            const msgDiv = document.createElement('div');
            msgDiv.className = 'alert alert-info text-center';
            msgDiv.style.margin = '20px';
            msgDiv.innerHTML = '<strong>' + text.replace(/\n/g, '<br>') + '</strong>';
            chatArea.appendChild(msgDiv);
            this.scrollToBottom();
        },

        // ⭐ 초기 AI 메시지
        showInitialMessage: function() {
            const chatArea = document.getElementById('trialChatArea');
            if (!chatArea) return;

            const initialMsg = `개정을 선언합니다. 피고인 김철수님은 형법 제329조 절도 혐의로 기소되었습니다.
피고인께서는 진술할 권리가 있으며, 진술을 거부할 권리도 있습니다.
먼저 피고인의 진술을 듣겠습니다.`;

            // 기존 초기 메시지 제거
            const existing = chatArea.querySelector('.message.role-ai');
            if (existing) {
                existing.remove();
            }

            this.addAIMessage(initialMsg);
        },

        syncPlaceholder: function() {
            const input = document.getElementById('trialInput');
            const role = this.roles[this.currentRoleId];
            if (input && role) {
                input.placeholder = role.label + '의 관점에서 메시지를 입력하세요...';
            }
        },

        updateRoleChip: function() {
            const chip = document.getElementById('activeRoleChip');
            const role = this.roles[this.currentRoleId];
            if (chip && role) {
                chip.textContent = role.label + ' 발언 준비';
            }
        },

        // ⭐ 개선: 메시지 전송 (역할 맥락 포함)
        send: function() {
            if (this.sending) return;

            const input = document.getElementById('trialInput');
            if (!input) return;

            const message = input.value.trim();
            if (!message) {
                input.focus();
                return;
            }

            // 사용자 메시지 추가
            const role = this.roles[this.currentRoleId];
            const decoratedMessage = role && role.promptPrefix ?
                    role.promptPrefix + ' ' + message : message;

            this.addUserMessage(decoratedMessage, role);

            input.value = '';
            this.sending = true;
            this.updateSendButtonState();

            // AI 응답 요청
            const loadingId = this.addLoadingMessage();
            this.fetchAIResponse(message, loadingId);
        },

        addUserMessage: function(text, role) {
            const chatArea = document.getElementById('trialChatArea');
            if (!chatArea || !role) {
                return;
            }

            const messageDiv = document.createElement('div');
            messageDiv.className = 'message human role-' + role.id;
            messageDiv.style.setProperty('--accent', this.pickAccent(role.id));

            messageDiv.appendChild(this.createAvatar(role));
            const { content } = this.createMessageContent(role, text);
            messageDiv.appendChild(content);

            chatArea.appendChild(messageDiv);
            this.scrollToBottom();
        },

        addLoadingMessage: function() {
            const chatArea = document.getElementById('trialChatArea');
            if (!chatArea) {
                return null;
            }

            const loadingId = 'loading-' + Date.now();
            const loadingDiv = document.createElement('div');
            loadingDiv.id = loadingId;
            loadingDiv.className = 'message role-ai';
            loadingDiv.style.setProperty('--accent', this.pickAccent('ai'));

            loadingDiv.appendChild(this.createAvatar(this.roles.ai));

            const content = document.createElement('div');
            content.className = 'message-content';

            const label = document.createElement('span');
            label.className = 'message-label';
            label.textContent = 'AI 재판부';

            const loading = document.createElement('div');
            loading.className = 'loading';

            const spinner = document.createElement('span');
            spinner.className = 'loading-spinner';

            const loadingText = document.createElement('span');
            loadingText.textContent = '답변을 준비 중입니다...';

            loading.appendChild(spinner);
            loading.appendChild(loadingText);

            content.appendChild(label);
            content.appendChild(loading);

            loadingDiv.appendChild(content);
            chatArea.appendChild(loadingDiv);

            this.scrollToBottom();
            return loadingId;
        },

        fetchAIResponse: function(message, loadingId) {
            const url = API_BASE_URL + '/trial-chat?message=' + encodeURIComponent(message);
            const eventSource = new EventSource(url);
            let aiResponse = '';
            let aiMessageId = null;

            eventSource.onmessage = (event) => {
                if (event.data === '[DONE]') {
                    eventSource.close();
                    this.removeLoadingMessage(loadingId);
                    this.sending = false;
                    this.updateSendButtonState();
                    return;
                }

                aiResponse += event.data;

                if (!aiMessageId) {
                    this.removeLoadingMessage(loadingId);
                    aiMessageId = this.addAIMessage(aiResponse);
                } else {
                    this.updateAIMessage(aiMessageId, aiResponse);
                }
            };

            eventSource.onerror = () => {
                eventSource.close();
                this.removeLoadingMessage(loadingId);
                this.sending = false;
                this.updateSendButtonState();
            };
        },

        addAIMessage: function(text) {
            const chatArea = document.getElementById('trialChatArea');
            if (!chatArea) {
                return null;
            }

            const messageId = 'ai-' + Date.now();
            const role = this.roles.ai;
            const messageDiv = document.createElement('div');
            messageDiv.id = messageId;
            messageDiv.className = 'message role-ai';
            messageDiv.style.setProperty('--accent', this.pickAccent('ai'));

            messageDiv.appendChild(this.createAvatar(role));
            const { content, textElement } = this.createMessageContent(role, text);
            messageDiv.appendChild(content);

            chatArea.appendChild(messageDiv);
            this.scrollToBottom();
            return messageId;
        },

        updateAIMessage: function(messageId, text) {
            const messageDiv = document.getElementById(messageId);
            if (messageDiv) {
                const textNode = messageDiv.querySelector('.message-text');
                if (textNode) {
                    textNode.textContent = text;
                }
            }
            this.scrollToBottom();
        },

        removeLoadingMessage: function(loadingId) {
            if (!loadingId) {
                return;
            }
            const loadingDiv = document.getElementById(loadingId);
            if (loadingDiv) {
                loadingDiv.remove();
            }
        },

        // ⭐ AI 자동 진행
        aiAutoProceed: function() {
            if (this.sending) return;

            this.sending = true;
            this.updateSendButtonState();

            const loadingId = this.addLoadingMessage();
            const url = API_BASE_URL + '/trial-ai-proceed?sessionId=' + encodeURIComponent(this.sessionId);

            const eventSource = new EventSource(url);
            let aiResponse = '';
            let aiMessageId = null;

            eventSource.onmessage = (event) => {
                if (event.data === '[DONE]') {
                    eventSource.close();
                    this.removeLoadingMessage(loadingId);
                    this.sending = false;
                    this.updateSendButtonState();
                    return;
                }

                aiResponse += event.data;

                if (!aiMessageId) {
                    this.removeLoadingMessage(loadingId);
                    aiMessageId = this.addAIMessage(aiResponse);
                } else {
                    this.updateAIMessage(aiMessageId, aiResponse);
                }
            };

            eventSource.onerror = () => {
                eventSource.close();
                this.removeLoadingMessage(loadingId);
                this.sending = false;
                this.updateSendButtonState();
            };
        },

        // ⭐ 판결 생성
        generateVerdict: function() {
            if (this.sending) return;

            this.sending = true;
            this.updateSendButtonState();

            const loadingId = this.addLoadingMessage();
            const url = API_BASE_URL + '/trial-verdict?sessionId=' + encodeURIComponent(this.sessionId);

            const eventSource = new EventSource(url);
            let verdictText = '';
            let messageId = null;

            eventSource.onmessage = (event) => {
                if (event.data === '[DONE]') {
                    eventSource.close();
                    this.removeLoadingMessage(loadingId);
                    this.sending = false;
                    this.updateSendButtonState();
                    return;
                }

                verdictText += event.data;

                if (!messageId) {
                    this.removeLoadingMessage(loadingId);
                    messageId = this.addAIMessage(verdictText);

                    // ⭐ 판결 메시지 스타일 변경
                    const msgDiv = document.getElementById(messageId);
                    if (msgDiv) {
                        msgDiv.style.background = 'linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%)';
                        msgDiv.style.borderLeft = '4px solid #ff9800';
                    }
                } else {
                    this.updateAIMessage(messageId, verdictText);
                }
            };

            eventSource.onerror = () => {
                eventSource.close();
                this.removeLoadingMessage(loadingId);
                this.sending = false;
                this.updateSendButtonState();
            };
        },

        // ⭐ 개선: 재판 종료
        completeTrial: function() {
            if (!confirm('재판을 종료하시겠습니까? 종료 후 새로운 사건을 선택할 수 있습니다.')) {
                return;
            }

            fetch(API_BASE_URL + '/trial-complete', { method: 'POST' })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            this.isTrialCompleted = true;
                            this.currentCase = null;

                            // 완료 메시지
                            const chatArea = document.getElementById('trialChatArea');
                            const completeMsg = document.createElement('div');
                            completeMsg.className = 'alert alert-success text-center';
                            completeMsg.style.margin = '20px';
                            completeMsg.innerHTML = '<strong>✅ 재판이 종료되었습니다.</strong><br>' +
                                    '<button class="btn btn-primary mt-2" onclick="trial.showCaseSelectionModal()">' +
                                    '<i class="fas fa-folder-open"></i> 새 사건 선택' +
                                    '</button>';
                            chatArea.appendChild(completeMsg);
                            this.scrollToBottom();

                            // 역할 선택 비활성화
                            this.disableRoleSelection();
                        }
                    })
                    .catch(err => {
                        console.error('재판 종료 실패:', err);
                        alert('재판 종료 중 오류가 발생했습니다.');
                    });
        },

        createAvatar: function(role) {
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.textContent = role.icon || '•';
            avatar.setAttribute('aria-hidden', 'true');
            return avatar;
        },

        createMessageContent: function(role, text) {
            const content = document.createElement('div');
            content.className = 'message-content';

            const label = document.createElement('span');
            label.className = 'message-label';
            label.textContent = role.label;

            const textElement = document.createElement('p');
            textElement.className = 'message-text';
            textElement.textContent = text;

            const meta = document.createElement('span');
            meta.className = 'message-meta';
            meta.textContent = this.formatTimestamp();

            content.appendChild(label);
            content.appendChild(textElement);
            content.appendChild(meta);

            return { content, textElement };
        },

        pickAccent: function(roleId) {
            switch (roleId) {
                case 'judge':
                    return '#7a5c2a';
                case 'prosecutor':
                    return '#be4d2d';
                case 'defender':
                    return '#2d6fb8';
                case 'defendant':
                    return '#0f766e';
                case 'witness':
                    return '#a855f7';
                case 'jury':
                    return '#6366f1';
                default:
                    return '#334155';
            }
        },

        formatTimestamp: function(date = new Date()) {
            const hours = date.getHours();
            const minutes = date.getMinutes().toString().padStart(2, '0');
            const period = hours >= 12 ? '오후' : '오전';
            const displayHour = hours % 12 === 0 ? 12 : hours % 12;
            return period + ' ' + displayHour + ':' + minutes;
        },

        updateSendButtonState: function() {
            const input = document.getElementById('trialInput');
            const sendBtn = document.getElementById('trialSendBtn');

            if (!input || !sendBtn) {
                return;
            }

            const hasText = input.value.trim().length > 0;
            sendBtn.disabled = this.sending || !hasText;
        },

        scrollToBottom: function() {
            const chatArea = document.getElementById('trialChatArea');
            if (chatArea) {
                chatArea.scrollTop = chatArea.scrollHeight;
            }
        }
    };

    $(function() {
        trial.init();
    });
</script>

<div class="col-sm-12">
    <div class="trial-shell">
        <div class="trial-layout">
            <aside class="trial-panel">
                <div class="trial-panel__header">
                    <h2>모의 재판 브리핑</h2>
                    <p>각 참가자의 역할을 자유롭게 바꿔가며 사건의 흐름을 구성해보세요. AI 재판부가 전체 절차를 정리해 드립니다.</p>
                    <div class="stage-indicator">사건 진행 제어판</div>
                </div>
                <div class="roles-panel">
                    <div class="roles-panel__intro">
                        <strong>발언 역할 선택</strong>
                        <span>메시지를 보낼 때마다 원하는 역할을 눌러주세요.</span>
                    </div>
                    <div class="role-grid">
                        <div class="role-card" data-role="judge" data-icon="⚖️">
                            <h3>재판장</h3>
                            <p>절차 진행과 판결 요지를 정리합니다.</p>
                        </div>
                        <div class="role-card" data-role="prosecutor" data-icon="🧾">
                            <h3>검사</h3>
                            <p>공소 사실과 증거를 제시하며 사건을 이끕니다.</p>
                        </div>
                        <div class="role-card" data-role="defender" data-icon="🛡️">
                            <h3>변호인</h3>
                            <p>피고인을 대변하고 반박 논리를 구성합니다.</p>
                        </div>
                        <div class="role-card" data-role="defendant" data-icon="👤">
                            <h3>피고인</h3>
                            <p>사건의 당사자로서 진술을 전달합니다.</p>
                        </div>
                        <div class="role-card" data-role="witness" data-icon="🗣️">
                            <h3>증인</h3>
                            <p>사실관계를 뒷받침할 증언을 남겨보세요.</p>
                        </div>
                        <div class="role-card" data-role="jury" data-icon="👥">
                            <h3>참심위원</h3>
                            <p>중립적 의견이나 평결 논의를 공유합니다.</p>
                        </div>
                    </div>
                </div>
            </aside>

            <section class="trial-panel chat-panel">
                <div class="chat-panel__header">
                    <div class="chat-panel__header-top">
                        <h3 class="chat-title">모의 법정 대화</h3>
                        <span class="case-chip">CASE PLAYGROUND</span>
                    </div>
                    <p>각 발언은 선택된 역할과 함께 기록됩니다. AI 재판부는 모든 메시지를 참조해 절차 요약과 판단 방향을 제시합니다.</p>
                </div>

                <div class="chat-area" id="trialChatArea" role="log" aria-live="polite" aria-label="모의 재판 대화 내용">
                    <div class="message role-ai" style="--accent: #334155;">
                        <div class="message-avatar" aria-hidden="true">AI</div>
                        <div class="message-content">
                            <span class="message-label">AI 재판부</span>
                            <p class="message-text">안녕하세요. 모의 재판 진행을 돕는 AI 재판부입니다. 참가자 역할을 선택하고 발언을 남기시면 절차와 쟁점을 정리해 드립니다.</p>
                            <span class="message-meta">방금</span>
                        </div>
                    </div>
                </div>

                <div class="input-panel">
                    <span class="active-role-chip" id="activeRoleChip">피고인 발언 준비</span>
                    <div class="input-group">
                        <div class="input-field-wrapper">
                            <label for="trialInput">선택한 역할의 발언 내용</label>
                            <textarea id="trialInput"
                                      placeholder="피고인의 관점에서 메시지를 입력하세요..."
                                      autocomplete="off"></textarea>
                        </div>
                        <button type="button" id="trialSendBtn" onclick="trial.send()">
                            발언 등록
                            <span class="btn-icon">➤</span>
                        </button>
                    </div>

                    <!-- ⭐ 추가 버튼들 -->
                    <!-- ⭐ 추가 버튼들 -->
                    <div style="margin-top: 15px; display: flex; gap: 10px; flex-wrap: wrap;">
                        <button type="button" class="btn btn-warning btn-sm" onclick="trial.generateVerdict()">
                            ⚖️ 판결 생성
                        </button>
                        <button type="button" class="btn btn-success btn-sm" onclick="trial.completeTrial()">
                            ✅ 재판 종료
                        </button>
                        <button type="button" class="btn btn-danger btn-sm" onclick="location.reload()">
                            🔄 초기화
                        </button>
                    </div>
                </div>
            </section>
        </div>
    </div>
</div>