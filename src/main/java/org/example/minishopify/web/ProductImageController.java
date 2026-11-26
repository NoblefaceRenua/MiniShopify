package org.example.minishopify.web;

import org.example.minishopify.model.Product;
import org.example.minishopify.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductImageController {
    private final ProductService products;
    public ProductImageController(ProductService products) { this.products = products; }

    @GetMapping("/products/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Product product = products.getProduct(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(product.getImageContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(product.getImageData());
    }
}
