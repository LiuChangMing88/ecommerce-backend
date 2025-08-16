package com.github.liuchangming88.ecommerce_backend.service.product;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    ProductRepository productRepository;
    ModelMapper modelMapper;

    public ProductService(ProductRepository productRepository, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    public List<ProductResponse> getAllProducts() {
        // Get all product from database and then convert into response body to give to the client
        List<Product> allProducts = productRepository.findAll();
        return allProducts.stream()
                .map(p -> modelMapper.map(p, ProductResponse.class))
                .toList();
    }

    public ProductResponse getProduct (Long productId) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isEmpty())
            throw new ResourceNotFoundException("Can't find product with ID " + productId);
        return modelMapper.map(optionalProduct.get(), ProductResponse.class);
    }
}