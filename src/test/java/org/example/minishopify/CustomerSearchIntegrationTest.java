package org.example.minishopify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import org.example.minishopify.model.Product;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;

    private Shop apparel;
    private Product greenShirt;
    private byte[] picture;

    @BeforeEach
    void setUp() throws Exception {
        products.deleteAll();
        shops.deleteAll();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        picture = bytes.toByteArray();

        apparel = shops.save(new Shop("Everyday Clothes", "Handmade clothing",
                " clothes, Handmade, clothes, "));
        Shop art = shops.save(new Shop("Art Studio", "Creative supplies", "art, gifts"));
        Shop party = shops.save(new Shop("Party Store", "Party supplies", "party, gifts"));
        greenShirt = product("Green Shirt", apparel);
        product("Canvas Bag", apparel);
        product("Watercolor Kit", art);
        product("Party Shirt", party);
    }

    private Product product(String name, Shop shop) {
        return products.save(new Product(name, "Product description",
                new BigDecimal("12.50"), 3, picture, "image/png", shop));
    }

    @Test
    void customersCanBrowseProductsAcrossShops() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/products"))
                .andExpect(model().attribute("products", hasSize(4)))
                .andExpect(content().string(containsString("Green Shirt")))
                .andExpect(content().string(containsString("Canvas Bag")))
                .andExpect(content().string(containsString("Watercolor Kit")))
                .andExpect(content().string(containsString("Party Shirt")))
                .andExpect(content().string(containsString("Everyday Clothes")))
                .andExpect(content().string(containsString("Art Studio")))
                .andExpect(content().string(containsString("CAD $")))
                .andExpect(content().string(containsString("12.50")))
                .andExpect(content().string(containsString("/products/" + greenShirt.getId() + "/image")))
                .andExpect(content().string(not(containsString("/delete"))))
                .andExpect(content().string(not(containsString("/edit"))));
        mvc.perform(get("/products/" + greenShirt.getId() + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(picture));
    }

    @ParameterizedTest
    @ValueSource(strings = {"shirt", "SHIRT", "  ShiRt  "})
    void productNameSearchIsPartialCaseInsensitiveAndTrimmed(String name) throws Exception {
        mvc.perform(get("/products").param("name", name))
                .andExpect(status().isOk())
                .andExpect(model().attribute("name", name.trim()))
                .andExpect(model().attribute("products", hasSize(2)))
                .andExpect(content().string(containsString("Green Shirt")))
                .andExpect(content().string(containsString("Party Shirt")))
                .andExpect(content().string(not(containsString("Canvas Bag"))))
                .andExpect(content().string(not(containsString("Watercolor Kit"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"art", "ART", "  art  "})
    void categorySearchMatchesWholeTagsIgnoringCase(String category) throws Exception {
        mvc.perform(get("/products").param("category", category))
                .andExpect(status().isOk())
                .andExpect(model().attribute("category", "art"))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(content().string(containsString("Watercolor Kit")))
                .andExpect(content().string(not(containsString("Party Shirt"))))
                .andExpect(content().string(not(containsString("Green Shirt"))))
                .andExpect(content().string(containsString("selected=\"selected\"")));
    }

    @Test
    void productNameAndCategoryFiltersAreCombined() throws Exception {
        mvc.perform(get("/products").param("name", "shirt").param("category", " handmade "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(content().string(containsString("Green Shirt")))
                .andExpect(content().string(not(containsString("Party Shirt"))))
                .andExpect(content().string(not(containsString("Canvas Bag"))))
                .andExpect(content().string(containsString("Clear filters")));
    }

    @Test
    void productMustMatchBothFilters() throws Exception {
        mvc.perform(get("/products").param("name", "kit").param("category", "clothes"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(0)))
                .andExpect(content().string(containsString("No products match your search")));
    }

    @Test
    void unknownProductNameShowsEmptyResultsAndClearLink() throws Exception {
        mvc.perform(get("/products").param("name", "missing product"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(0)))
                .andExpect(content().string(containsString("No products match your search")))
                .andExpect(content().string(containsString("Clear filters")));
    }

    @Test
    void unknownCategoryIsPreservedAndHasNoResults() throws Exception {
        mvc.perform(get("/products").param("category", "unknown"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("category", "unknown"))
                .andExpect(model().attribute("products", hasSize(0)))
                .andExpect(content().string(containsString("value=\"unknown\"")))
                .andExpect(content().string(containsString("No products match your search")));
    }

    @Test
    void blankFiltersShowAllProducts() throws Exception {
        mvc.perform(get("/products").param("name", "   ").param("category", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("name", ""))
                .andExpect(model().attribute("category", ""))
                .andExpect(model().attribute("products", hasSize(4)))
                .andExpect(content().string(not(containsString("Clear filters"))));
    }

    @Test
    void categoriesAreNormalizedDeduplicatedAndSorted() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categories",
                        contains("art", "clothes", "gifts", "handmade", "party")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_"})
    void productNameSearchTreatsSqlWildcardsAsLiteralText(String name) throws Exception {
        product("100% Cotton_Shirt", apparel);
        mvc.perform(get("/products").param("name", name))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(content().string(containsString("100% Cotton_Shirt")))
                .andExpect(content().string(not(containsString("Green Shirt"))));
    }

    @Test
    void emptyCatalogShowsHelpfulMessage() throws Exception {
        products.deleteAll();
        shops.deleteAll();
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(0)))
                .andExpect(model().attribute("categories", hasSize(0)))
                .andExpect(content().string(containsString("No products yet")));
    }

    @Test
    void searchValuesAreEscapedWhenRedisplayed() throws Exception {
        mvc.perform(get("/products").param("name", "<script>").param("category", "<script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void searchReflectsProductAndShopEdits() throws Exception {
        mvc.perform(post("/merchant/shops/" + apparel.getId() + "/products/" + greenShirt.getId())
                        .param("name", "Trail Jacket")
                        .param("description", "Warm jacket")
                        .param("price", "25.00")
                        .param("inventoryQuantity", "5"))
                .andExpect(redirectedUrl("/merchant/shops/" + apparel.getId()));
        mvc.perform(post("/merchant/shops/" + apparel.getId() + "/edit")
                        .param("name", "Outdoor Store")
                        .param("description", "Outdoor clothes")
                        .param("tags", "Outdoors"))
                .andExpect(redirectedUrl("/merchant/shops/" + apparel.getId()));

        mvc.perform(get("/products").param("name", "TRAIL").param("category", "outdoors"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(content().string(containsString("Trail Jacket")))
                .andExpect(content().string(containsString("Outdoor Store")));
        mvc.perform(get("/products").param("name", "Green Shirt"))
                .andExpect(model().attribute("products", hasSize(0)));
        mvc.perform(get("/products").param("category", "clothes"))
                .andExpect(model().attribute("products", hasSize(0)));
    }

    @Test
    void customerSearchIsLinkedFromHomeAndMerchantDashboard() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/products"));
        mvc.perform(get("/merchant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/products\"")));
    }
}
