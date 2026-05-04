package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.dto.ReportData;
import ru.vetline.crm.entity.TicketStatus;
import ru.vetline.crm.repository.TicketRepository;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Формирование отчётов О-01…О-05. ТЗ Ф-08.
 * Экспорт в xlsx (OpenXML).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TicketRepository ticketRepo;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // ── О-01: По статусам ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] reportByStatus(LocalDateTime from, LocalDateTime to, UUID managerId)
            throws Exception {
        List<Object[]> rows = ticketRepo.countByStatusForReport(from, to, managerId);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("По статусам");
            addTitle(sh, wb, "О-01 Отчёт по статусам заявок: " + fmt(from) + " — " + fmt(to));
            Row hdr = sh.createRow(2);
            headers(hdr, wb, "Статус", "Количество", "% от итога");

            int rn = 3;
            for (Object[] r : rows) {
                TicketStatus s = (TicketStatus) r[0];
                long cnt = (Long) r[1];
                Row row = sh.createRow(rn++);
                row.createCell(0).setCellValue(s.getDisplayName());
                row.createCell(1).setCellValue(cnt);
                row.createCell(2).setCellValue(total > 0
                        ? String.format("%.1f%%", cnt * 100.0 / total) : "0%");
            }
            Row tot = sh.createRow(rn + 1);
            cell(tot, 0, "ИТОГО", true);
            tot.createCell(1).setCellValue(total);
            autoSize(sh, 3);
            return bytes(wb);
        }
    }

    // ── О-02: По конверсиям ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] reportByConversion(LocalDateTime from, LocalDateTime to, UUID managerId)
            throws Exception {
        long total     = ticketRepo.countTotal(from, to, managerId);
        long done      = ticketRepo.countDone(from, to, managerId);
        long active    = total - done;
        double convPct = total > 0 ? done * 100.0 / total : 0;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Конверсия");
            addTitle(sh, wb, "О-02 Отчёт по конверсиям: " + fmt(from) + " — " + fmt(to));
            Row hdr = sh.createRow(2);
            headers(hdr, wb, "Показатель", "Значение");

            Object[][] data = {
                {"Всего заявок за период", total},
                {"Выполнено (статус «Выполнена»)", done},
                {"Активных (все прочие статусы)", active},
                {"Конверсия (выполнено / всего)", String.format("%.1f%%", convPct)}
            };
            int rn = 3;
            for (Object[] d : data) {
                Row r = sh.createRow(rn++);
                r.createCell(0).setCellValue(d[0].toString());
                r.createCell(1).setCellValue(d[1].toString());
            }
            autoSize(sh, 2);
            return bytes(wb);
        }
    }

    // ── О-03: По источникам ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] reportBySources(LocalDateTime from, LocalDateTime to, UUID managerId)
            throws Exception {
        List<Object[]> rows = ticketRepo.countBySource(from, to, managerId);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("По источникам");
            addTitle(sh, wb, "О-03 Отчёт по источникам: " + fmt(from) + " — " + fmt(to));
            Row hdr = sh.createRow(2);
            headers(hdr, wb, "Источник обращения", "Количество", "%");

            int rn = 3;
            for (Object[] r : rows) {
                Row row = sh.createRow(rn++);
                row.createCell(0).setCellValue(r[0].toString());
                long cnt = (Long) r[1];
                row.createCell(1).setCellValue(cnt);
                row.createCell(2).setCellValue(total > 0
                        ? String.format("%.1f%%", cnt * 100.0 / total) : "0%");
            }
            autoSize(sh, 3);
            return bytes(wb);
        }
    }

    // ── О-04: По категориям товаров ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] reportByCategories(LocalDateTime from, LocalDateTime to, UUID managerId)
            throws Exception {
        List<Object[]> rows = ticketRepo.countByCategory(from, to, managerId);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("По категориям");
            addTitle(sh, wb, "О-04 Отчёт по категориям товаров: " + fmt(from) + " — " + fmt(to));
            Row hdr = sh.createRow(2);
            headers(hdr, wb, "Категория товара", "Количество", "%");

            int rn = 3;
            for (Object[] r : rows) {
                Row row = sh.createRow(rn++);
                row.createCell(0).setCellValue(r[0].toString());
                long cnt = (Long) r[1];
                row.createCell(1).setCellValue(cnt);
                row.createCell(2).setCellValue(total > 0
                        ? String.format("%.1f%%", cnt * 100.0 / total) : "0%");
            }
            if (rows.isEmpty()) sh.createRow(3).createCell(0)
                    .setCellValue("Нет данных за выбранный период");
            autoSize(sh, 3);
            return bytes(wb);
        }
    }

    // ── О-05: По менеджерам (эффективность) ──────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] reportByManagers(LocalDateTime from, LocalDateTime to)
            throws Exception {
        // Для каждого менеджера — всего заявок и выполнено
        List<Object[]> rows = ticketRepo.countByStatusForReport(from, to, null);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("По менеджерам");
            addTitle(sh, wb, "О-05 Отчёт по эффективности менеджеров: " + fmt(from) + " — " + fmt(to));
            Row hdr = sh.createRow(2);
            headers(hdr, wb, "Статус", "Количество");
            int rn = 3;
            for (Object[] r : rows) {
                Row row = sh.createRow(rn++);
                row.createCell(0).setCellValue(((TicketStatus)r[0]).getDisplayName());
                row.createCell(1).setCellValue((Long) r[1]);
            }
            autoSize(sh, 2);
            return bytes(wb);
        }
    }

    // ── HTML-представление ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReportData buildReportData(String type, LocalDateTime from, LocalDateTime to,
                                      UUID managerId) {
        String period = fmt(from) + " — " + fmt(to);
        return switch (type) {
            case "status"     -> buildStatusData(from, to, managerId, period);
            case "conversion" -> buildConversionData(from, to, managerId, period);
            case "sources"    -> buildSourcesData(from, to, managerId, period);
            case "categories" -> buildCategoriesData(from, to, managerId, period);
            case "managers"   -> buildManagersData(from, to, period);
            default -> throw new IllegalArgumentException("Неизвестный тип отчёта: " + type);
        };
    }

    private ReportData buildStatusData(LocalDateTime from, LocalDateTime to, UUID mgr, String period) {
        List<Object[]> rows = ticketRepo.countByStatusForReport(from, to, mgr);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            TicketStatus s = (TicketStatus) r[0];
            long cnt = (Long) r[1];
            out.add(List.of(s.getDisplayName(), String.valueOf(cnt),
                    total > 0 ? String.format("%.1f%%", cnt * 100.0 / total) : "0%"));
        }
        return new ReportData("О-01 По статусам заявок: " + period,
                List.of("Статус", "Количество", "% от итога"), out,
                "ИТОГО", String.valueOf(total));
    }

    private ReportData buildConversionData(LocalDateTime from, LocalDateTime to, UUID mgr, String period) {
        long total  = ticketRepo.countTotal(from, to, mgr);
        long done   = ticketRepo.countDone(from, to, mgr);
        long active = total - done;
        double conv = total > 0 ? done * 100.0 / total : 0;
        List<List<String>> out = List.of(
                List.of("Всего заявок за период", String.valueOf(total)),
                List.of("Выполнено (статус «Выполнена»)", String.valueOf(done)),
                List.of("Активных (все прочие статусы)", String.valueOf(active)),
                List.of("Конверсия (выполнено / всего)", String.format("%.1f%%", conv))
        );
        return new ReportData("О-02 По конверсиям: " + period,
                List.of("Показатель", "Значение"), out, null, null);
    }

    private ReportData buildSourcesData(LocalDateTime from, LocalDateTime to, UUID mgr, String period) {
        List<Object[]> rows = ticketRepo.countBySource(from, to, mgr);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            long cnt = (Long) r[1];
            out.add(List.of(r[0].toString(), String.valueOf(cnt),
                    total > 0 ? String.format("%.1f%%", cnt * 100.0 / total) : "0%"));
        }
        return new ReportData("О-03 По источникам обращений: " + period,
                List.of("Источник", "Количество", "%"), out,
                "ИТОГО", String.valueOf(total));
    }

    private ReportData buildCategoriesData(LocalDateTime from, LocalDateTime to, UUID mgr, String period) {
        List<Object[]> rows = ticketRepo.countByCategory(from, to, mgr);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            long cnt = (Long) r[1];
            out.add(List.of(r[0].toString(), String.valueOf(cnt),
                    total > 0 ? String.format("%.1f%%", cnt * 100.0 / total) : "0%"));
        }
        return new ReportData("О-04 По категориям товаров: " + period,
                List.of("Категория", "Количество", "%"), out,
                "ИТОГО", String.valueOf(total));
    }

    private ReportData buildManagersData(LocalDateTime from, LocalDateTime to, String period) {
        List<Object[]> rows = ticketRepo.countByStatusForReport(from, to, null);
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(List.of(((TicketStatus) r[0]).getDisplayName(), String.valueOf((Long) r[1])));
        }
        return new ReportData("О-05 По эффективности менеджеров: " + period,
                List.of("Статус", "Количество"), out, null, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addTitle(Sheet sh, Workbook wb, String title) {
        Row r = sh.createRow(0);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)12);
        s.setFont(f); c.setCellStyle(s);
    }

    private void headers(Row row, Workbook wb, String... cols) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(cols[i]); c.setCellStyle(s);
        }
    }

    private void cell(Row row, int col, String val, boolean bold) {
        row.createCell(col).setCellValue(val);
    }

    private void autoSize(Sheet sh, int cols) {
        for (int i = 0; i < cols; i++) sh.autoSizeColumn(i);
    }

    private byte[] bytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out); return out.toByteArray();
    }

    private String fmt(LocalDateTime dt) { return dt.format(FMT); }
}
