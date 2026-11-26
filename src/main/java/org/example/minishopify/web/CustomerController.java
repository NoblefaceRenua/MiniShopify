package org.example.minishopify.web;

import java.util.Locale;
import org.example.minishopify.service.ProductService;
import org.example.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerController {
    private final ProductService products;
    private final ShopService shops;

    public CustomerController(ProductService products, ShopService shops) {
        this.products = products;
        this.shops = shops;
    }

    @GetMapping("/products")
    public String browse(@RequestParam(defaultValue = "") String name,
                         @RequestParam(defaultValue = "") String category,
                         Model model) {
        String searchName = name.trim();
        String searchCategory = category.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("name", searchName);
        model.addAttribute("category", searchCategory);
        model.addAttribute("categories", shops.listCategories());
        model.addAttribute("products", products.searchProducts(searchName, searchCategory));
        return "customer/products";
    }
}
