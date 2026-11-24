
package com.orderflow.product.specification;

import com.orderflow.product.entity.Product;
import com.orderflow.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductSpecificationIntegrationTest {

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

    @Test
    void shouldFindProductsByPartialNameIgnoringCase() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasName("samsung");

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder(
                        "SAMSUNG-001",
                        "SAMSUNG-002"
                );
    }

    @Test
    void shouldFindProductsByNameAfterTrimmingWhitespace() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasName("  SAMSUNG  ");

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFindProductByExactSkuIgnoringCase() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasSku("samsung-001");

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getSku())
                .isEqualTo("SAMSUNG-001");
    }

    @Test
    void shouldFindProductBySkuAfterTrimmingWhitespace() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasSku("  SAMSUNG-001  ");

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindProductsWithPriceGreaterThanOrEqualToMinimum() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasMinPrice(
                        new BigDecimal("90000.00")
                );

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(2);

        assertThat(result)
                .allSatisfy(product ->
                        assertThat(product.getPrice())
                                .isGreaterThanOrEqualTo(
                                        new BigDecimal("90000.00")
                                )
                );
    }

    @Test
    void shouldFindProductsWithPriceLessThanOrEqualToMaximum() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasMaxPrice(
                        new BigDecimal("80000.00")
                );

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(2);

        assertThat(result)
                .allSatisfy(product ->
                        assertThat(product.getPrice())
                                .isLessThanOrEqualTo(
                                        new BigDecimal("80000.00")
                                )
                );
    }

    @Test
    void shouldFindActiveProducts() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.isActive(true);

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(3);

        assertThat(result)
                .allMatch(product ->
                        Boolean.TRUE.equals(product.getActive())
                );
    }

    @Test
    void shouldFindInactiveProducts() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.isActive(false);

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getSku())
                .isEqualTo("ONEPLUS-001");
    }

    @Test
    void shouldCombineMultipleSpecificationsUsingAnd() {

        insertTestProducts();

        Specification<Product> specification =
                ProductSpecification.hasName("samsung")
                        .and(ProductSpecification.hasMinPrice(
                                new BigDecimal("100000.00")))
                        .and(ProductSpecification.isActive(true));

        List<Product> result =
                productRepository.findAll(specification);

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getSku())
                .isEqualTo("SAMSUNG-002");
    }
}
