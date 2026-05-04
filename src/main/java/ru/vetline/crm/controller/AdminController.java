package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.vetline.crm.entity.UserRole;
import ru.vetline.crm.service.UserService;

import java.util.UUID;

/**
 * Администрирование: управление пользователями и справочниками.
 * Доступ только для роли ADMIN. ТЗ п. 4.3.4.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // ── Список пользователей ──────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", UserRole.values());
        return "admin/users";
    }

    // ── Форма создания пользователя ───────────────────────────────────────────
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        return "admin/user_form";
    }

    @PostMapping("/users/new")
    public String createUser(@RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam UserRole role,
                             RedirectAttributes ra) {
        try {
            userService.create(fullName, email, password, role);
            ra.addFlashAttribute("success",
                    "Пользователь «" + fullName + "» успешно создан");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Форма редактирования ──────────────────────────────────────────────────
    @GetMapping("/users/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("user",  userService.findById(id));
        model.addAttribute("roles", UserRole.values());
        return "admin/user_form";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable UUID id,
                             @RequestParam String fullName,
                             @RequestParam UserRole role,
                             RedirectAttributes ra) {
        userService.update(id, fullName, role);
        ra.addFlashAttribute("success", "Данные пользователя обновлены");
        return "redirect:/admin/users";
    }

    // ── Смена пароля ─────────────────────────────────────────────────────────
    @PostMapping("/users/{id}/password")
    public String changePassword(@PathVariable UUID id,
                                 @RequestParam String password,
                                 RedirectAttributes ra) {
        userService.changePassword(id, password);
        ra.addFlashAttribute("success", "Пароль изменён");
        return "redirect:/admin/users";
    }

    // ── Деактивировать / активировать ────────────────────────────────────────
    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable UUID id,
                         @RequestParam boolean active,
                         RedirectAttributes ra) {
        userService.setActive(id, active);
        ra.addFlashAttribute("success",
                active ? "Пользователь активирован" : "Пользователь деактивирован");
        return "redirect:/admin/users";
    }
}
