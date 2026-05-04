package ru.vetline.crm.dto;

import lombok.*;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardDto {
    private long totalCount;
    private long activeCount;
    private long doneCount;
    private long prevCount;
    private double growthPct;
    private long planTarget;
    private double conversionPct;
    private Map<String, Long> byStatus;
    private List<String> days;
    private List<Long> dailyCounts;
    private List<Long> dailyDoneCounts;
    private List<TopManager> topManagers;
    private String period;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopManager {
        private String fullName;
        private long total;
        private long done;
    }
}
