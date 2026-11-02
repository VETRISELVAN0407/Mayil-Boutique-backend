package com.mayilboutique.tryon.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.core.security.CustomUserDetails;
import com.mayilboutique.tryon.dto.TryOnJobResponse;
import com.mayilboutique.tryon.service.TryOnJobService;

/** Under {@code /api/customer/**}, so only signed-in customers get here; every job is scoped to the caller. */
@RestController
@RequestMapping("/api/customer/try-on/jobs")
public class CustomerTryOnController {

    private final TryOnJobService service;

    public CustomerTryOnController(TryOnJobService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TryOnJobResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @RequestParam Long productId,
                                                   @RequestParam boolean consent,
                                                   @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .cacheControl(CacheControl.noStore())
                .body(service.create(principal.getUserId(), productId, photo, consent));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<TryOnJobResponse> get(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable Long jobId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(principal.getUserId(), jobId));
    }

    @GetMapping("/{jobId}/result")
    public ResponseEntity<Resource> result(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long jobId) {
        TryOnJobService.ResultFile file = service.result(principal.getUserId(), jobId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file.path()));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Long jobId) {
        service.delete(principal.getUserId(), jobId);
        return ResponseEntity.noContent().build();
    }
}
