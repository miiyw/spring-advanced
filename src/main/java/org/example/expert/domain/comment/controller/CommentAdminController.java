package org.example.expert.domain.comment.controller;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.service.CommentAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentAdminController {

    private final CommentAdminService commentAdminService;

    @DeleteMapping("/admin/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204
    public void deleteComment(@PathVariable long commentId) {
        commentAdminService.deleteComment(commentId);
    }
}
