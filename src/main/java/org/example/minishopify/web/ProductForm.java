package org.example.minishopify.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ProductForm {
    @NotBlank(message = "Enter a product name.")
    @Size(max = 100, message = "Name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "Enter a description.")
    @Size(max = 1000, message = "Description must be 1000 characters or fewer.")
    private String description;

    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "0.01", message = "Price must be at least $0.01.")
    @Digits(integer = 10, fraction = 2, message = "Enter a price with at most 10 whole digits and 2 decimal places.")
    private BigDecimal price;

    @NotNull(message = "Enter a stock quantity.")
    @Min(value = 0, message = "Stock cannot be negative.")
    private Integer inventoryQuantity;

    @NotNull(message = "Choose a PNG or JPEG picture.")
    private MultipartFile image;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getInventoryQuantity() { return inventoryQuantity; }
    public void setInventoryQuantity(Integer inventoryQuantity) { this.inventoryQuantity = inventoryQuantity; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
