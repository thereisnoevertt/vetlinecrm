package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.vetline.crm.entity.PlanTarget;
import ru.vetline.crm.entity.User;
import ru.vetline.crm.repository.PlanTargetRepository;
import ru.vetline.crm.repository.UserRepository;
import ru.vetline.crm.service.DashboardService;

/** Дашборд директора. ТЗ Ф-09. Только роль DIRECTOR. */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService    dashService;
    private final PlanTargetRepository planRepo;
    private final UserRepository      userRepo;

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "month") String period, Model model) {
        model.addAttribute("dash",   dashService.build(period));
        model.addAttribute("period", period);
        return "dashboard/index";
    }

    /** Установка планового показателя директором */
    @PostMapping("/plan")
    public String setPlan(@RequestParam int year,
                          @RequestParam(required = false) Integer month,
                          @RequestParam int target,
                          RedirectAttributes ra,
                          @AuthenticationPrincipal UserDetails principal) {
        User me = userRepo.findByEmailAndActiveTrue(principal.getUsername()).orElseThrow();
        planRepo.findByYearAndMonth(year, month).ifPresentOrElse(
                p -> { p.setTargetCount(target); planRepo.save(p); },
                () -> planRepo.save(PlanTarget.builder()
                        .year(year).month(month).targetCount(target).setByUser(me).build())
        );
        ra.addFlashAttribute("success", "Плановый показатель сохранён");
        return "redirect:/dashboard";
    }
}
