<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<style>
    .upload-zone {
        border: 3px dashed #667eea;
        border-radius: 10px;
        padding: 40px;
        text-align: center;
        background-color: #f8f9fa;
        transition: all 0.3s;
    }
    .upload-zone:hover {
        background-color: #e7e9fc;
        border-color: #764ba2;
    }
    .upload-zone.dragover {
        background-color: #d4d7ff;
        border-color: #667eea;
    }
</style>

<script>
    let uploadDoc = {
        selectedFile: null,

        init: function() {
            // 파일 선택 이벤트
            $('#fileInput').change((e) => {
                this.handleFileSelect(e.target.files[0]);
            });

            // 드래그 앤 드롭
            $('#uploadZone').on('dragover', (e) => {
                e.preventDefault();
                e.stopPropagation();
                $(e.currentTarget).addClass('dragover');
            });

            $('#uploadZone').on('dragleave', (e) => {
                e.preventDefault();
                e.stopPropagation();
                $(e.currentTarget).removeClass('dragover');
            });

            $('#uploadZone').on('drop', (e) => {
                e.preventDefault();
                e.stopPropagation();
                $(e.currentTarget).removeClass('dragover');
                this.handleFileSelect(e.originalEvent.dataTransfer.files[0]);
            });

            // 업로드 버튼
            $('#uploadBtn').click(() => {
                this.uploadFile();
            });

            // VectorStore 초기화
            $('#clearVectorBtn').click(() => {
                if (confirm('정말 VectorStore를 초기화하시겠습니까?\n모든 데이터가 삭제됩니다.')) {
                    this.clearVector();
                }
            });
        },

        handleFileSelect: function(file) {
            if (!file) return;

            // 파일 확장자 검증
            const ext = file.name.split('.').pop().toLowerCase();
            if (!['pdf', 'txt', 'docx'].includes(ext)) {
                alert('지원하지 않는 파일 형식입니다.\nPDF, TXT, DOCX 파일만 업로드 가능합니다.');
                return;
            }

            this.selectedFile = file;
            $('#fileName').text(file.name);
            $('#fileSize').text(this.formatFileSize(file.size));
            $('#fileInfo').show();
        },

        formatFileSize: function(bytes) {
            if (bytes < 1024) return bytes + ' B';
            else if (bytes < 1048576) return (bytes / 1024).toFixed(2) + ' KB';
            else return (bytes / 1048576).toFixed(2) + ' MB';
        },

        uploadFile: async function() {
            if (!this.selectedFile) {
                alert('파일을 선택해주세요.');
                return;
            }

            const lawType = $('#lawType').val();
            const formData = new FormData();
            formData.append('file', this.selectedFile);
            formData.append('lawType', lawType);

            $('#uploadBtn').prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> 처리 중...');
            $('#uploadResult').html('<div class="alert alert-info"><i class="fas fa-cog fa-spin"></i> ETL 처리 중입니다. 잠시만 기다려주세요...</div>');

            try {
                const response = await fetch('/api/lawdoc/upload', {
                    method: 'POST',
                    body: formData
                });

                const result = await response.text();
                $('#uploadResult').html(`<div class="alert alert-success">${result}</div>`);

                // 초기화
                this.selectedFile = null;
                $('#fileInfo').hide();
                $('#fileInput').val('');

            } catch (error) {
                $('#uploadResult').html(`<div class="alert alert-danger">❌ 업로드 실패: ${error.message}</div>`);
            } finally {
                $('#uploadBtn').prop('disabled', false).html('<i class="fas fa-upload"></i> 업로드 및 ETL 처리');
            }
        },

        clearVector: async function() {
            try {
                const response = await fetch('/api/lawdoc/clear-vector', {
                    method: 'POST'
                });

                const result = await response.text();
                alert(result);

            } catch (error) {
                alert('❌ 초기화 실패: ' + error.message);
            }
        }
    };

    $(function() {
        uploadDoc.init();
    });
</script>

<div class="content-header fade-in">
    <h2><i class="fas fa-upload"></i> 법률 문서 업로드 (RAG ETL)</h2>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/'/>">홈</a></li>
            <li class="breadcrumb-item active">문서 업로드</li>
        </ol>
    </nav>
</div>

<div class="row fade-in">
    <div class="col-md-8">
        <div class="card">
            <div class="card-header">
                <i class="fas fa-file-upload"></i> 파일 업로드
            </div>
            <div class="card-body">
                <div class="form-group">
                    <label><i class="fas fa-tag"></i> 법률 유형 선택</label>
                    <select class="form-control" id="lawType">
                        <option value="criminal_law">형법 (Criminal Law)</option>
                        <option value="civil_law">민법 (Civil Law)</option>
                    </select>
                </div>

                <div class="upload-zone" id="uploadZone">
                    <i class="fas fa-cloud-upload-alt fa-4x text-primary mb-3"></i>
                    <h5>파일을 드래그 앤 드롭하거나 클릭하여 선택하세요</h5>
                    <p class="text-muted">지원 형식: PDF, TXT, DOCX</p>
                    <input type="file" id="fileInput" accept=".pdf,.txt,.docx" style="display: none;">
                    <button class="btn btn-primary mt-3" onclick="$('#fileInput').click()">
                        <i class="fas fa-folder-open"></i> 파일 선택
                    </button>
                </div>

                <div id="fileInfo" class="mt-3" style="display: none;">
                    <div class="alert alert-success">
                        <i class="fas fa-file"></i> <strong>선택된 파일:</strong> 
                        <span id="fileName"></span> 
                        (<span id="fileSize"></span>)
                    </div>
                    <button class="btn btn-success btn-block" id="uploadBtn">
                        <i class="fas fa-upload"></i> 업로드 및 ETL 처리
                    </button>
                </div>

                <div id="uploadResult" class="mt-3"></div>
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card">
            <div class="card-header bg-info text-white">
                <i class="fas fa-info-circle"></i> ETL 처리 안내
            </div>
            <div class="card-body">
                <h6><strong>1. Extract (추출)</strong></h6>
                <p class="small">업로드된 파일에서 텍스트를 추출합니다.</p>

                <h6><strong>2. Transform (변환)</strong></h6>
                <p class="small">텍스트를 작은 청크로 분할하고 임베딩합니다.</p>

                <h6><strong>3. Load (적재)</strong></h6>
                <p class="small">Vector Store에 저장하여 RAG 검색이 가능하게 합니다.</p>

                <hr>
                <div class="alert alert-warning p-2 small">
                    <i class="fas fa-exclamation-triangle"></i> 
                    큰 파일은 처리 시간이 오래 걸릴 수 있습니다.
                </div>
            </div>
        </div>

        <div class="card mt-3">
            <div class="card-header bg-success text-white">
                <i class="fas fa-database"></i> VectorStore 관리
            </div>
            <div class="card-body">
                <button class="btn btn-danger btn-block" id="clearVectorBtn">
                    <i class="fas fa-trash-alt"></i> VectorStore 초기화
                </button>
                <small class="text-muted d-block mt-2">
                    ⚠️ 주의: 모든 임베딩 데이터가 삭제됩니다.
                </small>

                <hr>
                <h6>현재 상태</h6>
                <ul class="list-unstyled small" id="vectorStats">
                    <li><i class="fas fa-book"></i> 총 문서: 152개</li>
                    <li><i class="fas fa-gavel"></i> 형법: 78개</li>
                    <li><i class="fas fa-handshake"></i> 민법: 74개</li>
                    <li><i class="fas fa-vector-square"></i> 임베딩: 1,203개</li>
                </ul>
            </div>
        </div>
    </div>
</div>
