package com.orderflow.product.service;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.product.dto.PageResponse;
import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.entity.Product;
import com.orderflow.product.exception.ProductAlreadyExistsException;
import com.orderflow.product.exception.ProductNotFoundException;
import com.orderflow.product.repository.ProductRepository;
import com.orderflow.product.specification.ProductSpecification;

@Service
@Transactional
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "price",
            "createdAt"
    );

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // CREATE PRODUCT
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with SKU: " + request.sku()
            );
        }

        Product product = new Product();

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setQuantity(request.quantity());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    // GET ALL PRODUCTS WITH PAGINATION AND SORTING
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        return getAllProducts(
                page,
                size,
                sortBy,
                direction,
                null,
                null,
                null,
                null,
                null
        );
    }

    // GET ALL PRODUCTS WITH PAGINATION, SORTING AND DYNAMIC FILTERING
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String name,
            String sku,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active
    ) {

        // 1. Validate pagination
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }

        // 2. Validate sorting field
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy
            );
        }

        // 3. Validate sorting direction
        if (direction == null ||
                !(direction.equalsIgnoreCase("ASC")
                        || direction.equalsIgnoreCase("DESC"))) {

            throw new IllegalArgumentException(
                    "Sort direction must be ASC or DESC"
            );
        }

        // 4. Validate price filters
        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be negative"
            );
        }

        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Maximum price cannot be negative"
            );
        }

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        // 5. Create dynamic Specification
        Specification<Product> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        // 6. Add filters only when supplied
        if (name != null && !name.isBlank()) {
            specification = specification.and(
                    ProductSpecification.hasName(name)
            );
        }

        if (sku != null && !sku.isBlank()) {
            specification = specification.and(
                    ProductSpecification.hasSku(sku)
            );
        }

        if (minPrice != null) {
            specification = specification.and(
                    ProductSpecification.hasMinPrice(minPrice)
            );
        }

        if (maxPrice != null) {
            specification = specification.and(
                    ProductSpecification.hasMaxPrice(maxPrice)
            );
        }

        if (active != null) {
            specification = specification.and(
                    ProductSpecification.isActive(active)
            );
        }

        // 7. Create sorting
        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        Sort sort = Sort.by(sortDirection, sortBy);

        // Add ID as a tie-breaker for consistent ordering
        if (!"id".equals(sortBy)) {
            sort = sort.and(
                    Sort.by(Sort.Direction.ASC, "id")
            );
        }

        // 8. Create pagination
        Pageable pageable = PageRequest.of(page, size, sort);

        // 9. Execute dynamic database query
        Page<Product> productPage =
                productRepository.findAll(specification, pageable);

        // 10. Convert entities into response DTOs
        Page<ProductResponse> productResponsePage =
                productPage.map(this::mapToResponse);

        // 11. Convert Spring Page into our stable API response
        return PageResponse.from(productResponsePage);
    }

    // GET PRODUCT BY ID
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id
                ));

        return mapToResponse(product);
    }

    // UPDATE PRODUCT
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id
                ));

        // Check whether another product already uses this SKU
        if (productRepository.existsBySkuAndIdNot(
                request.sku(), id)) {

            throw new ProductAlreadyExistsException(
                    "Another product already exists with SKU: "
                            + request.sku()
            );
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());
        product.setQuantity(request.quantity());

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    // SOFT DELETE PRODUCT
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id
                ));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ProductNotFoundException(
                    "Product is already inactive with ID: " + id
            );
        }

        product.setActive(false);

        productRepository.save(product);
    }

    // ENTITY TO RESPONSE DTO MAPPING
    private ProductResponse mapToResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getQuantity(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}