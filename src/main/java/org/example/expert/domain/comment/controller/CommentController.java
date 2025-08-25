package org.example.expert.domain.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.service.CommentService;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/todos/{todoId}/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentSaveResponse> saveComment(
            @Auth AuthUser authUser,
            @PathVariable @Positive long todoId,
            @Valid @RequestBody CommentSaveRequest commentSaveRequest
    ) {
        CommentSaveResponse res = commentService.saveComment(authUser, todoId, commentSaveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(res); // 201 Created
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable @Positive long todoId) {
        List<CommentResponse> res = commentService.getComments(todoId);
        return ResponseEntity.ok(res); // 200 OK
    }
}
