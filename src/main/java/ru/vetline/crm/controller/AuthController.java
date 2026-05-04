package ru.vetline.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error   != null) model.addAttribute("errorMsg",
                "Неверный e-mail или пароль. Проверьте данные и попробуйте снова.");
        if (logout  != null) model.addAttribute("successMsg", "Вы успешно вышли из системы.");
        return "auth/login";
    }

    /** Корень редиректит на /tickets */
    @GetMapping("/")
    public String root() { return "redirect:/tickets"; }
}
