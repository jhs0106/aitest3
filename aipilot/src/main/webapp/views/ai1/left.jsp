<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-3">
    <h5 class="mb-3">AI1 자가 상담</h5>
    <div class="list-group list-group-flush">
        <a class="list-group-item list-group-item-action" href="<c:url value='/ai1/survey'/>">
            설문 작성
        </a>
        <a class="list-group-item list-group-item-action" href="<c:url value='/ai1/counsel'/>">
            상담 요청
        </a>
    </div>

    <div class="mt-4 small text-muted">
        <p class="mb-2">자가 테스트를 통해 내 상태를 기록하고, AI 상담 답변으로 다음 단계를 정리하세요.</p>
        <ul class="pl-3 mb-0">
            <li>설문 작성에서 감정/진로 등 스스로의 상태를 객관적으로 체크합니다.</li>
            <li>상담 요청 메뉴에서 저장된 자가 설문을 바탕으로 맞춤형 조언을 받아보세요.</li>
        </ul>
    </div>
</div>