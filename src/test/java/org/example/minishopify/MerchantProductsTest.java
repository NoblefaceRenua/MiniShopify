package org.example.minishopify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import jakarta.validation.ConstraintViolationException;
import org.example.minishopify.model.Product;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.repository.ShopRepository;
import org.example.minishopify.service.ProductService;
import org.example.minishopify.web.ProductForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantProductsTest {
    @Autowired MockMvc mvc;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired ProductService service;

    private Shop firstShop;
    private Shop secondShop;

    @BeforeEach
    void createShops() {
        products.deleteAll();
        shops.deleteAll();
        firstShop = shops.save(new Shop("Art shop", "Handmade art", "art"));
        secondShop = shops.save(new Shop("Book shop", "Used books", "books"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    void uploadedProductAndItsPictureAppearOnlyInTheChosenShop(String format) throws Exception {
        byte[] bytes = picture(format);
        // Deliberately misleading metadata: the decoded content determines the response type.
        MockMultipartFile image = new MockMultipartFile("image", "untrusted.txt", "text/plain", bytes);
        mvc.perform(productRequest(firstShop.getId(), image, "19.95", "8"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/merchant/shops/" + firstShop.getId()));

        List<Product> saved = products.findByShopIdOrderByIdAsc(firstShop.getId());
        assertEquals(1, saved.size());
        Product product = saved.get(0);
        assertEquals("Painted mug", product.getName());
        assertEquals("Hand painted ceramic", product.getDescription());
        assertEquals(new BigDecimal("19.95"), product.getPrice());
        assertEquals(8, product.getInventoryQuantity());
        assertEquals(firstShop.getId(), product.getShop().getId());
        assertArrayEquals(bytes, product.getImageData());
        assertTrue(products.findByShopIdOrderByIdAsc(secondShop.getId()).isEmpty());

        mvc.perform(get("/merchant/shops/{id}", firstShop.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Painted mug")))
                .andExpect(content().string(containsString("Hand painted ceramic")))
                .andExpect(content().string(containsString("CAD $19.95")))
                .andExpect(content().string(containsString("Stock: 8")))
                .andExpect(content().string(containsString("src=\"/products/" + product.getId() + "/image\"")));
        mvc.perform(get("/merchant/shops/{id}", secondShop.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This shop has no products yet.")))
                .andExpect(content().string(not(containsString("Painted mug"))))
                .andExpect(content().string(not(containsString("/products/" + product.getId() + "/image"))));
        mvc.perform(get("/products/{id}/image", product.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/" + format))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void productFormTargetsTheChosenShopAndUsesMultipartEncoding() throws Exception {
        mvc.perform(get("/merchant/shops/{id}/products/new", firstShop.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/product-form"))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("action=\"/merchant/shops/" + firstShop.getId() + "/products\"")))
                .andExpect(content().string(containsString("Add a product to Art shop")));
    }

    @Test
    void missingShopsAndProductsReturn404AndNeverSaveAProduct() throws Exception {
        long missingId = Long.MAX_VALUE;
        mvc.perform(get("/merchant/shops/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(get("/merchant/shops/{id}/products/new", missingId)).andExpect(status().isNotFound());
        mvc.perform(productRequest(missingId, upload("png"), "19.95", "8"))
                .andExpect(status().isNotFound());
        mvc.perform(productRequest(missingId, upload("png"), "0", "-1"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/products/{id}/image", missingId)).andExpect(status().isNotFound());
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.listProducts(missingId));
        assertEquals(404, failure.getStatusCode().value());
        assertEquals(0, products.count());
    }

    @ParameterizedTest
    @MethodSource("invalidNumbers")
    void rejectsInvalidPricesAndStock(String price, String stock, String field) throws Exception {
        mvc.perform(productRequest(firstShop.getId(), upload("png"), price, stock))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", field))
                .andExpect(content().string(containsString("Select your picture again before saving.")));
        assertEquals(0, products.count());
    }

    static Stream<Arguments> invalidNumbers() {
        return Stream.of(
                Arguments.of("0", "8", "price"),
                Arguments.of("-1.00", "8", "price"),
                Arguments.of("", "8", "price"),
                Arguments.of("abc", "8", "price"),
                Arguments.of("0.001", "8", "price"),
                Arguments.of("10000000000.00", "8", "price"),
                Arguments.of("19.95", "-1", "inventoryQuantity"),
                Arguments.of("19.95", "", "inventoryQuantity"),
                Arguments.of("19.95", "1.5", "inventoryQuantity"),
                Arguments.of("19.95", "2147483648", "inventoryQuantity"));
    }

    @Test
    void acceptsOneCentAndZeroStock() throws Exception {
        mvc.perform(productRequest(firstShop.getId(), upload("jpeg"), "0.01", "0"))
                .andExpect(status().is3xxRedirection());
        Product product = products.findAll().get(0);
        assertEquals(new BigDecimal("0.01"), product.getPrice());
        assertEquals(0, product.getInventoryQuantity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "text", "gif", "truncated", "oversized", "dimensions"})
    void rejectsInvalidOrOversizedPicturesWithoutSaving(String kind) throws Exception {
        byte[] bytes = switch (kind) {
            case "empty" -> new byte[0];
            case "text" -> "This is not an image".getBytes(StandardCharsets.UTF_8);
            case "gif" -> picture("gif");
            case "truncated" -> new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
            case "oversized" -> new byte[(int) ProductService.MAX_IMAGE_BYTES + 1];
            case "dimensions" -> oversizedDimensions();
            default -> throw new IllegalArgumentException(kind);
        };
        MockMultipartFile image = new MockMultipartFile("image", "looks-valid.png", "image/png", bytes);
        mvc.perform(productRequest(firstShop.getId(), image, "19.95", "8"))
                .andExpect(status().isOk())
                .andExpect(view().name("merchant/product-form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "image"));
        assertEquals(0, products.count());
    }

    @Test
    void rejectsMissingPictureAndBlankDetails() throws Exception {
        mvc.perform(multipart("/merchant/shops/{id}/products", firstShop.getId())
                        .param("name", " ").param("description", " ")
                        .param("price", "19.95").param("inventoryQuantity", "8"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("productForm", "name", "description", "image"));
        assertEquals(0, products.count());
    }

    @ParameterizedTest
    @MethodSource("invalidServiceNumbers")
    void serviceValidationAlsoProtectsCallersOutsideTheController(String price, int stock) throws Exception {
        ProductForm form = validForm();
        form.setPrice(new BigDecimal(price));
        form.setInventoryQuantity(stock);
        assertThrows(ConstraintViolationException.class, () -> service.addProduct(firstShop.getId(), form));
        assertEquals(0, products.count());
    }

    static Stream<Arguments> invalidServiceNumbers() {
        return Stream.of(Arguments.of("0", 8), Arguments.of("-1.00", 8),
                Arguments.of("0.001", 8), Arguments.of("10000000000.00", 8),
                Arguments.of("19.95", -1));
    }

    @Test
    void acceptsAnImageExactlyAtTheFiveMegabyteLimit() throws Exception {
        byte[] bytes = java.util.Arrays.copyOf(picture("png"), (int) ProductService.MAX_IMAGE_BYTES);
        mvc.perform(productRequest(firstShop.getId(),
                        new MockMultipartFile("image", "large.png", "image/png", bytes), "19.95", "8"))
                .andExpect(status().is3xxRedirection());
        assertArrayEquals(bytes, products.findAll().get(0).getImageData());
    }

    private ProductForm validForm() throws Exception {
        ProductForm form = new ProductForm();
        form.setName("Painted mug");
        form.setDescription("Hand painted ceramic");
        form.setPrice(new BigDecimal("19.95"));
        form.setInventoryQuantity(8);
        form.setImage(upload("png"));
        return form;
    }

    private MockHttpServletRequestBuilder productRequest(Long shopId, MockMultipartFile image,
                                                        String price, String stock) {
        return multipart("/merchant/shops/{id}/products", shopId).file(image)
                .param("name", " Painted mug ").param("description", " Hand painted ceramic ")
                .param("price", price).param("inventoryQuantity", stock);
    }

    static MockMultipartFile upload(String format) throws Exception {
        return new MockMultipartFile("image", "picture." + format, "image/" + format, picture(format));
    }

    static byte[] picture(String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output));
        return output.toByteArray();
    }

    private byte[] oversizedDimensions() throws Exception {
        byte[] png = picture("png");
        // PNG IHDR width and height: reject before allocating the decoded image.
        java.nio.ByteBuffer.wrap(png).putInt(16, 100_000).putInt(20, 100_000);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(png, 12, 17);
        java.nio.ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
        return png;
    }
}
