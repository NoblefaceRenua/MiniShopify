
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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopSearchIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ShopRepository shops;

    @Autowired
    ProductRepository products;

    @BeforeEach
    void resetDatabase() {
        products.deleteAll();
        shops.deleteAll();
    }

    private Shop shop(String name, String tags) {
        return shops.save(new Shop(name, "Shop description", tags));
    }

    @Test
    void searchByShopName() throws Exception {
        shop("Electronics World", "gadgets, technology");
        shop("Fashion Store", "clothes, accessories");

        mvc.perform(get("/merchant").param("q", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronics World")))
                .andExpect(content().string(not(containsString("Fashion Store"))));
    }

    @Test
    void searchByShopTag() throws Exception {
        shop("Tech Store", "electronics, gadgets");
        shop("Fashion Store", "clothes, accessories");

        mvc.perform(get("/merchant").param("q", "gadgets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tech Store")))
                .andExpect(content().string(not(containsString("Fashion Store"))));
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        shop("Electronics World", "gadgets, technology");

        mvc.perform(get("/merchant").param("q", "ELEC"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronics World")));
    }

    @Test
    void searchSupportsPartialMatches() throws Exception {
        shop("Electronics World", "gadgets, technology");

        mvc.perform(get("/merchant").param("q", "tron"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronics World")));
    }

    @Test
    void searchReturnsNoResultsForUnknownQuery() throws Exception {
        shop("Electronics World", "gadgets, technology");

        mvc.perform(get("/merchant").param("q", "random123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No shops found")))
                .andExpect(content().string(not(containsString("Electronics World"))));
    }

    @Test
    void emptySearchDisplaysAllShops() throws Exception {
        shop("Electronics World", "gadgets");
        shop("Fashion Store", "clothes");

        mvc.perform(get("/merchant").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronics World")))
                .andExpect(content().string(containsString("Fashion Store")));
    }

    @Test
    void dashboardWithoutSearchDisplaysAllShops() throws Exception {
        shop("Electronics World", "gadgets");
        shop("Fashion Store", "clothes");

        mvc.perform(get("/merchant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronics World")))
                .andExpect(content().string(containsString("Fashion Store")));
    }

    @Test
    void searchPageProvidesBackToAllShopsLink() throws Exception {
        shop("Electronics World", "gadgets");

        mvc.perform(get("/merchant").param("q", "random123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No shops found")))
                .andExpect(content().string(containsString("Back to all shops")))
                .andExpect(content().string(containsString("/merchant")));
    }

    @Test
    void searchTrimsSurroundingWhitespace() throws Exception {
        shop("Electronics World", "gadgets");
        shop("Fashion Store", "clothes");

        mvc.perform(get("/merchant").param("q", "  gadgets  "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", "gadgets"))
                .andExpect(content().string(containsString("Electronics World")))
                .andExpect(content().string(not(containsString("Fashion Store"))));
    }

    @Test
    void whitespaceOnlySearchDisplaysAllShops() throws Exception {
        shop("Electronics World", "gadgets");
        shop("Fashion Store", "clothes");

        mvc.perform(get("/merchant").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", ""))
                .andExpect(content().string(containsString("Electronics World")))
                .andExpect(content().string(containsString("Fashion Store")));
    }
}
