package com.urbanprint.backend.service;

import com.urbanprint.backend.model.Equipment;
import com.urbanprint.backend.model.EquipmentType;
import com.urbanprint.backend.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Equipment> findAll() {
        return equipmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Equipment> findById(Long id) {
        return equipmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Equipment> findByName(String name) {
        return equipmentRepository.findByNameIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Equipment> findByType(EquipmentType type) {
        return equipmentRepository.findByTypeOrderByNameAsc(type);
    }

    @Transactional(readOnly = true)
    public List<Equipment> findByActive(boolean active) {
        return equipmentRepository.findByActiveOrderByNameAsc(active);
    }

    @Transactional
    public Equipment create(Equipment equipment) {
        validateNameIsAvailable(equipment.getName(), null);

        if (equipment.getType() == null) {
            equipment.setType(EquipmentType.UNKNOWN);
        }

        return equipmentRepository.save(equipment);
    }

    @Transactional
    public Optional<Equipment> update(Long id, Equipment updatedEquipment) {
        return equipmentRepository.findById(id)
                .map(existingEquipment -> {
                    validateNameIsAvailable(
                            updatedEquipment.getName(),
                            existingEquipment.getId()
                    );

                    existingEquipment.setName(updatedEquipment.getName());
                    existingEquipment.setManufacturer(
                            updatedEquipment.getManufacturer()
                    );
                    existingEquipment.setModel(
                            updatedEquipment.getModel()
                    );
                    existingEquipment.setType(
                            updatedEquipment.getType() != null
                                    ? updatedEquipment.getType()
                                    : EquipmentType.UNKNOWN
                    );
                    existingEquipment.setActive(
                            updatedEquipment.isActive()
                    );
                    existingEquipment.setNotes(
                            updatedEquipment.getNotes()
                    );

                    return equipmentRepository.save(existingEquipment);
                });
    }

    @Transactional
    public Optional<Equipment> updateActiveStatus(
            Long id,
            boolean active
    ) {
        return equipmentRepository.findById(id)
                .map(equipment -> {
                    equipment.setActive(active);
                    return equipmentRepository.save(equipment);
                });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            return false;
        }

        equipmentRepository.deleteById(id);
        return true;
    }

    private void validateNameIsAvailable(
            String name,
            Long currentEquipmentId
    ) {
        equipmentRepository.findByNameIgnoreCase(name)
                .ifPresent(existingEquipment -> {
                    boolean belongsToCurrentRecord =
                            currentEquipmentId != null
                                    && existingEquipment.getId()
                                    .equals(currentEquipmentId);

                    if (!belongsToCurrentRecord) {
                        throw new IllegalArgumentException(
                                "An equipment with this name already exists"
                        );
                    }
                });
    }
}