package org.example.expert.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentAdminService {

    private final CommentRepository commentRepository;

    @Transactional
    public void deleteComment(long commentId) {
        // 존재 검사를 먼저 해서 메서드 내부에서 즉시 예외 발생 → AOP가 [ERROR] 로깅
        commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));

        commentRepository.deleteById(commentId);
    }
}
