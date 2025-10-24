<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-3">
    <h5 class="mb-3">AI1 메뉴</h5>
    <div class="list-group list-group-flush">
        <a class="list-group-item list-group-item-action" href="<c:url value='/ai1/survey'/>">
            설문 작성
        </a>
        <a class="list-group-item list-group-item-action" href="<c:url value='/ai1/counsel'/>">
            상담 요청
        </a>
    </div>

    <div class="mt-4 small text-muted">
        <p class="mb-2">AI1 기능을 사용해 내담자 설문을 저장하고 상담 답변을 생성할 수 있습니다.</p>
        <ul class="pl-3 mb-0">
            <li>설문 작성에서 내담자 정보를 기록하세요.</li>
            <li>상담 요청 메뉴에서 최근 설문을 바탕으로 상담 답변을 받아보세요.</li>
        </ul>
    </div>
</div>