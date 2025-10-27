<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
  const hauntedSetup = {
    currentScenario: '',
    init: function () {
      this.loadSupportedTypes();
      $('#scenarioApplyBtn').on('click', () => this.applyScenario());
      $('#scenarioName').on('keypress', (event) => {
        if (event.key === 'Enter') {
          event.preventDefault();
          this.applyScenario();
        }
      });
      $('#uploadBtn').on('click', () => this.uploadManual());
      $('#clearBtn').on('click', () => this.clearVectorStore());
    },
    loadSupportedTypes: async function () {
      try {
        const response = await fetch('/api/haunted/manual/supported-types');
        if (!response.ok) {
          return;
        }
        const types = await response.json();
        const container = $('#supportedTypes');
        container.text(types.join(', '));
      } catch (error) {
        console.error('loadSupportedTypes error', error);
      }
    },
    applyScenario: function () {
      const raw = $('#scenarioName').val().trim();
      if (!raw) {
        alert('시나리오 이름을 입력하세요.');
        return;
      }
      this.currentScenario = raw;
      $('#scenarioBadge').text(this.currentScenario);
    },
    uploadManual: async function () {
      if (!this.currentScenario) {
        alert('시나리오를 먼저 적용하세요.');
        return;
      }
      const file = document.getElementById('manualFile').files[0];
      if (!file) {
        alert('업로드할 문서를 선택하세요.');
        return;
      }
      const formData = new FormData();
      formData.append('scenario', this.currentScenario);
      formData.append('attach', file);
      try {
        const response = await fetch('/api/haunted/manual/upload', {
          method: 'post',
          body: formData
        });
        const message = await response.text();
        this.appendLog(message);
      } catch (error) {
        console.error('uploadManual error', error);
        this.appendLog('문서를 업로드하지 못했습니다. 다시 시도하세요.');
      }
    },
    clearVectorStore: async function () {
      try {
        const response = await fetch('/api/haunted/manual/clear', {
          method: 'post'
        });
        const message = await response.text();
        this.appendLog(message);
      } catch (error) {
        console.error('clearVectorStore error', error);
        this.appendLog('벡터 저장소 초기화에 실패했습니다.');
      }
    },
    appendLog: function (text) {
      if (!text) {
        return;
      }
      const container = $('#setupLog');
      const template = `
                <div class="border rounded p-2 mb-2 bg-dark text-light">
                    <small class="text-warning d-block">시스템</small>
                    <p class="mb-0" style="white-space: pre-wrap;">${text}</p>
                </div>`;
      container.prepend(template);
    }
  };

  $(document).ready(function () {
    hauntedSetup.init();
  });
</script>

<div class="col-sm-10">
  <h2 class="mb-3">괴담 시나리오 & 문서 업로드</h2>
  <p class="text-muted">시나리오별 규칙 문서를 벡터스토어에 적재해 두면 다른 근무 페이지에서 활용할 수 있습니다.</p>

  <div class="card mb-4 border-warning">
    <div class="card-header bg-warning text-dark">시나리오 설정</div>
    <div class="card-body">
      <div class="form-row align-items-center">
        <div class="col-sm-6 my-1">
          <label class="sr-only" for="scenarioName">시나리오 이름</label>
          <input type="text" class="form-control" id="scenarioName"
                 placeholder="예) 폐병원 야간 경비"/>
        </div>
        <div class="col-sm-3 my-1">
          <button type="button" class="btn btn-warning btn-block text-dark" id="scenarioApplyBtn">시나리오 적용</button>
        </div>
        <div class="col-sm-3 my-1 text-center">
          <span class="badge badge-dark p-2" id="scenarioBadge">미지정</span>
        </div>
      </div>
      <small class="form-text text-muted">다른 모듈과 동일하게 시나리오 이름은 직접 입력해 관리합니다.</small>
    </div>
  </div>

  <div class="card mb-4 shadow-sm">
    <div class="card-header bg-secondary text-white">문서 업로드</div>
    <div class="card-body">
      <div class="form-row align-items-center">
        <div class="col-sm-5 my-1">
          <input type="file" class="form-control-file" id="manualFile" accept=".txt,.pdf,.doc,.docx"/>
        </div>
        <div class="col-sm-3 my-1">
          <button type="button" class="btn btn-danger btn-block" id="uploadBtn">규칙 투입</button>
        </div>
        <div class="col-sm-2 my-1">
          <button type="button" class="btn btn-outline-light btn-block" id="clearBtn">벡터 초기화</button>
        </div>
      </div>
      <small class="form-text text-muted">업로드한 문서는 시나리오 메타데이터와 함께 자동으로 분할 저장됩니다.</small>
      <small class="form-text text-muted">지원 파일 형식: <span id="supportedTypes">.txt, .pdf, .doc, .docx</span></small>
    </div>
  </div>

  <div class="card bg-dark text-light">
    <div class="card-header">처리 로그</div>
    <div class="card-body" style="max-height: 220px; overflow-y:auto;" id="setupLog">
      <p class="text-muted">문서 업로드/초기화 상태가 여기에 기록됩니다.</p>
    </div>
  </div>
</div>
