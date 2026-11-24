
package com.orderflow.product.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.orderflow.product.entity.Product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class ProductSpecificationTest {

    private Root<Product> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder criteriaBuilder;
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        criteriaBuilder = mock(CriteriaBuilder.class);
        predicate = mock(Predicate.class);
    }

    @Test
    void shouldCreateNameFilterPredicate() {

        Path<String> namePath = mock(Path.class);
        Expression<String> lowerExpression = mock(Expression.class);

        when(root.<String>get("name")).thenReturn(namePath);
        when(criteriaBuilder.lower(namePath)).thenReturn(lowerExpression);
        when(criteriaBuilder.like(lowerExpression, "%iphone%"))
                .thenReturn(predicate);

        Specification<Product> specification =
                ProductSpecification.hasName("iPhone");

        Predicate result = specification.toPredicate(
                root, query, criteriaBuilder
        );

        assertNotNull(result);
        verify(criteriaBuilder).like(lowerExpression, "%iphone%");
    }

    @Test
    void shouldCreateSkuFilterPredicate() {

        Path<String> skuPath = mock(Path.class);
        Expression<String> lowerExpression = mock(Expression.class);

        when(root.<String>get("sku")).thenReturn(skuPath);
        when(criteriaBuilder.lower(skuPath)).thenReturn(lowerExpression);
        when(criteriaBuilder.equal(lowerExpression, "iphone-001"))
                .thenReturn(predicate);

        Specification<Product> specification =
                ProductSpecification.hasSku("IPHONE-001");

        Predicate result = specification.toPredicate(
                root, query, criteriaBuilder
        );

        assertNotNull(result);
        verify(criteriaBuilder).equal(lowerExpression, "iphone-001");
    }

    @Test
    void shouldCreateMinimumPricePredicate() {

        Path<BigDecimal> pricePath = mock(Path.class);
        when(root.<BigDecimal>get("price")).thenReturn(pricePath);
        when(criteriaBuilder.greaterThanOrEqualTo(
                pricePath, new BigDecimal("30000")
        )).thenReturn(predicate);

        Specification<Product> specification =
                ProductSpecification.hasMinPrice(
                        new BigDecimal("30000")
                );

        Predicate result = specification.toPredicate(
                root, query, criteriaBuilder
        );

        assertNotNull(result);
        verify(criteriaBuilder).greaterThanOrEqualTo(
                pricePath, new BigDecimal("30000")
        );
    }

    @Test
    void shouldCreateMaximumPricePredicate() {

        Path<BigDecimal> pricePath = mock(Path.class);

        when(root.<BigDecimal>get("price")).thenReturn(pricePath);
        when(criteriaBuilder.lessThanOrEqualTo(
                pricePath, new BigDecimal("80000")
        )).thenReturn(predicate);

        Specification<Product> specification =
                ProductSpecification.hasMaxPrice(
                        new BigDecimal("80000")
                );

        Predicate result = specification.toPredicate(
                root, query, criteriaBuilder
        );

        assertNotNull(result);
        verify(criteriaBuilder).lessThanOrEqualTo(
                pricePath, new BigDecimal("80000")
        );
    }

    @Test
    void shouldCreateActiveStatusPredicate() {

        Path<Boolean> activePath = mock(Path.class);
        when(root.<Boolean>get("active")).thenReturn(activePath);
        when(criteriaBuilder.equal(activePath, false))
                .thenReturn(predicate);

        Specification<Product> specification =
                ProductSpecification.isActive(false);

        Predicate result = specification.toPredicate(
                root, query, criteriaBuilder
        );

        assertNotNull(result);
        verify(criteriaBuilder).equal(activePath, false);
    }
}
