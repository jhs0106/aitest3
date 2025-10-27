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
    barrierUp: false, // 차단봉 올라갔는지 상태 기억

    init:function(){
      document.getElementById('cw_entry_btn').addEventListener('click', ()=> this.sendImage());
      document.getElementById('cw_entry_open').addEventListener('click', ()=> this.openBarrier());
      document.getElementById('cw_entry_next').addEventListener('click', ()=> this.goNext());

      document.getElementById('cw_entry_spinner').style.visibility = 'hidden';
      document.getElementById('cw_entry_attach').addEventListener('change', ()=> this.preview());

      // 시작할 때는 "다음 단계로" 비활성화 + 숨김
      this.disableNextButton(true); // true => 숨김
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

    // 1) 입차 처리 (번호판 인식 등)
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

      // 서버에서 준 barrier 상태로 초기화
      // ("UP"이면 이미 열려 있는 상태라고 간주)
      this.barrierUp = (data.barrier === 'UP');

      const cardHtml = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>🚘 입차 처리 결과</h6>
            <p>번호판: <b>${data.plate || ''}</b></p>
            <p>기존고객: <b>${data.known ? '예' : '아니오'}</b></p>
            <p>차단봉 상태:
              <b>${
                data.barrier == 'UP' ? '열림' :
                data.barrier == 'DOWN' ? '닫힘' :
                (data.barrier || '')
              }</b>
            </p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;

      document.getElementById('cw_entry_result').innerHTML = cardHtml;

      // 차단봉 버튼 상태 갱신
      this.updateOpenButtonState();

      // 차단봉이 이미 열려 있으면 바로 다음 단계로 갈 수 있게
      if (this.barrierUp && this.plate) {
        this.enableNextButton();
      } else {
        this.disableNextButton(false); // false => 버튼은 보여주되 클릭 막기
      }

      document.getElementById('cw_entry_spinner').style.visibility = 'hidden';
    },

    // 2) 차단봉 올리기 (입차용 수동 오픈)
    openBarrier: async function(){
      if(!this.plate){
        alert("먼저 차량 이미지를 업로드하고 [입차 처리]를 실행하세요.");
        return;
      }

      // 새로운 API: entry-gate-open (출차 로그 안 남기고 그냥 barrier만 올림)
      const url = '/ai6/entry-gate-open?plate=' + encodeURIComponent(this.plate);

      try {
        const res = await fetch(url, { method:'POST' });
        const data = await res.json();
        // 기대 응답: { plate:"...", barrier:"UP", message:"..." }

        if (data.barrier === 'UP') {
          this.barrierUp = true;
          alert('차단봉을 열었습니다.');

          this.enableNextButton();
          this.updateOpenButtonState(); // 올린 후에는 "차단봉 올리기" 버튼 비활성화
        } else {
          alert('차단봉을 열 수 없습니다. (' + (data.barrier || 'UNKNOWN') + ')');
          this.disableNextButton(false);
        }
      } catch(e){
        console.error(e);
        alert('차단봉 제어 중 오류가 발생했습니다.');
        this.disableNextButton(false);
      }
    },

    // 3) 다음 단계로 이동
    goNext:function(){
      if(!this.plate){
        alert("plate 정보가 없습니다. 입차 처리를 먼저 해주세요.");
        return;
      }
      if(!this.barrierUp){
        alert("차단봉이 아직 열리지 않았습니다.");
        return;
      }

      const nextUrl = '/springai3/carwash_plan?plate=' + encodeURIComponent(this.plate);
      window.location.href = nextUrl;
    },

    // ===== UI helper들 =====
    enableNextButton:function(){
      const btn = document.getElementById('cw_entry_next');
      btn.disabled = false;
      btn.style.visibility = 'visible';
      btn.innerText = '다음 단계로';
    },

    disableNextButton:function(hide){
      const btn = document.getElementById('cw_entry_next');
      btn.disabled = true;
      btn.innerText = '다음 단계로';
      if (hide) {
        btn.style.visibility = 'hidden';
      } else {
        btn.style.visibility = 'visible';
      }
    },

    updateOpenButtonState:function(){
      const openBtn = document.getElementById('cw_entry_open');

      // plate 없으면 못 열게
      if (!this.plate) {
        openBtn.disabled = true;
        openBtn.innerText = '차단봉 올리기';
        return;
      }

      // 이미 barrierUp이면 더 못 누르게
      if (this.barrierUp) {
        openBtn.disabled = true;
        openBtn.innerText = '차단봉 열림완료';
      } else {
        openBtn.disabled = false;
        openBtn.innerText = '차단봉 올리기';
      }
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> cw_entry.init());
</script>


<div class="cw-container col-sm-10 mx-auto">
  <div class="cw-header">
    <h2>🚗 세차장 — 입차 & 차단봉 제어</h2>
    <p class="text-muted">차량 번호판 인식 후, 차단봉 상태를 제어합니다. 차단봉이 올라가야만 다음 단계로 이동할 수 있습니다.</p>
  </div>

  <div class="row g-3">
    <!-- 업로드/미리보기 -->
    <div class="col-sm-6">
      <span class="input-group-text">📸 번호판이 보이는 차량 사진 업로드</span>
      <input id="cw_entry_attach" class="form-control" type="file"/>
      <img id="cw_entry_preview" alt="미리보기 이미지"/>
    </div>

    <!-- 입차 처리 -->
    <div class="col-sm-2 d-flex align-items-end">
      <button type="button" class="btn btn-primary w-100 cw-btn" id="cw_entry_btn">
        입차 처리
      </button>
    </div>

    <!-- 로딩 스피너 -->
    <div class="col-sm-2 d-flex align-items-end">
      <button class="btn cw-spinner-btn w-100 cw-btn" disabled>
        <span class="spinner-border spinner-border-sm" id="cw_entry_spinner"></span>
        Loading..
      </button>
    </div>

    <!-- 차단봉 올리기 (새로 추가된 버튼) -->
    <div class="col-sm-2 d-flex align-items-end">
      <button type="button" class="btn btn-warning w-100 cw-btn" id="cw_entry_open">
        차단봉 올리기
      </button>
    </div>

    <!-- 다음 단계로 -->
    <div class="col-sm-2 d-flex align-items-end">
      <button type="button" class="btn btn-success w-100 cw-btn" id="cw_entry_next">
        다음 단계로
      </button>
    </div>
  </div>

  <div id="cw_entry_result" class="p-3 mt-4"></div>
</div>
