<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <title>자가 상담 설문 기록</title>

  <!-- 필요한 경우 부트스트랩/제이쿼리 등을 상단에 로드 -->
  <!-- <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css"> -->
  <!-- <script src="https://cdn.jsdelivr.net/npm/jquery@3.7.1/dist/jquery.min.js"></script> -->

  <script>
    // === DOMContentLoaded 이후 초기화 ===
    document.addEventListener('DOMContentLoaded', function initSelfSurveyPage() {
      // ===== DOM 엘리먼트 캐싱 =====
      const form = document.getElementById('surveyForm');
      const statusBox = document.getElementById('surveyStatus');
      const clientIdInput = document.getElementById('clientId');
      const categorySelect = document.getElementById('category');
      const formInfoBox = document.getElementById('formInfo');
      const guideContainer = document.getElementById('guideContainer');
      const questionContainer = document.getElementById('questionContainer');
      const selfReflectionInput = document.getElementById('selfReflection');
      const tableBody = document.querySelector('#surveyTable tbody');
      const filterCategory = document.getElementById('filterCategory');
      const reloadButton = document.getElementById('reloadSurveys');
      const resetButton = document.getElementById('resetForm');

      // ===== 서버 경로 안전 생성 (컨텍스트 패스 자동 반영) =====
      const surveysApiBase = '<c:url value="/ai1/api/surveys"/>';
      const formsApi = '<c:url value="/ai1/api/forms"/>';

      // ===== 라벨 매핑 =====
      const categoryLabels = {
        psychology: '감정 · 스트레스',
        career: '진로 탐색 부담도'
      };

      let currentForm = null;

      // ===== 상태 메시지 =====
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

      // ===== 폼 상단 안내(제목/설명) - EL 충돌 없는 DOM 조립 방식 =====
      function renderFormInfo(formObj) {
        if (!formObj) {
          formInfoBox.classList.add('d-none');
          formInfoBox.innerHTML = '';
          return;
        }
        const title = formObj.title || (formObj.category && categoryLabels[formObj.category]) || '';
        const description = formObj.description || '';

        formInfoBox.classList.remove('d-none');

        formInfoBox.innerHTML = '';
        const strong = document.createElement('strong');
        strong.textContent = title;
        formInfoBox.appendChild(strong);

        if (description) {
          formInfoBox.appendChild(document.createElement('br'));
          formInfoBox.appendChild(document.createTextNode(description));
        }
      }

      // ===== 점수 가이드 렌더링 =====
      function renderGuides(guides) {
        guideContainer.innerHTML = '';
        if (!guides || guides.length === 0) return;

        const card = document.createElement('div');
        card.className = 'card';

        const header = document.createElement('div');
        header.className = 'card-header';
        header.textContent = '점수 해석 안내';

        const list = document.createElement('ul');
        list.className = 'list-group list-group-flush';

        guides.forEach((guide) => {
          const item = document.createElement('li');
          item.className = 'list-group-item';

          const top = document.createElement('div');
          const levelEl = document.createElement('strong');
          levelEl.textContent = guide.level ? String(guide.level) : '';
          top.appendChild(levelEl);

          const hasRange = typeof guide.minScore === 'number' && typeof guide.maxScore === 'number';
          if (hasRange) {
            const range = document.createElement('span');
            range.className = 'badge badge-secondary ml-2';
            range.textContent = guide.minScore + ' - ' + guide.maxScore + '점';
            top.appendChild(range);
          }

          const summary = document.createElement('div');
          summary.textContent = guide.summary || '';

          item.appendChild(top);
          item.appendChild(summary);

          if (guide.recommendation) {
            const rec = document.createElement('div');
            rec.className = 'small text-muted mt-1';
            rec.textContent = '권장: ' + guide.recommendation;
            item.appendChild(rec);
          }

          list.appendChild(item);
        });

        card.appendChild(header);
        card.appendChild(list);
        guideContainer.appendChild(card);
      }

      // ===== 설문 문항 렌더링 =====
      function renderQuestions(questions) {
        questionContainer.innerHTML = '';
        if (!questions || questions.length === 0) {
          const alert = document.createElement('div');
          alert.className = 'alert alert-warning';
          alert.textContent = '선택한 유형에 대한 설문 문항을 불러올 수 없습니다.';
          questionContainer.appendChild(alert);
          return;
        }

        questions.forEach((question, index) => {
          const card = document.createElement('div');
          card.className = 'card mb-3';

          const header = document.createElement('div');
          header.className = 'card-header font-weight-bold';
          header.textContent = 'Q' + (index + 1) + '. ' + (question.text || '');

          const body = document.createElement('div');
          body.className = 'card-body';

          (question.options || []).forEach((option, optionIndex) => {
            const wrapper = document.createElement('div');
            wrapper.className = 'form-check';

            const input = document.createElement('input');
            input.type = 'radio';
            input.className = 'form-check-input';
            input.name = question.id;
            input.id = question.id + '_' + option.id;
            input.value = option.id;
            if (optionIndex === 0) input.required = true;

            const label = document.createElement('label');
            label.className = 'form-check-label';
            label.setAttribute('for', input.id);
            label.textContent = option.text || '';

            wrapper.appendChild(input);
            wrapper.appendChild(label);
            body.appendChild(wrapper);
          });

          card.appendChild(header);
          card.appendChild(body);
          questionContainer.appendChild(card);
        });
      }

      function renderForm(form) {
        renderFormInfo(form);
        renderGuides(form && Array.isArray(form.guides) ? form.guides : []);
        renderQuestions(form && Array.isArray(form.questions) ? form.questions : []);
      }

      // ===== 테이블 유틸 =====
      function createCell(text, extraClass) {
        const cell = document.createElement('td');
        if (extraClass) cell.className = extraClass;
        cell.textContent = text;
        return cell;
      }

      // ===== 히스토리 렌더링 =====
      function renderSurveys(surveys) {
        tableBody.innerHTML = '';
        if (!surveys || surveys.length === 0) {
          const row = document.createElement('tr');
          const cell = document.createElement('td');
          cell.colSpan = 8;
          cell.className = 'text-center text-muted';
          cell.textContent = '조회된 자가 설문이 없습니다. 새로운 기록을 추가해 보세요.';
          row.appendChild(cell);
          tableBody.appendChild(row);
          return;
        }

        surveys.forEach((survey) => {
          const row = document.createElement('tr');
          const answers = Array.isArray(survey.answers) ? survey.answers : [];

          const detailElement = document.createElement('details');
          const summary = document.createElement('summary');
          summary.textContent = '보기';
          detailElement.appendChild(summary);

          const list = document.createElement('ul');
          list.className = 'small pl-3 mb-0';
          if (answers.length === 0) {
            const item = document.createElement('li');
            item.textContent = '응답 정보 없음';
            list.appendChild(item);
          } else {
            answers.forEach((answer) => {
              const item = document.createElement('li');
              const scoreText = typeof answer.score === 'number' ? ' (점수 ' + answer.score + ')' : '';
              const qText = answer.questionText || '';
              const optText = answer.selectedOptionText || '';
              item.textContent = qText + ' → ' + optText + scoreText;
              list.appendChild(item);
            });
          }
          detailElement.appendChild(list);

          const scoreText = (typeof survey.totalScore === 'number' && typeof survey.maxScore === 'number')
                  ? (survey.totalScore + ' / ' + survey.maxScore)
                  : '';

          row.appendChild(createCell(survey.submittedAt || ''));
          row.appendChild(createCell(categoryLabels[survey.category] || survey.category || ''));
          row.appendChild(createCell(scoreText));
          row.appendChild(createCell(survey.resultLevel || ''));
          row.appendChild(createCell(survey.resultSummary || ''));
          row.appendChild(createCell(survey.resultRecommendation || '', 'text-break'));
          row.appendChild(createCell(survey.selfReflection || '', 'text-break'));

          const detailCell = document.createElement('td');
          detailCell.appendChild(detailElement);
          row.appendChild(detailCell);

          tableBody.appendChild(row);
        });
      }

      // ===== 폼 스키마 로드 =====
      async function loadForm(category) {
        clearStatus();
        questionContainer.innerHTML = '';
        guideContainer.innerHTML = '';
        renderFormInfo(null);
        currentForm = null;

        try {
          const params = category ? ('?category=' + encodeURIComponent(category)) : '';
          const response = await fetch(formsApi + params);
          if (!response.ok) throw new Error('자가 테스트 정보를 불러오지 못했습니다.');

          const data = await response.json();
          const formData = Array.isArray(data) && data.length > 0 ? data[0] : null;
          if (!formData) throw new Error('선택한 유형의 자가 테스트 정보를 찾을 수 없습니다.');

          currentForm = formData;
          renderForm(formData);
        } catch (error) {
          showStatus(error.message, 'error');
        }
      }

      // ===== 히스토리 로드 =====
      async function loadSurveys() {
        const clientId = clientIdInput.value.trim();
        if (!clientId) {
          renderSurveys([]);
          return;
        }
        const params = new URLSearchParams();
        if (filterCategory.value) params.set('category', filterCategory.value);

        try {
          const queryString = params.toString();
          const baseClientPath = surveysApiBase + '/' + encodeURIComponent(clientId);
          const url = queryString ? (baseClientPath + '?' + queryString) : baseClientPath;

          const response = await fetch(url);
          if (!response.ok) throw new Error('설문을 조회할 수 없습니다.');

          const data = await response.json();
          renderSurveys(Array.isArray(data) ? data : []);
        } catch (error) {
          showStatus(error.message, 'error');
        }
      }

      // ===== 이벤트 바인딩 =====
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        clearStatus();

        if (!currentForm || !Array.isArray(currentForm.questions) || currentForm.questions.length === 0) {
          showStatus('문항 정보를 불러온 뒤 다시 시도해 주세요.', 'error');
          return;
        }

        const answers = [];
        for (const question of currentForm.questions) {
          const selected = form.querySelector('input[name="' + question.id + '"]:checked');
          if (!selected) {
            showStatus('모든 문항에 응답해야 저장할 수 있습니다.', 'error');
            return;
          }
          answers.push({
            questionId: question.id,
            selectedOptionId: selected.value
          });
        }

        const payload = {
          clientId: clientIdInput.value.trim(),
          category: categorySelect.value,
          answers: answers,
          selfReflection: selfReflectionInput.value.trim() || null
        };

        if (!payload.clientId || !payload.category) {
          showStatus('사용자 ID와 자가 테스트 유형을 입력해 주세요.', 'error');
          return;
        }

        try {
          const response = await fetch(surveysApiBase, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          });
          if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || '자가 설문 저장에 실패했습니다.');
          }
          showStatus('자가 설문이 저장되었습니다.', 'success');
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

      categorySelect.addEventListener('change', () => {
        loadForm(categorySelect.value);
      });

      resetButton.addEventListener('click', () => {
        form.reset();
        clearStatus();
        renderSurveys([]);
        currentForm = null;
        guideContainer.innerHTML = '';
        renderFormInfo(null);
        questionContainer.innerHTML = '';
        loadForm(categorySelect.value);
      });

      // 초기 로드
      loadForm(categorySelect.value);
    });
  </script>
</head>
<body>
<div class="col-sm-9">
  <h3 class="mb-3">자가 상담 설문 기록</h3>
  <p class="text-muted">SMSPRING 아카이브에서 사용하던 방식처럼, 본인이 직접 자가 테스트를 진행하고 점수 해석 가이드를 확인한 뒤 AI 상담에 활용해 보세요.</p>

  <form id="surveyForm" class="border rounded p-3 bg-light">
    <div class="form-row">
      <div class="form-group col-md-4">
        <label for="clientId">사용자 ID</label>
        <input type="text" class="form-control" id="clientId" placeholder="예: self-001" required>
        <small class="form-text text-muted">자가 상담 결과를 구분하기 위한 이름이나 별칭을 입력하세요.</small>
      </div>
      <div class="form-group col-md-4">
        <label for="category">자가 테스트 유형</label>
        <select id="category" class="form-control" required>
          <option value="psychology">감정 · 스트레스 점검</option>
          <option value="career">진로 탐색 부담도</option>
        </select>
      </div>
      <div class="form-group col-md-4">
        <label for="filterCategory" class="d-none d-md-block">히스토리 필터</label>
        <div class="d-flex d-md-block align-items-center mt-2 mt-md-0">
          <select id="filterCategory" class="form-control mr-2">
            <option value="">전체</option>
            <option value="psychology">감정 · 스트레스</option>
            <option value="career">진로 탐색</option>
          </select>
          <button type="button" class="btn btn-outline-primary" id="reloadSurveys">조회</button>
        </div>
      </div>
    </div>

    <div id="formInfo" class="alert alert-info d-none"></div>
    <div id="guideContainer" class="mb-3"></div>
    <div id="questionContainer" class="mb-3"></div>

    <div class="form-group">
      <label for="selfReflection">자가 소감 (선택)</label>
      <textarea class="form-control" id="selfReflection" rows="3" placeholder="테스트를 진행하며 느낀 점이나 다짐을 간단히 적어보세요."></textarea>
    </div>

    <button type="submit" class="btn btn-primary">자가 설문 저장</button>
    <button type="button" class="btn btn-outline-secondary ml-2" id="resetForm">초기화</button>
    <div id="surveyStatus" class="mt-3" role="alert" style="display:none;"></div>
  </form>

  <hr/>

  <div class="d-flex justify-content-between align-items-center mb-2">
    <h4 class="mb-0">최근 자가 설문 히스토리</h4>
    <small class="text-muted">상담 요청 시 이력이 요약되어 LLM 답변에 반영됩니다.</small>
  </div>
  <div class="table-responsive">
    <table class="table table-sm table-striped" id="surveyTable">
      <thead class="thead-light">
      <tr>
        <th scope="col">작성 일시</th>
        <th scope="col">유형</th>
        <th scope="col">점수</th>
        <th scope="col">결과 등급</th>
        <th scope="col">결과 요약</th>
        <th scope="col">권장 행동</th>
        <th scope="col">자가 소감</th>
        <th scope="col">응답 상세</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td colspan="8" class="text-center text-muted">사용자 ID를 입력하면 저장된 자가 설문이 표시됩니다.</td>
      </tr>
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
