<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-2" style="max-height: 600px; overflow:auto;">
    <h5 class="border-bottom pb-2">괴담 매뉴얼 모듈</h5>
    <div class="list-group mb-3">
        <a class="list-group-item list-group-item-action" href="<c:url value="/hauntedmanual/setup"/>">
            매뉴얼 & 문서 업로드
        </a>
        <a class="list-group-item list-group-item-action" href="<c:url value="/hauntedmanual/duty"/>">
            괴담 근무 진행
        </a>
    </div>
    <div class="alert alert-dark" role="alert">
        <strong>Tip</strong>
        <p class="mb-1">새 프로젝트에서도 매뉴얼을 생성하고 근무 시 매뉴얼 세션을 별도로 시작할 수 있도록 분리했습니다.</p>
        <small>근무 상황에 따른 매뉴얼 이름은 직접 입력하면 되며, 첫 응답은 근무 매뉴얼입니다.</small>
    </div>
</div>
