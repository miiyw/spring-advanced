package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
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
        // given: 존재하지 않는 todoId와 댓글 요청 객체 준비
        long todoId = 1L;    // 테스트할 todoId 값 설정
        CommentSaveRequest request = new CommentSaveRequest("contents");    // 댓글 내용이 포함된 요청 객체
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);     // 인증된 사용자 정보

        // todoRepository.findById(todoId)가 비어있는 Optional을 반환하도록 설정
        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when: 댓글 저장 시 InvalidRequestException 발생 및 메시지 검증
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.saveComment(authUser, todoId, request)
        );

        // then: 실제 서비스 예외 메시지와 일치하는지 확인
        // Spring 6 기준 getStatusCode()는 HttpStatusCode 이므로 equals 비교 가능
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("할 일을 찾을 수 없습니다.", ex.getReason());
    }

    @Test
    public void comment를_정상적으로_등록한다() {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        Comment comment = new Comment(request.getContents(), user, todo);

        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(commentRepository.save(any())).willReturn(comment);

        // when
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then
        assertNotNull(result);
    }
}