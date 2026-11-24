
package com.orderflow.product.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orderflow.product.dto.ApiErrorResponse;
import com.orderflow.product.dto.PageResponse;
import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.service.ProductService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/products")
@Tag(
        name = "Product Management",
        description = "APIs for creating, retrieving, updating and deleting products"
)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // CREATE PRODUCT
    @PostMapping
    @Operation(
            summary = "Create a product",
            description = "Creates a new product with a unique SKU."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(
                            schema = @Schema(implementation = ProductResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid product request",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Product SKU already exists",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL PRODUCTS WITH PAGINATION, SORTING AND DYNAMIC FILTERING
    @GetMapping
    @Operation(
            summary = "Get all products",
            description = """
                    Retrieves products with pagination, sorting and dynamic filtering.

                    Supported filters:
                    - Product name (partial, case-insensitive match)
                    - SKU (case-insensitive exact match)
                    - Minimum price
                    - Maximum price
                    - Active status

                    Maximum page size is 100.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Products retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination, sorting or filter parameters",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(

            // Pagination
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Number of products per page (1-100)", example = "10")
            @RequestParam(defaultValue = "10")
            int size,

            // Sorting
            @Parameter(
                    description = "Field used for sorting: id, name, price, createdAt",
                    example = "price"
            )
            @RequestParam(defaultValue = "id")
            String sortBy,

            @Parameter(description = "Sorting direction: ASC or DESC", example = "ASC")
            @RequestParam(defaultValue = "ASC")
            String direction,

            // Dynamic filtering
            @Parameter(description = "Filter by product name (partial match)", example = "Samsung")
            @RequestParam(required = false)
            String name,

            @Parameter(description = "Filter by exact SKU (case-insensitive)", example = "SAMSUNG-001")
            @RequestParam(required = false)
            String sku,

            @Parameter(description = "Filter products with price greater than or equal to this value", example = "1000")
            @RequestParam(required = false)
            BigDecimal minPrice,

            @Parameter(description = "Filter products with price less than or equal to this value", example = "100000")
            @RequestParam(required = false)
            BigDecimal maxPrice,

            @Parameter(description = "Filter by product active status", example = "true")
            @RequestParam(required = false)
            Boolean active

    ) {

        PageResponse<ProductResponse> products =
                productService.getAllProducts(
                        page,
                        size,
                        sortBy,
                        direction,
                        name,
                        sku,
                        minPrice,
                        maxPrice,
                        active
                );

        return ResponseEntity.ok(products);
    }

    // GET PRODUCT BY ID
    @GetMapping("/{id}")
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves a product using its unique product ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = ProductResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<ProductResponse> getProductById(

            @Parameter(description = "Unique product ID", example = "1")
            @PathVariable Long id

    ) {

        ProductResponse response =
                productService.getProductById(id);

        return ResponseEntity.ok(response);
    }

    // UPDATE PRODUCT
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a product",
            description = "Updates the details of an existing product."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product updated successfully",
                    content = @Content(
                            schema = @Schema(implementation = ProductResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid product request",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "SKU already belongs to another product",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<ProductResponse> updateProduct(

            @Parameter(description = "Unique product ID", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody ProductRequest request

    ) {

        ProductResponse response =
                productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    // DELETE PRODUCT (SOFT DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a product",
            description = """
                    Performs a soft delete by marking the product as inactive.
                    The product record remains in the database.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Product deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found or already inactive",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<Void> deleteProduct(

            @Parameter(description = "Unique product ID", example = "1")
            @PathVariable Long id

    ) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}
