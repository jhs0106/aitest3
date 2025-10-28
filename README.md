### 🧑‍💻TEAM 3 - springai4~5 기술 기반 기능 구현
***
### 시연 영상 : [![Video Label](http://img.youtube.com/vi/v=zlSSEia3yD0/0.jpg)]
https://www.youtube.com/watch?v=zlSSEia3yD0
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

## 2. Haunted Manual 매뉴얼 괴담
이 기능은 관리자/근무자가 상황 별 괴담 규칙(매뉴얼) 문서를 업로드하고, AI와의 대화를 통해 매뉴얼에 대한 정보를 제공함.

### 주요 화면 (Left)
- **모듈 내비게이션**: “ 매뉴얼(상황) & 문서 업로드”, “괴담 근무 진행” 메뉴로 이동하는 링크와 상황 분리 운영 팁을 제공 // 위치 : aipilot/src/main/webapp/views/hauntedmanual/left.jsp

### 주요 기능 (Setup: 상황 & 문서 업로드)
- **매뉴얼 설정**: 시나리오 이름을 입력 후 적용하면 현재 선택된 매뉴얼 뱃지와 로그에 반영. // 위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp
- **지원 형식 조회**: `/api/haunted/manual/supported-types`를 호출해 업로드 가능한 파일 확장자를 표시. //위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp
- **문서 업로드 & 로그**: 선택한 매뉴얼과 파일을 `/api/haunted/manual/upload`에 전송하고 처리 결과를 근무자/시스템 로그 카드로 누적 // 위치: aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp
- **벡터 저장소 초기화**: `/api/haunted/manual/clear` 호출로 규칙 벡터스토어를 초기화하고 로그로 //위치:aipilot/src/main/webapp/views/hauntedmanual/setup.jsp, aipilot/src/main/webapp/views/hauntedmanual/setup.jsp

### 주요 기능 (Duty: 괴담 근무 진행)
- **근무 시작**: 해당하는 매뉴얼 이름 선택 `/api/haunted/manual/start`에 전달해 매뉴얼을 받아오고, 응답으로 받은 근무 매뉴얼을 로그에 표시 // 위치: aipilot/src/main/webapp/views/hauntedmanual/duty.jsp, aipilot/src/main/webapp/views/hauntedmanual/duty.jsp
- **질문 처리**: 근무자 질문을 `/api/haunted/manual/ask`로 전송하여 규칙 업데이트 메시지를 받고 로그에 추가합니다. 사용자 발화와 AI 응답은 각각 다른 스타일로 나옴. // 위치: aipilot/src/main/webapp/views/hauntedmanual/duty.jsp, aipilot/src/main/webapp/views/hauntedmanual/duty.jsp
- **세션 초기화 & 상태 로그**: 근무 세션을 초기화하면 대화 기록을 비우고 상태 메시지를 제공하며, 모든 로그는 스크롤 영역에 순차적으로 누적됨.

## 3. Trial(모의 재판) 시스템
이 기능은 사용자가 가상의 법정에서 다양한 역할(판사, 검사, 변호인 등)을 수행하며 AI 기반으로 사건을 진행하고 판결을 생성하는 인터랙티브 시뮬레이션 환경을 제공함.

### 주요 화면 (Layout & UI)
- **역할 선택 패널**: 좌측에 위치하며, 7가지 역할 카드(판사, 검사, 변호인, 피고인, 증인, 참심위원, AI 재판부)가 아이콘과 설명으로 표시되어 클릭 시 역할 전환이 가능함.  
  → **위치:** `aipilot/src/main/webapp/views/trial/center.jsp`
- **대화 패널**: 우측에서 역할별 발언이 표시되며 SSE 스트리밍을 통해 AI 응답이 실시간으로 출력됨.
- **하단 컨트롤 영역**: 발언 입력창과 ‘판결 생성’, ‘재판 종료’, ‘초기화’ 버튼이 배치되어 주요 조작을 한 화면에서 수행 가능.
- **세션 관리**: 페이지 로드시 브라우저 `sessionStorage`에 `sessionId` 저장, 사건이 없을 경우 선택 안내를 표시.

### 주요 기능 (Case & Session)
- **사건 목록 조회**: `/ai2/api/trial-cases` 호출로 형사/민사 사건 목록을 받아오고, 모달창에서 사건 유형·피고인·혐의를 확인 후 재판 시작 가능.
- **세션 매핑 관리**: `TrialSessionManager`가 `sessionId`와 사건을 매핑하며, 초기 역할(피고인)을 설정해 역할별 대화 맥락을 분리.
- **재판 종료 처리**: `/ai2/api/trial-complete` 호출 시 사건 상태를 `closed`로 변경하고 세션 초기화.

### 주요 기능 (Role System & Messaging)
- **역할 전환**: `/ai2/api/trial-switch-role` 호출로 역할 변경, 서버는 사건 요약과 안내 메시지를 반환.  
- **대화 흐름**: 메시지 전송 시 자동으로 `[판사]`, `[검사]` 등의 말머리 추가, SSE 스트리밍으로 AI 응답 표시.  
- **역할별 시스템 프롬프트(RoleInstruction)**:  
  - 판사 → “공정성 유지”  
  - 검사 → “증거 입증”  
  - 변호인 → “정상참작”  
  등의 역할 특성에 맞는 지침이 적용됨.

### 주요 기능 (Memory 관리)
- **대화 맥락 유지**: 각 역할은 `sessionId-roleId` 형태의 독립 `conversationId`를 가지며, `PromptChatMemoryAdvisor`로 이전 대화 참고.  
- **사건 포함 프롬프트**: 사건 정보가 시스템 프롬프트에 삽입되어 맥락 유지형 답변 제공.

### 주요 기능 (Function Calling)
- **TrialRoleTools**: 실제 재판 절차를 시뮬레이션하는 4가지 도구 함수 제공  
  - `judgeOpenTrial()` : 개정 선언문 생성 (사건번호, 피고인, 혐의 포함)  
  - `prosecutorProsecute()` : 검사의 기소 및 구형 메시지  
  - `attorneyDefend()` : 변호인의 변론 및 정상참작 요청  
  - `judgeVerdict()` : 판결문 선고 메시지 생성  

### 주요 기능 (AI 보조 기능)
- **개정 선언 생성**: `startTrialWithCase()`에서 사건 정보 기반으로 오프닝 메시지 자동 생성 (형사/민사별 문구 차등).  
- **판결 생성**: `/ai2/api/trial-verdict` SSE 스트리밍 방식으로 판결문 생성.  
  포함 요소:
  - 사건 개요
  - 법조문
  - 진술 요약
  - 구형 요약
  - 변론 요약
  - 정상참작 사유
  - 형량
  - 판결 이유
- **법률 자문 (RAG 기반)**: `/ai2/api/legal-advice` 호출 시 벡터 스토어에서 관련 법률 문서 검색 후 자문 제공.  
- **통합 재판 모드**: `/ai2/api/trial-full`에서 Memory와 RAG를 결합하여 맥락 + 지식 기반 재판 진행.

### 주요 API 엔드포인트
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /ai2/api/trial-cases | 사건 목록 조회 |
| GET | /ai2/api/trial-session-info | 현재 세션 정보 조회 |
| GET | /ai2/api/trial-start | 사건 선택 및 재판 시작 (SSE) |
| POST | /ai2/api/trial-switch-role | 역할 전환 |
| GET | /ai2/api/trial-chat | 기본 재판 채팅 (SSE) |
| GET | /ai2/api/trial-memory-chat | Memory 기반 재판 (SSE) |
| GET | /ai2/api/legal-advice | RAG 법률 자문 (SSE) |
| GET | /ai2/api/trial-full | Memory + RAG 통합 재판 (SSE) |
| GET | /ai2/api/trial-ai-proceed | AI 자동 진행 (SSE) |
| GET | /ai2/api/trial-verdict | 판결 생성 (SSE) |
| POST | /ai2/api/trial-complete | 재판 종료 |

### 데이터베이스 구조 (trial_case)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| case_id | INT (PK) | 사건 고유 ID |
| case_number | VARCHAR | 사건번호 (예: 2024고단1234) |
| case_type | VARCHAR | 사건 유형 (‘criminal’, ‘civil’) |
| defendant | VARCHAR | 피고인 이름 |
| charge | VARCHAR | 혐의 또는 청구 내용 |
| description | TEXT | 사건 개요 |
| status | VARCHAR | 상태 (‘registered’, ‘in_progress’, ‘closed’) |
| verdict | TEXT | 판결 내용 |
| created_at | TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | 수정 일시 |


---

## 4. Smart-Home 제어 시스템
이 기능은 사용자의 음성·텍스트 명령을 기반으로 난방, 조명, 환기 등 IoT 기기를 제어하고, 매뉴얼 문서 기반의 RAG 검색 및 Memory 대화를 제공함.

### 주요 화면 (탭 구성)
- **RAG 검색 탭**: 매뉴얼에서 정보 검색. 입력창, 결과 표시, 샘플 질문, 업로드 안내 포함.  
- **Memory 대화 탭**: SSE 기반 실시간 대화 UI 제공.  
- **문서 업로드 탭**: PDF/DOCX/TXT 파일 업로드 가능.  
- **디바이스 상태 탭**: 난방·조명·환기 등 실시간 상태 표시 (색상 피드백 제공).  
  → **위치:** `aipilot/src/main/webapp/views/smarthome/center.jsp`

### 주요 기능 (Backend Architecture)
- **API 네임스페이스**: `/ai2/api` 하위 모든 엔드포인트 구성.  
- **음성 명령 처리**:  
  - STT(Speech-to-Text) → IoT 제어 → TTS(Text-to-Speech) 파이프라인  
  - `OpenAiAudioTranscriptionModel`, `OpenAiAudioSpeechModel` 활용  
  - 생성된 음성은 Base64 인코딩 후 브라우저 재생.  
- **텍스트 명령 처리**: Function Calling 기반 기기 제어.  
- **RAG 검색 및 Memory 대화**: 매뉴얼 기반 정보 검색과 사용자 맞춤 대화 수행.

### 주요 기능 (IoT 제어 Function)
- **Ai2IotTools** 3가지 도구 제공:  
  - `controlHeating(boolean on)` : 난방 제어  
  - `controlLight(boolean on)` : 조명 제어  
  - `controlVentilation(boolean on)` : 환기 제어  
- 기기 상태는 `ConcurrentHashMap`으로 관리하며, 자연어 명령(“춥다”, “불 켜줘”)을 AI가 자동 분석 후 호출.

### 주요 기능 (RAG 파이프라인)
- **문서 파싱**:  
  - PDF → `ParagraphPdfDocumentReader`  
  - DOCX → `TikaDocumentReader`  
  - TXT → `TextReader`  
- **벡터 저장소 구성**: `TokenTextSplitter`로 분할 후 type 메타데이터와 함께 저장.  
  `TRUNCATE TABLE vector_store` 로 초기화 가능.  
- **RAG 검색**: 상위 3개 문서(유사도 ≥ 0.5)를 기반으로 `QuestionAnswerAdvisor`로 답변 생성.

### 주요 기능 (센서 및 상태 관리)
- **센서 데이터 시뮬레이션**:  
  - 온도(18-32℃), 습도(40-80%), 조도(100-1000 lux) 범위의 랜덤 값 생성.  
- **디바이스 상태 표시**: `/ai2/api/device-status` 호출로 상태 조회.  
  - ON → 초록색, OFF → 빨간색으로 표시.  
  - jQuery의 `loadDeviceStatus()` / `updateDeviceUI(data)` 로 화면 갱신.

### 주요 기능 (Memory 대화)
- **대화 ID 관리**: `HttpSession`의 `sessionId`를 `conversationId`로 사용.  
- **PromptChatMemoryAdvisor**로 과거 대화 참조, 사용자의 선호도(예: 온도 설정) 학습.  
- **맥락 의존 명령 처리**: “어제처럼 해줘”, “내가 좋아하는 온도” 등 실행 가능.

### 주요 API 엔드포인트
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /ai2/api/sensor-data | 센서 데이터 조회 |
| POST | /ai2/api/voice-control | 음성 명령 처리 (multipart/form-data) |
| POST | /ai2/api/text-control | 텍스트 명령 처리 (JSON) |
| POST | /ai2/api/rag-search | RAG 매뉴얼 검색 (JSON) |
| GET | /ai2/api/memory-chat | Memory 기반 채팅 (SSE) |
| POST | /ai2/api/upload-document | 문서 업로드 |
| POST | /ai2/api/clear-vector | 벡터 저장소 초기화 |
| GET | /ai2/api/device-status | 디바이스 상태 조회 |

오늘의 센서 데이터 조회
음성 명령 처리
## 5. Ai 세차장 시스템 (사용자)
이 기능은 AI가 번호판·차량 사진을 분석해 **입차 → 세차 계획/실행 → 출차**까지 자동화하며, @Tool 로 **차단봉(게이트)**을 제어함.

### 주요 기능 (입차 처리)
- 차량 사진 업로드 시 **번호판 OCR** 수행
- 우리 DB에서 **기존 고객 여부 확인**
- **정책(멤버십/잔액/예약 등)** 에 따라 차단봉 자동 **상승/하강**
- **입차/차단봉 동작**이 로그 테이블에 기록
- 결과(번호판, 기존 고객 여부, 차단봉 상태)를 **카드 UI**로 표시하고 **다음 단계 페이지**로 이동

### 주요 기능 (세차 계획/실행)
- **차량 전체 사진(오염 상태)** 업로드 → AI가 차량에 맞는 **세차 단계(노즐/압력/약품/시간)** 자동 생성
- **가격, 예상 소요 시간, 안전 주의사항** 함께 산출
- 생성된 계획은 `wash_order` / `wash_log` 테이블에 저장되며 **실제 장비 구동**까지 연계 가능
- 화면에서 **단계별 카드**와 **주문번호** 등 상세를 시각적으로 제공

### 주요 기능 (출차/차단봉 제어)
- **번호판 기준**으로 **출구 차단봉 열기/닫기** 페이지 제공
- 버튼 클릭 시 **장치 제어 서비스 호출** 및 **로그 기록**
- **출차 시각/게이트 조작 내역**을 누적 카드로 확인 가능

### 데이터베이스 구조 (vehicle)
| 필드명 | 타입 | 설명                                 |
|--------|------|------------------------------------|
| plate | TEXT (PK) | 번호판                                |
| customer_id | BIGINT | 고객 ID                     |
| model | TEXT | 차종                                 |
| size | TEXT | 차량 크기 분류 (compact, midsize, suv 등) |
| color | TEXT | 차량 색상 (black, white등등)             |
| last_wash_at | TIMESTAMP | 마지막 세차 시각       |


### 데이터베이스 구조 (wash_order)
| 필드명 | 타입 | 설명           |
|--------|------|--------------|
| id | TEXT (PK) | 주문번호         |
| plate | TEXT | 차량 번호판       |
| recipe_json | TEXT | LLM이 생성한 전체 세차 레시피 JSON |
| status | TEXT | 주문 상태 (RUNNING, DONE 등) |
| price | INTEGER | 예상 금액        |
| eta_min | INTEGER | 예상 소요 시간(분)  |
| created_at | TIMESTAMP | 생성 일시       |


### 데이터베이스 구조 (wash_log)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| id | BIGSERIAL (PK) | 로그 ID |
| order_id | TEXT | 주문번호  |
| step_idx | INTEGER | 단계 인덱스 (0, 1, 2, …) |
| step_name | TEXT | 단계 이름 |
| started_at | TIMESTAMP | 실제 시작 시각 |
| ended_at | TIMESTAMP | 실제 종료 시각  |
| pressure_bar | INTEGER | 계획/실제 압력 |
| chem_code | TEXT | 계획/실제 케미컬 코드 |
| result | TEXT | 단계 결과  |

### 데이터베이스 구조 (carwash_gate_log)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| id | BIGSERIAL (PK) | 게이트 로그 ID |
| plate | TEXT | 번호판 |
| event_type | TEXT | 이벤트 유형 (ENTRY 또는 EXIT) |
| logged_at | TIMESTAMP | 기록 시각  |

### 주요 API 엔드포인트 (사용자)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /ai6/entry-image | 입차 처리 |
| POST | /ai6/entry-gate-open | 입차용 수동 차단봉 오픈 |
| POST | /ai6/plan-image | 세차 레시피 생성/실행 |
| POST | /ai6/exit-gate | 출차 게이트 제어 |

---

## 6. Ai 세차장 시스템 (관리자)
세차장 **출입·세차·장비** 운영 데이터를 보고하고, **운영 매뉴얼을 벡터 DB**로 관리해 **AI 도우미**로 활용.

### 주요 기능 (오늘 장사 요약 지표)
- **KPI 카드**로 오늘의 핵심 지표 요약: 방문 차량 수, 매출 합계, 평균 단가, 장비 이상 의심 건수, 휴면 단골 수 등
- 지표는 백엔드 집계 후 **대시보드 카드/차트**로 제공
- 일 단위 추세/전일 대비 증감 **배지 표기**

### 주요 기능 (사장님 보고서 · 경영 질의)
- 사장 질문을 **/ai6/admin/owner-ask** 로 전송
- 서버는 **오늘 지표(방문/매출/장비)** + **업로드된 운영 매뉴얼**을 결합해 **AI 답변 생성**
- 응답 형식: **현황 요약 → 원인 가능성 → 지금 할만한 액션 제안**
- 질문/답변이 **카드 형태**로 누적되어 히스토리 관리

### 주요 기능 (운영 가이드 업로드 · 조회 · 질문)
- 운영 매뉴얼/장비 점검/응대 스크립트 문서를 **관리자가 업로드**
- `txt / pdf / docx` 업로드 시 **조각 분할 → vector_store 저장**, `type = "ops_guide"` 로 태깅
- **최근 업로드 조각 미리보기**(본문 일부 + 메타데이터)
- 테스트 질문창: “폼샴푸 라인 압력 떨어졌을 때 대응?” 등 → **업로드 문서 근거**로 AI 답변 생성  
  *(주의: 본 화면의 답변은 운영 문서만 근거로 판단, 매출/방문수 같은 숫자 미포함)*

### 주요 기능 (사장님 요약 보고 · 질의)
- 별도 화면에서 **KPI 카드 + 질문 입력창**
- KPI는 **/ai6/admin/summary/today** 에서 수신
- 질의는 **/ai6/admin/owner-ask** 로 전송, **오늘 지표 + 운영 가이드** 결합 답변
- 응답 형식 고정: **현황 요약 → 원인 가능성 → 우선 조치 제안**
- 카드로 누적되어 **대화형 리포팅** 제공

### 주요 API 엔드포인트 (관리자)
| 메서드 | 경로 | 설명             |
|--------|------|----------------|
| GET | /ai6/admin/summary/today | 오늘의 KPI 요약 지표 조회 |
| POST | /ai6/admin/owner-ask | 사장님 질의 -> AI 보고서 생성 |
| GET | /ai6/admin/gate-logs | 최근 게이트 로그 조회   |
| GET | /ai6/admin/vehicles | 등록된 차량 목록 전체 조회 |
| GET | /ai6/admin/orders | 최근 세차 주문 20건 조회 |

---

## 7. AI CCTV 모니터링 시스템
AI가 **실시간 CCTV 프레임**을 분석해 **화재/사고/응급** 등 위험을 **자동 감지**하고 **신고 시뮬레이션**까지 수행.

### 주요 기능 (실시간 감시)
- 화면에 **웹캠 미리보기**로 현재 장면 확인
- **분석 시작** 시 AI가 **20초 간격 자동 캡처·분석**
- 캡처 프레임을 AI로 전송하여 **위험 탐지**

### 주요 기능 (AI 재난 감지 및 신고)
- 화재, 폭발, 교통사고, 쓰러진 사람 등 **재난 요소 인식**
- **위험도 임계값 초과** 시 **112 신고 시뮬레이션**
- 결과/분석 로그를 **실시간 출력**, **실패/오탐/정상** 모두 로그 테이블 누적

### 주요 기능 (기록 관리 및 시뮬레이션 로그)
- 모든 이벤트를 `event_log` 테이블에 저장  
  (발생 시각, 감지 유형, 위험도, 신고 여부, 시스템 응답 결과)
- **이력 조회/재생** 기능으로 과거 데이터 검증

### 주요 기능 (시각화 및 알림)
- 감지 결과를 **카드 UI**로 시각화(이미지 + 설명)
- **음성/시각 알림**으로 즉시 인지
- 관리자 페이지에서 **기간별 통계, 오탐률, 평균 감지 시간** 대시보드 제공

### 데이터베이스 구조 (door_user)
| 필드명 | 타입 | 설명            |
|--------|------|---------------|
| id | BIGSERIAL (PK) | 사용자 고유 ID     |
| name | VARCHAR(100)  | 사용자 이름        |
| face_signature | TEXT  | AI가 추출한 얼굴 특징 |
| created_at | TIMESTAMP | 생성 시각         |

---

## 8. AI DOORS (출입문 제어 시스템)
**AI 얼굴 인식** 기반으로 **등록 → 인식 → 기록 관리**의 3단계 출입 통제.

### (1) 얼굴 등록 (Registration)
- 사용자 **이름 입력 + 얼굴 사진 업로드**로 등록
- **웹캠 미리보기**로 즉시 촬영 가능
- AI가 얼굴 **임베딩**을 추출해 `face_profile` 테이블에 저장
- 등록 성공 시 **임베딩 요약**이 포함된 **완료 카드** 표시

### (2) 얼굴 인식 (Recognition)
- **실시간 웹캠**으로 얼굴 감지, **인식 시작** 시 캡처 이미지를 AI로 전송
- DB 내 임베딩과 비교, **유사도 0.7 이상**이면 동일 인물로 판단 → **OpenDoor 시뮬레이션**
- 인식 실패 시 **CloseDoor 유지**, 실패 로그 기록
- 모든 출입 시도는 `door_log` 테이블에 **이름/시각/결과**로 누적

### (3) 출입 기록 (Records)
- 페이지 로드 시 **전체 출입 기록 조회**를 테이블로 표시
- **새로고침**으로 최신 기록 갱신
- 성공/실패에 따라 **행 색상 구분**
- 관리자 화면에서 **기간별 통계(시도 수, 성공률, 사용자별 빈도)** 제공

### 데이터베이스 구조 (door_access_record)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| id | BIGSERIAL (PK) | 출입 기록 고유 ID |
| name | VARCHAR(100)  | 출입 시도 사용자 이름 |
| status | VARCHAR(10)  | 출입 결과 상태  |
| access_time | TIMESTAMP | 출입 시각  |

***
# db에 필요한 테이블들(sql 스크립트)
```
-- 차량 테이블
CREATE TABLE IF NOT EXISTS vehicle (
                                       plate         TEXT PRIMARY KEY,        -- 번호판
                                       customer_id   BIGINT NULL,             -- 고객ID
                                       model         TEXT NULL,               -- 차종
                                       size          TEXT NULL,               -- compact/midsize/suv 등.
                                       color         TEXT NULL,               -- black/white
                                       last_wash_at  TIMESTAMP NULL           -- 마지막 세차시간. 세차 시작할 때 now()로 갱신
);

```

```
-- 세차 주문 테이블
CREATE TABLE IF NOT EXISTS wash_order (
                                          id           TEXT PRIMARY KEY,         -- 주문번호
                                          plate        TEXT NOT NULL,            -- vehicle.plate 참조
                                          recipe_json  TEXT NOT NULL,            -- LLM이 만든 전체 레시피 JSON
                                          status       TEXT NOT NULL,            -- RUNNING / DONE 등
                                          price        INTEGER NULL,             -- 예상 금액
                                          eta_min      INTEGER NULL,             -- 예상 시간(분)
                                          created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

```
-- 세차 로그 테이블
CREATE TABLE IF NOT EXISTS wash_log (
                                        id            BIGSERIAL PRIMARY KEY,
                                        order_id      TEXT NOT NULL,          -- wash_order.id
                                        step_idx      INTEGER NOT NULL,       -- 0,1,2
                                        step_name     TEXT NOT NULL,          -- preRinse, foam, rinse
                                        started_at    TIMESTAMP NULL,         -- 실제 시작 시간
                                        ended_at      TIMESTAMP NULL,         -- 아직 안끝났으면 NULL
                                        pressure_bar  INTEGER NULL,           -- 계획된/실제 압력
                                        chem_code     TEXT NULL,              -- 계획된/실제 케미컬
                                        result        TEXT NULL               -- PENDING / OK / ERROR 등
);

```

```
-- 세차장 차단봉 로그
CREATE TABLE IF NOT EXISTS carwash_gate_log (
                                                id          BIGSERIAL PRIMARY KEY,
                                                plate       TEXT        NOT NULL,          -- 번호판
                                                event_type  TEXT        NOT NULL,          -- 'ENTRY' 또는 'EXIT'
                                                logged_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

```

```
-- AI 얼굴 인식 출입 시스템 등록 사용자 정보
CREATE TABLE IF NOT EXISTS door_user (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(100) NOT NULL UNIQUE,
                                         face_signature TEXT NOT NULL,
                                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE door_user IS 'AI 얼굴 인식 출입 시스템 등록 사용자 정보';
COMMENT ON COLUMN door_user.name IS '사용자 이름 (Unique)';
COMMENT ON COLUMN door_user.face_signature IS 'AI가 분석한 얼굴 특징 텍스트 (Face Signature)';
```
```
-- AI 얼굴 인식 출입 기록
CREATE TABLE IF NOT EXISTS door_access_record (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL,
                                                  status VARCHAR(10) NOT NULL,
                                                  access_time TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE door_access_record IS 'AI 얼굴 인식 출입 기록';
COMMENT ON COLUMN door_access_record.name IS '출입 시도 사용자 이름';
COMMENT ON COLUMN door_access_record.status IS '출입 결과 상태 (SUCCESS/FAILED)';
COMMENT ON COLUMN door_access_record.access_time IS '출입 시각';
```
