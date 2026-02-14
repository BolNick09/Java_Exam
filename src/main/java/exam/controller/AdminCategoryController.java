package exam.controller;

import exam.model.Category;
import exam.repository.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    public AdminCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String categories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    @PostMapping
    public String createCategory(@ModelAttribute Category category) {

        // защита от дубликатов
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            return "redirect:/admin/categories?error";
        }

        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }
}
