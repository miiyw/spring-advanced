package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock private ManagerRepository managerRepository;
    @Mock private UserRepository userRepository;
    @Mock private TodoRepository todoRepository;

    @InjectMocks
    private ManagerService managerService;

    // ===== saveManager() 분기 =====

    @Test
    public void 담당자_등록_요청자가_일정_작성자가_아니면_예외가_발생한다() {
        // given: 요청자 ID=1, 일정 작성자 ID=999 → 불일치
        AuthUser authUser = new AuthUser(1L, "owner@ex.com", UserRole.USER);
        User todoOwner = new User("owner999", "owner999@ex.com", UserRole.USER);
        ReflectionTestUtils.setField(todoOwner, "id", 999L);

        Todo todo = new Todo("Title", "Contents", "Sunny", todoOwner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then: 요청자가 작성자가 아니므로 InvalidRequestException 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 10L, new ManagerSaveRequest(2L)));

        assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", ex.getMessage());
    }

    @Test
    public void 일정_작성자가_본인을_담당자로_등록하면_예외가_발생한다() {
        // given: 작성자=요청자 동일, 담당자 등록 대상도 자기 자신
        AuthUser authUser = new AuthUser(1L, "owner@ex.com", UserRole.USER);
        User owner = User.fromAuthUser(authUser);

        Todo todo = new Todo("Title", "Contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(userRepository.findById(1L)).willReturn(Optional.of(owner)); // 본인 조회

        // when & then: 본인 등록 시 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 10L, new ManagerSaveRequest(1L)));

        assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", ex.getMessage());
    }

    @Test
    void todo의_user가_null인_경우_예외가_발생한다() {
        // given: to_do.owner == null
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", 10L);
        ReflectionTestUtils.setField(todo, "user", null);

        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then: 작성자 없는 일정 → 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 10L, new ManagerSaveRequest(2L)));

        assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", ex.getMessage());
    }

    @Test
    void todo가_정상적으로_등록된다() {
        // given: 작성자 != 담당자 → 정상 등록
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        User owner = User.fromAuthUser(authUser);

        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        User managerUser = new User("mgr@ex.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(managerUser, "id", 2L);

        // save가 id가 부여된 엔티티를 반환하도록 설정(커버리지/검증 안정)
        Manager saved = new Manager(managerUser, todo);
        ReflectionTestUtils.setField(saved, "id", 100L);

        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(userRepository.findById(2L)).willReturn(Optional.of(managerUser));
        given(managerRepository.save(any(Manager.class))).willReturn(saved);

        // when: 담당자 등록 실행
        ManagerSaveResponse response = managerService.saveManager(authUser, 10L, new ManagerSaveRequest(2L));

        // then: 반환값 및 저장 검증
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(2L, response.getUser().getId());
        assertEquals("mgr@ex.com", response.getUser().getEmail());
        verify(managerRepository).save(any(Manager.class));
    }

    // ===== getManagers() 분기 =====

    @Test
    public void manager_목록_조회_시_Todo가_없다면_InvalidRequestException_에러를_던진다() {
        // given: 존재하지 않는 todoId
        long todoId = 1L;
        given(todoRepository.findById(todoId)).willReturn(Optional.empty());

        // when & then: 조회 시 예외 발생
        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    public void manager_목록_조회에_성공한다_id포함() {
        // given: to_do + manager 정상 존재
        long todoId = 1L;
        User user = new User("user1@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 777L);

        Todo todo = new Todo("Title", "Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        Manager manager = new Manager(user, todo);
        ReflectionTestUtils.setField(manager, "id", 123L);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(List.of(manager));

        // when
        List<ManagerResponse> list = managerService.getManagers(todoId);

        // then: 매니저 정보 확인
        assertEquals(1, list.size());
        assertEquals(123L, list.get(0).getId());
        assertEquals("user1@example.com", list.get(0).getUser().getEmail());
    }

    @Test
    void manager_목록_없으면_빈리스트_반환() {
        // given: to_do는 존재하지만 manager 없음
        long todoId = 1L;
        User user = new User("user", "user1@example.com", UserRole.USER);
        Todo todo = new Todo("Title", "Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(List.of());

        // when & then: 빈 리스트 반환
        assertTrue(managerService.getManagers(todoId).isEmpty());
    }

    // ===== deleteManager() 분기 =====

    @Test
    public void 담당자_삭제에_성공한다() {
        // given: 요청자=작성자, 해당 to_do에 속한 manager 존재
        User owner = new User("owner", "owner@ex.com", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 1L);

        Todo todo = new Todo("Title", "Contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        Manager manager = new Manager(owner, todo);
        ReflectionTestUtils.setField(manager, "id", 100L);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(managerRepository.findById(100L)).willReturn(Optional.of(manager));

        // when: 삭제 실행
        managerService.deleteManager(1L, 10L, 100L);

        // then: 실제 삭제 호출 여부 검증
        verify(managerRepository).delete(manager);
    }

    @Test
    public void 담당자_삭제_요청시_해당_일정의_담당자가_아니면_예외가_발생한다() {
        // given: manager가 다른 to_do에 소속됨
        User owner = new User("owner", "owner@ex.com", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 1L);

        Todo todo = new Todo("Title", "Contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        Todo otherTodo = new Todo("Other", "Other", "Cloudy", owner);
        ReflectionTestUtils.setField(otherTodo, "id", 99L);

        Manager manager = new Manager(owner, otherTodo);
        ReflectionTestUtils.setField(manager, "id", 100L);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(managerRepository.findById(100L)).willReturn(Optional.of(manager));

        // when & then: 다른 to_do 소속 → 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));

        assertEquals("해당 일정에 등록된 담당자가 아닙니다.", ex.getMessage());
        verify(managerRepository, never()).delete(any());
    }

    @Test
    public void 담당자_삭제시_일정_작성자가_불일치하면_예외가_발생한다() {
        // given: 요청자 != to_do.owner
        User requestUser = new User("req@ex.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(requestUser, "id", 1L);

        User todoOwner = new User("owner@ex.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(todoOwner, "id", 2L);

        Todo todo = new Todo("Title", "Contents", "Sunny", todoOwner);
        ReflectionTestUtils.setField(todo, "id", 10L);

        given(userRepository.findById(1L)).willReturn(Optional.of(requestUser));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then: 작성자 불일치 → 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));

        assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", ex.getMessage());
        verify(managerRepository, never()).delete(any());
    }


    @Test
    public void 담당자_삭제시_일정_작성자가_null이면_예외가_발생한다() {
        // given: to_do.owner == null
        User requestUser = new User("owner", "owner@ex.com", UserRole.USER);
        ReflectionTestUtils.setField(requestUser, "id", 1L);

        Todo todo = new Todo("Title", "Contents", "Sunny", null);
        ReflectionTestUtils.setField(todo, "id", 10L);

        given(userRepository.findById(1L)).willReturn(Optional.of(requestUser));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then: 작성자 없음 → 예외 발생
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));

        assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", ex.getMessage());
        verify(managerRepository, never()).delete(any());
    }
}
