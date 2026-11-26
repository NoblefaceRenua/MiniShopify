package org.example.minishopify;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Shop dashboard/creation tests, adapted from Renny's MerchantIntegrationTest.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopPagesTest {
    @Autowired MockMvc mvc;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;

    @BeforeEach
    void resetDatabase() {
        products.deleteAll();
        shops.deleteAll();
    }

    private Shop shop(String name) {
        return shops.save(new Shop(name, "Shop description", "clothes"));
    }

    @Test
    void homeAndDashboardLoad() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/merchant"));
        mvc.perform(get("/merchant")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your shops")));
    }

    @Test
    void formPagesRender() throws Exception {
        Long id = shop("Renua's shop").getId();
        mvc.perform(get("/merchant/shops/new")).andExpect(status().isOk());
        mvc.perform(get("/merchant/shops/" + id + "/products/new"))
                .andExpect(status().isOk());
    }

    @Test
    void shopIsSavedAndDisplayed() throws Exception {
        mvc.perform(post("/merchant/shops")
                        .param("name", " Renua Store ")
                        .param("description", "Handmade products")
                        .param("tags", "Clothes, handmade, clothes"))
                .andExpect(status().is3xxRedirection());
        Shop saved = shops.findAll().get(0);
        assertEquals("Renua Store", saved.getName());
        assertEquals("clothes, handmade", saved.getTags());
        mvc.perform(get("/merchant/shops/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Renua Store")));
    }

    @Test
    void blankShopIsNotSaved() throws Exception {
        mvc.perform(post("/merchant/shops").param("name", " ")
                        .param("description", "Description").param("tags", "clothes"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "name"));
        assertEquals(0, shops.count());
    }

    @Test
    void emptyTagListIsNotSaved() throws Exception {
        mvc.perform(post("/merchant/shops").param("name", "Shop")
                        .param("description", "Description").param("tags", ", ,"))
                .andExpect(model().attributeHasFieldErrors("shopForm", "tags"));
        assertEquals(0, shops.count());
    }
}
