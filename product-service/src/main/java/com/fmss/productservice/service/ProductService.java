package com.fmss.productservice.service;

import com.fmss.productservice.exception.ProductNotFoundException;
import com.fmss.productservice.mapper.ProductMapper;
import com.fmss.productservice.model.Product;
import com.fmss.productservice.model.dto.ProductRequestDto;
import com.fmss.productservice.model.dto.ProductResponseDto;
import com.fmss.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private final FileUploadService fileUploadService;

    //@Cacheable(value = "allProducts", cacheManager = "cacheManager")

    @Cacheable(
            value = {"sharepoint"},
            key = "{#methodName}",
            unless = "#result == null"
    )
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.getAllProducts().parallelStream().map(productMapper::toResponseDto).toList();
    }

    @CacheEvict(value = "allProducts")
    @CacheEvict(
            value = {"page-manager"},
            allEntries = true
    )
    public ProductResponseDto updateProduct(ProductRequestDto productRequestDto, String productId) {
        Product product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);

        product.setName(productRequestDto.getName());
        product.setPrice(productRequestDto.getPrice());
        product.setStatus(productRequestDto.getStatus());

        return productMapper.toResponseDto(productRepository.save(product));
    }


    public void deleteProduct(String productId) {
        productRepository.findById(productId)
                .ifPresentOrElse(
                        product -> productRepository.delete(product),
                        () -> {
                            throw new ProductNotFoundException();
                        });
    }

    public ProductResponseDto getProductById(String productId){
        return productMapper.toResponseDto(productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found")));
    }

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto, MultipartFile multipartFile) {

        String url;
        try {
            String fileName = productRequestDto.getFileName() != null ? productRequestDto.getFileName() : "temp";
            assert productRequestDto.getFileName() != null;
            File convFile = File.createTempFile(fileName, "-advice");
            multipartFile.transferTo(convFile);
            url = fileUploadService.storeFile(convFile, productRequestDto.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Product persistenceProduct = productRepository.save(productMapper.toEntity(productRequestDto, url));

        return productMapper.toResponseDto(persistenceProduct);
    }
}
