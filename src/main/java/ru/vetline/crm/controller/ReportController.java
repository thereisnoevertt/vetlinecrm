package ru.vetline.crm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.vetline.crm.dto.ReportData;
import ru.vetline.crm.entity.User;
import ru.vetline.crm.entity.UserRole;
import ru.vetline.crm.repository.UserRepository;
import ru.vetline.crm.service.PdfReportService;
import ru.vetline.crm.service.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** Отчёты О-01…О-05. ТЗ Ф-08. Экспорт в xlsx и pdf. */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService     reportService;
    private final PdfReportService  pdfReportService;
    private final UserRepository    userRepo;

    @GetMapping
    public String reportsPage(Model model,
                              @AuthenticationPrincipal UserDetails principal) {
        User me = resolve(principal);
        model.addAttribute("isDirector", me.getRole() == UserRole.DIRECTOR);
        model.addAttribute("managers",
                userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER));
        model.addAttribute("currentUser", me);
        return "reports/index";
    }

    /**
     * Экспорт отчёта в xlsx или pdf.
     * type: status | conversion | sources | categories | managers
     * format: xlsx (по умолчанию) | pdf
     */
    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID managerId,
            @RequestParam(required = false, defaultValue = "xlsx") String format,
            @AuthenticationPrincipal UserDetails principal) {

        User me = resolve(principal);
        // Менеджер видит только свои данные (ТЗ Ф-08)
        UUID effectiveManagerId = me.getRole() == UserRole.MANAGER ? me.getId() : managerId;

        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.atTime(LocalTime.MAX);
        boolean pdf = "pdf".equalsIgnoreCase(format);

        try {
            byte[] data;
            if (pdf) {
                ReportData rd = reportService.buildReportData(type, dtFrom, dtTo, effectiveManagerId);
                data = pdfReportService.export(rd);
            } else {
                data = switch (type) {
                    case "status"      -> reportService.reportByStatus(dtFrom, dtTo, effectiveManagerId);
                    case "conversion"  -> reportService.reportByConversion(dtFrom, dtTo, effectiveManagerId);
                    case "sources"     -> reportService.reportBySources(dtFrom, dtTo, effectiveManagerId);
                    case "categories"  -> reportService.reportByCategories(dtFrom, dtTo, effectiveManagerId);
                    case "managers"    -> reportService.reportByManagers(dtFrom, dtTo);
                    default -> throw new IllegalArgumentException("Неизвестный тип отчёта: " + type);
                };
            }

            String ext = pdf ? "pdf" : "xlsx";
            String mime = pdf ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            String filename = "vetline_" + type + "_" + from + "_" + to + "." + ext;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(mime))
                    .body(data);
        } catch (Exception e) {
            log.error("Ошибка формирования отчёта type={} format={} from={} to={} managerId={}",
                    type, format, from, to, effectiveManagerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Просмотр отчёта в браузере. */
    @GetMapping("/view/{type}")
    public String view(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID managerId,
            Model model,
            @AuthenticationPrincipal UserDetails principal) {

        User me = resolve(principal);
        UUID effectiveManagerId = me.getRole() == UserRole.MANAGER ? me.getId() : managerId;

        try {
            ReportData data = reportService.buildReportData(
                    type,
                    from.atStartOfDay(),
                    to.atTime(LocalTime.MAX),
                    effectiveManagerId);
            model.addAttribute("report", data);
            model.addAttribute("type", type);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            model.addAttribute("managerId", managerId);
            model.addAttribute("currentUser", me);
            model.addAttribute("isDirector", me.getRole() == UserRole.DIRECTOR);
            return "reports/view";
        } catch (Exception e) {
            log.error("Ошибка формирования отчёта type={} from={} to={} managerId={}",
                    type, from, to, effectiveManagerId, e);
            model.addAttribute("error", "Не удалось сформировать отчёт: " + e.getMessage());
            model.addAttribute("currentUser", me);
            model.addAttribute("isDirector", me.getRole() == UserRole.DIRECTOR);
            model.addAttribute("managers",
                    userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER));
            return "reports/index";
        }
    }

    private User resolve(UserDetails p) {
        return userRepo.findByEmailAndActiveTrue(p.getUsername()).orElseThrow();
    }
}
