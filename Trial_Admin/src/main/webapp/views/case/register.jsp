<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<script>
    $(function() {
        // 사건 유형 변경 시 안내 메시지
        $('#caseType').change(function() {
            const type = $(this).val();
            if (type === 'criminal') {
                alert('형사 사건: 범죄 혐의에 대한 재판입니다.\n예: 절도, 사기, 폭행 등');
            } else if (type === 'civil') {
                alert('민사 사건: 개인 간 분쟁에 대한 재판입니다.\n예: 손해배상, 계약 분쟁 등');
            }
        });
    });
</script>

<div class="content-header fade-in">
    <h2><i class="fas fa-plus-circle"></i> 사건 등록</h2>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/'/>">홈</a></li>
            <li class="breadcrumb-item active">사건 등록</li>
        </ol>
    </nav>
</div>

<div class="row fade-in">
    <div class="col-md-8">
        <div class="card">
            <div class="card-header">
                <i class="fas fa-edit"></i> 사건 정보 입력
            </div>
            <div class="card-body">
                <form action="<c:url value='/case/register-impl'/>" method="post">
                    <div class="form-group">
                        <label for="caseType"><i class="fas fa-balance-scale"></i> 사건 유형 *</label>
                        <select class="form-control" id="caseType" name="caseType" required>
                            <option value="">선택하세요</option>
                            <option value="criminal">형사 사건</option>
                            <option value="civil">민사 사건</option>
                        </select>
                        <small class="form-text text-muted">
                            형사: 범죄 관련 사건 | 민사: 개인 간 분쟁
                        </small>
                    </div>

                    <div class="form-group">
                        <label for="defendant"><i class="fas fa-user"></i> 피고인 *</label>
                        <input type="text" class="form-control" id="defendant" name="defendant" 
                               placeholder="예: 김철수" required>
                    </div>

                    <div class="form-group">
                        <label for="charge"><i class="fas fa-file-alt"></i> 혐의 / 청구 내용 *</label>
                        <textarea class="form-control" id="charge" name="charge" rows="3" 
                                  placeholder="예: 편의점에서 라면 5개를 절취한 혐의" required></textarea>
                    </div>

                    <div class="form-group">
                        <label for="description"><i class="fas fa-info-circle"></i> 상세 설명</label>
                        <textarea class="form-control" id="description" name="description" rows="5" 
                                  placeholder="사건의 상세 내역을 입력하세요 (선택사항)"></textarea>
                    </div>

                    <div class="alert alert-info">
                        <i class="fas fa-lightbulb"></i> <strong>안내:</strong> 
                        사건을 등록하면 aipilot 법정 시스템에서 재판을 진행할 수 있습니다.
                    </div>

                    <div class="text-right">
                        <button type="button" class="btn btn-secondary" onclick="history.back()">
                            <i class="fas fa-times"></i> 취소
                        </button>
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-save"></i> 등록
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card">
            <div class="card-header bg-info text-white">
                <i class="fas fa-question-circle"></i> 사건 등록 가이드
            </div>
            <div class="card-body">
                <h6><strong>1. 사건 유형 선택</strong></h6>
                <p class="small">형사 또는 민사 사건을 선택하세요.</p>

                <h6><strong>2. 피고인 정보</strong></h6>
                <p class="small">피고인의 이름을 입력하세요.</p>

                <h6><strong>3. 혐의 내용</strong></h6>
                <p class="small">구체적인 혐의나 청구 내용을 작성하세요.</p>

                <h6><strong>4. 증거 첨부</strong></h6>
                <p class="small">등록 후 증거 관리 메뉴에서 증거를 추가할 수 있습니다.</p>

                <hr>
                <div class="alert alert-warning p-2 small">
                    <i class="fas fa-exclamation-triangle"></i> 
                    이 시스템은 교육용 모의 법정입니다.
                </div>
            </div>
        </div>

        <div class="card mt-3">
            <div class="card-header bg-success text-white">
                <i class="fas fa-check-circle"></i> 최근 등록 사건
            </div>
            <div class="card-body">
                <ul class="list-unstyled small">
                    <li class="mb-2">
                        <i class="fas fa-gavel"></i> 형사 - 절도죄 (김철수)
                        <br><small class="text-muted">2025-10-27 14:30</small>
                    </li>
                    <li class="mb-2">
                        <i class="fas fa-handshake"></i> 민사 - 손해배상 (이영희)
                        <br><small class="text-muted">2025-10-27 10:15</small>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</div>
