package com.deshko.userservice.controller;

import com.deshko.userservice.dto.PageResponseDto;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.PaymentCardResponseDto;
import com.deshko.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService paymentCardService;

    @PostMapping("/users/me/cards")
    public ResponseEntity<PaymentCardResponseDto> createMine(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentCardRequestDto dto) {
        return create(Long.valueOf(jwt.getSubject()), dto);
    }

    @GetMapping("/users/me/cards")
    public ResponseEntity<PageResponseDto<PaymentCardResponseDto>> getMine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return getAllByUserId(Long.valueOf(jwt.getSubject()), pageable);
    }

    @PostMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<PaymentCardResponseDto> create(@PathVariable Long userId,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        PaymentCardResponseDto created = paymentCardService.create(userId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/cards/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<PageResponseDto<PaymentCardResponseDto>> getAllByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDto.of(paymentCardService.getAllByUserId(userId, pageable)));
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
    public ResponseEntity<PaymentCardResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getById(id));
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDto<PaymentCardResponseDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDto.of(paymentCardService.getAll(name, surname, pageable)));
    }

    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
    public ResponseEntity<PaymentCardResponseDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        return ResponseEntity.ok(paymentCardService.update(id, dto));
    }

    @PatchMapping("/cards/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
    public ResponseEntity<PaymentCardResponseDto> changeStatus(@PathVariable Long id,
                                                               @RequestParam boolean active) {
        return ResponseEntity.ok(paymentCardService.setActive(id, active));
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
