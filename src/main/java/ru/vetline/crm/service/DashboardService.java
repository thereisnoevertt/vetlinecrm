package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.dto.DashboardDto;
import ru.vetline.crm.entity.TicketStatus;
import ru.vetline.crm.repository.PlanTargetRepository;
import ru.vetline.crm.repository.TicketRepository;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Дашборд генерального директора. ТЗ Ф-09. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository     ticketRepo;
    private final PlanTargetRepository planRepo;

    @Transactional(readOnly = true)
    public DashboardDto build(String period) {
        LocalDateTime[] range = range(period);
        LocalDateTime from = range[0], to = range[1];

        // ── KPI ──────────────────────────────────────────────────────────────
        long total  = ticketRepo.countByCreatedAtBetweenAndArchived(from, to, false);
        long active = ticketRepo.countActive();
        long done   = ticketRepo.countDone(from, to, null);
        double convPct = total > 0 ? done * 100.0 / total : 0;

        // ── Предыдущий период (рост) ──────────────────────────────────────────
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        long prev = ticketRepo.countByCreatedAtBetweenAndArchived(
                from.minusDays(days), from, false);
        double growth = prev > 0 ? (total - prev) * 100.0 / prev : 0;

        // ── Воронка по статусам ───────────────────────────────────────────────
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values()) byStatus.put(s.getDisplayName(), 0L);
        ticketRepo.countByStatusBetween(from, to)
                  .forEach(r -> byStatus.put(((TicketStatus)r[0]).getDisplayName(), (Long)r[1]));

        // ── Динамика по дням (новые и закрытые) ───────────────────────────────
        List<String> days2  = new ArrayList<>();
        List<Long>   counts = new ArrayList<>();
        Map<String, Long> doneByDay = new LinkedHashMap<>();
        ticketRepo.dailyCountsBetween(from, to).forEach(r -> {
            days2.add(r[0].toString());
            counts.add((Long) r[1]);
        });
        ticketRepo.dailyDoneCountsBetween(from, to)
                .forEach(r -> doneByDay.put(r[0].toString(), (Long) r[1]));
        List<Long> doneCounts = new ArrayList<>();
        for (String d : days2) doneCounts.add(doneByDay.getOrDefault(d, 0L));

        // ── План / Факт ───────────────────────────────────────────────────────
        int year  = from.getYear();
        int month = "month".equals(period) ? from.getMonthValue() : 0;
        long plan = planRepo.findByYearAndMonth(year, month > 0 ? month : null)
                            .map(p -> (long) p.getTargetCount()).orElse(0L);

        // ── Топ менеджеров ────────────────────────────────────────────────────
        List<DashboardDto.TopManager> top = new ArrayList<>();
        for (Object[] r : ticketRepo.topManagersBetween(from, to)) {
            long t = ((Number) r[1]).longValue();
            long d = r[2] == null ? 0 : ((Number) r[2]).longValue();
            top.add(DashboardDto.TopManager.builder()
                    .fullName((String) r[0]).total(t).done(d).build());
            if (top.size() >= 5) break;
        }

        return DashboardDto.builder()
                .totalCount(total).activeCount(active).doneCount(done)
                .prevCount(prev).growthPct(growth)
                .planTarget(plan).conversionPct(convPct)
                .byStatus(byStatus)
                .days(days2).dailyCounts(counts).dailyDoneCounts(doneCounts)
                .topManagers(top)
                .period(period).build();
    }

    private LocalDateTime[] range(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "week" -> new LocalDateTime[]{
                now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0), now
            };
            case "quarter" -> {
                int m = now.getMonthValue();
                int startMonth = ((m - 1) / 3) * 3 + 1;
                yield new LocalDateTime[]{
                    now.withMonth(startMonth).with(TemporalAdjusters.firstDayOfMonth())
                       .withHour(0).withMinute(0).withSecond(0).withNano(0), now
                };
            }
            case "year" -> new LocalDateTime[]{
                now.with(TemporalAdjusters.firstDayOfYear())
                   .withHour(0).withMinute(0).withSecond(0).withNano(0), now
            };
            default -> new LocalDateTime[]{
                now.with(TemporalAdjusters.firstDayOfMonth())
                   .withHour(0).withMinute(0).withSecond(0).withNano(0), now
            };
        };
    }
}
