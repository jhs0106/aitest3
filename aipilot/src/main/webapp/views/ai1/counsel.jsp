<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-9">
    <h3 class="mb-3">자가 설문 기반 상담</h3>
    <p class="text-muted">본인이 저장한 자가 설문 히스토리를 바탕으로 공감적인 AI 상담 답변을 제공합니다.</p>

    <form id="counselForm" class="border rounded p-3 bg-light mb-3">
        <div class="form-row">
            <div class="form-group col-md-4">
                <label for="counselClientId">사용자 ID</label>
                <input type="text" class="form-control" id="counselClientId" placeholder="예: self-001" required>
            </div>
            <div class="form-group col-md-4">
                <label for="counselCategory">자가 테스트 유형</label>
                <select id="counselCategory" class="form-control">
                    <option value="">전체</option>
                    <option value="psychology">감정 · 스트레스</option>
                    <option value="career">진로 탐색</option>
                </select>
                <small class="form-text text-muted">특정 유형을 고르면 해당 자가 설문만 참고합니다.</small>
            </div>
        </div>
        <div class="form-group">
            <label for="counselQuestion">상담 질문</label>
            <textarea class="form-control" id="counselQuestion" rows="4" placeholder="현재 고민이나 조언이 필요한 상황을 구체적으로 적어 주세요." required></textarea>
        </div>
        <button type="submit" id="submitCounsel" class="btn btn-primary">상담 요청</button>
        <div id="counselStatus" class="mt-3" role="alert" style="display:none;"></div>
    </form>

    <div id="responseCard" class="card mb-4" style="display:none;">
        <div class="card-header">상담 응답</div>
        <div class="card-body" id="counselAdvice"></div>
    </div>

    <div id="referencesSection" class="card mb-4" style="display:none;">
        <div class="card-header">참조한 설문</div>
        <ul class="list-group list-group-flush" id="counselReferences"></ul>
    </div>

    <div class="card" id="historySection">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span>최근 자가 설문 히스토리</span>
            <div>
                <button type="button" class="btn btn-sm btn-outline-secondary" id="refreshHistory">새로고침</button>
            </div>
        </div>
        <div class="table-responsive">
            <table class="table table-sm mb-0" id="historyTable">
                <thead class="thead-light">
                <tr>
                    <th scope="col">작성 일시</th>
                    <th scope="col">유형</th>
                    <th scope="col">점수</th>
                    <th scope="col">결과 등급</th>
                    <th scope="col">결과 요약</th>
                    <th scope="col">권장 행동</th>
                    <th scope="col">자가 소감</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colspan="7" class="text-center text-muted">사용자 ID를 입력하면 자가 설문 히스토리가 표시됩니다.</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    (function () {
        const form = document.getElementById('counselForm');
        const statusBox = document.getElementById('counselStatus');
        const clientIdInput = document.getElementById('counselClientId');
        const categorySelect = document.getElementById('counselCategory');
        const questionInput = document.getElementById('counselQuestion');
        const submitButton = document.getElementById('submitCounsel');
        const responseCard = document.getElementById('responseCard');
        const adviceBox = document.getElementById('counselAdvice');
        const referencesSection = document.getElementById('referencesSection');
        const referencesList = document.getElementById('counselReferences');
        const historyBody = document.querySelector('#historyTable tbody');
        const refreshHistoryButton = document.getElementById('refreshHistory');

        const surveysApiBase = '<c:url value="/ai1/api/surveys"/>';
        const counselApi = '<c:url value="/ai1/api/counsel"/>';

        const categoryLabels = {
            psychology: '감정 · 스트레스',
            career: '진로 탐색 부담도'
        };

        const defaultSubmitLabel = submitButton.textContent;

        function setLoading(isLoading) {
            submitButton.disabled = isLoading;
            submitButton.textContent = isLoading ? '상담 생성 중…' : defaultSubmitLabel;
        }

        function showStatus(message, type) {
            statusBox.style.display = 'block';
            if (type === 'success') {
                statusBox.className = 'alert alert-success';
            } else if (type === 'info') {
                statusBox.className = 'alert alert-info';
            } else {
                statusBox.className = 'alert alert-danger';
            }
            statusBox.textContent = message;
        }

        function clearStatus() {
            statusBox.style.display = 'none';
            statusBox.className = '';
            statusBox.textContent = '';
        }

        function clearAdvice() {
            responseCard.style.display = 'none';
            adviceBox.innerHTML = '';
            referencesSection.style.display = 'none';
            referencesList.innerHTML = '';
        }

        function renderAdvice(text) {
            adviceBox.innerHTML = text.replace(/\n/g, '<br>');
            responseCard.style.display = 'block';
        }

        function renderReferences(references) {
            referencesList.innerHTML = '';
            if (!references || references.length === 0) {
                const item = document.createElement('li');
                item.className = 'list-group-item text-muted';
                item.textContent = '참조한 자가 설문이 없습니다. 자가 설문을 먼저 저장해 주세요.';
                referencesList.appendChild(item);
                referencesSection.style.display = 'block';
                return;
            }
            references.forEach((reference) => {
                const item = document.createElement('li');
                item.className = 'list-group-item';
                const submittedAt = reference.submittedAt ? `(${reference.submittedAt})` : '';
                const scoreText = (typeof reference.totalScore === 'number' && typeof reference.maxScore === 'number')
                    ? `${reference.totalScore} / ${reference.maxScore}`
                    : '점수 정보 없음';
                const answers = Array.isArray(reference.answers) ? reference.answers : [];
                const answersHtml = answers.map(answer => {
                    const answerScore = typeof answer.score === 'number' ? ` (점수 ${answer.score})` : '';
                    return `<li>${answer.questionText} → ${answer.selectedOptionText}${answerScore}</li>`;
                }).join('');
                const reflection = reference.selfReflection ? `<div class="small text-muted">자가 소감: ${reference.selfReflection}</div>` : '';
                const level = reference.resultLevel ? `<div class="small">결과 등급: ${reference.resultLevel}</div>` : '';
                const recommendation = reference.resultRecommendation ? `<div class="small text-muted">권장 행동: ${reference.resultRecommendation}</div>` : '';
                const categoryName = categoryLabels[reference.category] || reference.category || '';
                item.innerHTML = `
          <div><strong>${categoryName}</strong> ${submittedAt}</div>
          <div class="small">점수: ${scoreText}</div>
          <div class="small">요약: ${reference.resultSummary || '요약 정보 없음'}</div>
          ${level}
          ${recommendation}
          ${reflection}
          <details class="mt-2">
            <summary class="small text-primary">문항별 응답 보기</summary>
            <ul class="small pl-3 mb-0">${answersHtml || '<li>응답 정보 없음</li>'}</ul>
          </details>
        `;
                referencesList.appendChild(item);
            });
            referencesSection.style.display = 'block';
        }

        function renderHistory(surveys, hasClientId) {
            historyBody.innerHTML = '';
            if (!hasClientId) {
                const row = document.createElement('tr');
                const cell = document.createElement('td');
                cell.colSpan = 7;
                cell.className = 'text-center text-muted';
                cell.textContent = '사용자 ID를 입력하면 최근 자가 설문을 확인할 수 있습니다.';
                row.appendChild(cell);
                historyBody.appendChild(row);
                return;
            }
            if (!surveys || surveys.length === 0) {
                const row = document.createElement('tr');
                const cell = document.createElement('td');
                cell.colSpan = 7;
                cell.className = 'text-center text-muted';
                cell.textContent = '저장된 자가 설문이 없습니다. 설문 작성 메뉴에서 먼저 기록해 주세요.';
                row.appendChild(cell);
                historyBody.appendChild(row);
                return;
            }
            surveys.forEach((survey) => {
                const row = document.createElement('tr');
                const scoreText = (typeof survey.totalScore === 'number' && typeof survey.maxScore === 'number')
                    ? `${survey.totalScore} / ${survey.maxScore}`
                    : '';
                row.appendChild(createCell(survey.submittedAt || ''));
                row.appendChild(createCell(categoryLabels[survey.category] || survey.category || ''));
                row.appendChild(createCell(scoreText));
                row.appendChild(createCell(survey.resultLevel || ''));
                row.appendChild(createCell(survey.resultSummary || ''));
                row.appendChild(createCell(survey.resultRecommendation || '', 'text-break'));
                row.appendChild(createCell(survey.selfReflection || '', 'text-break'));
                historyBody.appendChild(row);
            });
        }

        function createCell(text, extraClass) {
            const cell = document.createElement('td');
            if (extraClass) {
                cell.className = extraClass;
            }
            cell.textContent = text;
            return cell;
        }

        async function loadHistory() {
            const clientId = clientIdInput.value.trim();
            const hasClientId = clientId.length > 0;
            if (!hasClientId) {
                renderHistory([], false);
                return;
            }
            const params = new URLSearchParams();
            if (categorySelect.value) {
                params.set('category', categorySelect.value);
            }
            try {
                const query = params.toString();
                const baseClientPath = surveysApiBase + '/' + encodeURIComponent(clientId);
                const url = query ? baseClientPath + '?' + query : baseClientPath;
                const response = await fetch(url);
                if (!response.ok) {
                    throw new Error('자가 설문을 불러오지 못했습니다.');
                }
                const data = await response.json();
                renderHistory(Array.isArray(data) ? data : [], true);
            } catch (error) {
                showStatus(error.message, 'error');
            }
        }

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            clearStatus();
            clearAdvice();

            const payload = {
                clientId: clientIdInput.value.trim(),
                category: categorySelect.value || null,
                question: questionInput.value.trim()
            };

            if (!payload.clientId || !payload.question) {
                showStatus('사용자 ID와 상담 질문을 입력해 주세요.', 'error');
                return;
            }

            setLoading(true);
            try {
                const response = await fetch(counselApi, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                const text = await response.text();
                if (!response.ok) {
                    throw new Error(text || '상담 응답을 생성하지 못했습니다.');
                }

                let data;
                try {
                    data = text ? JSON.parse(text) : {};
                } catch (parseError) {
                    throw new Error('상담 응답을 해석할 수 없습니다.');
                }

                const advice = data.advice || '응답을 생성하지 못했습니다.';
                renderAdvice(advice);
                renderReferences(Array.isArray(data.references) ? data.references : []);
                showStatus('상담 응답이 생성되었습니다.', 'success');
            } catch (error) {
                showStatus(error.message, 'error');
            } finally {
                setLoading(false);
            }
        });

        refreshHistoryButton.addEventListener('click', () => {
            clearStatus();
            loadHistory();
        });

        clientIdInput.addEventListener('blur', () => {
            loadHistory();
        });

        categorySelect.addEventListener('change', () => {
            loadHistory();
        });

        renderHistory([], false);
        if (clientIdInput.value.trim().length > 0) {
            loadHistory();
        }
    })();
</script>