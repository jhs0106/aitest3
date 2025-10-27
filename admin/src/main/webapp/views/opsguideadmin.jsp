<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="true" %>

<style>
  .ops-wrap {
    max-width: 900px;
    margin: 20px auto;
    background: #f8f9fa;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    padding: 24px 28px;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans KR", sans-serif;
  }
  .ops-wrap h2 {
    font-weight:600;
    font-size:1.2rem;
    margin:0 0 6px 0;
    color:#333;
  }
  .ops-desc {
    font-size:0.8rem;
    color:#777;
    margin-bottom:16px;
  }

  .ops-block {
    background:#fff;
    border:1px solid #e1e4e8;
    border-radius:12px;
    padding:16px;
    margin-bottom:16px;
  }
  .ops-block label {
    font-size:0.8rem;
    font-weight:600;
    color:#444;
  }

  .ops-recent-box {
    max-height:200px;
    overflow:auto;
    background:#fff;
    border:1px solid #e1e4e8;
    border-radius:12px;
    padding:12px;
    font-size:0.8rem;
    color:#222;
  }
  .ops-recent-chunk {
    border-bottom:1px solid #eee;
    padding-bottom:8px;
    margin-bottom:8px;
  }
  .ops-recent-chunk:last-child {
    border-bottom:none;
    margin-bottom:0;
    padding-bottom:0;
  }

  .ask-row {
    display:flex;
    gap:12px;
    align-items:flex-start;
  }
  .ask-row textarea {
    flex:1;
    min-height:60px;
    font-size:0.9rem;
    border-radius:8px;
    border:1px solid #ccc;
    padding:8px;
  }
  .ask-row button {
    white-space:nowrap;
    height:40px;
  }
  .answer-box {
    background:#fff;
    border:1px solid #e1e4e8;
    border-radius:12px;
    padding:12px;
    font-size:0.9rem;
    color:#222;
    white-space:pre-wrap;
    margin-top:12px;
  }
</style>
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
          <div class="ops-wrap">
            <h2>🗂 운영 가이드 벡터스토어 관리</h2>
            <div class="ops-desc">
              현장 운영 매뉴얼 / 장비 점검 절차 / 고객 응대 가이드라인 같은 문서를 업로드하면
              LLM이 사장님 질문에 그 내용을 근거로 답변할 수 있게 됩니다.
              (metadata.type = "ops_guide")
            </div>

            <!-- 업로드 블록 -->
            <div class="ops-block">
              <label>문서 업로드</label>
              <div style="font-size:0.75rem; color:#888; margin-bottom:8px;">
                .txt / .pdf / .docx 지원. 큰 문서는 자동으로 chunk로 나뉘어 vector_db에 저장됩니다.
              </div>

              <div class="mb-2">
                <input id="ops_title" class="form-control" placeholder="문서 제목 (예: 폼샴푸 라인 점검 매뉴얼)"/>
              </div>

              <div class="mb-2">
                <input id="ops_file" class="form-control" type="file"/>
              </div>

              <button id="ops_upload_btn" class="btn btn-primary btn-sm">
                업로드 & 적재
              </button>

              <span id="ops_upload_spin" style="visibility:hidden; font-size:0.8rem; color:#666; margin-left:8px;">
      <span class="spinner-border spinner-border-sm"></span> 처리중...
    </span>

              <div id="ops_upload_result" style="font-size:0.8rem; color:#111; margin-top:8px;"></div>
            </div>

            <!-- 최근 적재된 chunk 미리보기 -->
            <div class="ops-block">
              <label>최근 업로드된 운영 가이드 일부</label>
              <div class="ops-recent-box" id="ops_recent_box">
                (불러오는 중...)
              </div>
              <button id="ops_refresh_btn" class="btn btn-light btn-sm" style="margin-top:8px;">
                새로고침
              </button>
            </div>

            <!-- 테스트 질의 -->
            <div class="ops-block">
              <label>운영 매뉴얼 테스트 질문</label>
              <div class="ask-row">
      <textarea id="ops_question"
                placeholder="예) 폼샴푸 라인 압력 떨어졌을 때 점검 순서 알려줘"></textarea>
                <button id="ops_ask_btn" class="btn btn-success btn-sm">질문</button>
              </div>

              <div class="answer-box" id="ops_answer_box">
                (응답 대기중)
              </div>
            </div>

          </div>

        </div>
      </div>
    </div>
  </div>

</div>

<script>
  const opsPage = {
    init(){
      document.getElementById('ops_upload_btn').addEventListener('click', ()=> this.upload());
      document.getElementById('ops_refresh_btn').addEventListener('click', ()=> this.loadRecent());
      document.getElementById('ops_ask_btn').addEventListener('click', ()=> this.ask());
      this.loadRecent();
    },

    setUploading(v){
      document.getElementById('ops_upload_spin').style.visibility = v ? 'visible':'hidden';
    },

    async upload(){
      const title = document.getElementById('ops_title').value.trim();
      const file = document.getElementById('ops_file').files[0];
      if(!title){ alert("제목을 입력하세요"); return; }
      if(!file){ alert("파일을 선택하세요"); return; }

      this.setUploading(true);

      const fd = new FormData();
      fd.append("title", title);
      fd.append("attach", file);

      try {
        const res = await fetch('/ai6/admin/ops-guide/upload', {
          method:'POST',
          body: fd
        });
        const txt = await res.text();
        document.getElementById('ops_upload_result').innerText = txt;
        document.getElementById('ops_title').value = '';
        document.getElementById('ops_file').value = '';
        this.loadRecent();
      } catch(e){
        console.error(e);
        document.getElementById('ops_upload_result').innerText =
                "오류 발생. 서버 로그 확인 필요.";
      }

      this.setUploading(false);
    },

    async loadRecent(){
      const box = document.getElementById('ops_recent_box');
      box.innerHTML = "(불러오는 중...)";
      try {
        const res = await fetch('/ai6/admin/ops-guide/recent');
        const data = await res.json(); // [{id,content,metadata},...]
        if(!Array.isArray(data) || data.length === 0){
          box.innerHTML = '<div style="color:#999;font-size:0.8rem;">업로드된 문서가 없습니다.</div>';
          return;
        }

        box.innerHTML = '';
        data.forEach(row=>{
          const preview = (row.content || '').substring(0,200).replace(/</g,"&lt;");
          const meta = JSON.stringify(row.metadata);
          const html = `
            <div class="ops-recent-chunk">
              <div style="color:#111;font-weight:600;">#${row.id}</div>
              <div style="color:#444; white-space:pre-wrap;">${preview}</div>
              <div style="color:#999; font-size:0.7rem;">${meta}</div>
            </div>
          `;
          box.insertAdjacentHTML('beforeend', html);
        });

      } catch(e){
        console.error(e);
        box.innerHTML = '<div style="color:#c00;font-size:0.8rem;">불러오기 실패</div>';
      }
    },

    async ask(){
      const q = document.getElementById('ops_question').value.trim();
      if(!q){
        alert("질문을 입력하세요.");
        return;
      }

      const ansBox = document.getElementById('ops_answer_box');
      ansBox.innerText = "(응답 생성중...)";

      try {
        const res = await fetch('/ai6/admin/ops-guide/ask', {
          method:'POST',
          headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
          body: new URLSearchParams({ q })
        });

        // ask()는 Flux<String> 스트리밍 응답이라면 text() 대신 reader 써야 하고
        // Mono/String이면 text()로 충분하거든?
        // 여기선 간단하게 스트리밍 처리 (네 ai4/rag-chat 스타일)
        const reader = res.body.getReader();
        const decoder = new TextDecoder('utf-8');

        let full = '';
        while(true){
          const {value, done} = await reader.read();
          if(done) break;
          full += decoder.decode(value);
          ansBox.innerText = full;
        }

      } catch(e){
        console.error(e);
        ansBox.innerText = "에러 발생. 서버 로그 확인 필요.";
      }
    }
  };

  document.addEventListener('DOMContentLoaded', ()=> opsPage.init());
</script>
