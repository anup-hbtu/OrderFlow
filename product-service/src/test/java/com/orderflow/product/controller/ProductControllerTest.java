
package com.orderflow.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.orderflow.product.dto.PageResponse;
import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.exception.ProductAlreadyExistsException;
import com.orderflow.product.exception.ProductNotFoundException;
import com.orderflow.product.service.ProductService;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // =========================================================
    // TEST DATA HELPERS
    // =========================================================

    private ProductRequest createProductRequest() {

        return new ProductRequest(
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20
        );
    }

    private ProductResponse createProductResponse() {

        return new ProductResponse(
                1L,
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20,
                true,
                OffsetDateTime.parse("2026-11-18T00:00:00+05:30"),
                OffsetDateTime.parse("2026-11-18T00:00:00+05:30")
        );
    }

    private PageResponse<ProductResponse> createPageResponse(
            List<ProductResponse> content,
            int pageNumber,
            int pageSize,
            long totalElements) {

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / pageSize);

        boolean first = pageNumber == 0;
        boolean last = totalPages == 0 || pageNumber >= totalPages - 1;

        boolean hasNext = pageNumber < totalPages - 1;
        boolean hasPrevious = pageNumber > 0;

        return new PageResponse<>(
                content,
                pageNumber,
                pageSize,
                totalElements,
                totalPages,
                first,
                last,
                hasNext,
                hasPrevious
        );
    }

    // =========================================================
    // POST /api/products
    // CREATE PRODUCT
    // =========================================================

    @Test
    void shouldCreateProductSuccessfully() throws Exception {

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(createProductResponse());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "iPhone 17",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone 17"))
                .andExpect(jsonPath("$.sku").value("IPHONE-001"))
                .andExpect(jsonPath("$.price").value(79999.00))
                .andExpect(jsonPath("$.quantity").value(20))
                .andExpect(jsonPath("$.active").value(true));

        verify(productService).createProduct(any(ProductRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenProductNameIsMissing() throws Exception {

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "Product name is required")));
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsInvalid() throws Exception {

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "iPhone 17",
                            "description": "Apple smartphone",
                            "price": 0,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnConflictWhenDuplicateSkuExists() throws Exception {

        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new ProductAlreadyExistsException(
                        "Product already exists with SKU: IPHONE-001"));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "iPhone 17",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // =========================================================
    // GET /api/products
    // PAGINATION AND SORTING
    // =========================================================

    @Test
    void shouldReturnAllProductsSuccessfully() throws Exception {

        ProductResponse product = createProductResponse();

        PageResponse<ProductResponse> productPage =
                createPageResponse(List.of(product), 0, 10, 1);

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, null
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("iPhone 17"))
                .andExpect(jsonPath("$.content[0].sku").value("IPHONE-001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, null
        );
    }

    @Test
    void shouldPassCustomPaginationAndSortingParameters() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        1,
                        5,
                        6
                );

        when(productService.getAllProducts(
                1, 5, "price", "DESC",
                null, null, null, null, null
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("page", "1")
                .param("size", "5")
                .param("sortBy", "price")
                .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(productService).getAllProducts(
                1, 5, "price", "DESC",
                null, null, null, null, null
        );
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeExceedsMaximum() throws Exception {

        when(productService.getAllProducts(
                0, 500, "id", "ASC",
                null, null, null, null, null
        )).thenThrow(new IllegalArgumentException(
                "Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/products")
                .param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Page size must be between 1 and 100"));
    }

    @Test
    void shouldReturnBadRequestWhenSortFieldIsInvalid() throws Exception {

        when(productService.getAllProducts(
                0, 10, "unknown", "ASC",
                null, null, null, null, null
        )).thenThrow(new IllegalArgumentException(
                "Invalid sort field: unknown"));

        mockMvc.perform(get("/api/products")
                .param("sortBy", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid sort field: unknown"));
    }

    // =========================================================
    // GET /api/products
    // DYNAMIC FILTERING
    // =========================================================

    @Test
    void shouldFilterProductsByName() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        0, 10, 1
                );

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                "iphone", null, null, null, null
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("name", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("iPhone 17"));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                "iphone", null, null, null, null
        );
    }

    @Test
    void shouldFilterProductsBySku() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        0, 10, 1
                );

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                null, "IPHONE-001", null, null, null
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("sku", "IPHONE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("IPHONE-001"));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                null, "IPHONE-001", null, null, null
        );
    }

    @Test
    void shouldFilterProductsByPriceRange() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        0, 10, 1
                );

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                null, null,
                new BigDecimal("30000"),
                new BigDecimal("80000"),
                null
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("minPrice", "30000")
                .param("maxPrice", "80000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                null, null,
                new BigDecimal("30000"),
                new BigDecimal("80000"),
                null
        );
    }

    @Test
    void shouldFilterActiveProducts() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        0, 10, 1
                );

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, true
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, true
        );
    }

    @Test
    void shouldFilterInactiveProducts() throws Exception {

        ProductResponse inactiveProduct = new ProductResponse(
                1L,
                "iPhone 17",
                "Apple smartphone",
                new BigDecimal("79999.00"),
                "IPHONE-001",
                20,
                false,
                OffsetDateTime.parse("2026-11-18T00:00:00+05:30"),
                OffsetDateTime.parse("2026-11-18T00:00:00+05:30")
        );

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(inactiveProduct),
                        0, 10, 1
                );

        when(productService.getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, false
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].active").value(false));

        verify(productService).getAllProducts(
                0, 10, "id", "ASC",
                null, null, null, null, false
        );
    }

    @Test
    void shouldApplyMultipleFiltersWithPaginationAndSorting() throws Exception {

        PageResponse<ProductResponse> productPage =
                createPageResponse(
                        List.of(createProductResponse()),
                        1, 5, 6
                );

        when(productService.getAllProducts(
                1, 5, "price", "DESC",
                "iphone",
                "IPHONE-001",
                new BigDecimal("30000"),
                new BigDecimal("80000"),
                true
        )).thenReturn(productPage);

        mockMvc.perform(get("/api/products")
                .param("page", "1")
                .param("size", "5")
                .param("sortBy", "price")
                .param("direction", "DESC")
                .param("name", "iphone")
                .param("sku", "IPHONE-001")
                .param("minPrice", "30000")
                .param("maxPrice", "80000")
                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        verify(productService).getAllProducts(
                1, 5, "price", "DESC",
                "iphone",
                "IPHONE-001",
                new BigDecimal("30000"),
                new BigDecimal("80000"),
                true
        );
    }

    // =========================================================
    // GET /api/products/{id}
    // GET PRODUCT BY ID
    // =========================================================

    @Test
    void shouldReturnProductByIdSuccessfully() throws Exception {

        when(productService.getProductById(1L))
                .thenReturn(createProductResponse());

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone 17"))
                .andExpect(jsonPath("$.sku").value("IPHONE-001"));

        verify(productService).getProductById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {

        when(productService.getProductById(999L))
                .thenThrow(new ProductNotFoundException(
                        "Product not found with ID: 999"));

        mockMvc.perform(get("/api/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================
    // PUT /api/products/{id}
    // UPDATE PRODUCT
    // =========================================================

    @Test
    void shouldUpdateProductSuccessfully() throws Exception {

        when(productService.updateProduct(
                org.mockito.ArgumentMatchers.eq(1L),
                any(ProductRequest.class)
        )).thenReturn(createProductResponse());

        mockMvc.perform(put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "iPhone 17",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone 17"))
                .andExpect(jsonPath("$.sku").value("IPHONE-001"));

        verify(productService).updateProduct(
                org.mockito.ArgumentMatchers.eq(1L),
                any(ProductRequest.class)
        );
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRequestIsInvalid() throws Exception {

        mockMvc.perform(put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnConflictWhenUpdatedSkuAlreadyExists() throws Exception {

        when(productService.updateProduct(
                org.mockito.ArgumentMatchers.eq(1L),
                any(ProductRequest.class)
        )).thenThrow(new ProductAlreadyExistsException(
                "Product already exists with SKU: IPHONE-001"));

        mockMvc.perform(put("/api/products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "iPhone 17",
                            "description": "Apple smartphone",
                            "price": 79999.00,
                            "sku": "IPHONE-001",
                            "quantity": 20
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // =========================================================
    // DELETE /api/products/{id}
    // SOFT DELETE PRODUCT
    // =========================================================

    @Test
    void shouldDeleteProductSuccessfully() throws Exception {

        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistingProduct() throws Exception {

        doThrow(new ProductNotFoundException(
                "Product not found with ID: 999"))
                .when(productService).deleteProduct(999L);

        mockMvc.perform(delete("/api/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
