<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<style>
  body {
    background-color: #f5f6fa;
  }

  .cw-container {
    background: #ffffff;
    border-radius: 15px;
    padding: 30px;
    margin-top: 30px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }

  .cw-header {
    text-align: center;
    margin-bottom: 25px;
  }

  .cw-header h2 {
    font-weight: 600;
    color: #333;
  }

  .cw-header p {
    color: #777;
    font-size: 0.95rem;
  }

  #cw_prog_plate_label {
    background: #f1f3f5;
    border-radius: 8px;
    text-align: center;
    font-weight: 600;
    font-size: 1rem;
    padding: 10px;
    color: #222;
  }

  .cw-btn {
    height: 45px;
    font-weight: 600;
    font-size: 15px;
    border-radius: 8px;
    transition: all 0.3s ease;
  }

  #cw_prog_open {
    background-color: #28a745;
    border: none;
  }

  #cw_prog_open:hover {
    background-color: #218838;
    transform: scale(1.02);
  }

  #cw_prog_close {
    background-color: #dc3545;
    border: none;
  }

  #cw_prog_close:hover {
    background-color: #c82333;
    transform: scale(1.02);
  }

  #cw_prog_result {
    background: #fff;
    border-radius: 10px;
    box-shadow: inset 0 0 5px rgba(0,0,0,0.05);
    padding: 15px;
    margin-top: 25px;
  }

  .cw-log-entry {
    background: #f9fafb;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 10px;
    border-left: 5px solid #007bff;
    transition: background 0.3s ease;
  }

  .cw-log-entry:hover {
    background: #eef3ff;
  }

  .cw-log-entry h6 {
    font-weight: bold;
    color: #007bff;
    margin-bottom: 5px;
  }

  .cw-log-entry p {
    margin: 0;
    font-size: 0.9rem;
  }

  .cw-footer-note {
    text-align: center;
    color: #999;
    font-size: 0.9rem;
    margin-top: 20px;
  }
</style>

<script>
  // ⚙ 기능 로직 그대로 유지 (절대 수정 없음)
  let cw_prog = {
    plate:null,
    init:function(){
      this.plate = this.getPlateFromQuery();
      document.getElementById('cw_prog_plate_label').innerText = this.plate ? this.plate : '(없음)';

      document.getElementById('cw_prog_open').addEventListener('click', ()=> this.openGate());
      document.getElementById('cw_prog_close').addEventListener('click', ()=> this.closeGate());
    },

    getPlateFromQuery:function(){
      const params = new URLSearchParams(window.location.search);
      return params.get('plate');
    },

    async openGate(){
      if(!this.plate){ alert("plate 없음"); return; }
      const res = await fetch('/ai6/exit-gate?plate='+encodeURIComponent(this.plate)+'&action=open', {method:'POST'});
      const data = await res.json();
      this.logStatus("출차 게이트 OPEN", data);
    },

    async closeGate(){
      if(!this.plate){ alert("plate 없음"); return; }
      const res = await fetch('/ai6/exit-gate?plate='+encodeURIComponent(this.plate)+'&action=close', {method:'POST'});
      const data = await res.json();
      this.logStatus("출차 게이트 CLOSE", data);
    },

    logStatus(title, data){
      const box = document.getElementById('cw_prog_result');
      const html = `
        <div class="cw-log-entry">
          <h6>${title}</h6>
          <p>번호판: <b>${data.plate || ''}</b></p>
          <p>게이트 상태: <b>${data.gate || ''}</b></p>
        </div>`;
      box.insertAdjacentHTML('afterbegin', html);
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> cw_prog.init());
</script>

<div class="cw-container col-sm-10 mx-auto">
  <div class="cw-header">
    <h2>🚗 세차장 — 출차 / 차단봉 제어</h2>
    <p>출차 시 차량 번호판을 인식하여 차단봉을 제어합니다.</p>
  </div>

  <div class="row g-3">
    <div class="col-sm-3">
      <span class="input-group-text">번호판</span>
      <div class="form-control" id="cw_prog_plate_label"></div>
    </div>

    <div class="col-sm-3 d-flex align-items-end">
      <button type="button" class="btn cw-btn w-100" id="cw_prog_open">출차 게이트 열기</button>
    </div>

    <div class="col-sm-3 d-flex align-items-end">
      <button type="button" class="btn cw-btn w-100" id="cw_prog_close">출차 게이트 닫기</button>
    </div>
  </div>

  <div id="cw_prog_result"></div>

  <div class="cw-footer-note">
    ※ 출차 게이트 동작 상태는 실시간으로 반영됩니다.
  </div>
</div>
