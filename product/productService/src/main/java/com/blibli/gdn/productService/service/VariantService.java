package com.blibli.gdn.productService.service;

import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.model.Variant;

public interface VariantService {
    Variant findBySku(String sku);
    
    Product findProductBySku(String sku);
}
