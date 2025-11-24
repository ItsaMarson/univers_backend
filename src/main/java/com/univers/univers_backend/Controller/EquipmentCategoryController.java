/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentCategoryDTO;
import com.univers.univers_backend.Service.EquipmentCategoryService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment-categories")
@Tag(name = "Equipment Category", description = "Equipment category management APIs")
public class EquipmentCategoryController {

    private final EquipmentCategoryService categoryService;

    public EquipmentCategoryController(EquipmentCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Create a new equipment category")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Category created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input")
            })
    @PostMapping
    public ResponseEntity<ApiResponse<EquipmentCategoryDTO>> createCategory(
            @RequestBody EquipmentCategoryDTO categoryDTO) {
        EquipmentCategoryDTO newCategory = categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", newCategory));
    }

    @Operation(summary = "Get all equipment categories")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Categories retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "204",
                        description = "No categories found")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<List<EquipmentCategoryDTO>>> getAllCategories() {
        List<EquipmentCategoryDTO> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.success("No categories found", categories));
        }
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @Operation(summary = "Get equipment category by public ID")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Category retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Category not found")
            })
    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<EquipmentCategoryDTO>> getCategoryByPublicId(
            @PathVariable String publicId) {
        EquipmentCategoryDTO category = categoryService.getCategoryByPublicId(publicId);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @Operation(summary = "Update an existing equipment category")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Category updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Category not found")
            })
    @PutMapping("/{publicId}")
    public ResponseEntity<ApiResponse<EquipmentCategoryDTO>> updateCategory(
            @PathVariable String publicId, @RequestBody EquipmentCategoryDTO categoryDTO) {
        EquipmentCategoryDTO updatedCategory =
                categoryService.updateCategory(publicId, categoryDTO);
        return ResponseEntity.ok(
                ApiResponse.success("Category updated successfully", updatedCategory));
    }

    @Operation(summary = "Delete an equipment category")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Category deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Category not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Category is in use and cannot be deleted")
            })
    @DeleteMapping("/{publicId}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable String publicId) {
        categoryService.deleteCategory(publicId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }
}
