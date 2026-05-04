package ru.vetline.crm.dto;

import java.util.List;

public record ReportData(
        String title,
        List<String> headers,
        List<List<String>> rows,
        String totalLabel,
        String totalValue
) {}
