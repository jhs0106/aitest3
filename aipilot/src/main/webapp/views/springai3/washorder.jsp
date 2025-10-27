<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<div class="col-sm-10">
  <h2 class="mb-4">세차 주문 / 실행 단계</h2>

  <div class="card mb-3" style="border:1px solid #ccc;">
    <div class="card-body" style="padding:12px; font-size:0.9rem; color:#555;">
      <p class="mb-2">
        최근 생성된 세차 주문(wash_order)과 각 단계별 진행 로그(wash_log)를 보여줍니다.
      </p>
      <ul style="padding-left:20px; margin:0; font-size:0.85rem; line-height:1.4;">
        <li><b>단계 이름</b>은 LLM이 차량 상태에 맞춰 만들어 낸 작업 설명입니다 (ex. "부드러운 스펀지로 ...").</li>
        <li><b>압력(bar)</b>, <b>케미컬</b>은 실제 장비 설정(또는 계획) 정보를 의미합니다.</li>
        <li><b>결과</b>는 PENDING / OK / ERROR 등으로 마킹할 수 있습니다.</li>
      </ul>
    </div>
  </div>

  <div id="orders_container">
    <!-- JS가 카드들 주입 -->
    <div class="text-muted" style="font-size:0.9rem;">불러오는 중...</div>
  </div>
</div>

<script>
  // 페이지 전체가 그려지고 나서 시작
  document.addEventListener('DOMContentLoaded', initOrdersPage);

  function initOrdersPage() {
    const container = document.getElementById('orders_container');
    if (!container) {
      console.warn('[orders] orders_container not found yet. Abort init.');
      return;
    }
    loadOrders();
  }

  async function loadOrders(){
    const container = document.getElementById('orders_container');
    if (!container) {
      // 방어 코드 (혹시라도 center 교체 타이밍 문제 있을 때)
      console.warn('[orders] orders_container missing at loadOrders() call.');
      return;
    }

    try {
      const res = await fetch('/ai6/admin/orders');
      const data = await res.json();
      // data: [
      //   {
      //     orderId, plate, status, price, etaMin, createdAt,
      //     steps: [
      //       { stepIdx, stepName, startedAt, endedAt, pressureBar, chemCode, result },
      //       ...
      //     ]
      //   }, ...
      // ]

      container.innerHTML = '';

      if (!Array.isArray(data) || data.length === 0) {
        container.innerHTML = `
        <div class="alert alert-secondary" style="font-size:0.9rem;">
          아직 세차 주문 기록이 없습니다.
        </div>`;
        return;
      }

      data.forEach(o => {
        const card = buildOrderCard(o);
        container.appendChild(card);
      });

    } catch(e){
      console.error(e);
      container.innerHTML = `
      <div class="alert alert-danger" style="font-size:0.9rem;">
        주문/단계 데이터를 불러오는 중 오류가 발생했습니다.
      </div>`;
    }
  }

  function buildOrderCard(o){
    // 주문 헤더
    const headerHtml = `
    <div class="card-header" style="font-weight:bold; font-size:0.95rem;">
      <div style="margin-bottom:4px;">
        주문번호: <span style="color:#0069d9;">${o.orderId || ''}</span>
      </div>
      <div style="font-size:0.85rem; line-height:1.4;">
        <b>번호판:</b> ${o.plate || ''} /
        <b>상태:</b> ${o.status || ''} /
        <b>예상가격:</b> ${o.price != null ? o.price + '원' : '-'} /
        <b>ETA:</b> ${o.etaMin != null ? (o.etaMin + '분') : '-'} /
        <b>생성시각:</b> ${o.createdAt || ''}
      </div>
    </div>
  `;

    // 단계 테이블 rows
    let stepsRows = '';
    (o.steps || []).forEach(s => {
      stepsRows += `
      <tr>
        <td>${s.stepIdx}</td>
        <td>${escapeHtml(s.stepName)}</td>
        <td>${s.startedAt || ''}</td>
        <td>${s.endedAt || ''}</td>
        <td>${s.pressureBar != null ? s.pressureBar : ''}</td>
        <td>${s.chemCode || ''}</td>
        <td>${s.result || ''}</td>
      </tr>
    `;
    });

    if (stepsRows === '') {
      stepsRows = `
      <tr>
        <td colspan="7" class="text-muted text-center" style="font-size:0.85rem;">
          아직 단계 로그가 없습니다.
        </td>
      </tr>`;
    }

    const bodyHtml = `
    <div class="card-body" style="padding:12px;">
      <table class="table table-sm table-bordered mb-0">
        <thead class="table-light">
          <tr style="font-size:0.8rem;">
            <th style="width:50px;">idx</th>
            <th>단계 이름</th>
            <th style="width:140px;">시작</th>
            <th style="width:140px;">종료</th>
            <th style="width:80px;">압력(bar)</th>
            <th style="width:120px;">케미컬</th>
            <th style="width:90px;">결과</th>
          </tr>
        </thead>
        <tbody style="font-size:0.8rem; line-height:1.4;">
          ${stepsRows}
        </tbody>
      </table>
    </div>
  `;

    const card = document.createElement('div');
    card.className = "card mb-3";
    card.style.border = "1px solid #ccc";
    card.innerHTML = headerHtml + bodyHtml;

    return card;
  }

  // 간단 XSS 방지용
  function escapeHtml(str){
    if(!str) return '';
    return String(str)
            .replace(/&/g,'&amp;')
            .replace(/</g,'&lt;')
            .replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;')
            .replace(/'/g,'&#039;');
  }
</script>
