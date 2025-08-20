package com.github.liuchangming88.ecommerce_backend.service.product;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public ProductService(ProductRepository productRepository, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    public Page<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Product> productPage = productRepository.findAll(pageable);

        return productPage.map(product -> modelMapper.map(product, ProductResponse.class));
    }

    public ProductResponse getProduct(Long productId) {
        return productRepository.findById(productId)
                .map(product -> modelMapper.map(product, ProductResponse.class))
                .orElseThrow(() -> new ResourceNotFoundException("Can't find product with ID " + productId));
    }
}
