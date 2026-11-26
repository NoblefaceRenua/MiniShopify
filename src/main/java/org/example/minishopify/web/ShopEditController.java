package org.example.minishopify.web;

import jakarta.validation.Valid;
import org.example.minishopify.model.Shop;
import org.example.minishopify.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/merchant/shops/{id}/edit")
public class ShopEditController {
    private final ShopService shops;

    public ShopEditController(ShopService shops) {
        this.shops = shops;
    }

    @GetMapping
    public String edit(@PathVariable Long id, Model model) {
        Shop shop = shops.getShop(id);

        ShopForm form = new ShopForm();
        form.setName(shop.getName());
        form.setDescription(shop.getDescription());
        form.setTags(shop.getTags());

        model.addAttribute("shop", shop);
        model.addAttribute("shopForm", form);
        return "merchant/shop-edit";
    }

    @PostMapping
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("shopForm") ShopForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect) {

        model.addAttribute("shop", shops.getShop(id));

        if (errors.hasErrors()) {
            return "merchant/shop-edit";
        }

        try {
            shops.updateShop(id, form);
        } catch (IllegalArgumentException ex) {
            errors.rejectValue("tags", "invalid", ex.getMessage());
            return "merchant/shop-edit";
        }

        redirect.addFlashAttribute("message", "Shop updated.");
        return "redirect:/merchant/shops/" + id;
    }
}