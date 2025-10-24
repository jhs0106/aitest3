<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="col-sm-9">
  <h3 class="mb-3">설문 입력 &amp; RAG 저장</h3>
  <p class="text-muted">내담자별 설문을 pgvector 벡터 스토어에 저장해 상담 맥락으로 활용합니다.</p>
  <form id="surveyForm" class="border rounded p-3 bg-light">
    <div class="form-row">
      <div class="form-group col-md-4">
        <label for="clientId">내담자 ID</label>
        <input type="text" class="form-control" id="clientId" placeholder="예: client-001" required>
      </div>
      <div class="form-group col-md-4">
        <label for="category">설문 유형</label>
        <select id="category" class="form-control" required>
          <option value="psychology">심리 상태 체크</option>
          <option value="career">진로 탐색</option>
        </select>
      </div>
      <div class="form-group col-md-4">
        <label for="sessionFocus">세션 초점</label>
        <input type="text" class="form-control" id="sessionFocus" placeholder="이번 상담의 핵심 주제">
      </div>
    </div>
    <div class="form-group">
      <label for="keyObservations">핵심 관찰 사항</label>
      <textarea class="form-control" id="keyObservations" rows="3" placeholder="내담자의 현재 상태, 감정, 환경 변화를 정리해 주세요."></textarea>
    </div>
    <div class="form-group">
      <label for="supportNeeds">필요한 지원</label>
      <textarea class="form-control" id="supportNeeds" rows="3" placeholder="내담자가 원하는 지원 또는 필요한 개입을 적어주세요."></textarea>
    </div>
    <div class="form-group">
      <label for="nextSteps">다음 단계</label>
      <textarea class="form-control" id="nextSteps" rows="3" placeholder="다음 상담 전 수행할 과제나 목표를 정리합니다."></textarea>
    </div>
    <button type="submit" class="btn btn-primary">설문 저장</button>
    <button type="button" class="btn btn-outline-secondary ml-2" id="resetForm">입력 초기화</button>
    <div id="surveyStatus" class="mt-3" role="alert" style="display:none;"></div>
  </form>

  <hr/>

  <div class="d-flex justify-content-between align-items-center mb-2">
    <h4 class="mb-0">저장된 설문 히스토리</h4>
    <div class="form-inline">
      <label class="mr-2" for="filterCategory">필터</label>
      <select id="filterCategory" class="form-control form-control-sm mr-2">
        <option value="">전체</option>
        <option value="psychology">심리 상태</option>
        <option value="career">진로 탐색</option>
      </select>
      <button type="button" class="btn btn-sm btn-outline-primary" id="reloadSurveys">조회</button>
    </div>
  </div>
  <div class="table-responsive">
    <table class="table table-sm table-striped" id="surveyTable">
      <thead class="thead-light">
      <tr>
        <th scope="col">작성 일시</th>
        <th scope="col">유형</th>
        <th scope="col">세션 초점</th>
        <th scope="col">핵심 관찰</th>
        <th scope="col">필요한 지원</th>
        <th scope="col">다음 단계</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td colspan="6" class="text-center text-muted">내담자 ID를 입력하면 설문 히스토리가 표시됩니다.</td>
      </tr>
      </tbody>
    </table>
  </div>
</div>

<script>
  (function () {
    const form = document.getElementById('surveyForm');
    const statusBox = document.getElementById('surveyStatus');
    const clientIdInput = document.getElementById('clientId');
    const categorySelect = document.getElementById('category');
    const sessionFocusInput = document.getElementById('sessionFocus');
    const keyObservationsInput = document.getElementById('keyObservations');
    const supportNeedsInput = document.getElementById('supportNeeds');
    const nextStepsInput = document.getElementById('nextSteps');
    const tableBody = document.querySelector('#surveyTable tbody');
    const filterCategory = document.getElementById('filterCategory');
    const reloadButton = document.getElementById('reloadSurveys');
    const resetButton = document.getElementById('resetForm');
    const surveysApiBase = '<c:url value="/ai1/api/surveys"/>';

    function showStatus(message, type) {
      statusBox.style.display = 'block';
      statusBox.className = type === 'success' ? 'alert alert-success' : 'alert alert-danger';
      statusBox.textContent = message;
    }

    function clearStatus() {
      statusBox.style.display = 'none';
      statusBox.className = '';
      statusBox.textContent = '';
    }

    function renderSurveys(surveys) {
      tableBody.innerHTML = '';
      if (!surveys || surveys.length === 0) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.className = 'text-center text-muted';
        cell.textContent = '조회된 설문이 없습니다. 새로운 기록을 추가해 보세요.';
        row.appendChild(cell);
        tableBody.appendChild(row);
        return;
      }
      surveys.forEach((survey) => {
        const row = document.createElement('tr');
        row.innerHTML =
                '<td>' + (survey.submittedAt || '') + '</td>' +
                '<td>' + (survey.category || '') + '</td>' +
                '<td>' + (survey.sessionFocus || '') + '</td>' +
                '<td>' + (survey.keyObservations || '') + '</td>' +
                '<td>' + (survey.supportNeeds || '') + '</td>' +
                '<td>' + (survey.nextSteps || '') + '</td>';
        tableBody.appendChild(row);
      });
    }

    async function loadSurveys() {
      const clientId = clientIdInput.value.trim();
      if (!clientId) {
        renderSurveys([]);
        return;
      }
      const params = new URLSearchParams();
      if (filterCategory.value) {
        params.set('category', filterCategory.value);
      }
      try {
        const queryString = params.toString();
        const baseClientPath = surveysApiBase + '/' + encodeURIComponent(clientId);
        const url = queryString ? baseClientPath + '?' + queryString : baseClientPath;
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error('설문을 조회할 수 없습니다.');
        }
        const data = await response.json();
        renderSurveys(Array.isArray(data) ? data : []);
      } catch (error) {
        showStatus(error.message, 'error');
      }
    }

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearStatus();
      const payload = {
        clientId: clientIdInput.value.trim(),
        category: categorySelect.value,
        sessionFocus: sessionFocusInput.value,
        keyObservations: keyObservationsInput.value,
        supportNeeds: supportNeedsInput.value,
        nextSteps: nextStepsInput.value
      };
      if (!payload.clientId || !payload.category) {
        showStatus('내담자 ID와 설문 유형을 입력해 주세요.', 'error');
        return;
      }
      try {
        const response = await fetch(surveysApiBase, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(errorText || '설문 저장에 실패했습니다.');
        }
        showStatus('설문이 저장되었습니다.', 'success');
        await loadSurveys();
      } catch (error) {
        showStatus(error.message, 'error');
      }
    });

    reloadButton.addEventListener('click', () => {
      clearStatus();
      loadSurveys();
    });

    clientIdInput.addEventListener('blur', () => {
      loadSurveys();
    });

    filterCategory.addEventListener('change', () => {
      loadSurveys();
    });

    resetButton.addEventListener('click', () => {
      form.reset();
      clearStatus();
      renderSurveys([]);
    });
  })();
</script>
