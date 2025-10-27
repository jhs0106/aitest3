<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="true" %>
<style>
  #to {
    width: 400px;
    height: 200px;
    overflow: auto;
    border: 2px solid green;
  }
</style>

<script>
  document.addEventListener('DOMContentLoaded', () => {
    loadGateLogs();
  });

  async function loadGateLogs(){
    try {
      const res = await fetch('/ai6/admin/gate-logs');
      const data = await res.json();
      // data: [ {id, plate, eventType, loggedAt}, ... ]

      const tbody = document.getElementById('gate_tbody');
      tbody.innerHTML = '';

      if (!Array.isArray(data) || data.length === 0) {
        tbody.innerHTML = `
        <tr>
          <td colspan="4" class="text-center text-muted">기록이 없습니다.</td>
        </tr>`;
        return;
      }

      data.forEach(row => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
        <td>${row.id}</td>
        <td><b>${row.plate || ''}</b></td>
        <td>${translateEvent(row.eventType)}</td>
        <td>${row.loggedAt || ''}</td>
      `;
        tbody.appendChild(tr);
      });
    } catch (e) {
      console.error(e);
      document.getElementById('gate_tbody').innerHTML = `
      <tr><td colspan="4" class="text-danger">데이터를 불러오는 중 오류가 발생했습니다.</td></tr>`;
    }
  }

  // "ENTRY", "EXIT", "GATE_OPEN", "GATE_CLOSE" -> 사람이 보기 좋은 한글로
  function translateEvent(ev){
    switch(ev){
      case 'ENTRY': return '입차';
      case 'EXIT': return '출차';
      case 'GATE_OPEN': return '차단봉 열림';
      case 'GATE_CLOSE': return '차단봉 닫힘';
      default: return ev || '';
    }
  }
</script>
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
          <div class="table-responsive">
            <h2 class="mb-4">출입 / 차단봉 로그</h2>

            <div class="card mb-3" style="border:1px solid #ccc;">
              <div class="card-body" style="padding:12px;">
                <p class="mb-1" style="font-size:0.9rem; color:#555;">
                  최근 게이트 이벤트(입차/출차, 차단봉 OPEN/CLOSE)를 시간순으로 보여줍니다.
                </p>
              </div>
            </div>

            <table class="table table-striped table-sm">
              <thead class="table-light">
              <tr>
                <th style="width:80px;">ID</th>
                <th style="width:140px;">번호판</th>
                <th style="width:140px;">이벤트</th>
                <th>발생 시각</th>
              </tr>
              </thead>
              <tbody id="gate_tbody">
              <!-- JS로 채움 -->
              <tr><td colspan="4" class="text-muted text-center">불러오는 중...</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>

</div>