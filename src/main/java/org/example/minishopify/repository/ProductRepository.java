package org.example.minishopify.repository;

import java.util.List;
import org.example.minishopify.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopIdOrderByIdAsc(Long shopId);
}
