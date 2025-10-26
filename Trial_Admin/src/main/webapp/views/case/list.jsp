<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script>
    function deleteCase(caseId, caseNumber) {
        if (confirm('사건 ' + caseNumber + '을(를) 정말 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) {
            // contextPath 포함해 안전하게 이동 (c:url 렌더링 결과 사용)
            window.location.href = '<c:url value="/case/delete?id="/>' + caseId;
        }
    }
</script>

<div class="content-header fade-in">
    <h2><i class="fas fa-list"></i> 사건 목록</h2>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><a href="<c:url value='/'/>">홈</a></li>
            <li class="breadcrumb-item active">사건 목록</li>
        </ol>
    </nav>
</div>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger fade-in">
        <i class="fas fa-exclamation-circle"></i> ${errorMessage}
    </div>
</c:if>

<div class="row fade-in">
    <div class="col-md-12">
        <div class="card">
            <div class="card-header">
                <div class="d-flex justify-content-between align-items-center">
                    <span><i class="fas fa-folder-open"></i> 전체 사건 목록</span>
                    <a href="<c:url value='/case/register'/>" class="btn btn-primary btn-sm">
                        <i class="fas fa-plus"></i> 새 사건 등록
                    </a>
                </div>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty caseList}">
                        <div class="alert alert-info text-center">
                            <i class="fas fa-info-circle"></i> 등록된 사건이 없습니다.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead class="thead-light">
                                <tr>
                                    <th>사건번호</th>
                                    <th>유형</th>
                                    <th>피고인</th>
                                    <th>혐의</th>
                                    <th>상태</th>
                                    <th>등록일시</th>
                                    <th>관리</th>
                                </tr>
                                </thead>
                                <tbody>
                                <!-- 변수명 통일: trialCase -->
                                <c:forEach items="${caseList}" var="trialCase">
                                    <tr>
                                        <td>
                                            <strong>${trialCase.caseNumber}</strong>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${trialCase.caseType eq 'criminal'}">
                                                    <span class="badge badge-danger">형사</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge badge-primary">민사</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${trialCase.defendant}</td>
                                        <td>
                                            <c:choose>
                                                <%-- 메서드 호출 금지 → fn:length(), null-safe로 not empty 추가 --%>
                                                <c:when test="${not empty trialCase.charge and fn:length(trialCase.charge) > 30}">
                                                    ${fn:substring(trialCase.charge, 0, 30)}...
                                                </c:when>
                                                <c:otherwise>
                                                    ${trialCase.charge}
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
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
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty trialCase.createdAt}">
                                                    ${fn:replace(fn:substring(trialCase.createdAt, 0, 16), 'T', ' ')}
                                                    <%-- 결과: yyyy-MM-dd HH:mm --%>
                                                </c:when>
                                                <c:otherwise>-</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="btn-group btn-group-sm">
                                                <a href="<c:url value='/case/detail?id=${trialCase.caseId}'/>"
                                                   class="btn btn-info" title="상세보기">
                                                    <i class="fas fa-eye"></i>
                                                </a>
                                                <a href="https://localhost:8445/ai2/trial?caseId=${trialCase.caseId}"
                                                   class="btn btn-success" target="_blank"
                                                   title="법정에서 재판">
                                                    <i class="fas fa-gavel"></i>
                                                </a>
                                                <button type="button" class="btn btn-danger"
                                                        onclick="deleteCase(${trialCase.caseId}, '${trialCase.caseNumber}')"
                                                        title="삭제">
                                                    <i class="fas fa-trash"></i>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>
