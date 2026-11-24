
package com.orderflow.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.orderflow.product.dto.PageResponse;
import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.entity.Product;
import com.orderflow.product.exception.ProductAlreadyExistsException;
import com.orderflow.product.exception.ProductNotFoundException;
import com.orderflow.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    // =========================================================
    // CREATE PRODUCT TESTS
    // =========================================================

    @Test
    void shouldCreateProductSuccessfully() {

        ProductRequest request = new ProductRequest(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20
        );

        Product savedProduct = createProduct(1L, request, true);

        when(productRepository.existsBySku("IPHONE-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertEquals(1L, response.id());
        assertEquals("iPhone 17", response.name());
        assertEquals("IPHONE-001", response.sku());
        assertEquals(new BigDecimal("79999.00"), response.price());
        assertEquals(20, response.quantity());
        assertTrue(response.active());

        verify(productRepository).existsBySku("IPHONE-001");
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        assertEquals("iPhone 17", capturedProduct.getName());
        assertEquals("Apple smartphone", capturedProduct.getDescription());
        assertEquals("IPHONE-001", capturedProduct.getSku());
        assertEquals(new BigDecimal("79999.00"), capturedProduct.getPrice());
        assertEquals(20, capturedProduct.getQuantity());
        assertTrue(capturedProduct.getActive());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "IPHONE-001",
            "SAMSUNG-001",
            "PIXEL-001"
    })
    void shouldThrowExceptionWhenDuplicateSkuExists(String sku) {

        ProductRequest request = new ProductRequest(
                "Test Smartphone",
                "Test description",
                new BigDecimal("50000.00"),
                sku,
                10
        );

        when(productRepository.existsBySku(sku))
                .thenReturn(true);

        assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository).existsBySku(sku);
        verify(productRepository, never()).save(any(Product.class));
    }

    @ParameterizedTest
    @CsvSource({
            "IPHONE-001, true",
            "SAMSUNG-001, true",
            "PIXEL-001, false"
    })
    void shouldHandleProductCreationBasedOnSkuAvailability(
            String sku, boolean skuExists) {

        ProductRequest request = new ProductRequest(
                "Test Smartphone",
                "Test description",
                new BigDecimal("50000.00"),
                sku,
                10
        );

        when(productRepository.existsBySku(sku))
                .thenReturn(skuExists);

        if (!skuExists) {
            Product savedProduct = createProduct(10L, request, true);

            when(productRepository.save(any(Product.class)))
                    .thenReturn(savedProduct);
        }

        if (skuExists) {

            assertThrows(
                    ProductAlreadyExistsException.class,
                    () -> productService.createProduct(request)
            );

            verify(productRepository, never()).save(any(Product.class));

        } else {

            ProductResponse response = productService.createProduct(request);

            assertEquals(10L, response.id());
            assertEquals(sku, response.sku());

            verify(productRepository).save(any(Product.class));
        }

        verify(productRepository).existsBySku(sku);
    }

    static Stream<Arguments> productRequests() {

        return Stream.of(
                Arguments.of(new ProductRequest(
                        "iPhone 17",
                        "Apple smartphone",
                        new BigDecimal("79999.00"),
                        "IPHONE-001",
                        20
                )),
                Arguments.of(new ProductRequest(
                        "Samsung S25",
                        "Samsung smartphone",
                        new BigDecimal("69999.00"),
                        "SAMSUNG-001",
                        15
                )),
                Arguments.of(new ProductRequest(
                        "Pixel 10",
                        "Google smartphone",
                        new BigDecimal("59999.00"),
                        "PIXEL-001",
                        10
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("productRequests")
    void shouldCreateDifferentProductsSuccessfully(ProductRequest request) {

        when(productRepository.existsBySku(request.sku()))
                .thenReturn(false);

        Product savedProduct = createProduct(100L, request, true);

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertEquals(request.name(), response.name());
        assertEquals(request.sku(), response.sku());
        assertEquals(request.price(), response.price());
        assertEquals(request.quantity(), response.quantity());

        verify(productRepository).existsBySku(request.sku());
        verify(productRepository).save(any(Product.class));
    }

    // =========================================================
    // GET ALL PRODUCTS - PAGINATION TESTS
    // =========================================================

    @Test
    void shouldReturnAllProductsSuccessfully() {

        Product product1 = createProduct(
                1L,
                new ProductRequest(
                        "iPhone 17",
                        "Apple smartphone",
                        new BigDecimal("79999.00"),
                        "IPHONE-001",
                        20
                ),
                true
        );

        Product product2 = createProduct(
                2L,
                new ProductRequest(
                        "Samsung S25",
                        "Samsung smartphone",
                        new BigDecimal("69999.00"),
                        "SAMSUNG-001",
                        15
                ),
                true
        );

        Page<Product> productPage = new PageImpl<>(
                List.of(product1, product2),
                PageRequest.of(0, 10),
                2
        );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(productPage);

        PageResponse<ProductResponse> response =
                productService.getAllProducts(0, 10, "id", "ASC");

        assertEquals(2, response.content().size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());

        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());

        assertTrue(response.first());
        assertTrue(response.last());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        assertEquals("iPhone 17", response.content().get(0).name());
        assertEquals("IPHONE-001", response.content().get(0).sku());

        assertEquals("Samsung S25", response.content().get(1).name());
        assertEquals("SAMSUNG-001", response.content().get(1).sku());

        verify(productRepository).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnEmptyPageWhenNoProductsExist() {

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty(PageRequest.of(0, 10)));

        PageResponse<ProductResponse> response =
                productService.getAllProducts(0, 10, "id", "ASC");

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());

        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());

        assertTrue(response.first());
        assertTrue(response.last());
        assertFalse(response.hasNext());
        assertFalse(response.hasPrevious());

        verify(productRepository).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void shouldReturnCorrectPaginationMetadataForMultiplePages() {

        Product product = createProduct(
                1L,
                new ProductRequest(
                        "iPhone 17",
                        "Apple smartphone",
                        new BigDecimal("79999.00"),
                        "IPHONE-001",
                        20
                ),
                true
        );

        Page<Product> productPage = new PageImpl<>(
                List.of(product),
                PageRequest.of(1, 2),
                5
        );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(productPage);

        PageResponse<ProductResponse> response =
                productService.getAllProducts(1, 2, "id", "ASC");

        assertEquals(1, response.pageNumber());
        assertEquals(2, response.pageSize());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());

        assertFalse(response.first());
        assertFalse(response.last());
        assertTrue(response.hasNext());
        assertTrue(response.hasPrevious());
    }

    // =========================================================
    // PAGINATION VALIDATION TESTS
    // =========================================================

    @Test
    void shouldThrowExceptionWhenPageNumberIsNegative() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(-1, 10, "id", "ASC")
        );

        verify(productRepository, never()).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101, 500})
    void shouldThrowExceptionWhenPageSizeIsInvalid(int size) {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(0, size, "id", "ASC")
        );

        verify(productRepository, never()).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenSortFieldIsInvalid() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        0, 10, "unknown", "ASC"
                )
        );

        verify(productRepository, never()).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenSortDirectionIsInvalid() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getAllProducts(
                        0, 10, "id", "INVALID"
                )
        );

        verify(productRepository, never()).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    // =========================================================
    // GET PRODUCT BY ID TESTS
    // =========================================================

    @Test
    void shouldReturnProductWhenIdExists() {

        ProductRequest request = new ProductRequest(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20
        );

        Product product = createProduct(1L, request, true);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertEquals(1L, response.id());
        assertEquals("iPhone 17", response.name());
        assertEquals("IPHONE-001", response.sku());
        assertEquals(20, response.quantity());
        assertTrue(response.active());

        verify(productRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenProductIdDoesNotExist() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        verify(productRepository).findById(999L);
    }

    // =========================================================
    // UPDATE PRODUCT TESTS
    // =========================================================

    @Test
    void shouldUpdateProductSuccessfully() {

        Long productId = 1L;

        Product existingProduct = createProduct(
                productId,
                new ProductRequest(
                        "iPhone 16",
                        "Old description",
                        new BigDecimal("69999.00"),
                        "IPHONE-001",
                        10
                ),
                true
        );

        ProductRequest updateRequest = new ProductRequest(
                "iPhone 17",
                "Updated Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-002",
                25
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot(
                "IPHONE-002", productId
        )).thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.updateProduct(productId, updateRequest);

        assertEquals(productId, response.id());
        assertEquals("iPhone 17", response.name());
        assertEquals("Updated Apple smartphone", response.description());
        assertEquals(new BigDecimal("79999.00"), response.price());
        assertEquals("IPHONE-002", response.sku());
        assertEquals(25, response.quantity());
        assertTrue(response.active());

        verify(productRepository).findById(productId);
        verify(productRepository).existsBySkuAndIdNot(
                "IPHONE-002", productId
        );

        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        assertEquals("iPhone 17", capturedProduct.getName());
        assertEquals("Updated Apple smartphone", capturedProduct.getDescription());
        assertEquals(new BigDecimal("79999.00"), capturedProduct.getPrice());
        assertEquals("IPHONE-002", capturedProduct.getSku());
        assertEquals(25, capturedProduct.getQuantity());
    }

    @Test
    void shouldAllowSameSkuWhenUpdatingSameProduct() {

        Long productId = 1L;

        Product existingProduct = createProduct(
                productId,
                new ProductRequest(
                        "iPhone 16",
                        "Old description",
                        new BigDecimal("69999.00"),
                        "IPHONE-001",
                        10
                ),
                true
        );

        ProductRequest updateRequest = new ProductRequest(
                "iPhone 16 Pro",
                "Updated description",
                new BigDecimal("89999.00"),
                "IPHONE-001",
                15
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot(
                "IPHONE-001", productId
        )).thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.updateProduct(productId, updateRequest);

        assertEquals("iPhone 16 Pro", response.name());
        assertEquals("IPHONE-001", response.sku());

        verify(productRepository).existsBySkuAndIdNot(
                "IPHONE-001", productId
        );

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingProductWithDuplicateSku() {

        Long productId = 1L;

        Product existingProduct = createProduct(
                productId,
                new ProductRequest(
                        "iPhone 16",
                        "Apple smartphone",
                        new BigDecimal("69999.00"),
                        "IPHONE-001",
                        10
                ),
                true
        );

        ProductRequest updateRequest = new ProductRequest(
                "Samsung S25",
                "Samsung smartphone",
                new BigDecimal("69999.00"),
                "SAMSUNG-001",
                15
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot(
                "SAMSUNG-001", productId
        )).thenReturn(true);

        assertThrows(
                ProductAlreadyExistsException.class,
                () -> productService.updateProduct(productId, updateRequest)
        );

        verify(productRepository).findById(productId);
        verify(productRepository).existsBySkuAndIdNot(
                "SAMSUNG-001", productId
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingProduct() {

        Long productId = 999L;

        ProductRequest request = new ProductRequest(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(productId, request)
        );

        verify(productRepository).findById(productId);

        verify(productRepository, never())
                .existsBySkuAndIdNot(any(String.class), any(Long.class));

        verify(productRepository, never()).save(any(Product.class));
    }

    // =========================================================
    // DELETE PRODUCT TESTS
    // =========================================================

    @Test
    void shouldSoftDeleteActiveProductSuccessfully() {

        Long productId = 1L;

        Product product = createProduct(
                productId,
                new ProductRequest(
                        "iPhone 17",
                        "Apple smartphone",
                        new BigDecimal("79999.00"),
                        "IPHONE-001",
                        20
                ),
                true
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteProduct(productId);

        assertFalse(product.getActive());

        verify(productRepository).findById(productId);
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        assertFalse(capturedProduct.getActive());
        assertEquals(productId, capturedProduct.getId());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingProduct() {

        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(productId)
        );

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingAlreadyDeletedProduct() {

        Long productId = 1L;

        Product inactiveProduct = createProduct(
                productId,
                new ProductRequest(
                        "iPhone 17",
                        "Apple smartphone",
                        new BigDecimal("79999.00"),
                        "IPHONE-001",
                        20
                ),
                false
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(inactiveProduct));

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(productId)
        );

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    // =========================================================
    // TEST DATA HELPER
    // =========================================================

    private Product createProduct(
            Long id,
            ProductRequest request,
            boolean active) {

        Product product = new Product();

        product.setId(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setQuantity(request.quantity());
        product.setActive(active);

        return product;
    }
}
