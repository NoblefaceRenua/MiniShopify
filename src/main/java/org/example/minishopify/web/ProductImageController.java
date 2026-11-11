package org.example.minishopify.web;

import org.example.minishopify.model.Product;
import org.example.minishopify.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductImageController {
    private final ProductService products;

    public ProductImageController(ProductService products) { this.products = products; }

    @GetMapping("/products/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Product product = products.getProduct(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(product.getImageContentType()))
                .contentLength(product.getImageData().length)
                .header("X-Content-Type-Options", "nosniff")
                .body(product.getImageData());
    }
}
