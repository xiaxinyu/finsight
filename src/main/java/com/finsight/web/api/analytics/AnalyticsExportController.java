package com.finsight.web.api.analytics;

import com.finsight.application.analytics.AnalyticsExportService;
import com.finsight.common.exception.AppServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsExportController {

    private final AnalyticsExportService analyticsExportService;

    public AnalyticsExportController(AnalyticsExportService analyticsExportService) {
        this.analyticsExportService = analyticsExportService;
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(
            @RequestParam(value = "format", defaultValue = "csv") String format,
            @RequestParam(value = "transactionDateStartStr", required = false) String startStr,
            @RequestParam(value = "transactionDateEndStr", required = false) String endStr,
            @RequestParam(value = "limit", defaultValue = "5000") int limit) throws AppServiceException {
        List<Map<String, Object>> rows = analyticsExportService.exportRows(startStr, endStr, limit);
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(rows);
        }
        String csv = analyticsExportService.toCsv(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions-analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
