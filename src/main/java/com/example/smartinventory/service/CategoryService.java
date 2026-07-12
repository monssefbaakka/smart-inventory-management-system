package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Category}. */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Updates the mutable fields of an existing category identified by {@code id}.
     *
     * @param id              identifier of the category to update
     * @param updatedCategory category carrying the new field values
     * @return the persisted, updated category
     */
    public Category update(Long id, Category updatedCategory) {
        Category existing = findById(id);
        existing.setName(updatedCategory.getName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    public void delete(Long id) {
        categoryRepository.delete(findById(id));
    }

}
