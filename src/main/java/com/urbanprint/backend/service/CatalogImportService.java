package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Product;
import com.urbanprint.backend.model.ProductPriceScale;
import com.urbanprint.backend.model.ProductSize;
import com.urbanprint.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports the Small Format product catalog from the printed Urban Print catalog.
 * Idempotent: products whose name already exists are skipped.
 * Prices are total selling prices from the catalog (totalCost is left empty
 * until the internal cost structure is entered by the user).
 */
@Service
public class CatalogImportService {

    private final ProductRepository productRepository;

    public CatalogImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Map<String, Object> importSmallFormatCatalog() {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Product product : buildSmallFormatProducts()) {
            if (productRepository.findFirstByProductNameIgnoreCase(product.getProductName()).isPresent()) {
                skipped.add(product.getProductName());
            } else {
                productRepository.save(product);
                created.add(product.getProductName());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("createdCount", created.size());
        result.put("skippedCount", skipped.size());
        result.put("created", created);
        result.put("skippedAlreadyExists", skipped);
        return result;
    }

    private List<Product> buildSmallFormatProducts() {
        List<Product> products = new ArrayList<>();

        // 1. Standard Paper (per-unit prices converted to scale totals)
        Product standardPaper = product("Standard Paper", "SP-LETTER",
                "B&W: $0.19/pc (1-500), $0.12/pc (500+). Color: $0.59/pc (1-500), $0.48/pc (500+). "
                        + "Scale totals derived from catalog per-unit prices.",
                null);
        standardPaper.addSize(size("Letter - Black & White", 8.5, 11, null,
                new int[]{1, 500, 1000}, new double[]{0.19, 95.00, 120.00}));
        standardPaper.addSize(size("Letter - Color", 8.5, 11, null,
                new int[]{1, 500, 1000}, new double[]{0.59, 295.00, 480.00}));
        products.add(standardPaper);

        // 2. Photographic Paper (flat price per print)
        Product photo = product("Photographic Paper", "PHOTO",
                "Photo prints - catalog price per print.", null);
        photo.addSize(size("4x6", 4, 6, null, new int[]{1}, new double[]{0.36}));
        photo.addSize(size("5x7", 5, 7, null, new int[]{1}, new double[]{1.29}));
        photo.addSize(size("11x14", 11, 14, null, new int[]{1}, new double[]{13.99}));
        photo.addSize(size("12x18", 12, 18, null, new int[]{1}, new double[]{17.99}));
        products.add(photo);

        // 3. Flyers
        Product flyers = product("Flyers", "FLY",
                "Flyers - finish: gloss or silk (catalog).", "Gloss or Silk");
        flyers.addSize(size("4x6", 4, 6, "Gloss or Silk",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{40, 61, 75, 115, 229, 365}));
        flyers.addSize(size("5x7", 5, 7, "Gloss or Silk",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{45, 73, 86, 145, 305, 449}));
        flyers.addSize(size("8.5x11", 8.5, 11, "Gloss or Silk",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{60, 115, 145, 269, 445, 725}));
        products.add(flyers);

        // 4. Brochures
        Product brochures = product("Brochures", "BRF",
                "Brochures on 100 lb Gloss Text or Silk Text (catalog).",
                "100 lb Gloss Text / Silk Text");
        brochures.addSize(size("8.5x11 - Tri-Fold", 8.5, 11, null,
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{89, 159, 267, 448, 959, 1587}));
        brochures.addSize(size("8.5x11 - Half-Fold", 8.5, 11, null,
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{87, 146, 242, 385, 689, 1195}));
        brochures.addSize(size("11x17 - Tri-Fold", 11, 17, null,
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{139, 219, 335, 499, 935, 1655}));
        brochures.addSize(size("11x17 - Half-Fold", 11, 17, null,
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{135, 249, 345, 499, 965, 1749}));
        products.add(brochures);

        // 5. Postcards (3 stocks x 3 sizes)
        Product postcards = product("Postcards", "PC",
                "Postcards in Gloss & Soft Touch, Mohawk Superfine and Kraft (catalog).", null);
        postcards.addSize(size("4x6 - Gloss & Soft Touch", 4, 6, "Gloss / Soft Touch",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{61, 109, 155, 245, 549, 1029}));
        postcards.addSize(size("5x7 - Gloss & Soft Touch", 5, 7, "Gloss / Soft Touch",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{72, 135, 209, 375, 839, 1615}));
        postcards.addSize(size("8.5x11 - Gloss & Soft Touch", 8.5, 11, "Gloss / Soft Touch",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{115, 239, 429, 809, 1935, 3805}));
        postcards.addSize(size("4x6 - Mohawk Superfine", 4, 6, "Mohawk Superfine",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{74, 139, 225, 395, 939, 1819}));
        postcards.addSize(size("5x7 - Mohawk Superfine", 5, 7, "Mohawk Superfine",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{95, 189, 325, 619, 1469, 2885}));
        postcards.addSize(size("8.5x11 - Mohawk Superfine", 8.5, 11, "Mohawk Superfine",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{175, 389, 739, 1439, 3525, 7005}));
        postcards.addSize(size("4x6 - Kraft", 4, 6, "100% Recycled Kraft",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{51, 85, 125, 205, 469, 885}));
        postcards.addSize(size("5x7 - Kraft", 5, 7, "100% Recycled Kraft",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{61, 109, 175, 319, 725, 1395}));
        postcards.addSize(size("8.5x11 - Kraft", 8.5, 11, "100% Recycled Kraft",
                new int[]{100, 250, 500, 1000, 2500, 5000},
                new double[]{95, 199, 365, 689, 1665, 3285}));
        products.add(postcards);

        // 6. Tent Cards (catalog quantity columns: 250 for 4x6, 200 for the others)
        Product tentCards = product("Tent Cards", "TENT",
                "16pt cardstock tent cards. Optional matte or gloss lamination +25% (catalog).",
                "16pt Cardstock");
        tentCards.addSize(size("4x6", 4, 6, null,
                new int[]{10, 25, 50, 100, 250, 500},
                new double[]{16, 39, 87, 115, 233, 587}));
        tentCards.addSize(size("5x7.5", 5, 7.5, null,
                new int[]{10, 25, 50, 100, 200, 500},
                new double[]{19, 49, 92, 135, 268, 604}));
        tentCards.addSize(size("8.5x11", 8.5, 11, null,
                new int[]{10, 25, 50, 100, 200, 500},
                new double[]{42, 76, 125, 189, 326, 808}));
        products.add(tentCards);

        // 7. Business Cards
        Product businessCards = product("Business Cards", "BC-16PT",
                "Premium 16pt cardstock. Rounded corners +15% (catalog).",
                "Premium 16pt Cardstock");
        businessCards.addSize(size("3.5x2 - Uncoated", 3.5, 2, "Uncoated",
                new int[]{100, 250, 500, 1000, 1500},
                new double[]{39, 67, 108, 204, 263}));
        businessCards.addSize(size("3.5x2 - Gloss & Soft Touch", 3.5, 2, "Gloss / Soft Touch",
                new int[]{100, 250, 500, 1000, 1500},
                new double[]{68, 89, 134, 208, 289}));
        products.add(businessCards);

        // 8. Folders
        Product folders = product("Folders", "FLD",
                "Presentation folders (catalog).", null);
        folders.addSize(size("6x9", 6, 9, null,
                new int[]{100, 250, 500, 750, 1000, 1500},
                new double[]{404, 461, 744, 1098, 1454, 2024}));
        folders.addSize(size("9x12", 9, 12, null,
                new int[]{100, 250, 500, 750, 1000, 1500},
                new double[]{470, 618, 999, 1471, 1947, 2304}));
        products.add(folders);

        // 9. Booklets (full matrix confirmed in the catalog PDF)
        Product booklets = product("Booklets", "BKL",
                "Saddle-stitched booklets, 8/16/24 pages (catalog).", null);
        booklets.addSize(size("8.5x11 - 8 Pages", 8.5, 11, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{34, 59, 109, 179, 229, 269}));
        booklets.addSize(size("8.5x11 - 16 Pages", 8.5, 11, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{44, 89, 169, 299, 389, 429}));
        booklets.addSize(size("8.5x11 - 24 Pages", 8.5, 11, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{54, 119, 229, 399, 499, 599}));
        booklets.addSize(size("5.5x8.5 - 8 Pages", 5.5, 8.5, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{29, 49, 89, 145, 179, 199}));
        booklets.addSize(size("5.5x8.5 - 16 Pages", 5.5, 8.5, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{39, 69, 125, 215, 279, 329}));
        booklets.addSize(size("5.5x8.5 - 24 Pages", 5.5, 8.5, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{49, 89, 169, 299, 399, 479}));
        booklets.addSize(size("6x9 - 8 Pages", 6, 9, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{31, 52, 95, 155, 188, 215}));
        booklets.addSize(size("6x9 - 16 Pages", 6, 9, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{41, 75, 135, 228, 297, 349}));
        booklets.addSize(size("6x9 - 24 Pages", 6, 9, null,
                new int[]{1, 10, 25, 50, 75, 100},
                new double[]{51, 95, 179, 319, 425, 509}));
        products.add(booklets);

        // 10. Laminated Menus
        Product menus = product("Laminated Menus", "MENU-LAM",
                "Laminated menus (catalog).", null);
        menus.addSize(size("8.5x11", 8.5, 11, null,
                new int[]{1, 5, 10, 25, 50, 100},
                new double[]{8, 30, 50, 82, 108, 175}));
        menus.addSize(size("8.5x14", 8.5, 14, null,
                new int[]{1, 5, 10, 25, 50, 100},
                new double[]{9, 35, 60, 98, 125, 200}));
        menus.addSize(size("11x17", 11, 17, null,
                new int[]{1, 5, 10, 25, 50, 100},
                new double[]{12, 45, 75, 122, 142, 245}));
        products.add(menus);

        return products;
    }

    private Product product(String name, String itemCode, String description, String defaultStock) {
        Product p = new Product();
        p.setProductName(name);
        p.setItemCode(itemCode);
        p.setCategory("Small Format");
        p.setDescription(description);
        p.setPricingMethod("QUANTITY_SCALE");
        p.setUnit("Each");
        p.setProductionDepartment("Digital Printing");
        p.setDefaultStock(defaultStock);
        p.setActive(true);
        return p;
    }

    private ProductSize size(String label, double width, double height, String stock,
                             int[] quantities, double[] prices) {
        ProductSize s = new ProductSize();
        s.setLabel(label);
        if (width > 0 && height > 0) {
            s.setWidth(BigDecimal.valueOf(width));
            s.setHeight(BigDecimal.valueOf(height));
        }
        s.setStock(stock);
        s.setActive(true);
        for (int i = 0; i < quantities.length; i++) {
            ProductPriceScale scale = new ProductPriceScale();
            scale.setQuantity(quantities[i]);
            scale.setTotalPrice(BigDecimal.valueOf(prices[i]));
            s.addPriceScale(scale);
        }
        return s;
    }
}