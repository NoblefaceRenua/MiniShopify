package org.example.minishopify.service;

import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShopService {
    private final ShopRepository shops;

    public ShopService(ShopRepository shops) { this.shops = shops; }

    public Shop getShop(Long id) {
        return shops.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
    }
}
