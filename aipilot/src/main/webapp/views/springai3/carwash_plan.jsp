<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<script>
  let cw_plan = {
    plate: null,
    orderId: null,
    parsedRecipe: null, // 여기 보관

    init:function(){
      this.plate = this.getPlateFromQuery();
      if(!this.plate){
        alert("plate 파라미터가 없습니다. 입차 단계부터 다시 진행하세요.");
      }

      document.getElementById('cw_plan_attach').addEventListener('change', ()=> this.preview());
      document.getElementById('cw_plan_btn').addEventListener('click', ()=> this.planAndExecute());
      document.getElementById('cw_plan_next').addEventListener('click', ()=> this.goNext());

      document.getElementById('cw_plan_spinner').style.visibility = 'hidden';

      // plate 표시
      document.getElementById('cw_plan_plate_label').innerText = this.plate ? this.plate : '(없음)';

      // "다음 단계로" 버튼은 처음엔 비활성화/숨김
      document.getElementById('cw_plan_next').disabled = true;
      document.getElementById('cw_plan_next').style.visibility = 'hidden';

      // 결과 표시 영역 초기화
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
      // data = { orderId:'W-xxx', recipeJson:'{...}', status:'RUNNING' }

      this.orderId = data.orderId || null;

      // textarea에는 그대로 원본 json 문자열 유지 (디버깅용은 이제 숨겨둘 거라 화면엔 안 나옴)
      document.getElementById('cw_plan_recipe_raw').value = data.recipeJson || '';

      // 예쁘게 뿌리기 위해 파싱 시도
      let pretty = null;
      try {
        pretty = JSON.parse(data.recipeJson);
      } catch(e) {
        console.warn('JSON parse error:', e);
      }
      this.parsedRecipe = pretty;

      // 예쁘게 카드로 만들기
      if(pretty){
        this.renderRecipe(pretty);
      }

      // 다음 단계 버튼 활성화
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

      // -------------------------
      // 1) 단계별 카드(recipe[])
      // -------------------------
      const steps = recipeObj.recipe;
      if (Array.isArray(steps) && steps.length > 0) {

        steps.forEach((stage, idx) => {
          let stepTitle   = `STEP ${idx+1}`;
          let nozzle      = '-';
          let pressure    = '-';
          let chem        = '-';
          let duration    = '-';
          let freeText    = null; // 문자열 케이스용

          if (typeof stage === 'string') {
            // LLM이 "문장 리스트"로만 준 경우
            freeText = stage;

          } else if (typeof stage === 'object' && stage !== null) {
            // 우리가 기대했던 구조화된 오브젝트인 경우
            if (stage.step) {
              stepTitle = stage.step;
            }
            nozzle   = stage.nozzle ?? '-';
            if (stage.pressureBar !== undefined) {
              pressure = stage.pressureBar + ' bar';
            }
            chem     = stage.chem ?? stage.chemCode ?? '-';
            if (stage.durationSec !== undefined) {
              duration = stage.durationSec + '초';
            } else if (stage.durationMin !== undefined) {
              duration = stage.durationMin + '분';
            }
          }

          let bodyHtml = '';
          if (freeText) {
            bodyHtml = `
              <div style="font-size:0.9rem; line-height:1.4;">
                ${freeText}
              </div>
            `;
          } else {
            bodyHtml = `
              <div style="font-size:0.9rem; line-height:1.4;">
                <div><b>노즐:</b> ${nozzle}</div>
                <div><b>압력:</b> ${pressure}</div>
                <div><b>케미컬:</b> ${chem}</div>
                <div><b>시간:</b> ${duration}</div>
              </div>
            `;
          }

          const cardHtml = `
            <div class="card mb-2" style="border:1px solid #ccc; border-radius:8px;">
              <div class="card-body" style="padding:12px;">
                <h6 style="margin:0 0 8px 0; font-weight:bold;">${idx+1}. ${stepTitle}</h6>
                ${bodyHtml}
              </div>
            </div>
          `;
          cardsContainer.insertAdjacentHTML('beforeend', cardHtml);
        });

      } else {
        cardsContainer.innerHTML = `
          <div class="alert alert-warning" style="font-size:0.9rem;">
            recipe 정보를 해석할 수 없습니다.
          </div>
        `;
      }

      // -------------------------
      // 2) 요약 블록 (주문번호, 가격, ETA)
      // -------------------------
      const price = (recipeObj.price !== undefined) ? recipeObj.price + '원' : '-';
      const eta   = (recipeObj.etaMin !== undefined) ? recipeObj.etaMin + '분 예상' : '-';
      const order = this.orderId ? this.orderId : '-';

      metaContainer.innerHTML = `
        <div class="card mb-2" style="border:1px solid #ddd; border-radius:8px;">
          <div class="card-body" style="padding:12px;">
            <h6 style="font-weight:bold; margin:0 0 8px 0;">요약</h6>
            <div style="font-size:0.9rem; line-height:1.4;">
              <div><b>주문 번호:</b> ${order}</div>
              <div><b>예상 가격:</b> ${price}</div>
              <div><b>예상 소요:</b> ${eta}</div>
            </div>
          </div>
        </div>
      `;

      // -------------------------
      // 3) 안전/주의사항
      // -------------------------
      if (Array.isArray(recipeObj.safetyNotes) && recipeObj.safetyNotes.length > 0) {
        let listHtml = '<ul style="padding-left:18px; margin:0;">';
        recipeObj.safetyNotes.forEach(note => {
          listHtml += `<li style="font-size:0.9rem; margin-bottom:4px;">${note}</li>`;
        });
        listHtml += '</ul>';

        safetyContainer.innerHTML = `
          <div class="card mb-2" style="border:1px solid #f5c6cb; border-radius:8px; background:#fff5f5;">
            <div class="card-body" style="padding:12px;">
              <h6 style="font-weight:bold; margin:0 0 8px 0; color:#c00;">안전/주의사항</h6>
              ${listHtml}
            </div>
          </div>
        `;
      } else {
        safetyContainer.innerHTML = `
          <div class="card mb-2" style="border:1px solid #eee; border-radius:8px;">
            <div class="card-body" style="padding:12px;">
              <h6 style="font-weight:bold; margin:0 0 8px 0;">안전/주의사항</h6>
              <div style="font-size:0.9rem;">특이사항 없음</div>
            </div>
          </div>
        `;
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

<div class="col-sm-10">
  <h2>세차장 — 레시피 생성 & 실행</h2>

  <div class="row">
    <!-- 왼쪽: plate / 이미지 업로드 -->
    <div class="col-sm-4">
      <span class="input-group-text">번호판</span>
      <div class="form-control" readonly id="cw_plan_plate_label" style="background:#eee;"></div>

      <span class="input-group-text mt-3">차량 상태 사진 (얼마나 더러운지 보이는 각도)</span>
      <input id="cw_plan_attach" class="form-control" type="file"/>
      <img id="cw_plan_preview" style="max-height:150px; margin-top:10px;" />
    </div>

    <!-- 가운데: 버튼들 -->
    <div class="col-sm-2 d-flex flex-column align-items-start mt-2 mt-sm-4">
      <button type="button" class="btn btn-primary w-100 mb-2" id="cw_plan_btn">
        레시피 생성 + 실행
      </button>

      <button class="btn btn-primary w-100 mb-2" disabled >
        <span class="spinner-border spinner-border-sm" id="cw_plan_spinner"></span>
        Loading..
      </button>

      <button type="button" class="btn btn-success w-100" id="cw_plan_next">
        다음 단계로
      </button>
    </div>

    <!-- 오른쪽: 요약/단계/안전 표시 -->
    <div class="col-sm-6">
      <!-- 상세 단계 카드들 -->
      <div id="cw_plan_recipe_cards"></div>

      <!-- 가격/ETA 등 요약 -->
      <div id="cw_plan_meta"></div>

      <!-- 안전 주의사항 -->
      <div id="cw_plan_safety"></div>
    </div>
  </div>

  <!-- 디버그 블록은 숨김 처리 -->
  <div class="row mt-4" style="display:none;">
    <div class="col-sm-12">
      <span class="input-group-text">생성된 레시피(JSON 원본)</span>
      <textarea id="cw_plan_recipe_raw" class="form-control"
                style="height:180px; font-family:monospace;"></textarea>
    </div>
  </div>

  <div id="cw_plan_result"
       class="container p-3 my-3 border"
       style="overflow:auto; min-height:100px; display:none;"></div>
</div>
