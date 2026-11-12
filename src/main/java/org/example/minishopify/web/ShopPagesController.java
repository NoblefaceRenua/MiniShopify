package org.example.minishopify.web;

import jakarta.validation.Valid;
import org.example.minishopify.model.Shop;
import org.example.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Shop dashboard and creation routes, taken from Renny's merchant prototype.
@Controller
@RequestMapping("/merchant")
public class ShopPagesController {
    private final ShopService shops;

    public ShopPagesController(ShopService shops) {
        this.shops = shops;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("shops", shops.listShops());
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
}
