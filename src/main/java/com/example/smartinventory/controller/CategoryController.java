package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.model.Category;
import com.example.smartinventory.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing {@link Category} resources. */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "CRUD operations for product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category", description = "Creates a new category. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<Category> create(@Valid @RequestBody Category category) {
        Category created = categoryService.create(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<Category> findById(
            @Parameter(description = "Identifier of the category") @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all categories")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    public ResponseEntity<List<Category>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category", description = "Replaces an existing category. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<Category> update(
            @Parameter(description = "Identifier of the category") @PathVariable Long id,
            @Valid @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.update(id, category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category", description = "Deletes a category. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the category") @PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
