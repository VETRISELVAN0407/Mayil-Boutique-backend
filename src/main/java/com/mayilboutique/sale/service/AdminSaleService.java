package com.mayilboutique.sale.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.sale.dto.SaleRequest;
import com.mayilboutique.sale.dto.SaleResponse;
import com.mayilboutique.sale.entity.Sale;
import com.mayilboutique.sale.repository.SaleRepository;

@Service
public class AdminSaleService {

    private final SaleRepository saleRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public AdminSaleService(SaleRepository saleRepository, CategoryRepository categoryRepository,
                            ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> list() {
        Instant now = Instant.now();
        return saleRepository.findAllByOrderByStartsAtDescIdDesc().stream().map(s -> SaleResponse.from(s, now)).toList();
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Sale sale = new Sale();
        apply(sale, request);
        sale.setActive(request.getActive() == null || request.getActive());
        return SaleResponse.from(saleRepository.save(sale), Instant.now());
    }

    @Transactional
    public SaleResponse update(Long id, SaleRequest request) {
        Sale sale = find(id);
        apply(sale, request);
        if (request.getActive() != null) {
            sale.setActive(request.getActive());
        }
        return SaleResponse.from(saleRepository.save(sale), Instant.now());
    }

    @Transactional
    public SaleResponse setActive(Long id, boolean active) {
        Sale sale = find(id);
        sale.setActive(active);
        return SaleResponse.from(saleRepository.save(sale), Instant.now());
    }

    /** Sales only change what is shown and charged while they run; orders keep their own prices, so delete is safe. */
    @Transactional
    public void delete(Long id) {
        saleRepository.delete(find(id));
    }

    private void apply(Sale sale, SaleRequest r) {
        Set<Long> productIds = r.getProductIds() == null ? Set.of() : new HashSet<>(r.getProductIds());
        if (!r.isAllProducts() && r.getCategoryId() == null && productIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Choose what the sale applies to: all products, a category or specific products");
        }
        if (!r.getEndsAt().isAfter(r.getStartsAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End date must be after the start date");
        }
        if (r.getCategoryId() != null && !categoryRepository.existsById(r.getCategoryId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Category not found");
        }
        if (!productIds.isEmpty() && productRepository.findAllById(productIds).size() != productIds.size()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "One or more selected products were not found");
        }
        sale.setName(r.getName().trim());
        sale.setPercentOff(r.getPercentOff());
        sale.setAllProducts(r.isAllProducts());
        sale.setCategoryId(r.getCategoryId());
        sale.setProductIds(productIds);
        sale.setStartsAt(r.getStartsAt());
        sale.setEndsAt(r.getEndsAt());
    }

    private Sale find(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Sale not found"));
    }
}
