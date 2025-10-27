<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<style>
  .cw-container {
    background: #f8f9fa;
    border-radius: 15px;
    padding: 30px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  }

  .cw-header {
    text-align: center;
    margin-bottom: 25px;
  }

  .cw-header h2 {
    font-weight: 600;
    color: #333;
  }

  #cw_entry_attach {
    border: 2px dashed #bbb;
    padding: 20px;
    background-color: #fff;
    border-radius: 10px;
    transition: border-color 0.3s ease;
  }

  #cw_entry_attach:hover {
    border-color: #007bff;
  }

  #cw_entry_preview {
    display: block;
    margin-top: 15px;
    border-radius: 10px;
    border: 1px solid #ddd;
    max-width: 100%;
  }

  #cw_entry_result {
    background-color: #fff;
    border-radius: 10px;
    box-shadow: inset 0 0 5px rgba(0,0,0,0.05);
    margin-top: 20px;
  }

  .cw-btn {
    height: 45px;
    font-weight: 500;
    font-size: 15px;
  }

  .cw-spinner-btn {
    background-color: #e9ecef;
    color: #555;
    border: none;
  }

  .media.border.p-3 {
    border-radius: 10px;
    background: #fdfdfd;
  }
</style>

<script>
  let cw_entry = {
    plate: null,

    init:function(){
      document.getElementById('cw_entry_btn').addEventListener('click', ()=> this.sendImage());
      document.getElementById('cw_entry_next').addEventListener('click', ()=> this.goNext());
      document.getElementById('cw_entry_spinner').style.visibility = 'hidden';
      document.getElementById('cw_entry_attach').addEventListener('change', ()=> this.preview());

      // "다음 단계로" 버튼은 처음엔 비활성화 / 숨김
      document.getElementById('cw_entry_next').disabled = true;
      document.getElementById('cw_entry_next').style.visibility = 'hidden';
    },

    preview:function(){
      const file = document.getElementById("cw_entry_attach").files[0];
      if(!file) return;
      const reader = new FileReader();
      reader.onload = (e)=>{
        document.getElementById("cw_entry_preview").src = e.target.result;
      };
      reader.readAsDataURL(file);
    },

    sendImage: async function(){
      const file = document.getElementById("cw_entry_attach").files[0];
      if(!file){
        alert("번호판이 잘 보이는 차량 사진을 업로드하세요.");
        return;
      }

      document.getElementById('cw_entry_spinner').style.visibility = 'visible';

      const formData = new FormData();
      formData.append("attach", file);

      const response = await fetch('/ai6/entry-image', {
        method:'POST',
        body: formData
      });

      const data = await response.json();

      this.plate = data.plate || null;

      const cardHtml = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>🚘 입차 처리 결과</h6>
            <p>번호판: <b>${data.plate || ''}</b></p>
            <p>기존고객: <b>${data.known ? '예' : '아니오'}</b></p>
            <p>차단봉 상태: <b>${data.barrier == 'UP' ? '열림' :
                              data.barrier == 'DOWN' ? '닫힘' :
                              (data.barrier || '')}</b></p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;

      document.getElementById('cw_entry_result').innerHTML = cardHtml;

      if (this.plate) {
        document.getElementById('cw_entry_next').disabled = false;
        document.getElementById('cw_entry_next').style.visibility = 'visible';
      }

      document.getElementById('cw_entry_spinner').style.visibility = 'hidden';
    },

    goNext:function(){
      if(!this.plate){
        alert("plate 정보가 없습니다. 입차 처리를 먼저 해주세요.");
        return;
      }
      const nextUrl = '/springai3/carwash_plan?plate=' + encodeURIComponent(this.plate);
      window.location.href = nextUrl;
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> cw_entry.init());
</script>

<div class="cw-container col-sm-10 mx-auto">
  <div class="cw-header">
    <h2>🚗 세차장 — 입차 & 차단봉 제어</h2>
    <p class="text-muted">차량 번호판 인식 후, 차단봉 상태를 자동 제어합니다.</p>
  </div>

  <div class="row g-3">
    <div class="col-sm-6">
      <span class="input-group-text">📸 번호판이 보이는 차량 사진 업로드</span>
      <input id="cw_entry_attach" class="form-control" type="file"/>
      <img id="cw_entry_preview" alt="미리보기 이미지"/>
    </div>

    <div class="col-sm-2 d-flex align-items-end">
      <button type="button" class="btn btn-primary w-100 cw-btn" id="cw_entry_btn">입차 처리</button>
    </div>

    <div class="col-sm-2 d-flex align-items-end">
      <button class="btn cw-spinner-btn w-100 cw-btn" disabled>
        <span class="spinner-border spinner-border-sm" id="cw_entry_spinner"></span>
        Loading..
      </button>
    </div>

    <div class="col-sm-2 d-flex align-items-end">
      <button type="button" class="btn btn-success w-100 cw-btn" id="cw_entry_next">다음 단계로</button>
    </div>
  </div>

  <div id="cw_entry_result" class="p-3 mt-4"></div>
</div>
