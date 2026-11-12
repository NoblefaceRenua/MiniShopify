package org.example.minishopify.model;

import jakarta.persistence.*;

@Entity
@Table(name = "shops")
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false, length = 250)
    private String tags;

    protected Shop() {}

    public Shop(String name, String description, String tags) {
        this.name = name;
        this.description = description;
        this.tags = tags;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTags() { return tags; }
}
