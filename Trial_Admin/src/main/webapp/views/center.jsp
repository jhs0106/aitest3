<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="content-header fade-in">
    <h2><i class="fas fa-tachometer-alt"></i> 대시보드</h2>
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item active">홈</li>
        </ol>
    </nav>
</div>

<!-- 통계 카드 -->
<div class="row fade-in">
    <div class="col-md-3">
        <div class="card">
            <div class="card-body stat-card">
                <i class="fas fa-folder-open fa-3x mb-3" style="color: #667eea;"></i>
                <div class="stat-number">24</div>
                <div class="stat-label">총 사건 수</div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3">
        <div class="card">
            <div class="card-body stat-card">
                <i class="fas fa-gavel fa-3x mb-3" style="color: #f093fb;"></i>
                <div class="stat-number">8</div>
                <div class="stat-label">진행 중인 재판</div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3">
        <div class="card">
            <div class="card-body stat-card">
                <i class="fas fa-book fa-3x mb-3" style="color: #4facfe;"></i>
                <div class="stat-number">152</div>
                <div class="stat-label">법률 문서</div>
            </div>
        </div>
    </div>
    
    <div class="col-md-3">
        <div class="card">
            <div class="card-body stat-card">
                <i class="fas fa-database fa-3x mb-3" style="color: #43e97b;"></i>
                <div class="stat-number">1,203</div>
                <div class="stat-label">Vector 임베딩</div>
            </div>
        </div>
    </div>
</div>

<!-- 최근 사건 & 시스템 상태 -->
<div class="row mt-4">
    <!-- 최근 등록된 사건 -->
    <div class="col-md-8">
        <div class="card fade-in">
            <div class="card-header">
                <i class="fas fa-clock"></i> 최근 등록된 사건
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>사건번호</th>
                                <th>유형</th>
                                <th>피고(인)</th>
                                <th>상태</th>
                                <th>등록일</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>2025고단123</td>
                                <td><span class="badge badge-danger badge-custom">형사</span></td>
                                <td>김철수</td>
                                <td><span class="badge badge-warning badge-custom">진행중</span></td>
                                <td>2025-01-15</td>
                            </tr>
                            <tr>
                                <td>2025가단456</td>
                                <td><span class="badge badge-primary badge-custom">민사</span></td>
                                <td>박영희</td>
                                <td><span class="badge badge-success badge-custom">완료</span></td>
                                <td>2025-01-14</td>
                            </tr>
                            <tr>
                                <td>2025고단124</td>
                                <td><span class="badge badge-danger badge-custom">형사</span></td>
                                <td>이민수</td>
                                <td><span class="badge badge-warning badge-custom">진행중</span></td>
                                <td>2025-01-13</td>
                            </tr>
                            <tr>
                                <td>2025가단457</td>
                                <td><span class="badge badge-primary badge-custom">민사</span></td>
                                <td>최지훈</td>
                                <td><span class="badge badge-secondary badge-custom">대기</span></td>
                                <td>2025-01-12</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div class="text-right">
                    <a href="<c:url value='/case/list'/>" class="btn btn-outline-primary btn-sm">
                        전체 보기 <i class="fas fa-arrow-right"></i>
                    </a>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 시스템 상태 -->
    <div class="col-md-4">
        <div class="card fade-in">
            <div class="card-header">
                <i class="fas fa-heartbeat"></i> 시스템 상태
            </div>
            <div class="card-body">
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-2">
                        <span><i class="fas fa-robot"></i> OpenAI API</span>
                        <span class="badge badge-success">정상</span>
                    </div>
                    <div class="progress" style="height: 5px;">
                        <div class="progress-bar bg-success" style="width: 100%"></div>
                    </div>
                </div>
                
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-2">
                        <span><i class="fas fa-database"></i> PostgreSQL</span>
                        <span class="badge badge-success">정상</span>
                    </div>
                    <div class="progress" style="height: 5px;">
                        <div class="progress-bar bg-success" style="width: 100%"></div>
                    </div>
                </div>
                
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-2">
                        <span><i class="fas fa-vector-square"></i> VectorStore</span>
                        <span class="badge badge-success">정상</span>
                    </div>
                    <div class="progress" style="height: 5px;">
                        <div class="progress-bar bg-success" style="width: 100%"></div>
                    </div>
                </div>
                
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-2">
                        <span><i class="fas fa-link"></i> WebSocket</span>
                        <span class="badge badge-warning">대기</span>
                    </div>
                    <div class="progress" style="height: 5px;">
                        <div class="progress-bar bg-warning" style="width: 0%"></div>
                    </div>
                </div>
                
                <hr>
                
                <div class="text-center">
                    <small class="text-muted">
                        <i class="fas fa-clock"></i> 마지막 업데이트: 방금 전
                    </small>
                </div>
            </div>
        </div>
        
        <!-- 빠른 실행 -->
        <div class="card mt-3 fade-in">
            <div class="card-header">
                <i class="fas fa-bolt"></i> 빠른 실행
            </div>
            <div class="card-body">
                <a href="<c:url value='/case/register'/>" class="btn btn-primary btn-block mb-2">
                    <i class="fas fa-plus"></i> 새 사건 등록
                </a>
                <a href="<c:url value='/lawdoc/upload'/>" class="btn btn-info btn-block mb-2">
                    <i class="fas fa-upload"></i> 법률 문서 업로드
                </a>
                <a href="https://localhost:8445" target="_blank" class="btn btn-success btn-block">
                    <i class="fas fa-gavel"></i> 법정 바로가기
                </a>
            </div>
        </div>
    </div>
</div>

<!-- 사건 유형 분포 차트 -->
<div class="row mt-4">
    <div class="col-md-6">
        <div class="card fade-in">
            <div class="card-header">
                <i class="fas fa-chart-pie"></i> 사건 유형 분포
            </div>
            <div class="card-body">
                <canvas id="caseTypeChart" height="200"></canvas>
            </div>
        </div>
    </div>
    
    <div class="col-md-6">
        <div class="card fade-in">
            <div class="card-header">
                <i class="fas fa-chart-line"></i> 월별 사건 추이
            </div>
            <div class="card-body">
                <canvas id="monthlyChart" height="200"></canvas>
            </div>
        </div>
    </div>
</div>

<!-- Chart.js -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@3.9.1/dist/chart.min.js"></script>

<script>
    $(function() {
        // 사건 유형 분포 차트
        var ctx1 = document.getElementById('caseTypeChart').getContext('2d');
        new Chart(ctx1, {
            type: 'doughnut',
            data: {
                labels: ['형사 사건', '민사 사건'],
                datasets: [{
                    data: [14, 10],
                    backgroundColor: [
                        'rgba(255, 99, 132, 0.8)',
                        'rgba(54, 162, 235, 0.8)'
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
        
        // 월별 추이 차트
        var ctx2 = document.getElementById('monthlyChart').getContext('2d');
        new Chart(ctx2, {
            type: 'line',
            data: {
                labels: ['10월', '11월', '12월', '1월'],
                datasets: [{
                    label: '등록된 사건',
                    data: [5, 8, 6, 10],
                    borderColor: 'rgba(102, 126, 234, 1)',
                    backgroundColor: 'rgba(102, 126, 234, 0.2)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
        
        // 애니메이션 지연
        $('.fade-in').each(function(index) {
            $(this).css('animation-delay', (index * 0.1) + 's');
        });
    });
</script>
