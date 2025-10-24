<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="true" %>
<%--강제로 jstl el을 끈다? 근데 이거 하면sessionscope가 안됨--%>
<script>
  let cw_entry = {
    init:function(){
      $('#cw_entry_btn').on('click', ()=> this.entry());
      $('#cw_entry_spinner').css('visibility','hidden');
    },
    entry: async function(){
      const plate = $('#cw_entry_plate').val().trim();
      if(!plate){ alert('번호판을 입력하세요.'); return; }
      $('#cw_entry_spinner').css('visibility','visible');
      //
      const res = await fetch('/ai6/entry-detect?plate=' + encodeURIComponent(plate), { method:'POST' });
      const data = await res.json();
      //
      const html = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>입차 처리</h6>
            <p>번호판: <b>${data.plate}</b></p>
            <p>기존고객: <b>${data.knownCustomer ? '예' : '아니오'}</b></p>
            <p>차단봉: <b>${data.gate}</b></p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;
      $('#cw_entry_result').prepend(html);

      $('#cw_entry_spinner').css('visibility','hidden');
    }
  }
  $(()=> cw_entry.init());
</script>

<div class="col-sm-10">
  <h2>세차장 — 입차 & 차단봉</h2>
  <div class="row">
    <div class="col-sm-6">
      <span class="input-group-text">번호판</span>
      <input id="cw_entry_plate" class="form-control" placeholder="예) 12가3456" />
    </div>
    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-primary w-100" id="cw_entry_btn">입차 처리</button>
    </div>
    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button class="btn btn-primary" disabled>
        <span class="spinner-border spinner-border-sm" id="cw_entry_spinner"></span>
        Loading..
      </button>
    </div>
  </div>

  <div id="cw_entry_result" class="container p-3 my-3 border" style="overflow:auto;height:300px;"></div>
</div>
