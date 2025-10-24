<%-- aipilot/src/main/webapp/views/doors/recognition.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
  let doorRecognition = {
    init: function() {
      this.previewCamera('video');
      $('#captureAndSend').click(() => this.captureFrame("video", (pngBlob) => {
        if (pngBlob) this.send(pngBlob);
      }));
      $('#spinner').css('visibility', 'hidden');
    },

    previewCamera: function(videoId) {
      const video = document.getElementById(videoId);
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

    captureFrame: function(videoId, handleFrame) {
      const video = document.getElementById(videoId);
      if (video.videoWidth === 0 || video.videoHeight === 0) {
        handleFrame(null);
        return;
      }
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const context = canvas.getContext('2d');
      context.drawImage(video, 0, 0, canvas.width, canvas.height);
      // PNG 포맷의 blob 데이터로 얻기
      canvas.toBlob((blob) => {
        handleFrame(blob);
      }, 'image/png');
    },

    send: async function(pngBlob) {
      $('#spinner').css('visibility', 'visible');

      // 사용자 이미지 미리보기
      const reader = new FileReader();
      reader.onload = (e) => {
        this.displayUserImage(e.target.result);
      };
      reader.readAsDataURL(pngBlob);

      const formData = new FormData();
      formData.append('attach', pngBlob, 'frame.png');

      // AI 인식 요청 스트리밍
      const response = await fetch('/doors/api/recognition', {
        method: "post",
        headers: {
          'Accept': 'application/x-ndjson'
        },
        body: formData
      });

      let uuid = this.makeAssistantUI("AI가 얼굴을 분석하고 문을 제어하는 중...");
      const readerStream = response.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let content = "";

      while (true) {
        const { value, done } = await readerStream.read();
        if (done) break;
        let chunk = decoder.decode(value);
        content += chunk;
        $('#' + uuid).html(content);
      }

      $('#spinner').css('visibility', 'hidden');
    },

    // 사용자 이미지를 결과창에 표시
    displayUserImage: function(base64Src) {
      const userHtml = `
                <div class="media border p-3">
                    <img src="${base64Src}" alt="캡처 이미지" class="mr-3 mt-3 rounded-circle" style="width:60px; object-fit: cover;">
                    <div class="media-body">
                        <h6>캡처 이미지</h6>
                        <p>출입을 시도하는 사용자 이미지입니다.</p>
                    </div>
                </div>
            `;
      $('#result').prepend(userHtml);
    },

    makeAssistantUI: function(initialMessage) {
      let uuid = "id-" + crypto.randomUUID();
      let aForm = `
                  <div class="media border p-3">
                    <div class="media-body">
                      <h6>AI 출입문 시스템</h6>
                      <p><pre id="`+uuid+`">${initialMessage}</pre></p>
                    </div>
                    <img src="<c:url value="/image/assistant.png"/>" alt="Assistant" class="ml-3 mt-3 rounded-circle" style="width:60px;">
                  </div>
            `;
      $('#result').prepend(aForm);
      return uuid;
    }

  }

  $(() => doorRecognition.init());
</script>

<div class="col-sm-10">
  <h2>AI 얼굴 인식 및 출입문 제어</h2>

  <div class="row mt-4">
    <div class="col-sm-9">
      <div class="row mb-3">
        <div class="col-sm-12">
          <button type="button" class="btn btn-success" id="captureAndSend">얼굴 인식 시작</button>
          <span class="text-muted ml-3" id="statusMessage">웹캠에 얼굴을 비추고 버튼을 누르세요.</span>
          <button class="btn btn-primary" disabled style="display: inline-block; margin-left: 10px;">
            <span class="spinner-border spinner-border-sm" id="spinner"></span>
            Loading..
          </button>
        </div>
      </div>

      <div id="result" class="container p-3 my-3 border" style="overflow: auto;width:auto;height: 500px;">
        <p class="text-info">버튼을 누르면 웹캠 이미지를 캡처하여 AI가 등록된 얼굴과 비교하고 출입문을 제어합니다.</p>
      </div>
    </div>

    <div class="col-sm-3">
      <div class="card shadow-sm">
        <div class="card-body">
          <h5 class="card-title">실시간 웹캠 미리보기</h5>
          <video id="video" style="width: 100%; border: 1px solid #ccc;" autoplay muted playsinline></video>
        </div>
      </div>
    </div>

  </div>
</div>