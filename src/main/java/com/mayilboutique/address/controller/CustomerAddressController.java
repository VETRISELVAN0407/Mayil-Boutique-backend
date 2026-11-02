package com.mayilboutique.address.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.address.dto.request.AddressRequest;
import com.mayilboutique.address.dto.response.AddressResponse;
import com.mayilboutique.address.service.AddressService;
import com.mayilboutique.core.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/addresses")
public class CustomerAddressController {

    private final AddressService addressService;

    public CustomerAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(addressService.list(principal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(principal.getUserId(), id, request));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(principal.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        addressService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
