<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<style>
  body {
    background-color: #f5f6fa;
  }

  .cw-container {
    background-color: #ffffff;
    border-radius: 15px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    padding: 30px;
    margin-top: 30px;
  }

  .cw-header {
    text-align: center;
    margin-bottom: 25px;
  }

  .cw-header h2 {
    font-weight: 600;
    color: #343a40;
  }

  .cw-header p {
    color: #888;
    font-size: 0.95rem;
  }

  #cw_plan_plate_label {
    background: #f1f1f1;
    font-weight: bold;
    text-align: center;
    border-radius: 8px;
    padding: 8px;
  }

  .cw-upload-box {
    border: 2px dashed #bbb;
    background: #fff;
    border-radius: 10px;
    padding: 15px;
    transition: all 0.3s ease;
  }

  .cw-upload-box:hover {
    border-color: #007bff;
    background-color: #f8fbff;
  }

  #cw_plan_preview {
    display: block;
    margin-top: 10px;
    border-radius: 10px;
    border: 1px solid #ddd;
    max-height: 150px;
    width: 100%;
    object-fit: contain;
  }

  .cw-btn {
    height: 45px;
    font-weight: 500;
    font-size: 15px;
    margin-bottom: 10px;
  }

  .cw-card {
    border-radius: 10px;
    border: 1px solid #ddd;
    transition: all 0.2s ease-in-out;
  }

  .cw-card:hover {
    transform: scale(1.02);
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  }

  .cw-meta-card, .cw-safety-card {
    border-radius: 10px;
    border: 1px solid #e0e0e0;
    background: #fafafa;
    padding: 15px;
    margin-top: 10px;
  }

  .cw-safety-card {
    background: #fff5f5;
    border: 1px solid #f5c6cb;
  }

  .cw-safety-card h6 {
    color: #c00;
    font-weight: bold;
  }

  .cw-footer-note {
    text-align: center;
    color: #999;
    margin-top: 20px;
    font-size: 0.9rem;
  }
</style>

<script>
  // 기존 JS 그대로 (한 글자도 변경 없음)
  let cw_plan = {
    plate: null,
    orderId: null,
    parsedRecipe: null,

    init:function(){
      this.plate = this.getPlateFromQuery();
      if(!this.plate){
        alert("plate 파라미터가 없습니다. 입차 단계부터 다시 진행하세요.");
      }

      document.getElementById('cw_plan_attach').addEventListener('change', ()=> this.preview());
      document.getElementById('cw_plan_btn').addEventListener('click', ()=> this.planAndExecute());
      document.getElementById('cw_plan_next').addEventListener('click', ()=> this.goNext());

      document.getElementById('cw_plan_spinner').style.visibility = 'hidden';

      document.getElementById('cw_plan_plate_label').innerText = this.plate ? this.plate : '(없음)';

      document.getElementById('cw_plan_next').disabled = true;
      document.getElementById('cw_plan_next').style.visibility = 'hidden';

      document.getElementById('cw_plan_recipe_cards').innerHTML = '';
      document.getElementById('cw_plan_meta').innerHTML = '';
      document.getElementById('cw_plan_safety').innerHTML = '';
    },

    getPlateFromQuery:function(){
      const params = new URLSearchParams(window.location.search);
      return params.get('plate');
    },

    preview:function(){
      const file = document.getElementById("cw_plan_attach").files[0];
      if(!file) return;
      const reader = new FileReader();
      reader.onload = (e)=>{
        document.getElementById("cw_plan_preview").src = e.target.result;
      };
      reader.readAsDataURL(file);
    },

    planAndExecute: async function(){
      const file = document.getElementById("cw_plan_attach").files[0];
      if(!file){
        alert("차량 전체 사진(오염 상태가 보이는 사진)을 업로드하세요.");
        return;
      }
      if(!this.plate){
        alert("차량 번호판 정보가 없습니다.");
        return;
      }

      document.getElementById('cw_plan_spinner').style.visibility = 'visible';
      document.getElementById('cw_plan_recipe_raw').value = '';
      document.getElementById('cw_plan_recipe_cards').innerHTML = '';
      document.getElementById('cw_plan_meta').innerHTML = '';
      document.getElementById('cw_plan_safety').innerHTML = '';

      const formData = new FormData();
      formData.append("attach", file);

      const response = await fetch('/ai6/plan-image?plate=' + encodeURIComponent(this.plate), {
        method:'POST',
        body: formData
      });

      const data = await response.json();
      this.orderId = data.orderId || null;
      document.getElementById('cw_plan_recipe_raw').value = data.recipeJson || '';

      let pretty = null;
      try {
        pretty = JSON.parse(data.recipeJson);
      } catch(e) {
        console.warn('JSON parse error:', e);
      }
      this.parsedRecipe = pretty;

      if(pretty){
        this.renderRecipe(pretty);
      }

      document.getElementById('cw_plan_next').disabled = false;
      document.getElementById('cw_plan_next').style.visibility = 'visible';
      document.getElementById('cw_plan_spinner').style.visibility = 'hidden';
    },

    renderRecipe:function(recipeObj){
      const cardsContainer = document.getElementById('cw_plan_recipe_cards');
      const metaContainer  = document.getElementById('cw_plan_meta');
      const safetyContainer= document.getElementById('cw_plan_safety');

      cardsContainer.innerHTML = '';
      metaContainer.innerHTML = '';
      safetyContainer.innerHTML = '';

      const steps = recipeObj.recipe;
      if (Array.isArray(steps) && steps.length > 0) {
        steps.forEach((stage, idx) => {
          let stepTitle = `STEP ${idx+1}`;
          let nozzle = '-', pressure = '-', chem = '-', duration = '-', freeText = null;
          if (typeof stage === 'string') freeText = stage;
          else if (typeof stage === 'object' && stage !== null) {
            if (stage.step) stepTitle = stage.step;
            nozzle = stage.nozzle ?? '-';
            if (stage.pressureBar !== undefined) pressure = stage.pressureBar + ' bar';
            chem = stage.chem ?? stage.chemCode ?? '-';
            if (stage.durationSec !== undefined) duration = stage.durationSec + '초';
            else if (stage.durationMin !== undefined) duration = stage.durationMin + '분';
          }

          const bodyHtml = freeText
                  ? `<div style="font-size:0.9rem;">${freeText}</div>`
                  : `<div style="font-size:0.9rem;"><b>노즐:</b> ${nozzle}<br><b>압력:</b> ${pressure}<br><b>케미컬:</b> ${chem}<br><b>시간:</b> ${duration}</div>`;

          const cardHtml = `
            <div class="cw-card mb-2">
              <div class="card-body">
                <h6>${idx+1}. ${stepTitle}</h6>
                ${bodyHtml}
              </div>
            </div>`;
          cardsContainer.insertAdjacentHTML('beforeend', cardHtml);
        });
      } else {
        cardsContainer.innerHTML = `<div class="alert alert-warning">recipe 정보를 해석할 수 없습니다.</div>`;
      }

      const price = (recipeObj.price !== undefined) ? recipeObj.price + '원' : '-';
      const eta   = (recipeObj.etaMin !== undefined) ? recipeObj.etaMin + '분 예상' : '-';
      const order = this.orderId ? this.orderId : '-';

      metaContainer.innerHTML = `
        <div class="cw-meta-card">
          <h6>요약</h6>
          <div><b>주문 번호:</b> ${order}</div>
          <div><b>예상 가격:</b> ${price}</div>
          <div><b>예상 소요:</b> ${eta}</div>
        </div>`;

      if (Array.isArray(recipeObj.safetyNotes) && recipeObj.safetyNotes.length > 0) {
        let listHtml = '<ul>';
        recipeObj.safetyNotes.forEach(note => { listHtml += `<li>${note}</li>`; });
        listHtml += '</ul>';
        safetyContainer.innerHTML = `
          <div class="cw-safety-card">
            <h6>안전/주의사항</h6>
            ${listHtml}
          </div>`;
      } else {
        safetyContainer.innerHTML = `
          <div class="cw-meta-card">
            <h6>안전/주의사항</h6>
            <div>특이사항 없음</div>
          </div>`;
      }
    },

    goNext:function(){
      if(!this.plate){
        alert("plate 정보가 없습니다.");
        return;
      }
      const nextUrl = '/springai3/carwash_progress?plate=' + encodeURIComponent(this.plate);
      window.location.href = nextUrl;
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> cw_plan.init());
</script>

<div class="cw-container col-sm-10 mx-auto">
  <div class="cw-header">
    <h2>🧽 세차장 — 레시피 생성 & 실행</h2>
    <p>AI가 차량 상태를 분석하여 최적의 세차 단계를 자동 구성합니다.</p>
  </div>

  <div class="row">
    <div class="col-sm-4">
      <span class="input-group-text">🚘 번호판</span>
      <div id="cw_plan_plate_label"></div>

      <span class="input-group-text mt-3">📷 차량 상태 사진 업로드</span>
      <div class="cw-upload-box">
        <input id="cw_plan_attach" class="form-control" type="file"/>
        <img id="cw_plan_preview" alt="미리보기"/>
      </div>
    </div>

    <div class="col-sm-2 d-flex flex-column align-items-start mt-4">
      <button type="button" class="btn btn-primary w-100 cw-btn" id="cw_plan_btn">세차장 가동</button>
      <button class="btn btn-secondary w-100 cw-btn" disabled>
        <span class="spinner-border spinner-border-sm" id="cw_plan_spinner"></span>
        Loading..
      </button>
      <button type="button" class="btn btn-success w-100 cw-btn" id="cw_plan_next">다음 단계로</button>
    </div>

    <div class="col-sm-6">
      <div id="cw_plan_recipe_cards"></div>
      <div id="cw_plan_meta"></div>
      <div id="cw_plan_safety"></div>
    </div>
  </div>

  <!-- 디버그 숨김 -->
  <div class="row mt-4" style="display:none;">
    <div class="col-sm-12">
      <textarea id="cw_plan_recipe_raw" class="form-control" style="height:180px;"></textarea>
    </div>
  </div>

  <div class="cw-footer-note">※ 레시피는 차량 상태와 세차 타입에 따라 자동으로 다르게 생성됩니다.</div>
</div>
