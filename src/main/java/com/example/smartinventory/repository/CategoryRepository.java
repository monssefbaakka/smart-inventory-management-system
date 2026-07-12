package com.example.smartinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Category;

/** Repository for {@link Category} persistence operations. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

}
