
package com.orderflow.product.specification;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.orderflow.product.entity.Product;

public class ProductSpecification {

    private ProductSpecification() {
        // Utility class - prevent object creation
    }

    // Filter by product name (partial, case-insensitive)
    public static Specification<Product> hasName(String name) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + name.trim().toLowerCase(Locale.ROOT) + "%"
                );
    }

    // Filter by SKU (exact, case-insensitive)
    public static Specification<Product> hasSku(String sku) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("sku")),
                        sku.trim().toLowerCase(Locale.ROOT)
                );
    }

    // Filter by minimum price
    public static Specification<Product> hasMinPrice(
            BigDecimal minPrice) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        minPrice
                );
    }

    // Filter by maximum price
    public static Specification<Product> hasMaxPrice(
            BigDecimal maxPrice) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        maxPrice
                );
    }

    // Filter by active status
    public static Specification<Product> isActive(Boolean active) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("active"),
                        active
                );
    }
}
