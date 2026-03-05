package ru.practicum.ewm.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryResponseDto create(CategoryNewDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        Category category = Category.builder().name(dto.getName()).build();
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryResponseDto update(Long catId, CategoryResponseDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        category.setName(dto.getName());
        return toDto(categoryRepository.save(category));
    }

    public List<CategoryResponseDto> getAll(int from, int size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public CategoryResponseDto getById(Long catId) {
        return toDto(categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found")));
    }

    public Category getEntityById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    public CategoryResponseDto toDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName()).build();
    }
}