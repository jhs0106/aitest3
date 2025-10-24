<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<script>
    let cctvAnalysis = {
        // 주기적 분석을 위한 인터벌 변수
        analysisInterval: null,
        // AI에 보낼 고정 질문: 재난 상황을 감지하도록 유도하는 프롬프트
        ANALYSIS_QUESTION: "현재 CCTV 영상의 상황을 분석해주세요. 특히, 화재, 심각한 사고, 응급상황과 같은 재난 상황이 감지되면 반드시 112 신고 도구(call112)를 사용하세요. 재난 상황이 아닐 경우, 간단히 현재 상황을 설명하세요.",
        // 분석 주기 (20000ms = 20초)
        ANALYSIS_INTERVAL_MS: 20000,
        // 분석 실행 상태
        isAnalysisRunning: false,

        init:function(){
            this.previewCamera('video');
            $('#startAnalysis').click(() => this.startAnalysisLoop());
            $('#stopAnalysis').click(() => this.stopAnalysisLoop());
            this.updateButtonState(false); // 초기 상태는 중지
            this.displayMessage('시스템 준비 완료. "분석 시작" 버튼을 눌러 CCTV 모니터링을 시작하세요.', 'text-info');
        },

        // 버튼 상태 및 상태 메시지를 업데이트하는 유틸리티 함수
        updateButtonState: function(isRunning) {
            this.isAnalysisRunning = isRunning;
            if (isRunning) {
                $('#startAnalysis').prop('disabled', true).text('분석 실행 중...');
                $('#stopAnalysis').prop('disabled', false).text('분석 중지');
                // 상태 메시지는 분석이 끝날 때마다 업데이트됨.
            } else {
                $('#startAnalysis').prop('disabled', false).text('분석 시작');
                $('#stopAnalysis').prop('disabled', true).text('분석 중지');
                $('#statusMessage').text('CCTV 모니터링이 중지되었습니다.');
            }
        },

        startAnalysisLoop: function() {
            if (this.isAnalysisRunning) return;
            // 기존 인터벌이 있다면 중지 (안전 장치)
            if (this.analysisInterval) {
                clearInterval(this.analysisInterval);
            }

            this.updateButtonState(true); // 버튼 상태: 실행 중
            this.displayMessage('CCTV 모니터링 시작. 20초마다 분석을 진행합니다.', 'text-success');

            // 최초 실행 (버튼 누르자마자 한 번 실행)
            this.captureFrame("video", (pngBlob) => {
                if (pngBlob) this.send(pngBlob);
            });
            // 주기적인 캡처 및 전송 시작
            this.analysisInterval = setInterval(() => {
                this.captureFrame("video", (pngBlob) => {
                    if (pngBlob) {
                        this.send(pngBlob);
                    }
                });
            }, this.ANALYSIS_INTERVAL_MS);
        },

        stopAnalysisLoop: function() {
            if (!this.isAnalysisRunning) return;
            clearInterval(this.analysisInterval);
            this.analysisInterval = null;
            this.updateButtonState(false); // 버튼 상태: 중지됨
        },

        previewCamera:function(videoId){
            const video = document.getElementById(videoId);
            //카메라를 활성화하고 <video>에서 보여주기
            navigator.mediaDevices.getUserMedia({ video: true })
                .then((stream) => {
                    video.srcObject = stream;
                    video.play();
                })
                .catch((error) => {
                    console.error('카메라 접근 에러:', error);
                    $('#statusMessage').text('⚠️ 카메라 접근에 실패했습니다. (에러: ' + error.name + ')');
                });
        },

        captureFrame:function(videoId, handleFrame){
            const video = document.getElementById(videoId);
            //캔버스를 생성해서 비디오 크기와 동일하게 맞춤
            const canvas = document.createElement('canvas');

            // 비디오가 로드되지 않았을 경우 캡처하지 않음
            if (video.videoWidth === 0 || video.videoHeight === 0) {
                handleFrame(null);
                return;
            }

            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            // 캔버스로부터  2D로 드로잉하는 Context를 얻어냄
            const context = canvas.getContext('2d');
            // 비디오 프레임을 캔버스에 드로잉
            context.drawImage(video, 0, 0, canvas.width, canvas.height);
            // 드로잉된 프레임을 PNG 포맷의 blob 데이터로 얻기
            canvas.toBlob((blob) => {
                handleFrame(blob);
            }, 'image/png');
        },

        send: async function(pngBlob){
            if (!this.isAnalysisRunning) return;

            // 분석 요청이 시작됨을 UI로 알림
            const tempUUID = 'temp-' + crypto.randomUUID();
            this.displayTempMessage("CCTV 영상 분석 및 재난 상황 감지 중...", tempUUID);

            // 상태 메시지 업데이트
            const nextTime = new Date(Date.now() + this.ANALYSIS_INTERVAL_MS).toLocaleTimeString('ko-KR');
            $('#statusMessage').text(`마지막 분석 완료: ${new Date().toLocaleTimeString('ko-KR')} · 다음 분석: ${nextTime} 경`);


            // 멀티파트 폼 구성하기
            const formData = new FormData();
            formData.append("question", this.ANALYSIS_QUESTION);
            formData.append('attach', pngBlob, 'frame.png');

            // 이미지 분석 스트림 요청 및 결과 수집
            try {
                // API 엔드포인트 변경: /cctv/api/analysis
                const analysisResponse = await fetch('/cctv/api/analysis', {
                    method: "post",
                    headers: {
                        'Accept': 'application/x-ndjson'
                    },
                    body: formData
                });

                // 결과 표시를 위한 새로운 UI 생성
                let uuid = this.makeAssistantUI();
                // 임시 메시지 제거
                $('#media-' + tempUUID).remove();

                const reader = analysisResponse.body.getReader();
                const decoder = new TextDecoder("utf-8");
                let content = "";

                // 스트리밍된 내용을 수집 및 실시간 업데이트
                while (true) {
                    const {value, done} = await reader.read();
                    if (done) break;
                    let chunk = decoder.decode(value);
                    content += chunk;
                    $('#' + uuid).html(content)
                }

                this.displayMessage('분석 결과 수신 완료.', 'text-success');

            } catch (error) {
                console.error('CCTV 분석 요청 실패:', error);
                // 임시 메시지 제거 후 오류 메시지 표시
                $('#media-' + tempUUID).remove();
                this.displayMessage('⚠️ CCTV 분석 요청에 실패했습니다: ' + error.message, 'text-danger');
            }
        },

        displayTempMessage: function(message, uuid) {
            const tempForm = `
                <div class="media border p-3" id="media-${uuid}">
                    <div class="media-body">
                      <h6>AI 감시 요원 (처리 중)</h6>
                      <p id="${uuid}"><span class="spinner-border spinner-border-sm"></span> ${message}</p>
                    </div>
                    <img src="<c:url value="/image/assistant.png"/>" alt="Assistant" class="ml-3 mt-3 rounded-circle" style="width:60px;">
                </div>
            `;
            $('#result').prepend(tempForm);
        },

        makeAssistantUI: function() {
            let uuid = "id-" + crypto.randomUUID();
            let aForm = `
                  <div class="media border p-3">
                    <div class="media-body">
                      <h6>AI 감시 요원 분석 결과 (${new Date().toLocaleTimeString('ko-KR')})</h6>
                      <p><pre id="`+uuid+`"></pre></p>
                    </div>
                    <img src="<c:url value="/image/assistant.png"/>" alt="Assistant" class="ml-3 mt-3 rounded-circle" style="width:60px;">
                  </div>
            `;
            $('#result').prepend(aForm);
            return uuid;
        },

        displayMessage: function(message, className = 'text-muted') {
            const statusBox = document.getElementById('statusMessage');
            // 클래스 초기화 후 새 클래스 추가
            statusBox.className = 'mt-3 small';
            statusBox.classList.add(className);
            statusBox.textContent = message;
        }

    }

    $(()=>{
        cctvAnalysis.init();
    });
</script>


<div class="col-sm-10">
    <h2>CCTV 재난 상황 자동 감지 및 신고 (시뮬레이션)</h2>

    <div class="row mt-4">
        <div class="col-sm-9">
            <div class="row mb-3">
                <div class="col-sm-12">
                    <button type="button" class="btn btn-success" id="startAnalysis">분석 시작</button>
                    <button type="button" class="btn btn-danger" id="stopAnalysis" disabled>분석 중지</button>
                    <p id="statusMessage" class="mt-3 small">시스템 준비 중...</p>
                </div>
            </div>

            <div id="result" class="container p-3 my-3 border" style="overflow: auto;width:auto;height: 500px;">
                <p class="text-info">재난 상황 발생 시, AI가 112에 신고하는 도구를 사용합니다.</p>
            </div>
        </div>

        <div class="col-sm-3">
            <div class="card shadow-sm">
                <div class="card-body">
                    <h5 class="card-title">실시간 CCTV (카메라 미리보기)</h5>
                    <video id="video" style="width: 100%; border: 1px solid #ccc;" autoplay muted playsinline></video>
                </div>
            </div>
        </div>

    </div>
</div>