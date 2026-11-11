package org.example.minishopify.repository;

import org.example.minishopify.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {}
