package org.example.minishopify.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.minishopify.model.Product;
import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ProductRepository;
import org.example.minishopify.web.ProductForm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class ProductService {
    public static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000;

    private final ProductRepository products;
    private final ShopService shops;

    public ProductService(ProductRepository products, ShopService shops) {
        this.products = products;
        this.shops = shops;
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts(Long shopId) {
        shops.getShop(shopId);
        return products.findByShopIdOrderByIdAsc(shopId);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return products.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product addProduct(Long shopId, @NotNull @Valid ProductForm form) {
        Shop shop = shops.getShop(shopId);
        MultipartFile file = form.getImage();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a PNG or JPEG picture.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Picture must be 5 MB or smaller.");
        }
        try {
            byte[] bytes = file.getBytes();
            String contentType = imageType(bytes);
            return products.save(new Product(form.getName().trim(), form.getDescription().trim(),
                    form.getPrice(), form.getInventoryQuantity(), bytes, contentType, shop));
        } catch (IOException ex) {
            throw new IllegalArgumentException("The picture could not be read. Choose a valid PNG or JPEG.", ex);
        }
    }

    private String imageType(byte[] bytes) throws IOException {
        // Detect and decode the content; never trust the supplied filename or MIME type.
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only valid PNG and JPEG pictures are supported.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                String type;
                if ("PNG".equalsIgnoreCase(format)) {
                    type = "image/png";
                } else if ("JPEG".equalsIgnoreCase(format)) {
                    type = "image/jpeg";
                } else {
                    throw new IllegalArgumentException("Only PNG and JPEG pictures are supported.");
                }
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Picture must contain 20 million pixels or fewer.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("The picture could not be read.");
                }
                return type;
            } finally {
                reader.dispose();
            }
        }
    }
}
