<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
    let smartHome = {
        init: function() {
            $('#ragSearchBtn').click(() => {
                this.searchManual();
            });
            $('#memoryChatBtn').click(() => {
                this.sendMemoryChat();
            });
            $('#uploadDocBtn').click(() => {
                this.uploadDocument();
            });
            $('#clearVectorBtn').click(() => {
                this.clearVectorStore();
            });
            this.loadDeviceStatus();
        },

        // RAG 매뉴얼 검색
        searchManual: function() {
            const question = $('#ragQuestion').val().trim();
            if(!question) {
                alert('질문을 입력해주세요.');
                return;
            }

            $('#ragResult').html('<div class="spinner-border text-primary"></div>');

            $.ajax({
                url: '/ai2/api/rag-search',
                method: 'POST',
                data: { question: question },
                success: (response) => {
                    $('#ragResult').html(`
                        <div class="alert alert-success">
                            <h6>📚 검색 결과</h6>
                            <p>${response.answer}</p>
                        </div>
                    `);
                },
                error: (err) => {
                    $('#ragResult').html('<div class="alert alert-danger">검색 실패</div>');
                }
            });
        },

        // Memory 기반 채팅
        sendMemoryChat: function() {
            const message = $('#memoryMessage').val().trim();
            if(!message) {
                alert('메시지를 입력해주세요.');
                return;
            }

            // 사용자 메시지 추가
            this.addChatMessage('user', message);
            $('#memoryMessage').val('');

            // AI 응답 받기 (스트리밍)
            const eventSource = new EventSource('/ai2/api/memory-chat?message=' + encodeURIComponent(message));
            let aiMessage = '';

            const messageId = 'ai-' + Date.now();
            this.addChatMessage('ai', '<span id="' + messageId + '"></span>');

            eventSource.onmessage = (event) => {
                aiMessage += event.data;
                $('#' + messageId).text(aiMessage);
            };

            eventSource.onerror = () => {
                eventSource.close();
            };
        },

        addChatMessage: function(role, content) {
            const isUser = role === 'user';
            const className = isUser ? 'bg-primary text-white' : 'bg-light';
            const align = isUser ? 'text-right' : '';

            const message = `
                <div class="${align} mb-2">
                    <span class="badge ${className} p-2" style="display: inline-block; max-width: 70%;">
                        ${content}
                    </span>
                </div>
            `;
            $('#chatMessages').append(message);
            $('#chatMessages').scrollTop($('#chatMessages')[0].scrollHeight);
        },

        // 문서 업로드
        uploadDocument: function() {
            const file = $('#docFile')[0].files[0];
            const type = $('#docType').val();

            if(!file) {
                alert('파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            formData.append('attach', file);
            formData.append('type', type);

            $('#uploadStatus').html('<div class="spinner-border text-primary"></div>');

            $.ajax({
                url: '/ai2/api/upload-document',
                method: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: (response) => {
                    $('#uploadStatus').html(`
                        <div class="alert alert-success">${response}</div>
                    `);
                    $('#docFile').val('');
                },
                error: (err) => {
                    $('#uploadStatus').html('<div class="alert alert-danger">업로드 실패</div>');
                }
            });
        },

        // 벡터 저장소 초기화
        clearVectorStore: function() {
            if(!confirm('모든 문서를 삭제하시겠습니까?')) {
                return;
            }

            $.ajax({
                url: '/ai2/api/clear-vector',
                method: 'POST',
                success: (response) => {
                    alert(response);
                }
            });
        },

        // 디바이스 상태 로딩
        loadDeviceStatus: function() {
            $.ajax({
                url: '/ai2/api/device-status',
                method: 'GET',
                success: (data) => {
                    this.updateDeviceUI(data);
                }
            });
        },

        updateDeviceUI: function(data) {
            // 디바이스 상태 업데이트
            $('#heatingStatus').text(data.heating ? 'ON' : 'OFF');
            $('#lightStatus').text(data.light ? 'ON' : 'OFF');
            $('#ventStatus').text(data.ventilation ? 'ON' : 'OFF');
        }
    };

    $(function() {
        smartHome.init();
    });
</script>

<div class="col-sm-10">
    <h2>🏠 스마트홈 고급 제어</h2>
    <p class="text-muted">RAG, Memory, Function Calling을 활용한 인텔리전트 제어</p>

    <!-- 탭 네비게이션 -->
    <ul class="nav nav-tabs mt-4" role="tablist">
        <li class="nav-item">
            <a class="nav-link active" data-toggle="tab" href="#ragTab">📚 매뉴얼 검색 (RAG)</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" data-toggle="tab" href="#memoryTab">🧠 학습형 대화 (Memory)</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" data-toggle="tab" href="#uploadTab">📤 문서 업로드</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" data-toggle="tab" href="#statusTab">📊 디바이스 상태</a>
        </li>
    </ul>

    <!-- 탭 컨텐츠 -->
    <div class="tab-content">
        <!-- RAG 탭 -->
        <div id="ragTab" class="container tab-pane active"><br>
            <h4>IoT 매뉴얼 검색</h4>
            <p class="text-muted">업로드된 매뉴얼에서 정보를 검색합니다.</p>
            <div class="form-group">
                <label>질문 입력</label>
                <input type="text" id="ragQuestion" class="form-control"
                       placeholder="예: 에어컨 청소 방법, 난방 효율 높이는 법">
            </div>
            <button id="ragSearchBtn" class="btn btn-primary">검색</button>
            <div id="ragResult" class="mt-3"></div>

            <hr>
            <h5>검색 예시</h5>
            <ul>
                <li>"에어컨 필터 청소는 어떻게 해?"</li>
                <li>"난방비 절약하는 방법 알려줘"</li>
                <li>"환기 시스템 점검 주기는?"</li>
            </ul>
        </div>

        <!-- Memory 탭 -->
        <div id="memoryTab" class="container tab-pane fade"><br>
            <h4>대화 기록 기반 제어</h4>
            <p class="text-muted">이전 대화를 기억하며 맞춤형 응답을 제공합니다.</p>

            <div class="card">
                <div class="card-body" id="chatMessages" style="height: 400px; overflow-y: auto;">
                    <p class="text-center text-muted">대화를 시작해보세요!</p>
                </div>
                <div class="card-footer">
                    <div class="input-group">
                        <input type="text" id="memoryMessage" class="form-control"
                               placeholder="예: 어제처럼 온도 설정해줘">
                        <div class="input-group-append">
                            <button id="memoryChatBtn" class="btn btn-success">전송</button>
                        </div>
                    </div>
                </div>
            </div>

            <hr>
            <h5>Memory 활용 예시</h5>
            <ul>
                <li>"내가 좋아하는 온도로 설정해줘" (이전 대화에서 학습)</li>
                <li>"어제처럼 설정해줘"</li>
                <li>"평소 자기 전에 하던대로 해줘"</li>
            </ul>
        </div>

        <!-- 업로드 탭 -->
        <div id="uploadTab" class="container tab-pane fade"><br>
            <h4>IoT 매뉴얼 업로드</h4>
            <p class="text-muted">PDF, DOCX, TXT 형식의 매뉴얼을 업로드하여 RAG에 활용합니다.</p>

            <div class="form-group">
                <label>문서 유형</label>
                <input type="text" id="docType" class="form-control"
                       placeholder="예: aircon, heating, ventilation">
                <small class="text-muted">검색 시 특정 유형만 필터링할 수 있습니다.</small>
            </div>

            <div class="form-group">
                <label>파일 선택</label>
                <input type="file" id="docFile" class="form-control-file"
                       accept=".pdf,.docx,.txt">
            </div>

            <button id="uploadDocBtn" class="btn btn-primary">업로드</button>
            <button id="clearVectorBtn" class="btn btn-danger">모든 문서 삭제</button>

            <div id="uploadStatus" class="mt-3"></div>
        </div>

        <!-- 상태 탭 -->
        <div id="statusTab" class="container tab-pane fade"><br>
            <h4>디바이스 현황</h4>
            <table class="table table-bordered">
                <thead>
                <tr>
                    <th>디바이스</th>
                    <th>상태</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>🔥 난방 시스템</td>
                    <td id="heatingStatus">OFF</td>
                </tr>
                <tr>
                    <td>💡 조명</td>
                    <td id="lightStatus">OFF</td>
                </tr>
                <tr>
                    <td>🌬️ 환기 시스템</td>
                    <td id="ventStatus">OFF</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>