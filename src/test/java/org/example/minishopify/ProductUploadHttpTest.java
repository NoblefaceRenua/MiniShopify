package org.example.minishopify;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

// Real HTTP is essential here: MockMvc bypasses the servlet multipart size limits.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:product-upload-http-tests;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ProductUploadHttpTest {
    @LocalServerPort int port;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;

    private Long shopId;

    @BeforeEach
    void createShop() {
        products.deleteAll();
        shops.deleteAll();
        shopId = shops.save(new Shop("Photo shop", "Pictures", "photos")).getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    void validImagesPassThroughTheRealMultipartParser(String format) throws Exception {
        byte[] image = MerchantProductsTest.picture(format);
        HttpResponse<String> response = postImage(image);
        assertEquals(302, response.statusCode());
        assertEquals("http://localhost:" + port + "/merchant/shops/" + shopId,
                URI.create("http://localhost:" + port).resolve(response.headers().firstValue("Location").orElseThrow()).toString());
        assertEquals(1, products.count());
        assertArrayEquals(image, products.findAll().get(0).getImageData());
    }

    @ParameterizedTest
    @ValueSource(ints = {5 * 1024 * 1024 + 1, 6 * 1024 * 1024 + 1})
    void servletRejectsOversizedFilesAndRequestsWithAHelpful413Page(int imageSize) throws Exception {
        HttpResponse<String> response = postImage(new byte[imageSize]);
        assertEquals(413, response.statusCode());
        assertTrue(response.body().contains("Picture must be 5 MB or smaller."));
        assertEquals(0, products.count());
    }

    private HttpResponse<String> postImage(byte[] image) throws Exception {
        String boundary = "MiniShopifyTestBoundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, boundary, "name", "A photograph");
        field(body, boundary, "description", "A framed photograph");
        field(body, boundary, "price", "19.95");
        field(body, boundary, "inventoryQuantity", "8");
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"picture.png\""
                + "\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/merchant/shops/" + shopId + "/products"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void field(ByteArrayOutputStream body, String boundary, String name, String value) throws Exception {
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
}
