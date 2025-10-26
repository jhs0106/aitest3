<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>모의 법정 Admin</title>
    
    <!-- Bootstrap 4 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    
    <!-- jQuery -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.bundle.min.js"></script>
    
    <style>
        /* 전체 레이아웃 */
        body {
            font-family: 'Noto Sans KR', sans-serif;
            background-color: #f4f6f9;
        }
        
        /* 헤더 */
        .navbar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            box-shadow: 0 2px 4px rgba(0,0,0,.1);
        }
        
        .navbar-brand {
            font-size: 1.5rem;
            font-weight: 700;
            color: white !important;
        }
        
        .navbar-brand i {
            margin-right: 10px;
        }
        
        .navbar .nav-link {
            color: rgba(255,255,255,0.9) !important;
            font-weight: 500;
            transition: all 0.3s;
        }
        
        .navbar .nav-link:hover {
            color: white !important;
            transform: translateY(-2px);
        }
        
        /* 사이드바 */
        .sidebar {
            background: white;
            min-height: calc(100vh - 56px);
            box-shadow: 2px 0 5px rgba(0,0,0,.05);
            padding: 20px 0;
        }
        
        .sidebar h5 {
            padding: 10px 20px;
            color: #667eea;
            font-weight: 600;
            margin-bottom: 15px;
            border-bottom: 2px solid #f0f0f0;
        }
        
        .sidebar .list-group-item {
            border: none;
            border-left: 3px solid transparent;
            transition: all 0.3s;
            padding: 12px 20px;
        }
        
        .sidebar .list-group-item:hover {
            background-color: #f8f9fa;
            border-left-color: #667eea;
            color: #667eea;
            transform: translateX(5px);
        }
        
        .sidebar .list-group-item.active {
            background-color: #e7e9fc;
            border-left-color: #667eea;
            color: #667eea;
            font-weight: 600;
        }
        
        .sidebar .list-group-item i {
            margin-right: 10px;
            width: 20px;
            text-align: center;
        }
        
        /* 메인 컨텐츠 */
        .main-content {
            padding: 30px;
            min-height: calc(100vh - 56px);
        }
        
        .content-header {
            margin-bottom: 30px;
            padding-bottom: 15px;
            border-bottom: 2px solid #e9ecef;
        }
        
        .content-header h2 {
            color: #2c3e50;
            font-weight: 700;
            margin-bottom: 5px;
        }
        
        .content-header .breadcrumb {
            background: none;
            padding: 0;
            margin: 0;
        }
        
        /* 카드 스타일 */
        .card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,.08);
            margin-bottom: 20px;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        
        .card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(0,0,0,.15);
        }
        
        .card-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 10px 10px 0 0 !important;
            font-weight: 600;
            padding: 15px 20px;
        }
        
        /* 푸터 */
        .footer {
            background: white;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            box-shadow: 0 -2px 5px rgba(0,0,0,.05);
        }
        
        /* 유틸리티 */
        .badge-custom {
            padding: 5px 12px;
            border-radius: 20px;
            font-weight: 500;
        }
        
        .stat-card {
            text-align: center;
            padding: 20px;
        }
        
        .stat-card .stat-number {
            font-size: 2.5rem;
            font-weight: 700;
            color: #667eea;
        }
        
        .stat-card .stat-label {
            color: #6c757d;
            font-size: 0.9rem;
        }
        
        /* 애니메이션 */
        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        .fade-in {
            animation: fadeIn 0.5s ease-in-out;
        }
        
        /* 스크롤바 커스텀 */
        ::-webkit-scrollbar {
            width: 8px;
        }
        
        ::-webkit-scrollbar-track {
            background: #f1f1f1;
        }
        
        ::-webkit-scrollbar-thumb {
            background: #667eea;
            border-radius: 4px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: #764ba2;
        }
    </style>
</head>
<body>

<!-- 헤더 -->
<nav class="navbar navbar-expand-lg navbar-dark sticky-top">
    <div class="container-fluid">
        <a class="navbar-brand" href="<c:url value='/'/>">
            <i class="fas fa-balance-scale"></i>
            모의 법정 Admin
        </a>
        
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ml-auto">
                <li class="nav-item">
                    <a class="nav-link" href="<c:url value='/'/>">
                        <i class="fas fa-home"></i> 대시보드
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="https://localhost:8445" target="_blank">
                        <i class="fas fa-gavel"></i> 법정 바로가기
                    </a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" id="userDropdown" data-toggle="dropdown">
                        <i class="fas fa-user-circle"></i> 관리자
                    </a>
                    <div class="dropdown-menu dropdown-menu-right">
                        <a class="dropdown-item" href="#"><i class="fas fa-cog"></i> 설정</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#"><i class="fas fa-sign-out-alt"></i> 로그아웃</a>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</nav>

<!-- 메인 컨텐츠 -->
<div class="container-fluid">
    <div class="row">
        <!-- 사이드바 -->
        <div class="col-md-2 sidebar">
            <c:choose>
                <c:when test="${left == null}">
                    <jsp:include page="left.jsp"/>
                </c:when>
                <c:otherwise>
                    <jsp:include page="${left}.jsp"/>
                </c:otherwise>
            </c:choose>
        </div>
        
        <!-- 메인 콘텐츠 영역 -->
        <div class="col-md-10 main-content">
            <c:choose>
                <c:when test="${center == null}">
                    <jsp:include page="center.jsp"/>
                </c:when>
                <c:otherwise>
                    <jsp:include page="${center}.jsp"/>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<!-- 푸터 -->
<div class="footer">
    <p class="mb-0">
        &copy; 2025 모의 법정 시스템 Admin | Powered by SpringAI
    </p>
</div>

</body>
</html>
