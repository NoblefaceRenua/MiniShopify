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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductEditDeleteTest {
    @Autowired MockMvc mvc;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;

    private static final byte[] PNG = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private Shop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        products.deleteAll();
        shops.deleteAll();
        shop = shops.save(new Shop("Book Nook", "Used books", "books"));
        product = products.save(new Product("Novel", "A good read",
                new BigDecimal("12.50"), 3, PNG, "image/png", shop));
    }

    @Test
    void editPageIsPrefilled() throws Exception {
        mvc.perform(get("/merchant/shops/" + shop.getId() + "/products/" + product.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Novel\"")))
                .andExpect(content().string(containsString("12.50")));
    }

    @Test
    void validEditUpdatesProductAndKeepsPicture() throws Exception {
        mvc.perform(multipart("/merchant/shops/" + shop.getId() + "/products/" + product.getId())
                        .file(new MockMultipartFile("image", new byte[0]))   // no new picture
                        .param("name", "Novel (2nd ed.)")
                        .param("description", "Updated")
                        .param("price", "15.00")
                        .param("inventoryQuantity", "7"))
                .andExpect(status().is3xxRedirection());

        Product updated = products.findById(product.getId()).orElseThrow();
        assertEquals("Novel (2nd ed.)", updated.getName());
        assertEquals(0, new BigDecimal("15.00").compareTo(updated.getPrice()));
        assertEquals(7, updated.getInventoryQuantity());
        assertArrayEquals(PNG, updated.getImageData());   // old picture kept
    }

    @Test
    void invalidEditShowsErrorAndChangesNothing() throws Exception {
        mvc.perform(multipart("/merchant/shops/" + shop.getId() + "/products/" + product.getId())
                        .param("name", "Novel")
                        .param("description", "A good read")
                        .param("price", "0")
                        .param("inventoryQuantity", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Price must be at least 0.01.")));
        assertEquals(0, new BigDecimal("12.50")
                .compareTo(products.findById(product.getId()).orElseThrow().getPrice()));
    }

    @Test
    void deleteRemovesProduct() throws Exception {
        mvc.perform(post("/merchant/shops/" + shop.getId() + "/products/" + product.getId() + "/delete"))
                .andExpect(status().is3xxRedirection());
        assertFalse(products.existsById(product.getId()));
    }

    @Test
    void getOnDeleteUrlIsRejected() throws Exception {
        mvc.perform(get("/merchant/shops/" + shop.getId() + "/products/" + product.getId() + "/delete"))
                .andExpect(status().isMethodNotAllowed());
        assertTrue(products.existsById(product.getId()));
    }

    @Test
    void cannotEditOrDeleteProductThroughAnotherShop() throws Exception {
        Shop other = shops.save(new Shop("Shoe Box", "Shoes", "shoes"));
        mvc.perform(get("/merchant/shops/" + other.getId() + "/products/" + product.getId() + "/edit"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/merchant/shops/" + other.getId() + "/products/" + product.getId() + "/delete"))
                .andExpect(status().isNotFound());
        assertTrue(products.existsById(product.getId()));
    }
}