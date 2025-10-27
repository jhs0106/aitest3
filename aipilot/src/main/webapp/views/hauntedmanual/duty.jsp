<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
  const hauntedDuty = {
    conversationId: '',
    currentScenario: '',
    init: function () {
      this.loadScenarios();
      $('#refreshScenariosBtn').on('click', () => this.loadScenarios());
      $('#sendQuestionBtn').on('click', () => this.handleQuestion());
      $('#resetSessionBtn').on('click', () => this.resetSession());
    },
    loadScenarios: async function () {
      try {
        const response = await fetch('/api/haunted/manual/scenarios');
        const scenarios = response.ok ? await response.json() : [];
        const select = $('#scenarioSelect');
        select.empty();
        select.append('<option value="">시나리오 선택</option>');
        scenarios.forEach(name => {
          const escaped = $('<div>').text(name).html();
          select.append(`<option value="${escaped}">${escaped}</option>`);
        });
      } catch (error) {
        console.error('loadScenarios error', error);
      }
    },
    handleQuestion: async function () {
      const scenario = $('#scenarioSelect').val().trim();
      const rumor = $('#dutyRumor').val().trim();
      const question = $('#dutyQuestion').val().trim();

      if (!scenario) {
        alert('먼저 시나리오를 선택하세요.');
        return;
      }

      if (!this.conversationId || scenario !== this.currentScenario) {
        await this.startScenario(scenario, rumor);
      }

      if (!question) {
        return;
      }

      this.appendUser(question);
      await this.sendQuestion(question, scenario, rumor);
    },
    startScenario: async function (scenario, rumor) {
      this.resetStory();
      const params = new URLSearchParams();
      params.append('scenario', scenario);
      if (rumor) {
        params.append('rumor', rumor);
      }
      try {
        const response = await fetch('/api/haunted/manual/start', {
          method: 'post',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: params.toString()
        });
        if (!response.ok) {
          throw new Error('시나리오 시작에 실패했습니다.');
        }
        const data = await response.json();
        this.conversationId = data.conversationId;
        this.currentScenario = data.scenario;
        this.appendStory('manual', data.manual);
        this.appendStatus(`${data.scenario} 근무 매뉴얼이 전송되었습니다.`);
      } catch (error) {
        console.error('startScenario error', error);
        this.appendStatus('근무 매뉴얼을 불러오지 못했습니다.');
        throw error;
      }
    },
    sendQuestion: async function (question, scenario, rumor) {
      const params = new URLSearchParams();
      params.append('conversationId', this.conversationId);
      params.append('question', question);
      params.append('scenario', scenario);
      if (rumor) {
        params.append('rumor', rumor);
      }
      try {
        const response = await fetch('/api/haunted/manual/ask', {
          method: 'post',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: params.toString()
        });
        if (!response.ok) {
          throw new Error('질문 전송에 실패했습니다.');
        }
        const data = await response.json();
        this.appendStory('update', data.message);
        $('#dutyQuestion').val('');
      } catch (error) {
        console.error('sendQuestion error', error);
        this.appendStatus('응답을 가져오지 못했습니다. 근무 상태를 다시 확인하세요.');
      }
    },
    resetSession: function () {
      this.conversationId = '';
      this.currentScenario = '';
      this.resetStory();
      this.appendStatus('근무 세션을 초기화했습니다. 시나리오를 다시 선택하세요.');
    },
    resetStory: function () {
      $('#dutyLog').empty();
    },
    appendUser: function (text) {
      if (!text) {
        return;
      }
      const container = $('#dutyLog');
      const template = `
                <div class="media border p-3 mb-2 bg-light text-dark">
                    <img src="/image/user.png" alt="duty" class="mr-3 rounded-circle" style="width:40px; height:40px;">
                    <div class="media-body">
                        <small class="text-muted d-block">근무자</small>
                        <p class="mb-0" style="white-space: pre-wrap;">${text}</p>
                    </div>
                </div>`;
      container.append(template);
      container.scrollTop(container[0].scrollHeight);
    },
    appendStory: function (type, text) {
      if (!text) {
        return;
      }
      const container = $('#dutyLog');
      const heading = type === 'manual' ? '근무 매뉴얼' : '규칙 업데이트';
      const template = `
                <div class="media border p-3 mb-2 bg-dark text-light">
                    <img src="/image/assistant.png" alt="caretaker" class="mr-3 rounded-circle" style="width:40px; height:40px;">
                    <div class="media-body">
                        <small class="text-muted d-block">${heading}</small>
                        <p class="mb-0" style="white-space: pre-wrap;">${text}</p>
                    </div>
                </div>`;
      container.append(template);
      container.scrollTop(container[0].scrollHeight);
    },
    appendStatus: function (text) {
      if (!text) {
        return;
      }
      const container = $('#dutyLog');
      const template = `
                <div class="border p-2 mb-2 bg-secondary text-light">
                    <small class="text-warning d-block">시스템</small>
                    <p class="mb-0">${text}</p>
                </div>`;
      container.append(template);
    }
  };

  $(document).ready(function () {
    hauntedDuty.init();
  });
</script>

<div class="col-sm-10">
  <h2 class="mb-3">괴담 근무 진행</h2>
  <p class="text-muted">시나리오를 선택하면 첫 응답으로 근무 매뉴얼이 도착하고, 이후 질문은 매뉴얼을 기반으로 이어집니다.</p>

  <div class="card mb-4 border-info">
    <div class="card-header bg-info text-dark">근무 시나리오 선택</div>
    <div class="card-body">
      <div class="form-row align-items-center">
        <div class="col-sm-5 my-1">
          <select class="form-control" id="scenarioSelect"></select>
        </div>
        <div class="col-sm-6 my-1">
          <input type="text" class="form-control" id="dutyRumor" placeholder="최신 소문"/>
        </div>
        <div class="col-sm-1 my-1 text-right">
          <button class="btn btn-outline-dark btn-block" type="button" id="refreshScenariosBtn">새로고침</button>
        </div>
      </div>
      <small class="form-text text-muted">시나리오는 업로드 페이지에서 문서를 등록하면 목록에 나타납니다.</small>
    </div>
  </div>

  <div class="card mb-4 border-danger">
    <div class="card-header bg-dark text-danger d-flex justify-content-between align-items-center">
      <span>상황 보고</span>
      <button class="btn btn-outline-light btn-sm" type="button" id="resetSessionBtn">근무 초기화</button>
    </div>
    <div class="card-body">
      <div class="form-group">
        <label for="dutyQuestion">질문 또는 보고 내용</label>
        <textarea class="form-control" id="dutyQuestion" rows="3"
                  placeholder="예) 순찰 중 체육관에서 안내 방송이 나왔습니다. 어떻게 해야 하나요?"></textarea>
      </div>
      <div class="text-right">
        <button class="btn btn-outline-danger" type="button" id="sendQuestionBtn">질문 보내기</button>
      </div>
    </div>
  </div>

  <div class="card bg-secondary text-light">
    <div class="card-header">근무 로그</div>
    <div class="card-body" style="max-height: 360px; overflow-y:auto;" id="dutyLog">
      <p class="text-muted">근무 매뉴얼과 규칙 업데이트가 이 영역에 순차적으로 표시됩니다.</p>
    </div>
  </div>
</div>