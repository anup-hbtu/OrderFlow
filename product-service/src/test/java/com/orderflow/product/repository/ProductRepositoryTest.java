
package com.orderflow.product.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.orderflow.product.entity.Product;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    // --------------------------------------------------
    // Test data helper
    // --------------------------------------------------

    private Product createProduct(
            String name,
            String sku,
            BigDecimal price,
            Integer quantity) {

        Product product = new Product();

        product.setName(name);
        product.setDescription(name + " description");
        product.setSku(sku);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setActive(true);

        return product;
    }

    // --------------------------------------------------
    // 1. SAVE PRODUCT
    // --------------------------------------------------

    @Test
    void shouldSaveProductSuccessfully() {

        // Arrange
        Product product = createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        );

        // Act
        Product savedProduct = productRepository.saveAndFlush(product);

        // Assert
        assertNotNull(savedProduct.getId());
        assertEquals("iPhone 17", savedProduct.getName());
        assertEquals("IPHONE-001", savedProduct.getSku());
        assertEquals(new BigDecimal("79999.00"), savedProduct.getPrice());
        assertEquals(20, savedProduct.getQuantity());
        assertTrue(savedProduct.getActive());
    }

    // --------------------------------------------------
    // 2. FIND ALL PRODUCTS
    // --------------------------------------------------

    @Test
    void shouldReturnAllProducts() {

        // Arrange
        productRepository.save(createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        ));

        productRepository.save(createProduct(
                "Samsung S25",
                "SAMSUNG-001",
                new BigDecimal("69999.00"),
                15
        ));

        productRepository.flush();

        // Act
        List<Product> products = productRepository.findAll();

        // Assert
        assertEquals(2, products.size());
        assertTrue(products.stream()
                .anyMatch(product -> product.getSku().equals("IPHONE-001")));
        assertTrue(products.stream()
                .anyMatch(product -> product.getSku().equals("SAMSUNG-001")));
    }

    // --------------------------------------------------
    // 3. FIND BY ID - PRODUCT EXISTS
    // --------------------------------------------------

    @Test
    void shouldFindProductById() {

        // Arrange
        Product savedProduct = productRepository.save(
                createProduct(
                        "iPhone 17",
                        "IPHONE-001",
                        new BigDecimal("79999.00"),
                        20
                )
        );

        // Act
        Optional<Product> result =
                productRepository.findById(savedProduct.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals("iPhone 17", result.get().getName());
        assertEquals("IPHONE-001", result.get().getSku());
    }

    // --------------------------------------------------
    // 4. FIND BY ID - PRODUCT DOES NOT EXIST
    // --------------------------------------------------

    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {

        // Act
        Optional<Product> result = productRepository.findById(999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    // --------------------------------------------------
    // 5. EXISTS BY SKU - TRUE
    // --------------------------------------------------

    @Test
    void shouldReturnTrueWhenSkuExists() {

        // Arrange
        productRepository.save(createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        ));

        // Act
        boolean exists = productRepository.existsBySku("IPHONE-001");

        // Assert
        assertTrue(exists);
    }

    // --------------------------------------------------
    // 6. EXISTS BY SKU - FALSE
    // --------------------------------------------------

    @Test
    void shouldReturnFalseWhenSkuDoesNotExist() {

        // Act
        boolean exists = productRepository.existsBySku("UNKNOWN-001");

        // Assert
        assertFalse(exists);
    }

    // --------------------------------------------------
    // 7. EXISTS BY SKU AND ID NOT - TRUE
    // --------------------------------------------------

    @Test
    void shouldReturnTrueWhenAnotherProductOwnsTheSku() {

        // Arrange
        Product firstProduct = productRepository.save(createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        ));

        Product secondProduct = productRepository.save(createProduct(
                "Samsung S25",
                "SAMSUNG-001",
                new BigDecimal("69999.00"),
                15
        ));

        // Act
        boolean exists = productRepository.existsBySkuAndIdNot(
                firstProduct.getSku(),
                secondProduct.getId()
        );

        // Assert
        assertTrue(exists);
    }

    // --------------------------------------------------
    // 8. EXISTS BY SKU AND ID NOT - FALSE
    // --------------------------------------------------

    @Test
    void shouldReturnFalseWhenSkuBelongsToSameProduct() {

        // Arrange
        Product product = productRepository.save(createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        ));

        // Act
        boolean exists = productRepository.existsBySkuAndIdNot(
                product.getSku(),
                product.getId()
        );

        // Assert
        assertFalse(exists);
    }

    // --------------------------------------------------
    // 9. DATABASE UNIQUE SKU CONSTRAINT
    // --------------------------------------------------

    @Test
    void shouldRejectDuplicateSkuAtDatabaseLevel() {

        // Arrange
        productRepository.saveAndFlush(createProduct(
                "iPhone 17",
                "IPHONE-001",
                new BigDecimal("79999.00"),
                20
        ));

        Product duplicateProduct = createProduct(
                "Another iPhone",
                "IPHONE-001",
                new BigDecimal("89999.00"),
                10
        );

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> productRepository.saveAndFlush(duplicateProduct)
        );
    }

    // --------------------------------------------------
    // 10. DELETE PRODUCT
    // --------------------------------------------------

    @Test
    void shouldDeleteProductSuccessfully() {

        // Arrange
        Product savedProduct = productRepository.saveAndFlush(
                createProduct(
                        "iPhone 17",
                        "IPHONE-001",
                        new BigDecimal("79999.00"),
                        20
                )
        );

        Long productId = savedProduct.getId();

        // Act
        productRepository.deleteById(productId);
        productRepository.flush();

        // Assert
        assertFalse(productRepository.existsById(productId));
    }
 // --------------------------------------------------
 // 11. AUDIT TIMESTAMPS ON PRODUCT CREATION
 // --------------------------------------------------

 @Test
 void shouldAutomaticallySetAuditTimestampsWhenProductIsCreated() {

     // Arrange
     Product product = createProduct(
             "iPhone 17",
             "IPHONE-001",
             new BigDecimal("79999.00"),
             20
     );

     // Act
     Product savedProduct = productRepository.saveAndFlush(product);

     // Assert
     assertNotNull(savedProduct.getCreatedAt());
     assertNotNull(savedProduct.getUpdatedAt());

     assertEquals(
             savedProduct.getCreatedAt(),
             savedProduct.getUpdatedAt()
     );
 }
//--------------------------------------------------
//12. AUDIT TIMESTAMPS ON PRODUCT UPDATE
//--------------------------------------------------

@Test
void shouldUpdateUpdatedAtButPreserveCreatedAtWhenProductIsUpdated()
      throws InterruptedException {

  // Arrange
  Product product = createProduct(
          "iPhone 17",
          "IPHONE-001",
          new BigDecimal("79999.00"),
          20
  );

  Product savedProduct = productRepository.saveAndFlush(product);

  OffsetDateTime originalCreatedAt = savedProduct.getCreatedAt();
  OffsetDateTime originalUpdatedAt = savedProduct.getUpdatedAt();

  // Ensure enough time passes to distinguish the timestamps
  Thread.sleep(20);

  // Act
  savedProduct.setPrice(new BigDecimal("89999.00"));

  Product updatedProduct = productRepository.saveAndFlush(savedProduct);

  // Assert
  assertEquals(originalCreatedAt, updatedProduct.getCreatedAt());

  assertTrue(
          updatedProduct.getUpdatedAt().isAfter(originalUpdatedAt),
          "updatedAt should be later than the original timestamp"
  );

  assertEquals(
          new BigDecimal("89999.00"),
          updatedProduct.getPrice()
  );
}
//--------------------------------------------------
//13. AUDIT TIMESTAMPS ON SOFT DELETE
//--------------------------------------------------

@Test
void shouldUpdateUpdatedAtWhenProductIsSoftDeleted()
     throws InterruptedException {

 // Arrange
 Product product = createProduct(
         "iPhone 17",
         "IPHONE-001",
         new BigDecimal("79999.00"),
         20
 );

 Product savedProduct = productRepository.saveAndFlush(product);

 OffsetDateTime originalCreatedAt = savedProduct.getCreatedAt();
 OffsetDateTime originalUpdatedAt = savedProduct.getUpdatedAt();

 Thread.sleep(20);

 // Act
 savedProduct.setActive(false);

 Product deletedProduct = productRepository.saveAndFlush(savedProduct);

 // Assert
 assertFalse(deletedProduct.getActive());

 assertEquals(originalCreatedAt, deletedProduct.getCreatedAt());

 assertTrue(
         deletedProduct.getUpdatedAt().isAfter(originalUpdatedAt),
         "updatedAt should change during soft deletion"
 );
}
}
