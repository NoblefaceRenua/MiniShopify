package org.example.minishopify.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

public class ProductForm {
    @NotBlank(message = "Enter a product name.")
    @Size(max = 100)
    private String name;
    @NotBlank(message = "Enter a description.")
    @Size(max = 1000)
    private String description;
    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01.")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;
    @NotNull(message = "Enter inventory quantity.")
    @Min(value = 0, message = "Inventory cannot be negative.")
    private Integer inventoryQuantity;
    private MultipartFile image;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getInventoryQuantity() { return inventoryQuantity; }
    public void setInventoryQuantity(Integer value) { this.inventoryQuantity = value; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
