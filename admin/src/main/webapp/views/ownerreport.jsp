<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>
<style>
  .owner-wrap {
    max-width: 1100px;
    margin: 20px auto;
    background: #f8f9fa;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    padding: 24px 28px;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans KR", sans-serif;
  }
  .owner-header h2 {
    font-weight:600;
    display:flex;
    align-items:center;
    font-size:1.3rem;
    color:#333;
    margin:0;
  }
  .owner-header .sub {
    font-size:0.8rem;
    color:#777;
    margin-top:6px;
  }

  .kpi-row {
    display:flex;
    flex-wrap:wrap;
    gap:12px;
    margin-top:20px;
    margin-bottom:24px;
  }
  .kpi-card {
    flex:1 1 160px;
    background:#fff;
    border-radius:12px;
    border:1px solid #e1e4e8;
    padding:16px;
    min-width:150px;
  }
  .kpi-label {
    font-size:0.8rem;
    color:#666;
    margin-bottom:4px;
  }
  .kpi-value {
    font-weight:600;
    font-size:1.2rem;
    color:#111;
  }
  .kpi-hint {
    font-size:0.7rem;
    color:#999;
    margin-top:4px;
  }

  .question-box {
    background:#fff;
    border-radius:12px;
    border:1px solid #e1e4e8;
    padding:16px;
    margin-bottom:16px;
  }
  .question-box textarea {
    width:100%;
    min-height:70px;
    font-size:0.9rem;
    padding:8px;
    border-radius:8px;
    border:1px solid #ccc;
    resize:vertical;
  }
  .ask-row {
    display:flex;
    flex-wrap:wrap;
    gap:12px;
    margin-top:12px;
  }
  .ask-row .btn-ask {
    background:#0069d9;
    color:#fff;
    border:none;
    border-radius:8px;
    padding:10px 16px;
    font-size:0.9rem;
    cursor:pointer;
  }
  .ask-row .btn-ask:disabled {
    background:#8fb1e0;
    cursor:default;
  }
  .ask-row .spinner-area {
    font-size:0.8rem;
    color:#666;
    display:flex;
    align-items:center;
  }

  .answer-area {
    max-height:260px;
    overflow:auto;
    border-radius:12px;
    border:1px solid #e1e4e8;
    background:#fff;
    padding:16px;
  }
  .answer-item {
    display:flex;
    align-items:flex-start;
    border:1px solid #ddd;
    border-radius:10px;
    background:#fefefe;
    padding:12px;
    margin-bottom:12px;
  }
  .answer-content {
    flex:1;
    font-size:0.9rem;
    color:#222;
    line-height:1.4;
  }
  .answer-icon {
    margin-left:12px;
    flex-shrink:0;
  }
  .answer-icon img {
    width:48px;
    border-radius:50%;
    box-shadow:0 2px 4px rgba(0,0,0,0.1);
  }

  .owner-footer-hint {
    margin-top:16px;
    font-size:0.7rem;
    color:#999;
    text-align:center;
  }
</style>

<!-- Begin Page Content -->
<div class="container-fluid">

  <!-- Page Heading -->
  <div class="d-sm-flex align-items-center justify-content-between mb-4">
    <h1 class="h3 mb-0 text-gray-800">GateLog
    </h1>
  </div>

  <!-- Content Row -->
  <div class="row d-none d-md-flex">
    <div class="col-xl-8 col-lg-7">
      <div class="card shadow mb-4">
        <!-- Card Header - Dropdown -->
        <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
          <h6 class="m-0 font-weight-bold text-primary">Customer Chat</h6>
        </div>
        <!-- Card Body -->
        <div class="card-body">
          <div class="owner-wrap">
            <div class="owner-header">
              <h2>📊 사장님 보고서</h2>
              <div class="sub">
                오늘 실적 / 이상 징후 / 고객 유지 상황을 요약하고,
                질문에 따라 액션까지 제안합니다.
              </div>
            </div>

            <!-- KPI 카드들 -->
            <div class="kpi-row">

              <div class="kpi-card">
                <div class="kpi-label">방문 차량 수 (오늘)</div>
                <div class="kpi-value" id="kpi_visit">-</div>
                <div class="kpi-hint">입차 로그 기준</div>
              </div>

              <div class="kpi-card">
                <div class="kpi-label">매출 합계 (오늘)</div>
                <div class="kpi-value" id="kpi_revenue">-</div>
                <div class="kpi-hint">wash_order.price 합계</div>
              </div>

              <div class="kpi-card">
                <div class="kpi-label">평균 단가</div>
                <div class="kpi-value" id="kpi_ticket">-</div>
                <div class="kpi-hint">오늘 건 기준</div>
              </div>

              <div class="kpi-card">
                <div class="kpi-label">장비 이상 의심</div>
                <div class="kpi-value" id="kpi_bay">-</div>
                <div class="kpi-hint">저압 세척 건수</div>
              </div>

              <div class="kpi-card">
                <div class="kpi-label">휴면 단골</div>
                <div class="kpi-value" id="kpi_dormant">-</div>
                <div class="kpi-hint">2주 이상 미방문</div>
              </div>

            </div>

            <!-- 질문 입력 / 버튼 -->
            <div class="question-box">
              <label style="font-size:0.85rem; color:#444; font-weight:600; display:block; margin-bottom:6px;">
                사장님 질문
              </label>
              <textarea id="ownerQuestion"
                        placeholder="예) 오늘 매출이 어제보다 떨어졌어? 원인 뭐 같아?&#10;예) 폼샴푸 라인 문제 있어 보이냐? 점검 필요해?"
              ></textarea>

              <div class="ask-row">
                <button id="ownerAskBtn" class="btn-ask">질문 전송</button>
                <div class="spinner-area">
          <span id="ownerSpinner" style="visibility:hidden;">
            <span class="spinner-border spinner-border-sm"></span>
            &nbsp;생각중..
          </span>
                </div>
              </div>
            </div>

            <!-- AI 답변 영역 -->
            <div class="answer-area" id="ownerAnswers">
              <!-- JS에서 prependAnswer()로 카드가 쌓인다 -->
            </div>

            <div class="owner-footer-hint">
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>

</div>

<script>
  let ownerPage = {
    init:function(){
      // KPI 불러오기
      this.loadSummary();

      // 버튼/입력 이벤트
      document.getElementById('ownerAskBtn').addEventListener('click', ()=> this.askAI());
      this.setLoading(false);
    },

    setLoading:function(isLoading){
      const btn = document.getElementById('ownerAskBtn');
      const spin = document.getElementById('ownerSpinner');
      if(isLoading){
        btn.disabled = true;
        spin.style.visibility = 'visible';
      }else{
        btn.disabled = false;
        spin.style.visibility = 'hidden';
      }
    },

    async loadSummary(){
      try {
        const res = await fetch('/ai6/admin/summary/today');
        const data = await res.json();
        // data = {
        //   visitCount, totalRevenue, avgTicket,
        //   suspiciousBayCount, dormantCustomerCount
        // }

        document.getElementById('kpi_visit').innerText = (data.visitCount ?? 0) + " 대";
        document.getElementById('kpi_revenue').innerText = (data.totalRevenue ?? 0) + " 원";
        document.getElementById('kpi_ticket').innerText = (data.avgTicket ?? 0) + " 원";
        document.getElementById('kpi_bay').innerText = (data.suspiciousBayCount ?? 0) + " 건";
        document.getElementById('kpi_dormant').innerText = (data.dormantCustomerCount ?? 0) + " 명";

      } catch(e){
        console.error(e);
        // 실패 시 기본값
        document.getElementById('kpi_visit').innerText = "-";
        document.getElementById('kpi_revenue').innerText = "-";
        document.getElementById('kpi_ticket').innerText = "-";
        document.getElementById('kpi_bay').innerText = "-";
        document.getElementById('kpi_dormant').innerText = "-";
      }
    },

    async askAI(){
      const q = document.getElementById('ownerQuestion').value.trim();
      if(!q){
        alert("사장님, 궁금하신 내용을 적어주세요.");
        return;
      }

      this.setLoading(true);

      try {
        const res = await fetch('/ai6/admin/owner-ask', {
          method: 'POST',
          headers: {'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
          body: new URLSearchParams({ question: q })
        });
        const data = await res.json();
        // data.answer = LLM 답변(한글 보고서)

        this.prependAnswer(q, data.answer || '(응답 없음)');
        document.getElementById('ownerQuestion').value = '';

      } catch(e){
        console.error(e);
        this.prependAnswer(
                q,
                "에러가 발생했습니다. 서버 로그를 확인해주세요."
        );
      }

      this.setLoading(false);
    },

    prependAnswer:function(question, answer){
      const box = document.getElementById('ownerAnswers');

      const html = `
        <div class="answer-item">
          <div class="answer-content">
            <div style="font-weight:600; color:#0069d9; margin-bottom:6px;">
              Q. ${question}
            </div>
            <div style="white-space:pre-wrap;">${this.escapeHtml(answer)}</div>
          </div>
          <div class="answer-icon">
            <img src="/image/assistant.png" alt="AI"/>
          </div>
        </div>
      `;
      box.insertAdjacentHTML('afterbegin', html);
    },

    escapeHtml:function(str){
      if(!str) return "";
      return str
              .replace(/&/g,"&amp;")
              .replace(/</g,"&lt;")
              .replace(/>/g,"&gt;");
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> ownerPage.init());
</script>