# Teammate integration contract

Merchant shop/product management and customer product search share the models,
repositories, and services below. Integrate changes into the existing package
structure rather than replacing the whole project.

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

## Existing routes (HTML forms)

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Redirect to the customer product catalog |
| GET | `/products?name=...&category=...` | Customer product search; filters can be combined |
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

`GET /products` now provides customer browsing and product-name/category search.
The `name` parameter matches literal, partial product names ignoring case. The
`category` parameter matches an exact comma-separated tag on the product's shop,
also ignoring case. Both parameters are trimmed and combined with AND; omitting
both lists all products. Products inherit categories from their shop rather than
having a new category field. The catalog has a link to `/merchant`, and the
merchant dashboard links back to `/products`.

`ProductRepository.findByNameContainingIgnoreCaseOrderByIdAsc` loads each
product's shop with an entity graph so the customer template works with
`spring.jpa.open-in-view=false`. `ProductService.searchProducts` applies the
whole-tag category filter. `ShopService.listCategories` supplies sorted, unique,
lowercase tags for the category dropdown. No database schema or configuration
change is needed.

Possible future routes are `GET /shops?name=...&tag=...` for customer shop search
and `GET /shops/{id}` for a customer shop catalog.

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

Cloud resources, GitHub issues, pull requests, and deployments are managed
separately through your team's repository and cloud account.
