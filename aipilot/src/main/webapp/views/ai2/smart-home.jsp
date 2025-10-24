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

            // ✅ Enter 키 이벤트 추가
            $('#ragQuestion').keypress((e) => {
                if(e.which === 13) {
                    this.searchManual();
                }
            });

            $('#memoryMessage').keypress((e) => {
                if(e.which === 13) {
                    this.sendMemoryChat();
                }
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

            const $spinner = $('<div>').addClass('spinner-border text-primary');
            $('#ragResult').empty().append($spinner);

            $.ajax({
                url: '/ai2/api/rag-search',
                method: 'POST',
                data: { question: question },
                success: (response) => {
                    console.log('RAG 검색 응답:', response);

                    $('#ragResult').empty();

                    const $alert = $('<div>').addClass('alert alert-success');
                    const $title = $('<h6>').text('📚 검색 결과');
                    const $content = $('<p>').text(response.answer || '검색 결과가 없습니다.');

                    $alert.append($title).append($content);
                    $('#ragResult').append($alert);
                },
                error: (err) => {
                    console.error('RAG 검색 실패:', err);
                    $('#ragResult').empty();
                    const $error = $('<div>').addClass('alert alert-danger').text('검색 실패: ' + (err.responseText || '알 수 없는 오류'));
                    $('#ragResult').append($error);
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

            console.log('메시지 전송:', message);

            // 사용자 메시지 추가
            this.addChatMessage('user', message);
            $('#memoryMessage').val('');

            // ✅ AI 응답 받기 (EventSource 수정)
            const url = '/ai2/api/memory-chat?message=' + encodeURIComponent(message);
            console.log('EventSource URL:', url);

            const eventSource = new EventSource(url);
            let aiMessage = '';

            const messageId = 'ai-' + Date.now();
            const $aiSpan = $('<span>').attr('id', messageId).text('');
            this.addChatMessage('ai', $aiSpan);

            eventSource.onmessage = (event) => {
                console.log('스트리밍 데이터:', event.data);
                aiMessage += event.data;
                $('#' + messageId).text(aiMessage);
            };

            eventSource.onerror = (error) => {
                console.error('EventSource 오류:', error);
                eventSource.close();
                if(!aiMessage) {
                    $('#' + messageId).text('응답을 받지 못했습니다. 다시 시도해주세요.');
                }
            };

            // ✅ 10초 후 자동 종료
            setTimeout(() => {
                eventSource.close();
                console.log('EventSource 종료');
            }, 10000);
        },

        // ✅ 채팅 메시지 추가 (jQuery로 DOM 생성)
        addChatMessage: function(role, content) {
            console.log('메시지 추가 - role:', role, 'content:', content);

            const isUser = role === 'user';
            const badgeClass = isUser ? 'bg-primary text-white' : 'bg-light';
            const alignClass = isUser ? 'text-right' : '';

            const $messageDiv = $('<div>').addClass(alignClass + ' mb-2');
            const $badge = $('<span>')
                    .addClass('badge ' + badgeClass + ' p-2')
                    .css({
                        'display': 'inline-block',
                        'max-width': '70%',
                        'word-wrap': 'break-word'
                    });

            // content가 jQuery 객체인 경우와 문자열인 경우 구분
            if(content instanceof jQuery) {
                $badge.append(content);
            } else {
                $badge.text(content);
            }

            $messageDiv.append($badge);

            // 초기 메시지 제거
            $('#chatMessages p.text-center').remove();
            $('#chatMessages').append($messageDiv);

            // 스크롤을 맨 아래로
            $('#chatMessages').scrollTop($('#chatMessages')[0].scrollHeight);
        },

        // 문서 업로드
        uploadDocument: function() {
            const file = $('#docFile')[0].files[0];
            const type = $('#docType').val().trim();

            if(!file) {
                alert('파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            formData.append('attach', file);
            formData.append('type', type || 'general');

            const $spinner = $('<div>').addClass('spinner-border text-primary');
            $('#uploadStatus').empty().append($spinner);

            $.ajax({
                url: '/ai2/api/upload-document',
                method: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: (response) => {
                    console.log('업로드 성공:', response);
                    $('#uploadStatus').empty();

                    const $success = $('<div>').addClass('alert alert-success').text(response);
                    $('#uploadStatus').append($success);

                    $('#docFile').val('');
                    $('#docType').val('');
                },
                error: (err) => {
                    console.error('업로드 실패:', err);
                    $('#uploadStatus').empty();

                    const $error = $('<div>').addClass('alert alert-danger').text('업로드 실패: ' + (err.responseText || '알 수 없는 오류'));
                    $('#uploadStatus').append($error);
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
                    $('#uploadStatus').empty();
                    const $info = $('<div>').addClass('alert alert-info').text('벡터 저장소가 초기화되었습니다.');
                    $('#uploadStatus').append($info);
                },
                error: (err) => {
                    alert('삭제 실패: ' + (err.responseText || '알 수 없는 오류'));
                }
            });
        },

        // 디바이스 상태 로딩
        loadDeviceStatus: function() {
            $.ajax({
                url: '/ai2/api/device-status',
                method: 'GET',
                success: (data) => {
                    console.log('디바이스 상태:', data);
                    this.updateDeviceUI(data);
                },
                error: (err) => {
                    console.error('디바이스 상태 로딩 실패:', err);
                }
            });
        },

        updateDeviceUI: function(data) {
            $('#heatingStatus').text(data.heating ? 'ON' : 'OFF')
                    .css('color', data.heating ? 'green' : 'red');
            $('#lightStatus').text(data.light ? 'ON' : 'OFF')
                    .css('color', data.light ? 'green' : 'red');
            $('#ventStatus').text(data.ventilation ? 'ON' : 'OFF')
                    .css('color', data.ventilation ? 'green' : 'red');
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

            <div class="alert alert-warning mt-3">
                <strong>⚠️ 참고:</strong> 문서를 먼저 업로드해야 검색이 가능합니다. "문서 업로드" 탭에서 PDF, DOCX, TXT 파일을 업로드하세요.
            </div>
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
            <button id="clearVectorBtn" class="btn btn-danger ml-2">모든 문서 삭제</button>

            <div id="uploadStatus" class="mt-3"></div>

            <hr>
            <h5>업로드 가이드</h5>
            <ul>
                <li><strong>PDF:</strong> IoT 기기 사용 설명서</li>
                <li><strong>DOCX:</strong> 관리 매뉴얼, 주의사항</li>
                <li><strong>TXT:</strong> FAQ, 간단한 안내문</li>
            </ul>
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

            <div class="alert alert-info">
                <strong>💡 팁:</strong> 메인 대시보드에서 텍스트/음성 명령으로 디바이스를 제어할 수 있습니다.
            </div>
        </div>
    </div>
</div>
