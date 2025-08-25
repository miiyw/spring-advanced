package org.example.expert.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentAdminService {

    private final CommentRepository commentRepository;

    @Transactional
    public void deleteComment(long commentId) {
        int rows = commentRepository.deleteByIdReturningCount(commentId);
        if (rows == 0) {
            // 존재하지 않으면 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // rows == 1 이면 정상 삭제 → 컨트롤러에서 204 반환
    }
}
