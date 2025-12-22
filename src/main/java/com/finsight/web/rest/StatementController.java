package com.finsight.web.rest;

import com.finsight.application.IStatementService;
import com.finsight.application.ITransactionService;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.importer.StatementImporterFactory;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Statement;
import com.finsight.domain.model.Transaction;
import com.finsight.web.rest.model.CommonResult;
import com.finsight.web.rest.model.ResultCode;
import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/statement")
public class StatementController {
    private static final Logger log = LoggerFactory.getLogger(StatementController.class);

    @Autowired
    private IStatementService statementService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private com.finsight.application.ITransactionTempService transactionTempService;

    // Temporary storage for preview before commit
    // private static final ConcurrentHashMap<String, List<Transaction>> TEMP_STORE = new ConcurrentHashMap<>();

    @GetMapping("/upload.html")
    public String uploadPage() {
        return "account/statement/upload";
    }
    
    @GetMapping("/list.html")
    public String listPage() {
        return "account/statement/list";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public com.finsight.web.rest.model.CollectionResult<Statement> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                       @RequestParam(value = "rows", defaultValue = "20") int rows) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Statement> p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, rows);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Statement> query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.orderByDesc(Statement::getCreatetime);
        statementService.page(p, query);
        
        com.finsight.web.rest.model.CollectionResult<Statement> result = new com.finsight.web.rest.model.CollectionResult<>();
        result.setTotal((int)p.getTotal());
        result.setRows(p.getRecords());
        return result;
    }

    @PostMapping("/upload")
    @ResponseBody
    public CommonResult upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "bankCode", required = false) String bankCode,
                               @RequestParam(value = "cardTypeCode", required = false) String cardTypeCode,
                               @RequestParam(value = "cardNo", required = false) String cardNo,
                               HttpServletRequest request) {
        String userName = authenticationFacade.getUserName();
        try {
            log.info("statement/upload request: user={}, bankCode={}, cardTypeCode={}, cardNo={}, fileName={}", userName, bankCode, cardTypeCode, cardNo, file == null ? null : file.getOriginalFilename());
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
                    return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "File is empty or unreadable");
                }
                dataRows = parsePlainTable(content);
            } else {
                content = readText(file);
                if (StringUtils.isBlank(content)) {
                    return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "File is empty or unreadable");
                }
                dataRows = parseCsv(content);
            }

            // 3. Create Statement Record
            Statement statement = new Statement();
            statement.setFileName(filename);
            statement.setContent(content);
            statement.setItemCount(dataRows.size());
            statement.setStatus("PENDING");
            statement.setSource(bankCode);
            statementService.createStatement(statement, userName);

            // 4. Parse Transactions
            List<Transaction> transactions = StatementImporterFactory
                    .get(StringUtils.trimToEmpty(bankCode), StringUtils.trimToEmpty(cardTypeCode))
                    .parse(dataRows, bankCode, cardTypeCode, cardNo);

            // 5. Enrich Transactions
            BankCard bankCard = bankCardService.getByBankTypeNo(StringUtils.trimToEmpty(bankCode), StringUtils.trimToEmpty(cardTypeCode), StringUtils.trimToEmpty(cardNo));
            if (bankCard == null && StringUtils.isNotBlank(cardNo)) {
                bankCard = bankCardService.getByCardNo(StringUtils.trimToEmpty(cardNo));
            }
            String bankCardId = bankCard == null ? null : bankCard.getId();
            String bankCardName = bankCard == null ? null : bankCard.getCardName();

            for (Transaction t : transactions) {
                if (StringUtils.isNotBlank(bankCardId)) {
                    t.setBankCardId(bankCardId);
                    t.setBankCardName(bankCardName);
                }
                // Classification
                ClassificationService.Result r = classificationService.classify(t.getTransactionDesc(), bankCode, cardTypeCode);
                if (r != null) {
                    t.setConsumeCode(r.id);
                    t.setConsumeName(r.name);
                }
                // Link to Statement
                 t.setRecordID(statement.getId()); 
            }

            // 6. Store in DB (Temp Table)
            // Clear old if exists
            transactionTempService.deleteByStatementId(statement.getId());
            // Add new
            if (transactions != null && !transactions.isEmpty()) {
                List<com.finsight.domain.model.TransactionTemp> temps = new java.util.ArrayList<>();
                for (Transaction t : transactions) {
                    com.finsight.domain.model.TransactionTemp temp = new com.finsight.domain.model.TransactionTemp();
                    org.springframework.beans.BeanUtils.copyProperties(t, temp);
                    temp.setId(java.util.UUID.randomUUID().toString()); // Generate ID for temp
                    temp.setCreateUser(userName);
                    temp.setUpdateUser(userName);
                    try{
                        String ds = t.getTransactionDate() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate());
                        String ts = org.apache.commons.lang3.StringUtils.trimToEmpty(t.getTransactionTime());
                        temp.setTransactionDateTime(org.apache.commons.lang3.StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
                    }catch(Exception ignore){}
                    temps.add(temp);
                }
                transactionTempService.saveBatch(temps);
            }
            
            log.info("statement/upload stored preview: statementId={}, rows={}, parsed={}", statement.getId(), dataRows.size(), transactions == null ? 0 : transactions.size());

            java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
            resp.put("statementId", statement.getId());
            resp.put("rows", dataRows.size());
            resp.put("parsed", transactions == null ? 0 : transactions.size());
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), JSON.toJSONString(resp));

        } catch (Exception e) {
            log.error("Upload failed", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), friendlyError(e));
        }
    }

    @PostMapping("/import-pdf-local")
    @ResponseBody
    public CommonResult importPdfLocal(@RequestParam("path") String path,
                                       @RequestParam(value = "bankCode", defaultValue = "CMB") String bankCode,
                                       @RequestParam(value = "cardTypeCode", defaultValue = "debit") String cardTypeCode,
                                       @RequestParam(value = "cardNo", required = false) String cardNo){
        String userName = authenticationFacade.getUserName();
        try{
            File f = new File(path);
            if(!f.exists() || !f.isFile()){
                return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "file_not_found");
            }
            org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(f);
            String content;
            try{
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setSortByPosition(true);
                content = stripper.getText(doc);
            } finally {
                doc.close();
            }
            List<String[]> dataRows = parsePlainTable(content);
            List<Transaction> transactions = StatementImporterFactory
                    .get(org.apache.commons.lang3.StringUtils.trimToEmpty(bankCode), org.apache.commons.lang3.StringUtils.trimToEmpty(cardTypeCode))
                    .parse(dataRows, bankCode, cardTypeCode, cardNo);
            BankCard bankCard = bankCardService.getByBankTypeNo(org.apache.commons.lang3.StringUtils.trimToEmpty(bankCode), org.apache.commons.lang3.StringUtils.trimToEmpty(cardTypeCode), org.apache.commons.lang3.StringUtils.trimToEmpty(cardNo));
            if (bankCard == null && org.apache.commons.lang3.StringUtils.isNotBlank(cardNo)) {
                bankCard = bankCardService.getByCardNo(org.apache.commons.lang3.StringUtils.trimToEmpty(cardNo));
            }
            String bankCardId = bankCard == null ? null : bankCard.getId();
            String bankCardName = bankCard == null ? null : bankCard.getCardName();
            for (Transaction t : transactions) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(bankCardId)) {
                    t.setBankCardId(bankCardId);
                    t.setBankCardName(bankCardName);
                }
                ClassificationService.Result r = classificationService.classify(t.getTransactionDesc(), bankCode, cardTypeCode);
                if (r != null) {
                    t.setConsumeCode(r.id);
                    t.setConsumeName(r.name);
                }
            }
            int imported = transactionService.addTransactions(transactions, userName);
            java.util.Map<String,Object> payload = new java.util.LinkedHashMap<>();
            payload.put("path", path);
            payload.put("rows", dataRows.size());
            payload.put("parsed", transactions == null ? 0 : transactions.size());
            payload.put("imported", imported);
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), com.alibaba.fastjson.JSON.toJSONString(payload));
        }catch(Exception e){
            log.error("import-pdf-local failed", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), friendlyError(e));
        }
    }

    @PostMapping("/upload-parsed")
    @ResponseBody
    public CommonResult uploadParsed(@RequestBody ParsedPayload payload){
        String userName = authenticationFacade.getUserName();
        try{
            log.info("statement/upload-parsed request: user={}, bankCode={}, cardTypeCode={}, cardNo={}, fileName={}, rows={}", userName, payload == null ? null : payload.bankCode, payload == null ? null : payload.cardTypeCode, payload == null ? null : payload.cardNo, payload == null ? null : payload.fileName, payload == null || payload.rows == null ? 0 : payload.rows.size());
            if(payload == null || payload.rows == null || payload.rows.isEmpty()){
                return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "empty_rows");
            }
            java.util.List<String[]> dataRows = new java.util.ArrayList<>();
            for(java.util.List<String> r : payload.rows){
                if(r == null){ dataRows.add(new String[0]); continue; }
                String[] arr = new String[r.size()];
                for(int i=0;i<r.size();i++){ arr[i] = org.apache.commons.lang3.StringUtils.trimToEmpty(r.get(i)); }
                dataRows.add(arr);
            }
            String content = toCsv(dataRows);
            Statement statement = new Statement();
            statement.setFileName(payload.fileName);
            statement.setContent(content);
            statement.setItemCount(dataRows.size());
            statement.setStatus("PENDING");
            statement.setSource(payload.bankCode);
            statementService.createStatement(statement, userName);

            java.util.List<Transaction> transactions = StatementImporterFactory
                    .get(org.apache.commons.lang3.StringUtils.trimToEmpty(payload.bankCode), org.apache.commons.lang3.StringUtils.trimToEmpty(payload.cardTypeCode))
                    .parse(dataRows, payload.bankCode, payload.cardTypeCode, payload.cardNo);
            log.info("statement/upload-parsed parsed transactions: {}", transactions == null ? 0 : transactions.size());

            BankCard bankCard = bankCardService.getByBankTypeNo(org.apache.commons.lang3.StringUtils.trimToEmpty(payload.bankCode), org.apache.commons.lang3.StringUtils.trimToEmpty(payload.cardTypeCode), org.apache.commons.lang3.StringUtils.trimToEmpty(payload.cardNo));
            if (bankCard == null && org.apache.commons.lang3.StringUtils.isNotBlank(payload.cardNo)) {
                bankCard = bankCardService.getByCardNo(org.apache.commons.lang3.StringUtils.trimToEmpty(payload.cardNo));
            }
            String bankCardId = bankCard == null ? null : bankCard.getId();
            String bankCardName = bankCard == null ? null : bankCard.getCardName();

            for (Transaction t : transactions) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(bankCardId)) {
                    t.setBankCardId(bankCardId);
                    t.setBankCardName(bankCardName);
                }
                ClassificationService.Result r = classificationService.classify(t.getTransactionDesc(), payload.bankCode, payload.cardTypeCode);
                if (r != null) {
                    t.setConsumeCode(r.id);
                    t.setConsumeName(r.name);
                }
                t.setRecordID(statement.getId());
            }
            // Store in DB (Temp Table)
            transactionTempService.deleteByStatementId(statement.getId());
            if (transactions != null && !transactions.isEmpty()) {
                List<com.finsight.domain.model.TransactionTemp> temps = new java.util.ArrayList<>();
                for (Transaction t : transactions) {
                    com.finsight.domain.model.TransactionTemp temp = new com.finsight.domain.model.TransactionTemp();
                    org.springframework.beans.BeanUtils.copyProperties(t, temp);
                    temp.setId(java.util.UUID.randomUUID().toString());
                    temp.setCreateUser(userName);
                    temp.setUpdateUser(userName);
                    try{
                        String ds = t.getTransactionDate() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate());
                        String ts = org.apache.commons.lang3.StringUtils.trimToEmpty(t.getTransactionTime());
                        temp.setTransactionDateTime(org.apache.commons.lang3.StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
                    }catch(Exception ignore){}
                    temps.add(temp);
                }
                transactionTempService.saveBatch(temps);
            }

            log.info("statement/upload-parsed stored preview: statementId={}, size={}", statement.getId(), transactions == null ? 0 : transactions.size());
            java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
            resp.put("statementId", statement.getId());
            resp.put("rows", dataRows.size());
            resp.put("parsed", transactions == null ? 0 : transactions.size());
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), JSON.toJSONString(resp));
        }catch(Exception e){
            log.error("Upload parsed failed", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), friendlyError(e));
        }
    }

    public static class ParsedPayload{
        public String fileName;
        public String bankCode;
        public String cardTypeCode;
        public String cardNo;
        public java.util.List<java.util.List<String>> rows;
    }

    private String friendlyError(Exception e){
        String msg = e == null ? "" : String.valueOf(e.getMessage());
        String lower = msg == null ? "" : msg.toLowerCase();
        Throwable cause = e;
        while(cause != null){
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
        if (lower.contains("access denied for user") || lower.contains("authentication")){
            return "数据库认证失败，请检查数据库用户名和密码是否正确。";
        }
        if (lower.contains("unknown database")){
            return "数据库不存在，请检查数据库名称配置。";
        }
        return "系统出现错误";
    }

    @GetMapping("/preview")
    @ResponseBody
    public List<com.finsight.domain.model.TransactionTemp> preview(@RequestParam("statementId") String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return new ArrayList<>();
        }
        try {
            // Retrieve from Temp Table
            List<com.finsight.domain.model.TransactionTemp> list = transactionTempService.getByStatementId(statementId);
            if(list != null && !list.isEmpty()){
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                for(com.finsight.domain.model.TransactionTemp t : list){
                    try{
                        String ds = t.getTransactionDate() == null ? "" : sdf.format(t.getTransactionDate());
                        String ts = org.apache.commons.lang3.StringUtils.trimToEmpty(t.getTransactionTime());
                        t.setTransactionDateTime(org.apache.commons.lang3.StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds);
                    }catch(Exception ignore){}
                }
            }
            log.info("statement/preview fetch: statementId={}, size={}", statementId, list == null ? 0 : list.size());
            return list;
        } catch (Exception e) {
            log.error("Preview fetch failed", e);
            return new ArrayList<>();
        }
    }

    @PostMapping("/commit")
    @ResponseBody
    public CommonResult commit(@RequestParam("statementId") String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "Invalid Statement ID");
        }
        try {
            // Retrieve from Temp
            List<com.finsight.domain.model.TransactionTemp> temps = transactionTempService.getByStatementId(statementId);
            if (temps == null || temps.isEmpty()) {
                return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "No transactions found to commit");
            }

            // Convert and Save to Official Table
            List<Transaction> transactions = new java.util.ArrayList<>();
            for (com.finsight.domain.model.TransactionTemp temp : temps) {
                Transaction t = new Transaction();
                org.springframework.beans.BeanUtils.copyProperties(temp, t);
                transactions.add(t);
            }
            int imported = transactionService.addTransactions(transactions, authenticationFacade.getUserName());

            // Remove from Temp
            transactionTempService.deleteByStatementId(statementId);

            // Update Statement Status
            Statement statement = statementService.getById(statementId);
            if (statement != null) {
                statement.setStatus("COMMITTED");
                statementService.updateById(statement);
            }
            
            java.util.Map<String,Object> payload = new java.util.LinkedHashMap<>();
            payload.put("statementId", statementId);
            int total = transactions.size();
            payload.put("total", total);
            payload.put("imported", imported);
            payload.put("failed", Math.max(0, total - imported));
            return new CommonResult(ResultCode.OPERATION_SUCCEED.getCodeValue(), JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("Commit failed", e);
            return new CommonResult(ResultCode.OPERATION_FAILED.getCodeValue(), "系统出现错误");
        }
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
        for(String line : lines){
            line = StringUtils.trim(line);
            if(StringUtils.isBlank(line)) continue;
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
        for(String line : lines){
            String ln = org.apache.commons.lang3.StringUtils.trimToEmpty(line);
            if(org.apache.commons.lang3.StringUtils.isBlank(ln)) continue;
            String[] cols = ln.split("\\s+");
            rows.add(cols);
        }
        return rows;
    }

    private List<String[]> parseExcelEasy(MultipartFile file) throws Exception {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        try (java.io.InputStream in = file.getInputStream()) {
            // Read as List<Map<Integer, Object>> to avoid ClassCastException if cells are numeric/date
            java.util.List<java.util.Map<Integer, Object>> list = com.alibaba.excel.EasyExcel.read(in).sheet().doReadSync();
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
                            // Format date to yyyyMMdd or yyyy-MM-dd if it's a date object
                            // Importer expects yyyyMMdd often, but let's give standard iso
                            v = new java.text.SimpleDateFormat("yyyyMMdd").format((java.util.Date)val);
                        } else {
                            v = org.apache.commons.lang3.StringUtils.trim(String.valueOf(val));
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


    private String toCsv(List<String[]> rows){
        StringBuilder sb = new StringBuilder();
        for(String[] r : rows){
            if(r == null){ sb.append("\n"); continue; }
            for(int i=0;i<r.length;i++){
                if(i>0) sb.append(",");
                String v = r[i] == null ? "" : r[i].replaceAll("[\\r\\n]+"," ").trim();
                sb.append(v);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @GetMapping("/export")
    public void export(@RequestParam("statementId") String statementId, HttpServletResponse response) throws Exception {
        java.util.List<com.finsight.domain.model.TransactionTemp> list = transactionTempService.getByStatementId(statementId);

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=statement_preview.xlsx");
        java.util.List<java.util.List<String>> data = new java.util.ArrayList<>();
        data.add(java.util.Arrays.asList("Card Name","Posting Date","Narration","Currency","Amount","Category","Remarks"));
        if(list != null){
            for (com.finsight.domain.model.TransactionTemp t : list) {
                String date = t.getTransactionDate() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getTransactionDate());
                String cat = org.apache.commons.lang3.StringUtils.isNotBlank(t.getConsumeName()) ? t.getConsumeName() : org.apache.commons.lang3.StringUtils.trimToEmpty(t.getConsumeCode());
                data.add(java.util.Arrays.asList(
                        org.apache.commons.lang3.StringUtils.trimToEmpty(t.getBankCardName()),
                        date,
                        org.apache.commons.lang3.StringUtils.trimToEmpty(t.getTransactionDesc()),
                        org.apache.commons.lang3.StringUtils.trimToEmpty(t.getBalanceCurrency()),
                        t.getBalanceMoney() == null ? "" : String.valueOf(t.getBalanceMoney()),
                        cat,
                        org.apache.commons.lang3.StringUtils.trimToEmpty(t.getDemoArea())
                ));
            }
        }
        com.alibaba.excel.EasyExcel.write(response.getOutputStream()).sheet("Preview").doWrite(data);
    }
}
