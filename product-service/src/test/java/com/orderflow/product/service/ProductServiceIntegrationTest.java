
package com.orderflow.product.service;

import com.orderflow.product.dto.PageResponse;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.entity.Product;
import com.orderflow.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ProductService.class)
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product createProduct(
            String name,
            String sku,
            String price,
            Integer quantity,
            Boolean active) {

        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(new BigDecimal(price));
        product.setQuantity(quantity);
        product.setDescription(name + " description");
        product.setActive(active);

        return product;
    }

    private void insertTestProducts() {

        productRepository.saveAll(List.of(
                createProduct(
                        "Samsung Galaxy S25",
                        "SAMSUNG-001",
                        "80000.00",
                        10,
                        true
                ),
                createProduct(
                        "Samsung Galaxy S25 Ultra",
                        "SAMSUNG-002",
                        "120000.00",
                        5,
                        true
                ),
                createProduct(
                        "Apple iPhone 17",
                        "APPLE-001",
                        "90000.00",
                        8,
                        true
                ),
                createProduct(
                        "OnePlus 13",
                        "ONEPLUS-001",
                        "65000.00",
                        12,
                        false
                )
        ));

        productRepository.flush();
    }

    // =========================================================
    // TEST 1: COMBINED FILTERING THROUGH PRODUCT SERVICE
    // =========================================================

    @Test
    void shouldApplyMultipleFiltersThroughProductService() {

        // Arrange
        insertTestProducts();

        // Act
        PageResponse<ProductResponse> response =
                productService.getAllProducts(
                        0,
                        10,
                        "price",
                        "DESC",
                        "samsung",
                        null,
                        new BigDecimal("100000.00"),
                        null,
                        true
                );

        // Assert
        assertThat(response.content()).hasSize(1);

        assertThat(response.content())
                .extracting(ProductResponse::sku)
                .containsExactly("SAMSUNG-002");

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.pageNumber()).isZero();
        assertThat(response.pageSize()).isEqualTo(10);
    }

    // =========================================================
    // TEST 2: FILTERING + PAGINATION + SORTING
    // =========================================================

    @Test
    void shouldApplyFilteringWithPaginationAndSorting() {

        // Arrange
        insertTestProducts();

        // Act - First page
        PageResponse<ProductResponse> firstPage =
                productService.getAllProducts(
                        0,
                        1,
                        "price",
                        "DESC",
                        "samsung",
                        null,
                        null,
                        null,
                        true
                );

        // Act - Second page
        PageResponse<ProductResponse> secondPage =
                productService.getAllProducts(
                        1,
                        1,
                        "price",
                        "DESC",
                        "samsung",
                        null,
                        null,
                        null,
                        true
                );

        // Assert - First page
        assertThat(firstPage.content()).hasSize(1);

        assertThat(firstPage.content().get(0).sku())
                .isEqualTo("SAMSUNG-002");

        assertThat(firstPage.pageNumber()).isZero();
        assertThat(firstPage.pageSize()).isEqualTo(1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.hasPrevious()).isFalse();

        // Assert - Second page
        assertThat(secondPage.content()).hasSize(1);

        assertThat(secondPage.content().get(0).sku())
                .isEqualTo("SAMSUNG-001");

        assertThat(secondPage.pageNumber()).isEqualTo(1);
        assertThat(secondPage.pageSize()).isEqualTo(1);
        assertThat(secondPage.totalElements()).isEqualTo(2);
        assertThat(secondPage.totalPages()).isEqualTo(2);

        assertThat(secondPage.first()).isFalse();
        assertThat(secondPage.last()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();
    }
}
