package org.example.expert.domain.todo.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * To.do 관련 API 컨트롤러
 * - 전역 예외 처리(GlobalExceptionHandler)로 에러 응답 표준화
 * - N+1 문제 해결은 Service/Repository(@EntityGraph 또는 fetch join)에서 처리
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/todos") // 공통 경로 prefix
public class TodoController {

    private final TodoService todoService;

    // To.do 생성
    @PostMapping
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @Auth AuthUser authUser,                                // 인증된 사용자 정보 (JwtFilter→@Auth 리졸버)
            @Valid @RequestBody TodoSaveRequest todoSaveRequest     // To.do 생성 요청 DTO(Bean Validation 적용)
    ) {
        // TodoService를 통해 To.do 생성 후 응답 반환
        // 상태코드: 201 Created
        TodoSaveResponse res = todoService.saveTodo(authUser, todoSaveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // To.do 목록 조회 (페이징 처리)
    @GetMapping
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @RequestParam(defaultValue = "1") @Min(1) int page,     // 페이지 번호(1 이상)
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size     // 페이지 크기(1~100)
    ) {
        // TodoService에서 @EntityGraph 기반 메서드를 호출하여, N+1 문제를 해결한 To.do 목록 반환
        Page<TodoResponse> res = todoService.getTodos(page, size);
        return ResponseEntity.ok(res); // 200 OK
    }

    // To.do 단건 조회
    @GetMapping("/{todoId}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable @Positive long todoId) { // 양수 ID 검증
        // TodoService에서 @EntityGraph 기반 메서드를 호출하여, 단건 조회 시 연관된 User도 즉시 로딩하여 반환
        TodoResponse res = todoService.getTodo(todoId);
        return ResponseEntity.ok(res); // 200 OK
    }
}
