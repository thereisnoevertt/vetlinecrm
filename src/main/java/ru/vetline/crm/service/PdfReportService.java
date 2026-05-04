package ru.vetline.crm.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vetline.crm.dto.ReportData;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * PDF-экспорт отчётов с поддержкой кириллицы (OpenPDF).
 * Шрифт ищется в системных каталогах; если не найден — берётся встроенный Helvetica
 * с кодировкой CP1251 (читабельно для большинства просмотрщиков).
 */
@Service
@Slf4j
public class PdfReportService {

    private static final String[] FONT_CANDIDATES = {
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/calibri.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
        "/Library/Fonts/Arial.ttf"
    };

    private final BaseFont baseFont = resolveFont();
    private final Font titleFont   = new Font(baseFont, 14, Font.BOLD, new Color(15, 23, 42));
    private final Font headerFont  = new Font(baseFont, 11, Font.BOLD, Color.WHITE);
    private final Font cellFont    = new Font(baseFont, 10, Font.NORMAL, new Color(51, 65, 85));
    private final Font totalFont   = new Font(baseFont, 11, Font.BOLD, new Color(15, 23, 42));
    private final Font footerFont  = new Font(baseFont, 8, Font.ITALIC, new Color(148, 163, 184));

    public byte[] export(ReportData data) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Paragraph title = new Paragraph(data.title(), titleFont);
        title.setSpacingAfter(14f);
        doc.add(title);

        if (data.rows().isEmpty()) {
            doc.add(new Paragraph("За указанный период данных нет.", cellFont));
        } else {
            PdfPTable table = new PdfPTable(data.headers().size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(4f);

            for (String h : data.headers()) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setBackgroundColor(new Color(37, 99, 235));
                c.setPadding(7f);
                c.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(c);
            }

            boolean stripe = false;
            for (List<String> row : data.rows()) {
                Color bg = stripe ? new Color(248, 250, 252) : Color.WHITE;
                stripe = !stripe;
                for (String value : row) {
                    PdfPCell c = new PdfPCell(new Phrase(value, cellFont));
                    c.setBackgroundColor(bg);
                    c.setPadding(6f);
                    table.addCell(c);
                }
            }

            if (data.totalLabel() != null) {
                int span = data.headers().size() - 1;
                PdfPCell label = new PdfPCell(new Phrase(data.totalLabel(), totalFont));
                label.setColspan(span);
                label.setPadding(7f);
                label.setBackgroundColor(new Color(241, 245, 249));
                label.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(label);
                PdfPCell value = new PdfPCell(new Phrase(data.totalValue(), totalFont));
                value.setPadding(7f);
                value.setBackgroundColor(new Color(241, 245, 249));
                table.addCell(value);
            }

            doc.add(table);
        }

        Paragraph footer = new Paragraph(
                "Сформировано в ИС «Ветлайн ВК» — " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                footerFont);
        footer.setSpacingBefore(20f);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private BaseFont resolveFont() {
        for (String path : FONT_CANDIDATES) {
            try {
                if (new File(path).isFile()) {
                    BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.info("PDF: используем шрифт {}", path);
                    return bf;
                }
            } catch (Exception ignored) {}
        }
        try {
            log.warn("PDF: системный TTF-шрифт не найден, fallback Helvetica/Cp1251");
            return BaseFont.createFont(BaseFont.HELVETICA, "Cp1251", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось инициализировать шрифт PDF", e);
        }
    }
}
