package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
        // given: 존재하지 않는 todoId와 요청 준비
        long todoId = 1L;    // 테스트할 todoId 값 설정
        CommentSaveRequest request = new CommentSaveRequest("contents");    // 댓글 내용이 포함된 요청 객체
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);     // 인증된 사용자 정보

        // TodoRepository가 해당 ID의 To.do를 찾지 못하도록 설정
        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then: 댓글 저장 시 ResponseStatusException 발생 확인
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.saveComment(authUser, todoId, request)
        );

        // 예외의 상태 코드와 메시지가 서비스에서 정의한 값과 동일한지 검증
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("할 일을 찾을 수 없습니다.", ex.getReason());
    }

    @Test
    public void comment를_정상적으로_등록한다() {
        // given: 정상적인 todoId, 요청, 사용자, 할일, 댓글 객체 준비
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        Comment comment = new Comment(request.getContents(), user, todo);

        // Repository 동작 Mock 설정: To.do 존재, Comment 저장 성공
        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(commentRepository.save(any())).willReturn(comment);

        // when: 댓글 저장 실행
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then: 반환 결과가 null이 아님을 확인
        assertNotNull(result);
    }

    @Test
    void getComments_Todo없으면_404() {
        // given: 존재하지 않는 To.do
        long todoId = 1L;
        given(todoRepository.existsById(todoId)).willReturn(false);

        // when & then: 조회 시 404 예외 발생 확인
        var ex = assertThrows(ResponseStatusException.class,
                () -> commentService.getComments(todoId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("할 일을 찾을 수 없습니다.", ex.getReason());
    }

    @Test
    void getComments_댓글없으면_빈리스트200() {
        // given: To.do는 존재하지만 댓글은 없는 경우
        long todoId = 1L;
        given(todoRepository.existsById(todoId)).willReturn(true);
        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(List.of()); // 빈 목록

        // when: 댓글 조회 실행
        var result = commentService.getComments(todoId);

        // then: 빈 리스트 반환 확인
        assertNotNull(result);
        assertTrue(result.isEmpty()); // [] 반환 확인
    }

    @Test
    void getComments_댓글있으면_목록반환200() {
        // given: To.do 존재 + 댓글 한 개 존재
        long todoId = 1L;
        given(todoRepository.existsById(todoId)).willReturn(true);

        AuthUser authUser = new AuthUser(1L, "email@ex.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        Comment comment = new Comment("hi", user, todo);

        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(java.util.List.of(comment));

        // when: 댓글 조회 실행
        var list = commentService.getComments(todoId);

        // then: 결과 리스트 크기 및 값 검증
        assertEquals(1, list.size());
        assertEquals("hi", list.get(0).getContents());
        assertEquals(1L, list.get(0).getUser().getId());
        assertEquals("email@ex.com", list.get(0).getUser().getEmail());
    }
}