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
            // 5초마다 센서 데이터 갱신
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

                    // 온도에 따른 색상 변경
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

            $.ajax({
                url: '/ai2/api/voice-control',
                method: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: (response) => {
                    this.addHistory('음성', response.command, response.result);
                    if(response.audio) {
                        const audio = new Audio('data:audio/mp3;base64,' + response.audio);
                        audio.play();
                    }
                    $('#voiceSpinner').hide();
                    $('#voiceBtn').prop('disabled', false);
                },
                error: (err) => {
                    console.error('음성 명령 실패:', err);
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

            $.ajax({
                url: '/ai2/api/text-control',
                method: 'POST',
                data: { command: command },
                success: (response) => {
                    this.addHistory('텍스트', command, response.result);
                    $('#textCommand').val('');
                    $('#sendTextBtn').prop('disabled', false);
                },
                error: (err) => {
                    console.error('텍스트 명령 실패:', err);
                    $('#sendTextBtn').prop('disabled', false);
                }
            });
        },

        addHistory: function(type, command, result) {
            const now = new Date().toLocaleTimeString('ko-KR');
            const badge = type === '음성' ? 'badge-primary' : 'badge-success';
            const historyItem = `
                <div class="media border p-3 mb-2">
                    <div class="media-body">
                        <h6><span class="badge ${badge}">${type}</span> ${now}</h6>
                        <p class="mb-1"><strong>명령:</strong> ${command}</p>
                        <p class="mb-0 text-muted"><strong>결과:</strong> ${result}</p>
                    </div>
                </div>
            `;
            $('#history').prepend(historyItem);

            // 최대 5개까지만 보여주기
            $('#history .media').slice(5).remove();
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