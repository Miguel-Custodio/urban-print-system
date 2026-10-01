package com.urbanprint.backend.config;

import com.urbanprint.backend.model.InventoryCategory;
import com.urbanprint.backend.model.InventoryItem;
import com.urbanprint.backend.model.UnitOfMeasure;
import com.urbanprint.backend.repository.InventoryItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class InventoryDataInitializer {

    @Bean
    CommandLineRunner initializeInventoryItems(
            InventoryItemRepository inventoryItemRepository
    ) {
        return args -> {
            List<InventoryItem> initialItems = List.of(
                    createItem("Coroplast 4mm", InventoryCategory.WIDE_FORMAT),
                    createItem("Coroplast 10mm", InventoryCategory.WIDE_FORMAT),
                    createItem("PVC Branco 4mm", InventoryCategory.WIDE_FORMAT),
                    createItem("PVC Branco 6mm", InventoryCategory.WIDE_FORMAT),
                    createItem("PVC Preto 4mm", InventoryCategory.WIDE_FORMAT),
                    createItem("PVC Preto 6mm", InventoryCategory.WIDE_FORMAT),
                    createItem("Foamboard 4.5mm", InventoryCategory.WIDE_FORMAT),
                    createItem("ACM Single Sided", InventoryCategory.WIDE_FORMAT),
                    createItem("ACA Double Sided", InventoryCategory.WIDE_FORMAT),
                    createItem("Husky Opaque Smooth 80#", InventoryCategory.DIGITAL),
                    createItem("13x19 - Silk Cover 80#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Coated Cover 80#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Text Gloss 80#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Silk Text 100#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Pacesetter Dig Silk Text 100#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Text Gloss 100#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Smooth Uncoated 110#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Cover Gloss 111#", InventoryCategory.DIGITAL),
                    createItem("12x18 - 16PT Uncoated 120#", InventoryCategory.DIGITAL),
                    createItem("13x19 - Moh Loop TEXTURE Cover 130#", InventoryCategory.DIGITAL),
                    createItem("12x18 - Titan Dull/Silk Cover 130#", InventoryCategory.DIGITAL)
            );

            initialItems.forEach(item ->
                    inventoryItemRepository
                            .findByNameIgnoreCase(item.getName())
                            .orElseGet(() ->
                                    inventoryItemRepository.save(item)
                            )
            );
        };
    }

    private InventoryItem createItem(
            String name,
            InventoryCategory category
    ) {
        InventoryItem item = new InventoryItem();

        item.setName(name);
        item.setCategory(category);
        item.setUnit(UnitOfMeasure.SHEET);
        item.setQuantity(BigDecimal.ZERO);
        item.setMinimumStock(BigDecimal.ZERO);
        item.setActive(true);

        return item;
    }
}