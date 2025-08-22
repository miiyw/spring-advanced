package org.example.expert.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // Todo 생성
    @PostMapping("/todos")
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @Auth AuthUser authUser,                                // 인증된 사용자 정보
            @Valid @RequestBody TodoSaveRequest todoSaveRequest     // Todo 생성 요청 DTO
    ) {
        // TodoService를 통해 Todo 생성 후 응답 반환
        return ResponseEntity.ok(todoService.saveTodo(authUser, todoSaveRequest));
    }

    // Todo 목록 조회 (페이징 처리)
    @GetMapping("/todos")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @RequestParam(defaultValue = "1") int page,     // 페이지 번호
            @RequestParam(defaultValue = "10") int size     // 페이지 크기
    ) {
        // TodoService에서 @EntityGraph 기반 메서드를 호출하여, N+1 문제를 해결한 Todo 목록 반환
        return ResponseEntity.ok(todoService.getTodos(page, size));
    }

    // Todo 단건 조회
    @GetMapping("/todos/{todoId}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable long todoId) {
        // TodoService에서 @EntityGraph 기반 메서드를 호출하여, 단건 조회 시 연관된 User도 즉시 로딩하여 반환
        return ResponseEntity.ok(todoService.getTodo(todoId));
    }
}
