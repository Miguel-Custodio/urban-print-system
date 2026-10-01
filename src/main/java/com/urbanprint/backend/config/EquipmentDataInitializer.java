package com.urbanprint.backend.config;

import com.urbanprint.backend.model.Equipment;
import com.urbanprint.backend.model.EquipmentType;
import com.urbanprint.backend.repository.EquipmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EquipmentDataInitializer {

    @Bean
    CommandLineRunner initializeEquipment(
            EquipmentRepository equipmentRepository
    ) {
        return args -> {
            List<Equipment> initialEquipment = List.of(
                    createEquipment(
                            "Mimaki UCJV 300-160",
                            "Mimaki",
                            "UCJV 300-160",
                            EquipmentType.WIDE_FORMAT_PRINTING
                    ),
                    createEquipment(
                            "Canon imagePRESS V700",
                            "Canon",
                            "imagePRESS V700",
                            EquipmentType.DIGITAL_PRINTING
                    ),
                    createEquipment(
                            "Mtutech 1314 Flatbed",
                            "Mtutech",
                            "1314 Flatbed",
                            EquipmentType.WIDE_FORMAT_PRINTING
                    ),
                    createEquipment(
                            "Brother SP-1 Sublimation",
                            "Brother",
                            "SP-1",
                            EquipmentType.SUBLIMATION
                    ),
                    createEquipment(
                            "Ruika",
                            "Ruika",
                            null,
                            EquipmentType.UNKNOWN
                    ),
                    createEquipment(
                            "Epson WorkForce WF7840",
                            "Epson",
                            "WorkForce WF7840",
                            EquipmentType.DIGITAL_PRINTING
                    ),
                    createEquipment(
                            "Mimaki LA 160W Laminadora",
                            "Mimaki",
                            "LA 160W",
                            EquipmentType.LAMINATION
                    ),
                    createEquipment(
                            "EBA 485EP Cutter",
                            "EBA",
                            "485EP",
                            EquipmentType.CUTTING
                    ),
                    createEquipment(
                            "Omnitech 150W Corte a Laser",
                            "Omnitech",
                            "150W",
                            EquipmentType.LASER_CUTTING
                    ),
                    createEquipment(
                            "CNC Chinesa",
                            null,
                            null,
                            EquipmentType.CNC
                    )
            );

            initialEquipment.forEach(equipment ->
                    equipmentRepository
                            .findByNameIgnoreCase(equipment.getName())
                            .orElseGet(() ->
                                    equipmentRepository.save(equipment)
                            )
            );
        };
    }

    private Equipment createEquipment(
            String name,
            String manufacturer,
            String model,
            EquipmentType type
    ) {
        Equipment equipment = new Equipment();

        equipment.setName(name);
        equipment.setManufacturer(manufacturer);
        equipment.setModel(model);
        equipment.setType(type);
        equipment.setActive(true);

        return equipment;
    }
}