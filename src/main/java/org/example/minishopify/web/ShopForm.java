package org.example.minishopify.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShopForm {
    @NotBlank(message = "Enter a shop name.")
    @Size(max = 100)
    private String name;
    @NotBlank(message = "Enter a description.")
    @Size(max = 1000)
    private String description;
    @NotBlank(message = "Enter at least one category or tag.")
    @Size(max = 250)
    private String tags;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
