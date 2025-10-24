<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
    let dashboard = {
        init: function() {
            this.loadSensorData();
            this.initVoiceControl();
            $('#voiceBtn').click(() => {
                this.startVoiceCommand();
            });
            $('#sendTextBtn').click(() => {
                this.sendTextCommand();
            });
            $('#textCommand').keypress((e) => {
                if(e.which === 13) {
                    this.sendTextCommand();
                }
            });
            setInterval(() => {
                this.loadSensorData();
            }, 5000);
        },

        loadSensorData: function() {
            $.ajax({
                url: '/ai2/api/sensor-data',
                method: 'GET',
                success: (data) => {
                    $('#temperature').text(data.temperature + '°C');
                    $('#humidity').text(data.humidity + '%');
                    $('#light').text(data.light + ' lux');

                    if(data.temperature >= 26) {
                        $('#tempCard').removeClass('border-primary border-success').addClass('border-danger');
                    } else if(data.temperature >= 20) {
                        $('#tempCard').removeClass('border-danger border-primary').addClass('border-success');
                    } else {
                        $('#tempCard').removeClass('border-danger border-success').addClass('border-primary');
                    }
                },
                error: (err) => {
                    console.error('센서 데이터 로딩 실패:', err);
                }
            });
        },

        initVoiceControl: function() {
            $('#voiceSpinner').hide();
        },

        startVoiceCommand: function() {
            $('#voiceSpinner').show();
            $('#voiceBtn').prop('disabled', true);

            springai.voice.initMic(this);
        },

        handleVoice: function(mp3Blob) {
            const formData = new FormData();
            formData.append('speech', mp3Blob, 'voice.mp3');
            const self = this;

            $.ajax({
                url: '/ai2/api/voice-control',
                method: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function(response) {
                    console.log('음성 명령 응답:', response);
                    const command = response.command || '(명령 없음)';
                    const result = response.result || '(결과 없음)';
                    self.addHistory('음성', command, result);

                    if(response.audio) {
                        const audio = new Audio('data:audio/mp3;base64,' + response.audio);
                        audio.play();
                    }
                    $('#voiceSpinner').hide();
                    $('#voiceBtn').prop('disabled', false);
                },
                error: function(err) {
                    console.error('음성 명령 실패:', err);
                    self.addHistory('음성', '(오류)', '음성 명령 처리 중 오류가 발생했습니다.');
                    $('#voiceSpinner').hide();
                    $('#voiceBtn').prop('disabled', false);
                }
            });
        },

        sendTextCommand: function() {
            const command = $('#textCommand').val().trim();
            if(!command) {
                alert('명령을 입력해주세요.');
                return;
            }

            $('#sendTextBtn').prop('disabled', true);
            const self = this;

            $.ajax({
                url: '/ai2/api/text-control',
                method: 'POST',
                data: { command: command },
                success: function(response) {
                    console.log('텍스트 명령 응답:', response);
                    const result = response.result || '(결과 없음)';
                    self.addHistory('텍스트', command, result);
                    $('#textCommand').val('');
                    $('#sendTextBtn').prop('disabled', false);
                },
                error: function(err) {
                    console.error('텍스트 명령 실패:', err);
                    self.addHistory('텍스트', command, '명령 처리 중 오류가 발생했습니다.');
                    $('#sendTextBtn').prop('disabled', false);
                }
            });
        },

        addHistory: function(type, command, result) {
            console.log('addHistory 호출됨 - type:', type, 'command:', command, 'result:', result);

            const now = new Date().toLocaleTimeString('ko-KR');
            const badge = type === '음성' ? 'badge-primary' : 'badge-success';

            const safeCommand = command || '(명령 정보 없음)';
            const safeResult = result || '(결과 정보 없음)';

            // ✅ 핵심 수정: jQuery로 DOM 요소 직접 생성
            const $historyItem = $('<div>').addClass('media border p-3 mb-2');
            const $mediaBody = $('<div>').addClass('media-body');

            const $header = $('<h6>');
            const $badge = $('<span>').addClass('badge ' + badge).text(type);
            $header.append($badge).append(' ' + now);

            const $commandP = $('<p>').addClass('mb-1');
            $commandP.append($('<strong>').text('명령: ')).append(safeCommand);

            const $resultP = $('<p>').addClass('mb-0 text-muted');
            $resultP.append($('<strong>').text('결과: ')).append(safeResult);

            $mediaBody.append($header).append($commandP).append($resultP);
            $historyItem.append($mediaBody);

            $('#history p.text-center').remove();
            $('#history').prepend($historyItem);

            // 최대 5개까지만 보여주기
            $('#history .media').slice(5).remove();

            console.log('이력 추가 완료');
        }
    };

    $(function() {
        dashboard.init();
    });
</script>

<div class="col-sm-10">
    <h2>AI2 IoT 대시보드</h2>
    <p class="text-muted">AI 기반 스마트홈 제어 시스템</p>

    <!-- 센서 현황 -->
    <div class="row mt-4">
        <div class="col-md-4">
            <div class="card border-primary" id="tempCard">
                <div class="card-body text-center">
                    <h5 class="card-title">🌡️ 온도</h5>
                    <h2 id="temperature" class="text-primary">--°C</h2>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-info">
                <div class="card-body text-center">
                    <h5 class="card-title">💧 습도</h5>
                    <h2 id="humidity" class="text-info">--%</h2>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-warning">
                <div class="card-body text-center">
                    <h5 class="card-title">💡 조도</h5>
                    <h2 id="light" class="text-warning">-- lux</h2>
                </div>
            </div>
        </div>
    </div>

    <!-- 간편 제어 -->
    <div class="row mt-4">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header bg-primary text-white">
                    <h5>🎤 음성 명령</h5>
                </div>
                <div class="card-body text-center">
                    <p>버튼을 클릭하고 명령을 말해보세요</p>
                    <button id="voiceBtn" class="btn btn-primary btn-lg">
                        <i class="fa fa-microphone"></i> 음성 명령 시작
                    </button>
                    <div id="voiceSpinner" class="spinner-border text-primary mt-3"></div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card">
                <div class="card-header bg-success text-white">
                    <h5>💬 텍스트 명령</h5>
                </div>
                <div class="card-body">
                    <div class="input-group">
                        <input type="text" id="textCommand" class="form-control"
                               placeholder="예: 난방 24도로 설정해줘">
                        <div class="input-group-append">
                            <button id="sendTextBtn" class="btn btn-success">전송</button>
                        </div>
                    </div>
                    <small class="text-muted">예시: "불 켜줘", "온도 올려줘", "환기 시작"</small>
                </div>
            </div>
        </div>
    </div>

    <!-- 최근 제어 이력 -->
    <div class="row mt-4">
        <div class="col-md-12">
            <div class="card">
                <div class="card-header">
                    <h5>📋 최근 제어 이력</h5>
                </div>
                <div class="card-body" style="max-height: 400px; overflow-y: auto;">
                    <div id="history">
                        <p class="text-center text-muted">명령을 실행하면 이력이 표시됩니다.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
