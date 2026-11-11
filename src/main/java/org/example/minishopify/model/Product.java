package org.example.minishopify.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private int inventoryQuantity;
    @Lob
    @Column(nullable = false)
    private byte[] imageData;
    @Column(nullable = false, length = 30)
    private String imageContentType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    protected Product() {}

    public Product(String name, String description, BigDecimal price,
                   int inventoryQuantity, byte[] imageData,
                   String imageContentType, Shop shop) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.inventoryQuantity = inventoryQuantity;
        this.imageData = imageData;
        this.imageContentType = imageContentType;
        this.shop = shop;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getInventoryQuantity() { return inventoryQuantity; }
    public byte[] getImageData() { return imageData; }
    public String getImageContentType() { return imageContentType; }
    public Shop getShop() { return shop; }
}
