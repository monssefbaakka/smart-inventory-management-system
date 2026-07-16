package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createSavesCategory() {
        Category category = Category.builder().name("Electronics").build();
        when(categoryRepository.save(category)).thenReturn(category);

        Category result = categoryService.create(category);

        assertThat(result).isSameAs(category);
    }

    @Test
    void findByIdReturnsCategoryWhenPresent() {
        Category category = Category.builder().id(1L).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThat(categoryService.findById(1L)).isSameAs(category);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllReturnsAllCategories() {
        Category category = Category.builder().id(1L).build();
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        assertThat(categoryService.findAll()).containsExactly(category);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        Category existing = Category.builder().id(1L).name("Old").build();
        Category updated = Category.builder().name("New").description("desc").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.update(1L, updated);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("desc");
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category existing = Category.builder().id(1L).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.delete(1L);

        verify(categoryRepository).delete(existing);
    }

}
