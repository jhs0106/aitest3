<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="true" %>

<script>
  let cw_plan = {
    lastRecipeJson: '',
    init:function(){
      $('#cw_plan_btn').on('click', ()=> this.plan());
      $('#cw_exec_btn').on('click', ()=> this.exec());
      $('#cw_plan_spinner').css('visibility','hidden');
    },
    plan: async function(){
      const plate = $('#cw_plan_plate').val().trim();
      const soil  = $('#cw_plan_soil').val();
      const pm10  = $('#cw_plan_pm10').val();
      const rain  = $('#cw_plan_rain').val();
      if(!plate){ alert('번호판을 입력하세요.'); return; }

      $('#cw_plan_spinner').css('visibility','visible');
      $('#cw_plan_recipe').val('');

      const body = JSON.stringify({ vision:{ soilLevel: soil }, weather:{ pm10: Number(pm10||0), rainProb: Number(rain||0) } });

      const response = await fetch('/ai6/plan?plate=' + encodeURIComponent(plate), {
        method:'POST',
        headers:{ 'Content-Type': 'application/json', 'Accept':'text/plain' },
        body
      });
      //
      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let content = '';
      while(true){
        const {value, done} = await reader.read();
        if(done) break;
        const chunk = decoder.decode(value);
        content += chunk;
        $('#cw_plan_recipe').val(content);
      }
      this.lastRecipeJson = content.trim();
      $('#cw_plan_spinner').css('visibility','hidden');
    },
    exec: async function(){
      const orderId = $('#cw_plan_order').val().trim() || ('W-'+crypto.randomUUID());
      let recipe = $('#cw_plan_recipe').val().trim() || this.lastRecipeJson;
      if(!recipe){ alert('먼저 레시피를 생성하세요.'); return; }

      const res = await fetch('/ai6/execute?orderId=' + encodeURIComponent(orderId), {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        body: recipe
      });
      const text = await res.text();

      const html = `
        <div class="media border p-3">
          <div class="media-body">
            <h6>실행 결과</h6>
            <p>Order: <b>${orderId}</b></p>
            <p>Status: <b>${text}</b></p>
          </div>
          <img src="/image/assistant.png" class="ml-3 mt-3 rounded-circle" style="width:60px;">
        </div>`;
      $('#cw_plan_result').prepend(html);
    }
  }
  $(()=> cw_plan.init());
</script>

<div class="col-sm-10">
  <h2>세차장 — 레시피 생성 & 실행</h2>

  <div class="row">
    <div class="col-sm-3">
      <span class="input-group-text">번호판</span>
      <input id="cw_plan_plate" class="form-control" placeholder="예) 12가3456"/>
    </div>
    <div class="col-sm-3">
      <span class="input-group-text">오염도</span>
      <select id="cw_plan_soil" class="form-control">
        <option value="light">light</option>
        <option value="medium" selected>medium</option>
        <option value="heavy">heavy</option>
      </select>
    </div>
    <div class="col-sm-2">
      <span class="input-group-text">PM10</span>
      <input id="cw_plan_pm10" class="form-control" type="number" placeholder="예) 60"/>
    </div>
    <div class="col-sm-2">
      <span class="input-group-text">강수확률(%)</span>
      <input id="cw_plan_rain" class="form-control" type="number" placeholder="예) 10"/>
    </div>
    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button type="button" class="btn btn-primary w-100" id="cw_plan_btn">레시피 생성</button>
    </div>
  </div>

  <div class="row mt-3">
    <div class="col-sm-8">
      <span class="input-group-text">레시피(JSON)</span>
      <textarea id="cw_plan_recipe" class="form-control" style="height:220px;"></textarea>
    </div>
    <div class="col-sm-2">
      <span class="input-group-text">Order ID</span>
      <input id="cw_plan_order" class="form-control" placeholder="미입력시 자동 생성"/>
      <button type="button" class="btn btn-success mt-3 w-100" id="cw_exec_btn">실행</button>
    </div>
    <div class="col-sm-2 d-flex align-items-end mt-2 mt-sm-4">
      <button class="btn btn-primary" disabled >
        <span class="spinner-border spinner-border-sm" id="cw_plan_spinner"></span>
        Loading..
      </button>
    </div>
  </div>

  <div id="cw_plan_result" class="container p-3 my-3 border" style="overflow:auto;height:220px;"></div>
</div>
