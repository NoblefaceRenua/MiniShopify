package org.example.minishopify.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class UploadErrorHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("Picture is too large. Go back and choose a PNG or JPEG under 5 MB.");
    }
}
