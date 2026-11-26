package org.example.minishopify;

import java.math.BigDecimal;
import org.example.minishopify.model.Product;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopEditIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;

    private Shop shop;

    @BeforeEach
    void setUp() {
        products.deleteAll();
        shops.deleteAll();
        shop = shops.save(new Shop("Book Nook", "Used books", "books"));
    }

    @Test
    void editPageIsLinkedAndPrefilled() throws Exception {
        mvc.perform(get("/merchant/shops/" + shop.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "href=\"/merchant/shops/" + shop.getId() + "/edit\"")));
        mvc.perform(get("/merchant/shops/" + shop.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Book Nook\"")))
                .andExpect(content().string(containsString(">Used books</textarea>")))
                .andExpect(content().string(containsString("value=\"books\"")));
    }

    @Test
    void validEditNormalizesDetailsAndPreservesProducts() throws Exception {
        Product product = products.save(new Product("Novel", "A good read",
                new BigDecimal("12.50"), 3, new byte[]{1, 2, 3}, "image/png", shop));

        mvc.perform(post("/merchant/shops/" + shop.getId() + "/edit")
                        .param("name", " New Nook ")
                        .param("description", " Updated description ")
                        .param("tags", " Gifts, Handmade, GIFTS, "))
                .andExpect(redirectedUrl("/merchant/shops/" + shop.getId()))
                .andExpect(flash().attribute("message", "Shop updated."));

        Shop updated = shops.findById(shop.getId()).orElseThrow();
        assertEquals("New Nook", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("gifts, handmade", updated.getTags());
        assertEquals(1, shops.count());
        assertEquals(product.getId(), products.findByShopIdOrderByIdAsc(shop.getId()).get(0).getId());
        mvc.perform(get("/merchant/shops/" + shop.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New Nook")))
                .andExpect(content().string(containsString("Novel")));
    }

    @Test
    void blankNameIsRejectedWithoutChangingShop() throws Exception {
        mvc.perform(post("/merchant/shops/" + shop.getId() + "/edit")
                        .param("name", " ")
                        .param("description", "Changed description")
                        .param("tags", "gifts"))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/shop-edit"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "name"));
        assertOriginalDetails();
    }

    @Test
    void emptyNormalizedTagsAreRejectedWithoutChangingShop() throws Exception {
        mvc.perform(post("/merchant/shops/" + shop.getId() + "/edit")
                        .param("name", "Changed name")
                        .param("description", "Changed description")
                        .param("tags", ", ,"))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/shop-edit"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "tags"));
        assertOriginalDetails();
    }

    @Test
    void searchUsesEditedNameAndTags() throws Exception {
        mvc.perform(post("/merchant/shops/" + shop.getId() + "/edit")
                        .param("name", "Craft Corner")
                        .param("description", "Handmade gifts")
                        .param("tags", "Handmade, GIFTS"))
                .andExpect(redirectedUrl("/merchant/shops/" + shop.getId()));

        for (String query : new String[]{"CRAFT", "GIFTS"}) {
            mvc.perform(get("/merchant").param("q", query))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("shops", hasSize(1)))
                    .andExpect(content().string(containsString("Craft Corner")));
        }
        mvc.perform(get("/merchant").param("q", "books"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("shops", hasSize(0)));
    }

    @Test
    void editingMissingShopReturns404() throws Exception {
        mvc.perform(get("/merchant/shops/999999/edit"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/merchant/shops/999999/edit")
                        .param("name", "Missing shop")
                        .param("description", "Description")
                        .param("tags", "gifts"))
                .andExpect(status().isNotFound());
        assertEquals(1, shops.count());
    }

    private void assertOriginalDetails() {
        Shop unchanged = shops.findById(shop.getId()).orElseThrow();
        assertEquals("Book Nook", unchanged.getName());
        assertEquals("Used books", unchanged.getDescription());
        assertEquals("books", unchanged.getTags());
    }
}
