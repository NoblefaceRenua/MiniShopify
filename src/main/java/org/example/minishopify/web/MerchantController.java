package org.example.minishopify.web;

import jakarta.validation.Valid;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ShopRepository;
import org.example.minishopify.service.ProductService;
import org.example.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/merchant")
public class MerchantController {
    private final ShopRepository shopRepository;
    private final ShopService shops;
    private final ProductService products;

    public MerchantController(ShopRepository shopRepository, ShopService shops,
                              ProductService products) {
        this.shopRepository = shopRepository;
        this.shops = shops;
        this.products = products;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("shops", shopRepository.findAll());
        return "merchant/dashboard";
    }

    @GetMapping("/shops/new")
    public String newShop(Model model) {
        model.addAttribute("shopForm", new ShopForm());
        return "merchant/shop-form";
    }

    @PostMapping("/shops")
    public String createShop(@Valid @ModelAttribute ShopForm shopForm,
                             BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "merchant/shop-form";
        Shop shop;
        try {
            shop = shops.createShop(shopForm);
        } catch (IllegalArgumentException ex) {
            errors.rejectValue("tags", "invalid", ex.getMessage());
            return "merchant/shop-form";
        }
        redirect.addFlashAttribute("message", "Shop created.");
        return "redirect:/merchant/shops/" + shop.getId();
    }

    @GetMapping("/shops/{id}")
    public String showShop(@PathVariable Long id, Model model) {
        model.addAttribute("shop", shops.getShop(id));
        model.addAttribute("products", products.listProducts(id));
        return "merchant/shop";
    }

    @GetMapping("/shops/{id}/products/new")
    public String newProduct(@PathVariable Long id, Model model) {
        model.addAttribute("shop", shops.getShop(id));
        model.addAttribute("productForm", new ProductForm());
        return "merchant/product-form";
    }

    @PostMapping("/shops/{id}/products")
    public String addProduct(@PathVariable Long id,
                             @Valid @ModelAttribute ProductForm productForm,
                             BindingResult errors, Model model,
                             RedirectAttributes redirect) {
        model.addAttribute("shop", shops.getShop(id));
        if (errors.hasErrors()) return "merchant/product-form";
        try {
            products.addProduct(id, productForm);
        } catch (IllegalArgumentException ex) {
            errors.rejectValue("image", "invalid", ex.getMessage());
            return "merchant/product-form";
        }
        redirect.addFlashAttribute("message", "Product added.");
        return "redirect:/merchant/shops/" + id;
    }
}
