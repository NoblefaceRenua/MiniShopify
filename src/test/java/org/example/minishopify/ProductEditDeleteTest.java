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
        mvc.perform(multipart("/merchant/shops/" + other.getId() + "/products/" + product.getId())
                        .param("name", "Unauthorized change")
                        .param("description", "Changed")
                        .param("price", "1.00")
                        .param("inventoryQuantity", "0"))
                .andExpect(status().isNotFound());
        assertTrue(products.existsById(product.getId()));
        assertEquals("Novel", products.findById(product.getId()).orElseThrow().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    void validReplacementPictureIsSavedAndServed(String format) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
        MockMultipartFile image = new MockMultipartFile(
                "image", "replacement." + format, "image/" + format, bytes.toByteArray());

        mvc.perform(multipart("/merchant/shops/" + shop.getId() + "/products/" + product.getId())
                        .file(image)
                        .param("name", " Updated novel ")
                        .param("description", " New description ")
                        .param("price", "15.00")
                        .param("inventoryQuantity", "7"))
                .andExpect(redirectedUrl("/merchant/shops/" + shop.getId()));

        Product updated = products.findById(product.getId()).orElseThrow();
        assertEquals("Updated novel", updated.getName());
        assertEquals("New description", updated.getDescription());
        assertArrayEquals(image.getBytes(), updated.getImageData());
        mvc.perform(get("/products/" + product.getId() + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/" + format))
                .andExpect(content().bytes(image.getBytes()));
    }

    @Test
    void invalidReplacementPictureLeavesProductUnchanged() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "fake.png", "image/png", "not a picture".getBytes());
        assertRejectedReplacement(image);
    }

    @Test
    void oversizedReplacementPictureLeavesProductUnchanged() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        assertRejectedReplacement(image);
    }

    private void assertRejectedReplacement(MockMultipartFile image) throws Exception {
        mvc.perform(multipart("/merchant/shops/" + shop.getId() + "/products/" + product.getId())
                        .file(image)
                        .param("name", "Changed name")
                        .param("description", "Changed description")
                        .param("price", "99.00")
                        .param("inventoryQuantity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/product-edit"))
                .andExpect(model().attributeHasFieldErrors("productForm", "image"));

        Product unchanged = products.findById(product.getId()).orElseThrow();
        assertEquals("Novel", unchanged.getName());
        assertEquals("A good read", unchanged.getDescription());
        assertEquals(0, new BigDecimal("12.50").compareTo(unchanged.getPrice()));
        assertEquals(3, unchanged.getInventoryQuantity());
        assertArrayEquals(PNG, unchanged.getImageData());
        assertEquals("image/png", unchanged.getImageContentType());
    }
}
