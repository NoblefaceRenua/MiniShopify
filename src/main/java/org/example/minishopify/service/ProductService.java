package org.example.minishopify.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.example.minishopify.model.Product;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.web.ProductForm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository products;
    private final ShopService shops;

    public ProductService(ProductRepository products, ShopService shops) {
        this.products = products;
        this.shops = shops;
    }

    public List<Product> listProducts(Long shopId) {
        return products.findByShopIdOrderByIdAsc(shopId);
    }

    public Product getProduct(Long id) {
        return products.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product addProduct(Long shopId, ProductForm form) {
        Shop shop = shops.getShop(shopId);
        MultipartFile file = form.getImage();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a PNG or JPEG picture.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Picture must be 5 MB or smaller.");
        }
        try {
            byte[] bytes = file.getBytes();
            // Check the actual file content instead of trusting its filename.
            String type = imageType(bytes);
            if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new IllegalArgumentException("The picture could not be read.");
            }
            return products.save(new Product(form.getName().trim(),
                    form.getDescription().trim(), form.getPrice(),
                    form.getInventoryQuantity(), bytes, type, shop));
        } catch (IOException ex) {
            throw new IllegalArgumentException("The picture could not be read.");
        }
    }

    private String imageType(byte[] data) {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (data.length >= 8 && java.util.Arrays.equals(png,
                java.util.Arrays.copyOf(data, 8))) return "image/png";
        if (data.length >= 3 && (data[0] & 255) == 255 &&
                (data[1] & 255) == 216 && (data[2] & 255) == 255) return "image/jpeg";
        throw new IllegalArgumentException("Only PNG and JPEG pictures are supported.");
    }
}
