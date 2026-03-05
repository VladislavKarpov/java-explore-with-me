package ru.practicum.ewm.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto addCategory(@Valid @RequestBody CategoryNewDto dto) {
        return categoryService.create(dto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        categoryService.delete(catId);
    }

    @PatchMapping("/{catId}")
    public CategoryResponseDto updateCategory(@PathVariable Long catId,
                                              @Valid @RequestBody CategoryResponseDto dto) {
        return categoryService.update(catId, dto);
    }
}