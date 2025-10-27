<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="col-sm-2">
    <p>Carwash</p>
    <ul class="nav nav-pills flex-column">
        <li class="nav-item"><a class="nav-link" href="<c:url value='/springai3/carwash_entry'/>">입차</a></li>
        <li class="nav-item"><a class="nav-link" href="<c:url value='/springai3/carwash_plan'/>">레시피</a></li>
        <li class="nav-item"><a class="nav-link" href="<c:url value='/springai3/carwash_progress'/>">진행</a></li>

    </ul>
    <hr class="d-sm-none">
</div>
