<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<script>
  let cw_prog = {
    plate:null,
    init:function(){
      this.plate = this.getPlateFromQuery();
      document.getElementById('cw_prog_plate_label').innerText = this.plate ? this.plate : '(없음)';

      document.getElementById('cw_prog_open').addEventListener('click', ()=> this.openGate());
      document.getElementById('cw_prog_close').addEventListener('click', ()=> this.closeGate());

      // 필요하다면 페이지 로드 시 자동 출차개방:
      // this.openGate();
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
        <div class="media border p-3">
          <div class="media-body">
            <h6>${title}</h6>
            <p>번호판: <b>${data.plate || ''}</b></p>
            <p>게이트: <b>${data.gate || ''}</b></p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;
      box.insertAdjacentHTML('afterbegin', html);
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> cw_prog.init());
</script>

<div class="col-sm-10">
  <h2>세차장 — 출차 / 차단봉 제어</h2>

  <div class="row">
    <div class="col-sm-3">
      <span class="input-group-text">번호판</span>
      <div class="form-control" id="cw_prog_plate_label" style="background:#eee;"></div>
    </div>

    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-success w-100" id="cw_prog_open">출차 게이트 열기</button>
    </div>

    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-danger w-100" id="cw_prog_close">출차 게이트 닫기</button>
    </div>
  </div>

  <div id="cw_prog_result" class="container p-3 my-3 border" style="overflow:auto; min-height:150px;"></div>
</div>
