package org.example.minishopify.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ShopRepository;
import org.example.minishopify.web.ShopForm;
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

    public Shop createShop(ShopForm form) {
        String tags = Arrays.stream(form.getTags().split(","))
                .map(String::trim).filter(tag -> !tag.isEmpty())
                .map(tag -> tag.toLowerCase(Locale.ROOT)).distinct()
                .collect(Collectors.joining(", "));
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one category or tag.");
        }
        return shops.save(new Shop(form.getName().trim(),
                form.getDescription().trim(), tags));
    }
}

