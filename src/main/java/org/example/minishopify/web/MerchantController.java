package org.example.minishopify.web;

import jakarta.validation.Valid;
import org.example.minishopify.service.ProductService;
import org.example.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/merchant/shops")
public class MerchantController {
    private final ShopService shops;
    private final ProductService products;

    public MerchantController(ShopService shops, ProductService products) {
        this.shops = shops;
        this.products = products;
    }

    @GetMapping("/{id}")
    public String shop(@PathVariable Long id, Model model) {
        model.addAttribute("shop", shops.getShop(id));
        model.addAttribute("products", products.listProducts(id));
        return "merchant/shop";
    }

    @GetMapping("/{id}/products/new")
    public String newProduct(@PathVariable Long id, Model model) {
        model.addAttribute("shop", shops.getShop(id));
        model.addAttribute("productForm", new ProductForm());
        return "merchant/product-form";
    }

    @PostMapping("/{id}/products")
    public String addProduct(@PathVariable Long id,
                             @Valid @ModelAttribute("productForm") ProductForm form,
                             BindingResult errors, Model model) {
        model.addAttribute("shop", shops.getShop(id));
        if (!errors.hasErrors()) {
            try {
                products.addProduct(id, form);
                return "redirect:/merchant/shops/" + id;
            } catch (IllegalArgumentException ex) {
                errors.rejectValue("image", "image.invalid", ex.getMessage());
            }
        }
        return "merchant/product-form";
    }
}
