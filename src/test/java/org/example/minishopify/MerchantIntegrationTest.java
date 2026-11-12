package org.example.minishopify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MerchantIntegrationTest {
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

    private MockMultipartFile picture() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("image", "shirt.png", "image/png", bytes.toByteArray());
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

    @Test
    void productAndPictureAreSavedAndDisplayedOnlyInTheirShop() throws Exception {
        Shop first = shop("First shop");
        Shop second = shop("Second shop");
        MockMultipartFile image = picture();
        mvc.perform(multipart("/merchant/shops/" + first.getId() + "/products")
                        .file(image).param("name", "Green shirt")
                        .param("description", "Cotton shirt")
                        .param("price", "19.95").param("inventoryQuantity", "8"))
                .andExpect(redirectedUrl("/merchant/shops/" + first.getId()));
        var saved = products.findAll().get(0);
        assertEquals(8, saved.getInventoryQuantity());
        assertEquals("19.95", saved.getPrice().toPlainString());
        mvc.perform(get("/merchant/shops/" + first.getId()))
                .andExpect(content().string(containsString("Green shirt")));
        assertTrue(products.findByShopIdOrderByIdAsc(second.getId()).isEmpty());
        mvc.perform(get("/products/" + saved.getId() + "/image"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(image.getBytes()));
    }

    @Test
    void negativeInventoryIsRejected() throws Exception {
        Long id = shop("Shop").getId();
        mvc.perform(multipart("/merchant/shops/" + id + "/products")
                        .file(picture()).param("name", "Shirt")
                        .param("description", "Cotton").param("price", "10.00")
                        .param("inventoryQuantity", "-1"))
                .andExpect(model().attributeHasFieldErrors("productForm", "inventoryQuantity"));
        assertEquals(0, products.count());
    }

    @Test
    void invalidPriceIsRejected() throws Exception {
        Long id = shop("Shop").getId();
        mvc.perform(multipart("/merchant/shops/" + id + "/products")
                        .file(picture()).param("name", "Shirt")
                        .param("description", "Cotton").param("price", "0")
                        .param("inventoryQuantity", "3"))
                .andExpect(model().attributeHasFieldErrors("productForm", "price"));
        assertEquals(0, products.count());
    }

    @Test
    void fakeImageIsRejected() throws Exception {
        Long id = shop("Shop").getId();
        var fake = new MockMultipartFile("image", "fake.png", "image/png", "not a picture".getBytes());
        mvc.perform(multipart("/merchant/shops/" + id + "/products")
                        .file(fake).param("name", "Shirt")
                        .param("description", "Cotton").param("price", "10")
                        .param("inventoryQuantity", "3"))
                .andExpect(model().attributeHasFieldErrors("productForm", "image"));
        assertEquals(0, products.count());
    }

    @Test
    void missingImageIsRejected() throws Exception {
        Long id = shop("Shop").getId();
        mvc.perform(multipart("/merchant/shops/" + id + "/products")
                        .param("name", "Shirt").param("description", "Cotton")
                        .param("price", "10").param("inventoryQuantity", "3"))
                .andExpect(model().attributeHasFieldErrors("productForm", "image"));
        assertEquals(0, products.count());
    }

    @Test
    void missingResourcesReturn404() throws Exception {
        mvc.perform(get("/merchant/shops/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/products/999999/image")).andExpect(status().isNotFound());
    }
}
