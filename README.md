# 🚀 Spring 애플리케이션 품질 향상

<details>
<summary><h2>✅ Lv 1. 코드 개선</h2></summary>

### 1-1. ✂️ Early Return (`AuthService.signup`)
조건에 맞지 않는 경우 즉시 리턴하여 **불필요한 로직 실행 방지**

**리팩토링 전**  
- 이메일 중복 확인 전에 `passwordEncoder.encode()` 실행  
- 불필요한 암호화 작업 발생 가능  

**리팩토링 후**  
- 이메일 중복 검사를 **먼저 수행**  
- 중복 시 즉시 예외 발생 → `encode()` 실행 방지  

👉 성능 최적화 및 코드 가독성 개선

---

### 1-2. 🔀 불필요한 if-else 제거 (`WeatherClient.getTodayWeather`)
복잡한 `if-else` 구조 제거하여 **코드 간결화**  

**리팩토링 전**  
- 상태 코드 검증 후에도 `else` 블록 안에서 추가 검증  
- 불필요한 중첩 발생  

**리팩토링 후**  
- 상태 코드가 `OK`가 아니면 **즉시 예외 발생**  
- 날씨 데이터가 없으면 **즉시 예외 발생**  
- `else` 블록 제거 → 가독성 향상  

👉 "조건 불일치 → 바로 종료" 원칙 적용

---

### 1-3. 🛡️ Validation 적용 `(UserService.changePassword`)  
비밀번호 유효성 검증을 **서비스 로직에서 DTO로 이동**  

**리팩토링 전**  
- `UserService` 내부에서 직접 문자열 길이, 정규식 검사 수행  

**리팩토링 후**  
- `UserChangePasswordRequest` DTO에 `@Validated` 적용  
- `@Size`, `@Pattern`, `@NotBlank` 등 어노테이션 기반 검증  
- 서비스는 **비즈니스 로직만 담당**, 검증 책임 분리  

👉 검증 책임 분리 → 코드 유지보수성 향상

</details>

---

<details>
<summary><h2>⚡ Lv 2. N+1 문제 해결</h2></summary>

### 🔍 `@EntityGraph`를 사용하여 `N+1` 문제 해결
기존에는 JPQL `fetch join`으로 `Todo`와 연관된 `User`를 한 번에 가져왔지만,<br>
이를 **`@EntityGraph`** 기반으로 수정하여 동일한 동작을 수행하도록 리팩토링 진행  

**리팩토링 전**  
- JPQL `LEFT JOIN FETCH`로 `User`를 즉시 로딩  
- 쿼리문이 길어지고 가독성이 떨어짐  

**리팩토링 후**  
- `@EntityGraph(attributePaths = "user")` 적용  
- JPQL 없이도 동일하게 **연관 엔티티(User) 즉시 로딩**  
- 쿼리 가독성 향상 및 유지보수성 개선  

👉 `N+1` 문제 해결 → 성능 최적화

</details>

---

<details>
<summary><h2>🧪 Lv 3. 테스트 코드 연습</h2></summary>

<details>
<summary><h3>3-1. 🔑 PasswordEncoderTest (matches_메서드가_정상적으로_동작한다())</h3></summary>

**문제**  
- `matches(raw, encoded)`의 **인자 순서**를 반대로 사용하여 테스트 실패  

**수정**  
- `matches(rawPassword, encodedPassword)`로 **원본 → 해시** 순서 교정  
- `assertTrue(matches)`로 정상 통과 확인  

👉 기본 사용 규약 준수로 오탑 방지 및 테스트 신뢰도 확보

</details>

---

<details>
<summary><h3>3-2. 📝 ManagerServiceTest & CommentServiceTest</h3></summary>

<details>
<summary><h4>1번 케이스. ManagerServiceTest — 예외 타입/메시지 불일치</h4></summary>

**문제**  
- 메서드명: `manager_목록_조회_시_Todo가_없다면_NPE_에러를_던진다()`  
- 실제 서비스는 `NullPointerException`이 아니라 **`InvalidRequestException("Todo not found")`를 던짐**  

**수정**  
- **메서드명**을 `..._InvalidRequestException_에러를_던진다()`로 변경  
- **검증 로직**을 `assertThrows(InvalidRequestException.class)`로 수정  
- **메시지**를 `"Todo not found"`로 일치  

👉 테스트 명세를 실제 컨텍스트와 정합화

</details>

---

<details>
<summary><h4>2번 케이스. CommentServiceTest — 잘못된 예외 기대 & stub 범위 과도</h4></summary>

**문제**  
- 서비스는 `InvalidRequestException("Todo not found")`를 던지는데 **테스트는 `ServerException`을 기대**  
- `given(todoRepository.findById(anyLong()))`로 **과도한 매칭** 사용  

**수정**  
- 기대 예외를 **`InvalidRequestException`으로 변경**  
- `given(todoRepository.findById(todoId)).willReturn(Optional.empty())`로 **명시적 id** 스텁  
- 메시지 검증: `"Todo not found"`  

👉 서비스 계약에 맞춘 예외 타입/메시지 정합 + 테스트 의도 명확화

</details>

---

<details>
<summary><h4>3번 케이스. ManagerService — todo 작성자 `null` NPE 방지</h4></summary>

**문제**  
- `todo.getUser().getId()` 접근 전에 **작성자 null** 가능성 미고려 → NPE 발생  
- 팀원 로직 변경 후 테스트 `todo의_user가_null인_경우_예외가_발생한다()` 실패  

**수정**  
- `User todoOwner = todo.getUser();` 로 참조 분리  
- `if (todoOwner == null || !nullSafeEquals(user.getId(), todoOwner.getId())) { throw new InvalidRequestException(...); }`  
- NPE 없이 **의도된 `InvalidRequestException`로 수렴**  

👉 null-safe 비교로 안정성 향상 및 테스트 회복

</details>

</details>

</details>

---

<details>
<summary><h2>🔐 Lv 4. API 로깅</h2></summary>

### 🎯 어드민 전용 API 요청/응답 로깅 개요
어드민 전용 API에 대해 Interceptor와 AOP를 조합해 **접근 제어 + 요청·응답 로깅** 구현

**대상 API (관리자 전용)**  
- `CommentAdminController` → `deleteComment()`
- `UserAdminController` → `changeUserRole()`

---

### 🧱 Interceptor — 접근 제어 & 접근 로그
- 권한 확인: 로그인하지 않은 경우 `401`, ADMIN 권한이 아닌 경우 `403`  
- `요청 메서드`, `URI`, `userId`를 사전 로깅  
- `/admin/comments/**`, `/admin/users/**` 경로에만 적용  
- ADMIN 권한 통과 시 `[ADMIN-ALLOW]` 로그 기록

---

### 🧭 AOP — 요청·응답 본문 JSON 로깅
- Around Advice를 사용해 **메서드 실행 전/후/예외 시점 로깅**  
- 요청 정보: `요청자 ID`, `요청 시각`, `URL`, `HTTP 메서드`, `RequestBody(JSON)`  
- 응답 정보: `ResponseBody(JSON)`, `소요 시간(ms)`  
- 예외 발생 시: `errorType`, `errorMessage`까지 포함해 **경고 로그 출력**  
- 두 관리자 API(`deleteComment`, `changeUserRole`)만 대상으로 지정

---

### 📌 요약
- **Interceptor**: 관리자 전용 경로에서 **사전 권한 검증 + 접근 로그**  
- **AOP**: 대상 메서드 실행 전후로 **요청/응답/에러 로그(JSON)** 기록  
- 효과: 보안성 강화, 운영 시 분석 가능한 구조적 로그 확보  

</details>

---

<details>
<summary><h2>🛠️ Lv 5. 내가 정의한 문제와 해결 과정</h2></summary>

### 1. 🔍 문제 인식 및 정의
- **예외 응답 비표준화**
  - 동일한 상황에서도 응답 포맷과 메시지가 제각각  
  - 예: `Comment not found`, `404 NOT_FOUND`, HTML 에러 페이지 등
- **삭제/조회 결과의 모호함**
  - 존재하지 않는 댓글 삭제 시 `204` 응답  
  - 없는 Todo 조회 시 `200 []` 반환  

---

### 2. 💡 해결 방안

#### 2-1. ⚖️ 의사결정 과정
- **전역 예외 표준화 기준 수립**
  - 최소 포맷: `{ "code": <ERROR_CODE>, "message": <문구>, ["errors": [...]] }`
  - 상태별 코드 매핑:
    - `400` → VALIDATION_ERROR  
    - `401` → AUTH_REQUIRED  
    - `403` → FORBIDDEN  
    - `404` → NOT_FOUND  
    - `5xx` → SERVER_ERROR
  - `ResponseStatusException`의 `reason`은 노출하지 않음 (일관 메시지 유지)

- **JWT 처리 단일화**
  - `JwtFilter`에서 파싱/만료/서명 검증 + `AuthUser` 구성  
  - 컨트롤러는 `@Auth AuthUser`만 받도록 단순화

- **삭제/조회 일관성**
  - 없는 리소스: `404` 
  - 존재하지만 결과 없음: `200` + 빈 배열  
  - 삭제는 **영향 행 수 기준**으로 `404`/`204` 구분 (경합 시 안전)

- **클라이언트 편의**
  - 토큰 응답은 `"Bearer <JWT>"` 단일 필드  
  - 내부 유틸은 접두어 제거 후 **순수 토큰만 사용**

#### 2-2. 🛠️ 해결 과정
- **GlobalExceptionHandler 정비**
  - 커스텀 예외 / 스프링 기본 예외 / 검증 예외를 **단일 포맷**으로 변환  
  - `ResponseStatusException`은 `reason` 무시 → 상태별 표준 메시지 강제  
  - `404` 계열(N`oHandlerFound`, `NoResourceFound`)도 **JSON 포맷**으로 통일

- **JwtFilter 개선**
  - `Authorization` 헤더 미제공, 형식 오류, 만료, 서명 오류 → `401 AUTH_REQUIRED` 통일  
  - `userRole` 속성 정규화 (대소문자, 접두어 `ROLE_` 처리)  
  - `authUser`를 `request attribute`에 주입 (레거시 `userId`/`email`/`userRole`도 병행 세팅)

- **DTO/컨트롤러 검증 강화**
  - 생성 → `201`, 수정/삭제 → `204`, 조회 → `200`  
  - try-catch 제거, 전역 핸들러에 예외 위임  
  - DTO에 **Bean Validation 적용** (`@NotBlank`, `@Pattern` 등)

---

### 3. ✅ 해결 완료

#### 3-1. 🔄 회고
- **일관성/단순성**
  - 예외가 어디서 발생하든 **동일 포맷** → **클라이언트 처리 단순화** 
  - 컨트롤러는 성공 플로우만 유지 → **가독성·테스트 용이성 ↑**

- **보안/정확성**
  - 토큰 만료/서명 오류가 확실히 `401`로 떨어짐  
  - 파싱 중복 제거 → **오류 지점 명확**  
  - 회원 가입/비밀번호 변경 검증이 DTO 레벨에서 통일

#### 3-2. 📊 전후 비교

| 시나리오              | Before                                           | After                                                                 |
|------------------------|--------------------------------------------------|----------------------------------------------------------------------|
| 없는 댓글 삭제        | `204 No Content` / `"Comment not found"` / HTML      | `404` + `{ "code": "NOT_FOUND", "message": "리소스를 찾을 수 없습니다." }` |
| 잘못된 토큰           | `400`/`500`/HTML 혼재                                | `401` + `{ "code": "AUTH_REQUIRED", "message": "유효하지 않은 JWT 서명입니다." }` |
| 깨진 JSON 본문        | HTML 에러 페이지                                 | `400` + `{ "code": "VALIDATION_ERROR", "message": "요청 값이 올바르지 않습니다." }` |
| 없는 URL              | `HTML 404`                                         | `404` + `{ "code": "NOT_FOUND", "message": "리소스를 찾을 수 없습니다." }` |

---

### 📌 상태별 최종 처리 기준
- ❌ **`400` Bad Request** — 요청/검증 오류  
  - DTO 검증 실패, 파라미터 검증 실패
- ❌ **`401` Unauthorized** — 토큰 문제 (`JwtFilter`)  
  - 미제공, 잘못된/만료 토큰
- ❌ **`403` Forbidden** — 권한 부족  
  - 관리자 전용 API에 일반 사용자 접근
- ❌ **`404` Not Found** — 리소스 없음  
  - 존재하지 않는 ID, 없는 경로  

</details>

---

<details>
<summary><h2>📊 Lv 6. 테스트 커버리지</h2></summary>

### 🟢 Line Coverage
- **정의**: 테스트가 코드의 **몇 % 라인을 실행했는지** 확인하는 지표  
- **확인 방법**: IntelliJ → `Run with Coverage` 실행 → 초록(테스트된 코드), 빨강(미커버 코드) 표시  
- **활용**: 빨강 부분을 기준으로 추가 테스트 작성 → 커버리지 향상  

---

### 🔵 Condition Coverage
- **정의**: 조건식 `(A && B)`의 A/B 각각이 **참/거짓**으로 평가되는 케이스가 모두 실행되었는지 측정  
- **효과**: 단순히 `if`문을 통과했는지 여부가 아니라, **각 분기별 테스트 보장**  

---

### 📂 주요 테스트 대상
- **`CommentServiceTest`**
  - 댓글 등록 시 Todo 없음 → `InvalidRequestException` 발생  
  - 정상 등록 시 댓글 저장 및 응답 DTO 검증  

- **`ManagerServiceTest`**
  - Todo 없음 → `InvalidRequestException("Todo not found")`  
  - todo.user가 null → NPE 방지, `InvalidRequestException`으로 통일  
  - 정상 조회/저장 시 담당자 목록 DTO 반환 검증  

- **`PasswordEncoderTest`**
  - `matches()` 메서드 인자 순서 수정 → 원본/암호화 값 비교 정상화  

- **`AuthServiceTest`** (신규 작성)
  - **`signup`**
    - 중복 이메일: `encode/save/JWT` 호출 안 됨 검증  
    - 정상 가입: `encode/save/JWT` 호출, `Bearer` 토큰 반환  
    - JWT가 이미 `Bearer` 시작 → 접두어 중복 방지 검증  
  - **`signin`**
    - 가입되지 않은 이메일 → 예외  
    - 비밀번호 불일치 → 예외  
    - 정상 로그인 → `Bearer` 토큰 반환  
    - JWT가 이미 `Bearer` 시작 → 접두어 중복 방지 검증  

---

### 🖼️ 커버리지 결과 첨부

- `CommentServiceTest`

<img width="1344" height="1558" alt="Image" src="https://github.com/user-attachments/assets/8d1328d0-b1c1-4e93-a146-67afb1804d5c" /> | <img width="2006" height="1462" alt="Image" src="https://github.com/user-attachments/assets/5a2746fd-8172-4456-98de-9ba04c0835d0" /> | <img width="2056" height="1012" alt="Image" src="https://github.com/user-attachments/assets/bda90821-0bbc-4061-9bbc-e0f44164adf0" />
---|---|---|
  
- `ManagerServiceTest`

<img width="350" height="350" alt="Image" src="https://github.com/user-attachments/assets/84c678ef-5aed-46b5-97f5-06a80d1dfdfe" />

<img width="2034" height="1406" alt="Image" src="https://github.com/user-attachments/assets/46eadcaf-bd96-49a5-8d1c-80e246f4bf65" /> | <img width="2054" height="1190" alt="Image" src="https://github.com/user-attachments/assets/1911a6e5-39ad-44e4-a5fd-5d1d285a6f66" /> | <img width="2024" height="966" alt="Image" src="https://github.com/user-attachments/assets/115e5d61-35a9-490f-8a50-6b75a855cd7c" />
---|---|---|

- `PasswordEncoderTest` / `AuthServiceTest`

<img width="1286" height="1378" alt="Image" src="https://github.com/user-attachments/assets/63183f62-b329-4ba9-84b8-7b41579c2121" /> | <img width="1592" height="1460" alt="Image" src="https://github.com/user-attachments/assets/ddc40d40-da33-499e-a100-e95c85890b32" /> | <img width="1782" height="1224" alt="Image" src="https://github.com/user-attachments/assets/6992b5cc-413c-4ef4-b8f7-987b62af7b4f" />
---|---|---|


---

### ✅ 요약
- **Line Coverage**를 통해 **전체 코드 실행 범위** 확인  
- **Condition Coverage**를 통해 **분기 조건별 테스트 보장** 
- `AuthServiceTest` 추가 작성으로 **비즈니스 로직 핵심 경로**까지 커버

</details>
