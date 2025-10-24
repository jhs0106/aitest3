<%-- aipilot/src/main/webapp/views/doors/left.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Ai1 left page</title>
</head>
<body>
<div class="col-sm-2">
    <p>Ai1 Left Menu</p>
    <ul class="nav nav-pills flex-column">
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/doors/recognition"/>">얼굴인식</a> <%-- 신규 추가 --%>
        </li>
    </ul>
    <ul class="nav nav-pills flex-column">
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/doors/records"/>">출입기록</a>
        </li>
    </ul>
    <%-- [신규] 등록 메뉴 추가 --%>
    <ul class="nav nav-pills flex-column">
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/doors/registration"/>">얼굴 등록</a>
        </li>
    </ul>
    <hr class="d-sm-none">
</div>
</body>
</html>