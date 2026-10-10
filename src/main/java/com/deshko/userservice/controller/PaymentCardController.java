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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService paymentCardService;

    @PostMapping("/users/{userId}/cards")
    public ResponseEntity<PaymentCardResponseDto> create(@PathVariable Long userId,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        PaymentCardResponseDto created = paymentCardService.create(userId, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/cards/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<PageResponseDto<PaymentCardResponseDto>> getAllByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDto.of(paymentCardService.getAllByUserId(userId, pageable)));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getById(id));
    }

    @GetMapping("/cards")
    public ResponseEntity<PageResponseDto<PaymentCardResponseDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDto.of(paymentCardService.getAll(name, surname, pageable)));
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        return ResponseEntity.ok(paymentCardService.update(id, dto));
    }

    @PatchMapping("/cards/{id}/status")
    public ResponseEntity<PaymentCardResponseDto> changeStatus(@PathVariable Long id,
                                                               @RequestParam boolean active) {
        return ResponseEntity.ok(paymentCardService.setActive(id, active));
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
