<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    .trial-container {
        background: white;
        border-radius: 10px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        overflow: hidden;
    }
    .trial-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 20px;
        text-align: center;
    }
    .chat-area {
        height: 500px;
        overflow-y: auto;
        padding: 20px;
        background-color: #fafafa;
    }
    .message {
        margin-bottom: 20px;
        display: flex;
        align-items: flex-start;
    }
    .message.user {
        flex-direction: row-reverse;
    }
    .message-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background-color: #667eea;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        flex-shrink: 0;
    }
    .message.user .message-avatar {
        background-color: #764ba2;
    }
    .message-content {
        max-width: 70%;
        padding: 12px 16px;
        border-radius: 12px;
        margin: 0 10px;
        word-wrap: break-word;
    }
    .message.ai .message-content {
        background-color: white;
        border: 1px solid #e0e0e0;
    }
    .message.user .message-content {
        background-color: #667eea;
        color: white;
    }
    .input-area {
        padding: 20px;
        background-color: white;
        border-top: 1px solid #e0e0e0;
    }
    .input-group-custom {
        display: flex;
        gap: 10px;
    }
    #trialInput {
        flex: 1;
        padding: 12px;
        border: 2px solid #e0e0e0;
        border-radius: 8px;
    }
    #trialInput:focus {
        outline: none;
        border-color: #667eea;
    }
    #trialSendBtn {
        padding: 12px 24px;
        background-color: #667eea;
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-weight: bold;
    }
    #trialSendBtn:hover {
        background-color: #5568d3;
    }
    #trialSendBtn:disabled {
        background-color: #ccc;
        cursor: not-allowed;
    }
    .loading {
        display: inline-block;
        padding: 8px 12px;
        background-color: #f0f0f0;
        border-radius: 8px;
        font-style: italic;
        color: #666;
    }
</style>

<script>
    let trial = {
        sending: false,
        sessionId: null,  // 세션 ID

        init: function() {
            console.log('=== 모의 법정 초기화 ===');

            // 세션 ID 생성 (브라우저 새로고침해도 유지)
            this.sessionId = sessionStorage.getItem('trial-session-id');
            if (!this.sessionId) {
                this.sessionId = 'trial-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
                sessionStorage.setItem('trial-session-id', this.sessionId);
            }
            console.log('세션 ID:', this.sessionId);

            const chatArea = document.getElementById('trialChatArea');
            console.log('채팅 영역 찾기:', chatArea ? '성공' : '실패');
        },

        send: function() {
            console.log('=== send() 호출 ===');

            const input = document.getElementById('trialInput');
            const sendBtn = document.getElementById('trialSendBtn');
            const message = input.value.trim();

            console.log('입력 메시지:', message);

            if (!message) {
                alert('메시지를 입력하세요.');
                return;
            }

            if (this.sending) {
                console.log('이미 전송 중...');
                return;
            }

            this.sending = true;
            sendBtn.disabled = true;

            // 1. 사용자 메시지 표시
            console.log('1. 사용자 메시지 추가');
            this.addUserMessage(message);
            input.value = '';

            // 2. 로딩 표시
            console.log('2. 로딩 표시');
            const loadingId = this.addLoadingMessage();

            // 3. AI 응답 받기
            console.log('3. AI 응답 요청');
            this.fetchAIResponse(message, loadingId, sendBtn);
        },

        addUserMessage: function(text) {
            const chatArea = document.getElementById('trialChatArea');

            const messageDiv = document.createElement('div');
            messageDiv.className = 'message user';

            // avatar 생성
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.textContent = '나';

            // content 생성
            const content = document.createElement('div');
            content.className = 'message-content';
            content.textContent = text;  // textContent는 자동으로 escape

            messageDiv.appendChild(avatar);
            messageDiv.appendChild(content);
            chatArea.appendChild(messageDiv);

            this.scrollToBottom();
            console.log('✓ 사용자 메시지 추가 완료');
        },

        addLoadingMessage: function() {
            const chatArea = document.getElementById('trialChatArea');
            const loadingId = 'loading-' + Date.now();

            const loadingDiv = document.createElement('div');
            loadingDiv.id = loadingId;
            loadingDiv.className = 'message ai';

            // avatar 생성
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.textContent = '판';

            // loading 생성
            const loading = document.createElement('div');
            loading.className = 'loading';
            loading.textContent = '답변 생성 중...';

            loadingDiv.appendChild(avatar);
            loadingDiv.appendChild(loading);
            chatArea.appendChild(loadingDiv);

            this.scrollToBottom();
            console.log('✓ 로딩 메시지 추가 완료');
            return loadingId;
        },

        fetchAIResponse: function(message, loadingId, sendBtn) {
            const url = '/ai2/api/trial-chat?message=' + encodeURIComponent(message) +
                    '&sessionId=' + encodeURIComponent(this.sessionId);
            console.log('API 호출:', url);

            const eventSource = new EventSource(url);
            let aiResponse = '';
            let aiMessageId = null;

            eventSource.onmessage = (event) => {
                console.log('응답 청크:', event.data);

                // 로딩 제거
                const loadingDiv = document.getElementById(loadingId);
                if (loadingDiv) {
                    loadingDiv.remove();
                }

                // 응답 누적
                aiResponse += event.data;

                // AI 메시지 업데이트
                if (!aiMessageId) {
                    aiMessageId = this.addAIMessage(aiResponse);
                } else {
                    this.updateAIMessage(aiMessageId, aiResponse);
                }
            };

            eventSource.onerror = () => {
                console.log('스트리밍 종료');
                eventSource.close();

                // 로딩 제거
                const loadingDiv = document.getElementById(loadingId);
                if (loadingDiv) {
                    loadingDiv.remove();
                }

                // 전송 가능 상태로
                this.sending = false;
                sendBtn.disabled = false;

                console.log('✓ 대화 완료');
            };
        },

        addAIMessage: function(text) {
            const chatArea = document.getElementById('trialChatArea');
            const messageId = 'ai-msg-' + Date.now();

            const messageDiv = document.createElement('div');
            messageDiv.id = messageId;
            messageDiv.className = 'message ai';

            // avatar 생성
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.textContent = '판';

            // content 생성
            const content = document.createElement('div');
            content.className = 'message-content';
            content.textContent = text;  // textContent는 자동으로 escape

            messageDiv.appendChild(avatar);
            messageDiv.appendChild(content);
            chatArea.appendChild(messageDiv);

            this.scrollToBottom();
            return messageId;
        },

        updateAIMessage: function(messageId, text) {
            const messageDiv = document.getElementById(messageId);
            if (messageDiv) {
                const content = messageDiv.querySelector('.message-content');
                if (content) {
                    content.textContent = text;
                }
            }
            this.scrollToBottom();
        },

        scrollToBottom: function() {
            const chatArea = document.getElementById('trialChatArea');
            if (chatArea) {
                chatArea.scrollTop = chatArea.scrollHeight;
            }
        }
    };

    // jQuery 사용 가능하면 사용
    $(function() {
        trial.init();
    });
</script>

<div class="col-sm-10">
    <div class="trial-container">
        <!-- 헤더 -->
        <div class="trial-header">
            <h2>⚖️ 모의 법정 시스템</h2>
            <p>AI 판사와 대화해보세요 <span style="font-size: 0.8em;">🧠 대화 기억 ON</span></p>
        </div>

        <!-- 채팅 영역 -->
        <div class="chat-area" id="trialChatArea">
            <div class="message ai">
                <div class="message-avatar">판</div>
                <div class="message-content">
                    안녕하세요. 저는 AI 판사입니다.<br>
                    무엇을 도와드릴까요?
                </div>
            </div>
        </div>

        <!-- 입력 영역 -->
        <div class="input-area">
            <div class="input-group-custom">
                <input type="text"
                       id="trialInput"
                       placeholder="메시지를 입력하세요..."
                       onkeypress="if(event.key==='Enter') trial.send()">
                <button id="trialSendBtn" onclick="trial.send()">전송</button>
            </div>
        </div>
    </div>
</div>
