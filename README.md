### 🧑‍💻TEAM 3 - springai4~5 기술 기반 기능 구현
***
### 시연 영상 : [![Video Label](http://img.youtube.com/vi/JNBo558s100/0.jpg)]

***
# 🎯 주제
-  Spring Boot와 JSP 그리고 OpenAI API를 중심으로 구축된 spring ai4~5 기술 기반 기능 구현 
***
# ⌛개발 기간
- 25.10.23 ~ 25.10.27
***
# 🕺구성원/역할
1. 주민성(Rediaum)          <https://github.com/Rediaum>
- PM(sub)

2. 주희성(jhs0106)        <https://github.com/jhs0106>
- PL&PM, readme.md 작성

3. 이다온(daon217)         <https://github.com/daon217>
- DEV

3. 이승호(lsh030412)         <https://github.com/lsh030412>
- DEV
***
# 기능 설명
## 1. AI1 자가 상담
이 기능은 사용자가 스스로 감정·진로 상태를 기록하고, 저장된 설문을 기반으로 AI 상담을 받는 워크플로를 제공

### 주요 화면 (Center & Left)
- **센터 대시보드**: 설문 작성 → 소감 기록 → 상담 요청의 3단계 절차를 안내하며 자가 상담 흐름을 추림. // 위치 : aipilot/src/main/webapp/views/ai1/center.jsp
- **사이드 메뉴**: 설문 작성과 상담 요청 페이지로 이동하는 링크와 자가 테스트 활용 팁을 제공 // 위치 : aipilot/src/main/webapp/views/ai1/left.jsp

### 주요 기능 (Survey: 자가 설문 관리)
- **폼  로드**: 선택한 유형(감정·스트레스, 진로 탐색)에 맞춰 `/ai1/api/forms`에서 문항과 점수 해석 가이드를 받아와 동적으로 렌더링 // 위치 : aipilot/src/main/webapp/views/ai1/survey.jsp
- **설문 작성 & 저장**: 사용자 ID, 유형, 문항별 응답, 자가 소감을 입력해 `/ai1/api/surveys`로 제출하고 성공 시 히스토리를 다시 조회, 필수 응답 완료가 안될 시 오류 메시지를 노출 // 위치 : aipilot/src/main/webapp/views/ai1/survey.jsp
- **히스토리 조회**: 사용자 ID 입력과 유형 필터에 따라 저장된 설문을 테이블로 보여주며 점수, 등급, 권장 행동, 문항별 응답 상세를 확인할 수 있음 // 위치: aipilot/src/main/webapp/views/ai1/survey.jsp

### 주요 기능 (Counsel: 상담 요청)
- **상담 요청 폼**: 사용자 ID, 참조할 설문 유형, 상담 질문을 입력해 `/ai1/api/counsel`에 전송, 로딩 시 버튼을 변경하고 오류 메시지를 관리 // 위치 : aipilot/src/main/webapp/views/ai1/counsel.jsp
- **AI 응답 & 참조 설문 표시**: 생성된 상담 답변을 카드에 렌더링하고, 참조된 설문 목록에서 점수·요약·권장 행동·자가 소감을 포함한 세부 정보를 제공 // 위치: aipilot/src/main/webapp/views/ai1/counsel.jsp
- **최근 설문 히스토리**: 상담 화면에서도 사용자 ID와 유형에 따라 `/ai1/api/surveys` 데이터를 불러와 테이블로 보여주고, 저장된 기록이 없을 때 안내 메시지를 제공 위치: aipilot/src/main/webapp/views/ai1/counsel.jsp

## 2. Haunted Manual 괴담 근무 모듈
이 기능은 관리자/근무자가 상황 별 괴담 규칙(매뉴얼) 문서를 업로드하고, AI와의 대화를 통해 매뉴얼에 대한 정보를 제공함.

### 주요 화면 (Left)
- **모듈 내비게이션**: “ 매뉴얼(상황) & 문서 업로드”, “괴담 근무 진행” 메뉴로 이동하는 링크와 상황 분리 운영 팁을 제공 // 위치 : aipilot/src/main/webapp/views/hauntedmanual/left.jsp

### 주요 기능 (Setup: 상황 & 문서 업로드)
- **매뉴얼 설정**: 시나리오 이름을 입력 후 적용하면 현재 선택된 매뉴얼 뱃지와 로그에 반영. // 위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp
- **지원 형식 조회**: `/api/haunted/manual/supported-types`를 호출해 업로드 가능한 파일 확장자를 표시. //위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp†
- **문서 업로드 & 로그**: 선택한 매뉴얼과 파일을 `/api/haunted/manual/upload`에 전송하고 처리 결과를 근무자/시스템 로그 카드로 누적 // 위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp
- **벡터 저장소 초기화**: `/api/haunted/manual/clear` 호출로 규칙 벡터스토어를 초기화하고 로그로 //위치:aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp

### 주요 기능 (Duty: 괴담 근무 진행)
- **근무 시작**: 해당하는 매뉴얼 이름 선택 `/api/haunted/manual/start`에 전달해 매뉴얼을 받아오고, 응답으로 받은 근무 매뉴얼을 로그에 표시 // 위치: aipilot/src/main/webapp/views/hauntedmanual/duty.jsp, aipilot/src/main/webapp/views/hauntedmanual/duty.jsp
- **질문 처리**: 근무자 질문을 `/api/haunted/manual/ask`로 전송하여 규칙 업데이트 메시지를 받고 로그에 추가합니다. 사용자 발화와 AI 응답은 각각 다른 스타일로 나옴. // 위치: aipilot/src/main/webapp/views/hauntedmanual/duty.jsp, aipilot/src/main/webapp/views/hauntedmanual/duty.jsp
- **세션 초기화 & 상태 로그**: 근무 세션을 초기화하면 대화 기록을 비우고 상태 메시지를 제공하며, 모든 로그는 스크롤 영역에 순차적으로 누적됨.
