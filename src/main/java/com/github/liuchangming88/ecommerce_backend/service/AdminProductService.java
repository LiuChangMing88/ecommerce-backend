package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.exception.DuplicateResourceException;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.Product;
import com.github.liuchangming88.ecommerce_backend.model.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class AdminProductService {
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public AdminProductService(ProductRepository productRepository, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        // Check if a product with the same name already exists
        if (productRepository.existsByName(productRequest.getName())) {
            throw new DuplicateResourceException("Product with name '" + productRequest.getName() + "' already exists.");
        }

        // Map ProductRequest to Product entity
        Product product = modelMapper.map(productRequest, Product.class);

        // Initialize Inventory and associate it with the product
        Inventory inventory = new Inventory();
        inventory.setQuantity(productRequest.getQuantity());
        inventory.setProduct(product);
        product.setInventory(inventory);

        // Save the product (cascades to inventory)
        Product savedProduct = productRepository.save(product);

        // Map the saved product to ProductResponse DTO
        return modelMapper.map(savedProduct, ProductResponse.class);
    }


    public ProductResponse updateProduct(Long productId, ProductRequest productRequest) {
        // Retrieve the existing product
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Check for name change and potential duplication
        if (!existingProduct.getName().equals(productRequest.getName()) &&
                productRepository.existsByName(productRequest.getName())) {
            throw new DuplicateResourceException("Product with name '" + productRequest.getName() + "' already exists.");
        }

        // Update product fields
        existingProduct.setName(productRequest.getName());
        existingProduct.setShortDescription(productRequest.getShortDescription());
        existingProduct.setLongDescription(productRequest.getLongDescription());
        existingProduct.setPrice(productRequest.getPrice());

        // Update inventory quantity
        Inventory inventory = existingProduct.getInventory();
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setProduct(existingProduct);
        }
        inventory.setQuantity(productRequest.getQuantity());
        existingProduct.setInventory(inventory);

        // Save the updated product
        Product updatedProduct = productRepository.save(existingProduct);

        // Map the updated product to ProductResponse DTO
        return modelMapper.map(updatedProduct, ProductResponse.class);
    }


    public void deleteProduct(Long productId) {
        // Retrieve the existing product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Delete the product
        productRepository.delete(product);
    }


    public void updateProductQuantity(Long productId, Long quantity) {
        // Retrieve the existing product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Retrieve or create inventory
        Inventory inventory = product.getInventory();
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setProduct(product);
        }

        // Update quantity
        inventory.setQuantity(quantity);
        product.setInventory(inventory);

        // Save the updated product
        productRepository.save(product);
    }

}
