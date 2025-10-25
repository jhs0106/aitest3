<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

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
      // data = { plate:"12가3456", knownCustomer:true/false, gate:"OPENED" }

      this.plate = data.plate || null;

      // 카드 UI 업데이트
      const cardHtml = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>입차 처리 결과</h6>
            <p>번호판: <b>${data.plate || ''}</b></p>
            <p>기존고객: <b>${data.knownCustomer ? '예' : '아니오'}</b></p>
            <p>차단봉: <b>${data.gate || ''}</b></p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;
      document.getElementById('cw_entry_result').innerHTML = cardHtml;

      // 다음 단계 버튼 활성화
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

<div class="col-sm-10">
  <h2>세차장 — 입차 & 차단봉</h2>

  <div class="row">
    <div class="col-sm-6">
      <span class="input-group-text">번호판이 보이는 차량 사진</span>
      <input id="cw_entry_attach" class="form-control" type="file"/>
      <img id="cw_entry_preview" style="max-height:150px; margin-top:10px;" />
    </div>

    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-primary w-100" id="cw_entry_btn">입차 처리</button>
    </div>

    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button class="btn btn-primary" disabled >
        <span class="spinner-border spinner-border-sm" id="cw_entry_spinner"></span>
        Loading..
      </button>
    </div>

    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-success w-100" id="cw_entry_next">다음 단계로</button>
    </div>
  </div>

  <div id="cw_entry_result"
       class="container p-3 my-3 border"
       style="overflow:auto; min-height:180px;"></div>
</div>
