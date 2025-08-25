package org.example.expert.domain.manager.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.service.ManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/todos/{todoId}/managers")
public class ManagerController {

    private final ManagerService managerService;

    @PostMapping
    public ResponseEntity<ManagerSaveResponse> saveManager(
            @Auth AuthUser authUser,
            @PathVariable @Positive long todoId,
            @Valid @RequestBody ManagerSaveRequest managerSaveRequest
    ) {
        ManagerSaveResponse res = managerService.saveManager(authUser, todoId, managerSaveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(res); // 201 Created
    }

    @GetMapping
    public ResponseEntity<List<ManagerResponse>> getManagers(@PathVariable @Positive long todoId) {
        List<ManagerResponse> res = managerService.getManagers(todoId);
        return ResponseEntity.ok(res); // 200 OK
    }

    @DeleteMapping("/{managerId}")
    public ResponseEntity<Void> deleteManager(
            @Auth AuthUser authUser,
            @PathVariable @Positive long todoId,
            @PathVariable @Positive long managerId
    ) {
        managerService.deleteManager(authUser.getId(), todoId, managerId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
