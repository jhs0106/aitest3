<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<h5>
    <i class="fas fa-bars"></i> 메뉴
</h5>

<div class="list-group list-group-flush">
    <a href="<c:url value='/'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-tachometer-alt"></i>
        대시보드
    </a>
</div>

<h5 class="mt-4">
    <i class="fas fa-folder-open"></i> 사건 관리
</h5>

<div class="list-group list-group-flush">
    <a href="<c:url value='/case/register'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-plus-circle"></i>
        사건 등록
    </a>
    <a href="<c:url value='/case/list'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-list"></i>
        사건 목록
    </a>
    <a href="<c:url value='/case/statistics'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-chart-bar"></i>
        사건 통계
    </a>
</div>

<h5 class="mt-4">
    <i class="fas fa-book"></i> 법률 문서
</h5>

<div class="list-group list-group-flush">
    <a href="<c:url value='/lawdoc/upload'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-upload"></i>
        문서 업로드 (RAG)
    </a>
    <a href="<c:url value='/lawdoc/list'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-file-alt"></i>
        문서 목록
    </a>
    <a href="<c:url value='/lawdoc/vector'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-database"></i>
        VectorStore 관리
    </a>
</div>

<h5 class="mt-4">
    <i class="fas fa-cog"></i> 시스템
</h5>

<div class="list-group list-group-flush">
    <a href="<c:url value='/system/logs'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-terminal"></i>
        시스템 로그
    </a>
    <a href="<c:url value='/system/settings'/>" class="list-group-item list-group-item-action">
        <i class="fas fa-wrench"></i>
        설정
    </a>
</div>

<script>
    // 현재 페이지 active 표시
    $(function() {
        var currentPath = window.location.pathname;
        $('.list-group-item').each(function() {
            var href = $(this).attr('href');
            if (currentPath.indexOf(href) !== -1 && href !== '/') {
                $(this).addClass('active');
            }
        });
        
        // 대시보드 특별 처리
        if (currentPath === '/' || currentPath === '/admin/') {
            $('.list-group-item').first().addClass('active');
        }
    });
</script>
