# Teammate integration contract

This ZIP assumes the author of this contribution owns merchant shop/product
creation, while the teammate owns customer discovery and browsing. Confirm that
split with your team. If the team has an existing project, merge code into its
package structure rather than replacing the whole project.

## Shared code

Package root: `org.example.minishopify`.

- `model.Shop`: id, name, description, tags (comma-separated lowercase string).
- `model.Product`: id, name, description, BigDecimal price, inventoryQuantity,
  imageData, imageContentType, shop.
- `repository.ShopRepository`: JpaRepository<Shop, Long>.
- `repository.ProductRepository`: JpaRepository<Product, Long> with
  `List<Product> findByShopIdOrderByIdAsc(Long shopId)`.
- `service.ShopService.getShop(id)` returns a shop or HTTP 404.
- `service.ProductService.listProducts(shopId)` returns products for that shop.
- `GET /products/{id}/image` returns picture bytes with PNG/JPEG content type.

Reuse these entities. Do not create a second Shop/Product entity or another
database. Update diagrams and tests if you change their fields or relationships.
The product-to-shop relationship is unidirectional; there is no `shop.getProducts()`.
The inventory field stores the available quantity, not a SKU or product identifier.

## Existing merchant routes (HTML forms)

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Temporary redirect to merchant dashboard |
| GET | `/merchant` | List all shops for prototype management |
| GET | `/merchant/shops/new` | New shop form |
| POST | `/merchant/shops` | Save shop: name, description, tags |
| GET | `/merchant/shops/{id}` | Merchant catalog preview |
| GET | `/merchant/shops/{id}/products/new` | Product form |
| POST | `/merchant/shops/{id}/products` | Multipart fields: name, description, price, inventoryQuantity, image |
| GET | `/products/{id}/image` | Product picture |

These POST routes accept form fields, not JSON. A successful save redirects to
the merchant shop page. Validation errors redisplay the form with messages.

## Customer work

Recommended customer routes: `GET /shops?name=...&tag=...` for search and
`GET /shops/{id}` for a customer-facing shop catalog. These do not collide with
the merchant routes. Replace HomeController's `/` redirect with the customer home
page when ready, and keep a link to `/merchant` during this prototype.

You can add a method to ShopRepository for case-insensitive name search:

```java
List<Shop> findByNameContainingIgnoreCase(String name);
```

For exact tag matching with the current string storage, split `shop.getTags()`
on commas and compare trimmed values ignoring case. Avoid using substring
matching for exact tags (for example, `art` should not match `party`). For a larger
catalog, consider a shared tag table / element collection as a coordinated change.

When displaying products, reuse `/products/{id}/image` as the image source, and
use `ProductRepository.findByShopIdOrderByIdAsc(id)` for catalog data. Avoid
returning the entities directly as JSON; image bytes and lazy JPA relationships
should not become part of a catalog JSON response.

## Purchase logic for a future sprint

No checkout code exists here. Validate quantities server-side, combine duplicate
cart entries, reject unavailable stock, and update stock/order records in one
transaction. Introduce locking or an optimistic version field before concurrent
checkout so two buyers cannot both purchase the same remaining item. Do not
subtract inventory only in browser JavaScript. Add integration tests for successful
checkout, excessive quantities, duplicate cart lines, and concurrent purchases.

## Merge checklist

1. Match the base package with the team's application scanning root.
2. Add the five Spring starters, H2 runtime, and test dependency from this POM
   only if missing; retain the team's compatible Spring Boot version.
3. Agree on the shared entity field names before writing customer templates.
4. Integrate properties carefully; keep the test database isolated.
5. Run all tests and verify the packaged JAR.
6. Open a feature PR and obtain two teammate reviews before merging.
7. Deploy the integrated main branch to the team's Mini-Shopify App Service.

No customer code, cloud resource, GitHub issue, PR, or deployment is created by
extracting this project. Those must be added through your team's repository.
