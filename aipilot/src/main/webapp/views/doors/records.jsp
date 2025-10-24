<%-- aipilot/src/main/webapp/views/doors/records.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<script>
  let doorRecords = {
    init: function() {
      // 새로고침 버튼 ID를 사용
      $('#refreshBtn').click(() => this.fetchRecords());
      this.fetchRecords();
    },
    fetchRecords: async function() {
      $('#recordTableBody').html('<tr><td colspan="4" class="text-center"><span class="spinner-border spinner-border-sm"></span> 기록을 불러오는 중...</td></tr>');
      try {
        // API 호출 및 JSON 데이터 수신
        const response = await fetch('/doors/api/records');
        if (!response.ok) throw new Error('API 호출 실패');
        const records = await response.json();
        this.renderRecords(records);
      } catch (e) {
        console.error(e);
        $('#recordTableBody').html('<tr><td colspan="4" class="text-center text-danger">출입 기록을 불러오는 데 실패했습니다.</td></tr>');
      }
    },
    renderRecords: function(records) {
      if (records.length === 0) {
        $('#recordTableBody').html('<tr><td colspan="4" class="text-center text-muted">표시할 출입 기록이 없습니다.</td></tr>');
        return;
      }

      let html = records.map(record => {
        const statusClass = record.status === 'SUCCESS' ? 'table-success' : 'table-danger';
        const statusText = record.status === 'SUCCESS' ? '✅ 성공' : '❌ 실패';
        // 서버에서 이미 올바른 ISO 문자열로 넘어오고 있으므로 new Date() 사용은 유지
        const date = new Date(record.accessTime).toLocaleString('ko-KR');

        // [핵심 수정]: 템플릿 리터럴 대신 안전한 문자열 결합 사용
        return '<tr class="' + statusClass + '">'
                + '<td>' + record.id + '</td>'
                + '<td><strong>' + record.name + '</strong></td>'
                + '<td>' + statusText + '</td>'
                + '<td>' + date + '</td>'
                + '</tr>';
      }).join('');

      $('#recordTableBody').html(html);
    }
  }

  $(() => {
    doorRecords.init();
  });
</script>

<div class="col-sm-10">
  <h2>AI 출입 기록</h2>
  <p class="text-muted">AI 얼굴 인식 시스템을 통한 출입 기록 목록입니다.</p>

  <div class="row mb-3">
    <div class="col-sm-12">
      <button type="button" class="btn btn-secondary" id="refreshBtn">새로고침</button>
    </div>
  </div>

  <table class="table table-striped table-bordered">
    <thead>
    <tr>
      <th>ID</th>
      <th>사용자 이름</th>
      <th>상태</th>
      <th>출입 시각</th>
    </tr>
    </thead>
    <tbody id="recordTableBody">
    </tbody>
  </table>
</div>