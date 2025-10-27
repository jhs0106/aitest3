<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<style>
    .case-detail-card .card-header {
        display: flex;
        flex-direction: column;
        gap: 6px;
    }

    .case-detail-card .case-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        align-items: center;
        font-size: 0.95rem;
        color: #6c757d;
    }

    .case-detail-card .detail-list dt {
        font-weight: 600;
        color: #495057;
    }

    .case-detail-card .detail-list dd {
        margin-bottom: 16px;
        color: #212529;
        white-space: pre-line;
    }

    .case-description,
    .case-verdict {
        background-color: #f8f9fa;
        border-radius: 8px;
        padding: 16px;
        border: 1px solid #e9ecef;
        white-space: pre-line;
        min-height: 80px;
    }

    .case-side-card .action-buttons .btn {
        margin-bottom: 10px;
    }

    .case-side-card .action-buttons .btn:last-child {
        margin-bottom: 0;
    }

    .case-side-card .list-group-item {
        border: none;
        padding-left: 0;
        padding-right: 0;
    }

    .case-side-card .list-group-item + .list-group-item {
        border-top: 1px solid rgba(0, 0, 0, 0.05);
    }
</style>

<script>
    function deleteCase(caseId, caseNumber) {
        const id = parseInt(caseId, 10);
        if (Number.isNaN(id)) {
            alert('사건 정보를 확인할 수 없습니다.');
            return;
        }

        if (confirm('사건 ' + caseNumber + '을(를) 정말 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) {
            window.location.href = '<c:url value="/case/delete?id="/>' + id;
        }
    }
</script>

<div class="content-header fade-in">
    <h2>
        <i class="fas fa-file-alt"></i> 사건 상세
        <c:if test="${not empty trialCase}">
            <small class="text-muted">#<c:out value="${trialCase.caseNumber}"/></small>
        </c:if>
    </h2>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/'/>">홈</a></li>
            <li class="breadcrumb-item"><a href="<c:url value='/case/list'/>">사건 목록</a></li>
            <li class="breadcrumb-item active">사건 상세</li>
        </ol>
    </nav>
</div>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger fade-in">
        <i class="fas fa-exclamation-circle"></i> ${errorMessage}
    </div>
</c:if>

<c:choose>
    <c:when test="${empty trialCase}">
        <div class="alert alert-warning fade-in">
            <i class="fas fa-info-circle"></i> 표시할 사건 정보가 없습니다.
        </div>
    </c:when>
    <c:otherwise>
        <div class="row fade-in">
            <div class="col-lg-8">
                <div class="card case-detail-card">
                    <div class="card-header">
                        <span><i class="fas fa-folder-open"></i> 사건 개요</span>
                        <div class="case-meta">
                            <span>
                                <i class="fas fa-balance-scale"></i>
                                <c:choose>
                                    <c:when test="${trialCase.caseType eq 'criminal'}">형사 사건</c:when>
                                    <c:when test="${trialCase.caseType eq 'civil'}">민사 사건</c:when>
                                    <c:otherwise>미지정</c:otherwise>
                                </c:choose>
                            </span>
                            <span>
                                <i class="fas fa-user"></i> 피고인: <c:out value="${trialCase.defendant}"/>
                            </span>
                            <span>
                                <i class="fas fa-flag"></i>
                                <c:choose>
                                    <c:when test="${trialCase.status eq 'registered'}">
                                        <span class="badge badge-secondary">등록됨</span>
                                    </c:when>
                                    <c:when test="${trialCase.status eq 'in_progress'}">
                                        <span class="badge badge-warning">진행중</span>
                                    </c:when>
                                    <c:when test="${trialCase.status eq 'closed'}">
                                        <span class="badge badge-success">종결</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-light">-</span>
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>
                    <div class="card-body">
                        <dl class="row detail-list">
                            <dt class="col-sm-3">사건번호</dt>
                            <dd class="col-sm-9"><strong><c:out value="${trialCase.caseNumber}"/></strong></dd>

                            <dt class="col-sm-3">피고인</dt>
                            <dd class="col-sm-9"><c:out value="${trialCase.defendant}"/></dd>

                            <dt class="col-sm-3">혐의 / 청구 내용</dt>
                            <dd class="col-sm-9"><c:out value="${trialCase.charge}"/></dd>
                        </dl>

                        <div class="mb-4">
                            <h5 class="mb-2"><i class="fas fa-info-circle"></i> 사건 설명</h5>
                            <div class="case-description">
                                <c:choose>
                                    <c:when test="${not empty trialCase.description}">
                                        <c:out value="${trialCase.description}"/>
                                    </c:when>
                                    <c:otherwise>등록된 상세 설명이 없습니다.</c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <div class="mb-4">
                            <h5 class="mb-2"><i class="fas fa-gavel"></i> 판결</h5>
                            <div class="case-verdict">
                                <c:choose>
                                    <c:when test="${not empty trialCase.verdict}">
                                        <c:out value="${trialCase.verdict}"/>
                                    </c:when>
                                    <c:otherwise>판결이 아직 등록되지 않았습니다.</c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <dl class="row detail-list mb-0">
                            <dt class="col-sm-3">등록일</dt>
                            <dd class="col-sm-9">
                                <c:choose>
                                    <c:when test="${not empty trialCase.createdAt}">
                                        ${fn:replace(fn:substring(trialCase.createdAt, 0, 16), 'T', ' ')}
                                    </c:when>
                                    <c:otherwise>-</c:otherwise>
                                </c:choose>
                            </dd>

                            <dt class="col-sm-3">최종 수정</dt>
                            <dd class="col-sm-9">
                                <c:choose>
                                    <c:when test="${not empty trialCase.updatedAt}">
                                        ${fn:replace(fn:substring(trialCase.updatedAt, 0, 16), 'T', ' ')}
                                    </c:when>
                                    <c:otherwise>-</c:otherwise>
                                </c:choose>
                            </dd>
                        </dl>
                    </div>
                </div>
            </div>

            <div class="col-lg-4">
                <div class="card case-side-card">
                    <div class="card-header bg-light">
                        <i class="fas fa-cogs"></i> 사건 관리
                    </div>
                    <div class="card-body">
                        <div class="d-flex flex-column action-buttons">
                            <a href="<c:url value='/case/list'/>" class="btn btn-outline-secondary">
                                <i class="fas fa-list"></i> 사건 목록으로
                            </a>
                            <a href="https://localhost:8445/ai2/trial?caseId=${trialCase.caseId}" class="btn btn-success" target="_blank">
                                <i class="fas fa-gavel"></i> 모의 법정에서 재판 진행
                            </a>
                            <button type="button" class="btn btn-danger"
                                    data-case-id="${trialCase.caseId}"
                                    data-case-number="<c:out value='${trialCase.caseNumber}'/>"
                                    onclick="deleteCase(this.dataset.caseId, this.dataset.caseNumber)">
                                <i class="fas fa-trash"></i> 사건 삭제
                            </button>
                        </div>
                    </div>
                    <ul class="list-group list-group-flush">
                        <li class="list-group-item">
                            <strong><i class="fas fa-stream"></i> 현재 상태</strong><br>
                            <c:choose>
                                <c:when test="${trialCase.status eq 'registered'}">등록됨</c:when>
                                <c:when test="${trialCase.status eq 'in_progress'}">진행중</c:when>
                                <c:when test="${trialCase.status eq 'closed'}">종결</c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </li>
                        <li class="list-group-item">
                            <strong><i class="fas fa-history"></i> 마지막 업데이트</strong><br>
                            <c:choose>
                                <c:when test="${not empty trialCase.updatedAt}">
                                    ${fn:replace(fn:substring(trialCase.updatedAt, 0, 16), 'T', ' ')}
                                </c:when>
                                <c:otherwise>기록 없음</c:otherwise>
                            </c:choose>
                        </li>
                        <li class="list-group-item">
                            <strong><i class="fas fa-sticky-note"></i> 참고사항</strong><br>
                            재판을 시작하면 사건 상태가 자동으로 진행중으로 변경됩니다.
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>