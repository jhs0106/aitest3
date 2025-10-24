<%-- aipilot/src/main/webapp/views/doors/registration.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
  let doorRegistration = {
    init: function() {
      $('#send').click(() => this.send());
      $('#attach').change(() => this.previewImage());
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

    // 파일 선택 시 미리보기
    previewImage: function() {
      const file = document.getElementById("attach").files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = function (e) {
          $('#imagePreview').html(`<img src="${e.target.result}" alt="미리보기 이미지" class="img-fluid rounded" style="max-height: 200px;"/>`);
        };
        reader.readAsDataURL(file);
      }
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
  <p class="text-muted">등록할 사용자 이름과 얼굴 사진을 업로드하면 AI가 얼굴 특징을 추출하여 데이터베이스에 저장합니다. (Face Signature 방식)</p>

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
        <button type="button" class="btn btn-primary" id="send">등록</button>
      </div>
    </div>
    <div class="col-sm-4">
      <h5 class="mt-0">웹캠 미리보기</h5>
      <video id="videoPreview" style="width: 100%; max-width: 300px; border: 1px solid #ccc;" autoplay muted playsinline></video>
    </div>
  </div>

  <div id="result" class="container p-3 my-3 border" style="overflow: auto;width:auto;height: 100px;">
  </div>
</div>