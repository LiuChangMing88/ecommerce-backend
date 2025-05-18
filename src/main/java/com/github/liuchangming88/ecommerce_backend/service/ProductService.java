package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.model.Product;
import com.github.liuchangming88.ecommerce_backend.model.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

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
}