package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 최근 수정일 순으로 할 일 목록을 페이지네이션하여 조회
    // @EntityGraph를 사용하여 'user' 연관 엔티티 즉시 로딩
    @EntityGraph(attributePaths = "user")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    // 특정 ID의 할 일을 조회하고, 해당 할 일의 사용자 정보를 함께 가져옴
    // @EntityGraph를 사용하여 'user' 연관 엔티티 즉시 로딩
    @EntityGraph(attributePaths = "user")
    Optional<Todo> findByIdWithUser(@Param("todoId") Long todoId);

    // 특정 할 일의 개수를 조회 (주로 특정 ID의 할 일이 존재하는지 확인할 때 사용)
    int countById(Long todoId);
}
