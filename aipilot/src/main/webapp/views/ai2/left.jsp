<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="col-sm-2">
    <p>AI2 IoT Control</p>
    <ul class="nav nav-pills flex-column">
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/ai2"/>">
                <i class="fa fa-home"></i> 대시보드
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/ai2/smart-home"/>">
                <i class="fa fa-lightbulb-o"></i> 스마트홈 제어
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="<c:url value="/ai2/trial"/>">
                <i class="fa fa-gavel"></i> 모의 법정
            </a>
        </li>
    </ul>
    <hr class="d-sm-none">
</div>