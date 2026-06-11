package com.finsight.application.statement;

import com.alibaba.fastjson.JSON;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.domain.model.Statement;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.TransactionTemp;
import com.finsight.application.finance.DataQualityService;
import com.finsight.domain.port.TransactionTempRepository;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.web.api.dto.CommonResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StatementFacade {

    private static final Logger log = LoggerFactory.getLogger(StatementFacade.class);

    @Autowired
    private IStatementService statementService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private TransactionTempRepository transactionTempRepository;

    @Autowired
    private StatementProcessingService statementProcessingService;

    @Autowired
    private DataQualityService dataQualityService;

    @Autowired
    private StatementSkippedLinesService statementSkippedLinesService;

    public CollectionResult<Statement> list(int page, int rows) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Statement> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, rows);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Statement> query =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.orderByDesc(Statement::getCreatedAt);
        statementService.page(p, query);

        CollectionResult<Statement> result = new CollectionResult<>();
        result.setTotal((int) p.getTotal());
        result.setRows(p.getRecords());
        return result;
    }

    public CommonResult upload(MultipartFile file, String bankCode, String cardTypeCode, String cardNo) {
        String userName = authenticationFacade.getUserName();
        try {
            if (file == null || file.isEmpty()) {
                return CommonResult.fail("File is empty or unreadable");
            }
            log.info("statement/upload request: user={}, bankCode={}, cardTypeCode={}, cardNo={}, fileName={}",
                    userName, bankCode, cardTypeCode, cardNo, file.getOriginalFilename());

            String filename = file.getOriginalFilename();
            String suffix = filename == null ? "" : filename.trim().toLowerCase();
            List<String[]> dataRows;
            String content;
            if (suffix.endsWith(".xls") || suffix.endsWith(".xlsx")) {
                dataRows = parseExcelEasy(file);
                content = toCsv(dataRows);
            } else if (suffix.endsWith(".pdf")) {
                content = readPdf(file);
                if (StringUtils.isBlank(content)) {
                    return CommonResult.fail("File is empty or unreadable");
                }
                dataRows = parsePlainTable(content);
            } else {
                content = readText(file);
                if (StringUtils.isBlank(content)) {
                    return CommonResult.fail("File is empty or unreadable");
                }
                dataRows = parseCsv(content);
            }

            Statement statement = new Statement();
            statement.setFileName(filename);
            statement.setContent(content);
            statement.setItemCount(dataRows.size());
            statement.setStatus("PENDING");
            statement.setSource(bankCode);
            statementService.createStatement(statement, userName);

            List<Transaction> transactions = statementProcessingService.parseAndEnrichTransactions(
                    dataRows, bankCode, cardTypeCode, cardNo, statement.getId());
            statementProcessingService.savePreviewTemps(statement.getId(), transactions, userName);

            int parsedCount = transactions == null ? 0 : transactions.size();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("statementId", statement.getId());
            putImportLineStats(resp, statement, cardTypeCode, parsedCount);
            log.info("statement/upload stored preview: statementId={}, stats={}", statement.getId(), resp);
            return CommonResult.success(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return CommonResult.fail(friendlyError(e));
        }
    }

    public CommonResult importPdfLocal(String path, String bankCode, String cardTypeCode, String cardNo) {
        String userName = authenticationFacade.getUserName();
        try {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                return CommonResult.fail("file_not_found");
            }
            org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(f);
            String content;
            try {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setSortByPosition(true);
                content = stripper.getText(doc);
            } finally {
                doc.close();
            }
            List<String[]> dataRows = parsePlainTable(content);
            List<Transaction> transactions = statementProcessingService.parseAndEnrichTransactions(
                    dataRows, bankCode, cardTypeCode, cardNo, null);
            int imported = transactionService.addTransactions(transactions, userName);
            int rawPdf = dataRows.size();
            int parsedPdf = transactions == null ? 0 : transactions.size();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("path", path);
            payload.put("rows", rawPdf);
            payload.put("parsed", parsedPdf);
            payload.put("skipped", Math.max(0, rawPdf - parsedPdf));
            payload.put("imported", imported);
            return CommonResult.success(JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("import-pdf-local failed", e);
            return CommonResult.fail(friendlyError(e));
        }
    }

    public CommonResult uploadParsed(String fileName,
                                    String bankCode,
                                    String cardTypeCode,
                                    String cardNo,
                                    List<List<String>> rows) {
        String userName = authenticationFacade.getUserName();
        try {
            log.info("statement/upload-parsed request: user={}, bankCode={}, cardTypeCode={}, cardNo={}, fileName={}, rows={}",
                    userName, bankCode, cardTypeCode, cardNo, fileName, rows == null ? 0 : rows.size());
            if (rows == null || rows.isEmpty()) {
                return CommonResult.fail("empty_rows");
            }
            List<String[]> dataRows = new ArrayList<>();
            for (List<String> r : rows) {
                if (r == null) {
                    dataRows.add(new String[0]);
                    continue;
                }
                String[] arr = new String[r.size()];
                for (int i = 0; i < r.size(); i++) {
                    arr[i] = StringUtils.trimToEmpty(r.get(i));
                }
                dataRows.add(arr);
            }
            String content = toCsv(dataRows);
            Statement statement = new Statement();
            statement.setFileName(fileName);
            statement.setContent(content);
            statement.setItemCount(dataRows.size());
            statement.setStatus("PENDING");
            statement.setSource(bankCode);
            statementService.createStatement(statement, userName);

            List<Transaction> transactions = statementProcessingService.parseAndEnrichTransactions(
                    dataRows, bankCode, cardTypeCode, cardNo, statement.getId());
            log.info("statement/upload-parsed parsed transactions: {}", transactions == null ? 0 : transactions.size());
            statementProcessingService.savePreviewTemps(statement.getId(), transactions, userName);

            int parsedUp = transactions == null ? 0 : transactions.size();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("statementId", statement.getId());
            putImportLineStats(resp, statement, cardTypeCode, parsedUp);
            return CommonResult.success(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("Upload parsed failed", e);
            return CommonResult.fail(friendlyError(e));
        }
    }

    public List<SkippedImportRow> skippedLines(String statementId, String cardTypeCode) {
        if (StringUtils.isBlank(statementId)) {
            return new ArrayList<>();
        }
        try {
            Statement statement = statementService.getById(statementId);
            if (statement == null) {
                return new ArrayList<>();
            }
            List<SkippedImportRow> skipped = statementSkippedLinesService.analyze(
                    statement, StringUtils.defaultIfBlank(cardTypeCode, "debit"));
            log.info("statement/skipped-lines: statementId={}, size={}", statementId, skipped.size());
            return skipped;
        } catch (Exception e) {
            log.error("Skipped lines fetch failed", e);
            return new ArrayList<>();
        }
    }

    public List<TransactionTemp> preview(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return new ArrayList<>();
        }
        try {
            List<TransactionTemp> list = transactionTempRepository.findByStatementId(statementId);
            if (list != null && !list.isEmpty()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                Set<String> duplicateIds = new HashSet<>();
                try {
                    List<String> dupIds = dataQualityService.duplicatePreviewTempIds(statementId);
                    if (dupIds != null) {
                        duplicateIds.addAll(dupIds);
                    }
                } catch (Exception e) {
                    log.warn("statement/preview dedup check failed: statementId={}", statementId, e);
                }
                for (TransactionTemp t : list) {
                    try {
                        String ds = t.getTransactionDate() == null ? "" : sdf.format(t.getTransactionDate());
                        String ts = StringUtils.trimToEmpty(t.getTransactionTime());
                        t.setTransactionDateTime(StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
                    } catch (Exception ignore) {
                    }
                    t.setPossibleDuplicate(duplicateIds.contains(t.getId()));
                }
            }
            log.info("statement/preview fetch: statementId={}, size={}", statementId, list == null ? 0 : list.size());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.error("Preview fetch failed", e);
            return new ArrayList<>();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public CommonResult commit(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return CommonResult.fail("Invalid Statement ID");
        }
        try {
            List<TransactionTemp> temps = transactionTempRepository.findByStatementId(statementId);
            if (temps == null || temps.isEmpty()) {
                return CommonResult.fail("No transactions found to commit");
            }

            Set<String> duplicateIds = new HashSet<>();
            try {
                List<String> dupIds = dataQualityService.duplicatePreviewTempIds(statementId);
                if (dupIds != null) {
                    duplicateIds.addAll(dupIds);
                }
            } catch (Exception e) {
                log.warn("statement/commit dedup check failed: statementId={}", statementId, e);
            }

            List<Transaction> transactions = new ArrayList<>();
            int skippedDuplicates = 0;
            for (TransactionTemp temp : temps) {
                if (temp == null) {
                    continue;
                }
                if (duplicateIds.contains(temp.getId())) {
                    skippedDuplicates++;
                    continue;
                }
                Transaction t = new Transaction();
                org.springframework.beans.BeanUtils.copyProperties(temp, t);
                transactions.add(t);
            }
            if (transactions.isEmpty()) {
                return CommonResult.fail("No new transactions to commit (all rows flagged as possible duplicates)");
            }

            String userName = authenticationFacade.getUserName();
            int imported = transactionService.importTransactionsStrict(transactions, userName);

            transactionTempRepository.softDeleteByStatementId(statementId);

            Statement statement = statementService.getById(statementId);
            if (statement != null) {
                statement.setStatus("COMMITTED");
                statementService.updateById(statement);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("statementId", statementId);
            payload.put("total", temps.size());
            payload.put("imported", imported);
            payload.put("skippedDuplicates", skippedDuplicates);
            payload.put("failed", 0);
            transactionService.invalidateHomeSummaryCache();
            return CommonResult.success(JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("Commit failed", e);
            return CommonResult.fail("Import failed — no rows were saved. " + friendlyError(e));
        }
    }

    public List<List<String>> exportData(String statementId) {
        List<TransactionTemp> list = transactionTempRepository.findByStatementId(statementId);
        List<List<String>> data = new ArrayList<>();
        data.add(java.util.Arrays.asList("Card Name", "Posting Date", "Narration", "Currency", "Amount", "Category", "Remarks"));
        if (list != null) {
            for (TransactionTemp t : list) {
                String date = t.getTransactionDate() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate());
                String cat = StringUtils.isNotBlank(t.getConsumeName()) ? t.getConsumeName() : StringUtils.trimToEmpty(t.getConsumeCode());
                data.add(java.util.Arrays.asList(
                        StringUtils.trimToEmpty(t.getBankCardName()),
                        date,
                        StringUtils.trimToEmpty(t.getTransactionDesc()),
                        StringUtils.trimToEmpty(t.getBalanceCurrency()),
                        t.getBalanceMoney() == null ? "" : String.valueOf(t.getBalanceMoney()),
                        cat,
                        StringUtils.trimToEmpty(t.getDemoArea())
                ));
            }
        }
        return data;
    }

    private String friendlyError(Exception e) {
        String msg = e == null ? "" : String.valueOf(e.getMessage());
        String lower = msg == null ? "" : msg.toLowerCase();
        Throwable cause = e;
        while (cause != null) {
            String cm = String.valueOf(cause.getMessage());
            String cl = cm == null ? "" : cm.toLowerCase();
            lower = lower + " " + cl;
            cause = cause.getCause();
        }
        if (e instanceof org.springframework.jdbc.CannotGetJdbcConnectionException
                || lower.contains("cannot get jdbc connection")
                || lower.contains("communications link failure")
                || lower.contains("failed to obtain jdbc connection")
                || (lower.contains("jdbc") && lower.contains("connect"))) {
            return "数据库连接失败，请确认数据库服务已启动，并检查网络与配置。";
        }
        if (lower.contains("access denied for user") || lower.contains("authentication")) {
            return "数据库认证失败，请检查数据库用户名和密码是否正确。";
        }
        if (lower.contains("unknown database")) {
            return "数据库不存在，请检查数据库名称配置。";
        }
        return "系统出现错误";
    }

    private void putImportLineStats(Map<String, Object> resp, Statement statement, String cardTypeCode, int parsedCount) {
        ImportLineStats stats = statementSkippedLinesService.summarize(statement, cardTypeCode, parsedCount);
        resp.put("rows", stats.lines());
        resp.put("parsed", stats.transactions());
        resp.put("skipped", stats.skipped());
        resp.put("ignored", stats.ignored());
        resp.put("linked", stats.linked());
    }

    private String readText(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String content = new String(bytes, "UTF-8");
        if (content.contains("交易日") || content.contains("入账日")) return content;
        return new String(bytes, "GBK");
    }

    private List<String[]> parseCsv(String content) {
        String[] lines = content.split("\n");
        List<String[]> rows = new ArrayList<>();
        String delimiter = content.contains(",") ? "," : "\\t";
        if (content.contains(";")) delimiter = ";";
        for (String line : lines) {
            line = StringUtils.trim(line);
            if (StringUtils.isBlank(line)) continue;
            String[] cols;
            if (",".equals(delimiter)) {
                cols = line.split("\\s*,\\s*");
            } else {
                cols = line.split(delimiter);
            }
            rows.add(cols);
        }
        return rows;
    }

    private String readPdf(MultipartFile file) throws Exception {
        try (java.io.InputStream in = file.getInputStream()) {
            org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(in);
            try {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(doc);
            } finally {
                doc.close();
            }
        }
    }

    private List<String[]> parsePlainTable(String content) {
        String[] lines = content.split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            String ln = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(ln)) continue;
            String[] cols = ln.split("\\s+");
            rows.add(cols);
        }
        return rows;
    }

    private List<String[]> parseExcelEasy(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (java.io.InputStream in = file.getInputStream()) {
            List<java.util.Map<Integer, Object>> list = com.alibaba.excel.EasyExcel.read(in).sheet().doReadSync();
            for (java.util.Map<Integer, Object> m : list) {
                if (m == null || m.isEmpty()) {
                    rows.add(new String[0]);
                    continue;
                }
                int max = java.util.Collections.max(m.keySet());
                int len = Math.max(max + 1, 20);
                String[] cols = new String[len];
                for (java.util.Map.Entry<Integer, Object> e : m.entrySet()) {
                    int idx = e.getKey() == null ? 0 : e.getKey();
                    Object val = e.getValue();
                    String v = "";
                    if (val != null) {
                        if (val instanceof java.util.Date) {
                            v = new java.text.SimpleDateFormat("yyyyMMdd").format((java.util.Date) val);
                        } else {
                            v = StringUtils.trim(String.valueOf(val));
                        }
                    }
                    if (idx < len) {
                        cols[idx] = v;
                    }
                }
                rows.add(cols);
            }
        }
        return rows;
    }

    private String toCsv(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] r : rows) {
            if (r == null) {
                sb.append("\n");
                continue;
            }
            for (int i = 0; i < r.length; i++) {
                if (i > 0) sb.append(",");
                String v = r[i] == null ? "" : r[i].replaceAll("[\\r\\n]+", " ").trim();
                sb.append(v);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

