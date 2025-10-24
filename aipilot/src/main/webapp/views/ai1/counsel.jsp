<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-9">
    <h3 class="mb-3">설문 기반 상담</h3>
    <p class="text-muted">내담자의 설문 히스토리를 바탕으로 공감적인 상담 답변을 생성합니다.</p>

    <form id="counselForm" class="border rounded p-3 bg-light mb-3">
        <div class="form-row">
            <div class="form-group col-md-4">
                <label for="counselClientId">내담자 ID</label>
                <input type="text" class="form-control" id="counselClientId" placeholder="예: client-001" required>
            </div>
            <div class="form-group col-md-4">
                <label for="counselCategory">설문 유형</label>
                <select id="counselCategory" class="form-control">
                    <option value="">전체</option>
                    <option value="psychology">심리 상태</option>
                    <option value="career">진로 탐색</option>
                </select>
                <small class="form-text text-muted">필요 시 특정 설문 유형만 참조합니다.</small>
            </div>
        </div>
        <div class="form-group">
            <label for="counselQuestion">상담 질문</label>
            <textarea class="form-control" id="counselQuestion" rows="4" placeholder="내담자의 고민이나 궁금한 점을 구체적으로 작성해 주세요." required></textarea>
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
            <span>최근 설문 히스토리</span>
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
                item.textContent = '참조한 설문이 없습니다. 최근 설문을 먼저 저장해 주세요.';
                referencesList.appendChild(item);
                referencesSection.style.display = 'block';
                return;
            }
            references.forEach((reference) => {
                const item = document.createElement('li');
                item.className = 'list-group-item';
                const submittedAt = reference.submittedAt ? `(${reference.submittedAt})` : '';
                item.innerHTML = `
          <div><strong>${reference.category || ''}</strong> ${submittedAt}</div>
          <div class="small text-muted">세션 초점: ${reference.sessionFocus || ''}</div>
          <div class="small text-muted">핵심 관찰: ${reference.keyObservations || ''}</div>
          <div class="small text-muted">필요한 지원: ${reference.supportNeeds || ''}</div>
          <div class="small text-muted">다음 단계: ${reference.nextSteps || ''}</div>
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
                cell.colSpan = 6;
                cell.className = 'text-center text-muted';
                cell.textContent = '내담자 ID를 입력하면 최근 설문을 확인할 수 있습니다.';
                row.appendChild(cell);
                historyBody.appendChild(row);
                return;
            }
            if (!surveys || surveys.length === 0) {
                const row = document.createElement('tr');
                const cell = document.createElement('td');
                cell.colSpan = 6;
                cell.className = 'text-center text-muted';
                cell.textContent = '저장된 설문이 없습니다. 설문 저장 메뉴에서 먼저 기록해 주세요.';
                row.appendChild(cell);
                historyBody.appendChild(row);
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
                historyBody.appendChild(row);
            });
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
                    throw new Error('설문을 불러오지 못했습니다.');
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
                showStatus('내담자 ID와 상담 질문을 입력해 주세요.', 'error');
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