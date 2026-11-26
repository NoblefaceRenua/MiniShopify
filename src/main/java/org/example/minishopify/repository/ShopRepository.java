package org.example.minishopify.repository;

import org.example.minishopify.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query("""
        SELECT s FROM Shop s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(s.tags) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Shop> searchShops(@Param("query") String query);
}
