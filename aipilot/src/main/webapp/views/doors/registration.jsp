<%-- aipilot/src/main/webapp/views/doors/registration.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
  let doorRegistration = {
    init: function() {
      $('#send').click(() => this.send());
      $('#attach').change(() => {}); // 이미지 미리보기 기능 제거됨
      $('#capture').click(() => this.captureAndSetFile());
      this.previewCamera('videoPreview'); // 웹캠 미리보기 시작
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
                alert('⚠️ 카메라 접근에 실패했습니다. (에러: ' + error.name + ')');
              });
    },

    // 웹캠 캡처 함수
    captureFrame: function(videoId, handleFrame) {
      const video = document.getElementById(videoId);
      if (video.videoWidth === 0 || video.videoHeight === 0) {
        alert("⚠️ 비디오 스트림이 활성화되지 않았습니다. 잠시 후 다시 시도하세요.");
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

    // 캡처 후 파일 입력에 설정하는 함수
    captureAndSetFile: function() {
      this.captureFrame("videoPreview", (pngBlob) => {
        if (!pngBlob) return;

        // Blob을 File 객체로 변환
        const capturedFile = new File([pngBlob], "captured_face.png", { type: "image/png" });

        // DataTransfer 객체를 사용하여 FileList 생성 및 파일 input에 설정
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(capturedFile);

        const attachInput = document.getElementById("attach");
        attachInput.files = dataTransfer.files;

        // [복구] 캡처 완료 알림
        alert("웹캠 이미지가 캡처되어 '얼굴 사진 파일'에 자동 설정되었습니다.");
      });
    },

    // 등록 요청
    send: async function() {
      const name = $('#name').val().trim();
      const attach = document.getElementById("attach").files[0];

      if (!name) {
        alert("이름을 입력해야 합니다.");
        return;
      }
      if (!attach) {
        alert("등록할 얼굴 사진을 선택해야 합니다.");
        return;
      }

      $('#spinner').css('visibility', 'visible');
      $('#result').html('AI가 얼굴 특징을 추출하고 등록하는 중... <span class="spinner-border spinner-border-sm"></span>');
      const formData = new FormData();
      formData.append("name", name);
      formData.append("attach", attach);

      try {
        const response = await fetch('/doors/api/register', {
          method: "post",
          body: formData
        });
        const resultText = await response.text();
        if (response.ok) {
          $('#result').html(`<div class="alert alert-success">${resultText}</div>`);
        } else {
          $('#result').html(`<div class="alert alert-danger">등록 실패 ${resultText}</div>`);
        }
      } catch (error) {
        console.error("등록 요청 실패", error);
        $('#result').html(`<div class="alert alert-danger">통신 오류 발생 ${error.message}</div>`);
      } finally {
        $('#spinner').css('visibility', 'hidden');
      }
    }
  }

  $(() => doorRegistration.init());
</script>

<div class="col-sm-10">
  <h2>AI 얼굴 등록</h2>
  <p class="text-muted">등록할 사용자 이름과 얼굴 사진을 업로드하면 AI가 얼굴 특징을 추출하여 데이터베이스에 저장합니다.
    (Face Signature 방식)</p>

  <div class="row">
    <div class="col-sm-8">
      <div class="form-group">
        <label for="name">사용자 이름</label>
        <input id="name" class="form-control" type="text" placeholder="홍길동" required/>
      </div>
      <div class="form-group">
        <label for="attach">얼굴 사진 파일</label>
        <input id="attach" class="form-control-file" type="file" accept="image/*" required/>
      </div>
      <div class="form-group">
        <button type="button" class="btn btn-success mr-2" id="capture">웹캠 캡처</button>
        <button type="button" class="btn btn-primary" id="send">등록</button>
      </div>
    </div>
    <div class="col-sm-4">
      <h5 class="mt-0">웹캠 미리보기</h5>
      <video id="videoPreview" style="width: 100%; max-width: 300px; border: 1px solid #ccc;"
             autoplay muted playsinline></video>
    </div>
  </div>

  <div id="result" class="container p-3 my-3 border" style="overflow: auto;width:auto;height: 100px;">
  </div>
</div>