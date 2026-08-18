package com.smashmate.controller;

import com.smashmate.common.ApiResponse;
import com.smashmate.dto.request.AssignAccountRequest;
import com.smashmate.dto.request.CreateMemberRequest;
import com.smashmate.dto.request.UpdateMemberRequest;
import com.smashmate.dto.response.MemberResponse;
import com.smashmate.entity.enums.MemberStatus;
import com.smashmate.entity.enums.Role;
import com.smashmate.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getAll(
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getAllMembers(role)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> create(
            @Valid @RequestBody CreateMemberRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberService.createMember(req), "Member created"));
    }

    @PostMapping("/guests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> createGuest(
            @RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "fullName is required"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberService.createGuest(fullName), "Guest created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMemberById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> update(
            @PathVariable Long id, @RequestBody UpdateMemberRequest req) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(id, req), "Updated"));
    }

    @PatchMapping("/{id}/account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> assignAccount(
            @PathVariable Long id, @Valid @RequestBody AssignAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success(memberService.assignAccount(id, req), "Account assigned"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
            memberService.updateStatus(id, MemberStatus.valueOf(body.get("status"))), "Status updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        memberService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Member deleted"));
    }
}
