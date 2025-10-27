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
    loadVehicles();
  });

  async function loadVehicles(){
    try {
      const res = await fetch('/ai6/admin/vehicles');
      const data = await res.json();
      // data: [ {plate, size, color, lastWashAt}, ... ]

      const tbody = document.getElementById('veh_tbody');
      tbody.innerHTML = '';

      if (!Array.isArray(data) || data.length === 0) {
        tbody.innerHTML = `
        <tr>
          <td colspan="4" class="text-center text-muted">등록된 차량이 없습니다.</td>
        </tr>`;
        return;
      }

      data.forEach(v => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
        <td><b>${v.plate || ''}</b></td>
        <td>${humanSize(v.size)}</td>
        <td>${v.color || ''}</td>
        <td>${v.lastWashAt || ''}</td>
      `;
        tbody.appendChild(tr);
      });
    } catch(e){
      console.error(e);
      document.getElementById('veh_tbody').innerHTML = `
      <tr><td colspan="4" class="text-danger">데이터를 불러오는 중 오류가 발생했습니다.</td></tr>`;
    }
  }

  function humanSize(sz){
    if(!sz) return '';
    // DB에 "SUV","midsize","compact" 등 들어갈 수 있음 → 보기좋게
    if(sz.toLowerCase() === 'suv') return 'SUV';
    if(sz.toLowerCase() === 'midsize') return '중형';
    if(sz.toLowerCase() === 'compact') return '소형';
    return sz;
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
          <h2 class="mb-4">등록 차량 현황</h2>

          <div class="card mb-3" style="border:1px solid #ccc;">
            <div class="card-body" style="padding:12px;">
              <p class="mb-1" style="font-size:0.9rem; color:#555;">
                세차장에 방문한 차량들의 번호판 / 차 크기 / 색상 / 마지막 세차 시각입니다.<br/>
                (색상과 크기는 LLM이 차량 이미지를 분석하면서 자동으로 기록된 값일 수 있습니다.)
              </p>
            </div>
          </div>

          <table class="table table-striped table-sm">
            <thead class="table-light">
            <tr>
              <th style="width:140px;">번호판</th>
              <th style="width:100px;">차 크기</th>
              <th style="width:100px;">색상</th>
              <th>마지막 세차 시간</th>
            </tr>
            </thead>
            <tbody id="veh_tbody">
            <!-- JS로 채움 -->
            <tr><td colspan="4" class="text-muted text-center">불러오는 중...</td></tr>
            </tbody>
          </table>

        </div>
      </div>
    </div>
  </div>

</div>