package com.finsight.web.restful.statement;

import com.finsight.application.statement.StatementFacade;
import com.finsight.domain.model.Statement;
import com.finsight.web.restful.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/statement")
public class StatementController {
    @Autowired
    private StatementFacade statementFacade;

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
    public com.finsight.web.restful.model.CollectionResult<Statement> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                                                       @RequestParam(value = "rows", defaultValue = "20") int rows) {
        return statementFacade.list(page, rows);
    }

    @PostMapping("/upload")
    @ResponseBody
    public CommonResult upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "bankCode", required = false) String bankCode,
                               @RequestParam(value = "cardTypeCode", required = false) String cardTypeCode,
                               @RequestParam(value = "cardNo", required = false) String cardNo,
                               HttpServletRequest request) {
        return statementFacade.upload(file, bankCode, cardTypeCode, cardNo);
    }

    @PostMapping("/import-pdf-local")
    @ResponseBody
    public CommonResult importPdfLocal(@RequestParam("path") String path,
                                       @RequestParam(value = "bankCode", defaultValue = "CMB") String bankCode,
                                       @RequestParam(value = "cardTypeCode", defaultValue = "debit") String cardTypeCode,
                                       @RequestParam(value = "cardNo", required = false) String cardNo){
        return statementFacade.importPdfLocal(path, bankCode, cardTypeCode, cardNo);
    }

    @PostMapping("/upload-parsed")
    @ResponseBody
    public CommonResult uploadParsed(@RequestBody ParsedPayload payload){
        if (payload == null) {
            return CommonResult.fail("empty_rows");
        }
        return statementFacade.uploadParsed(payload.fileName, payload.bankCode, payload.cardTypeCode, payload.cardNo, payload.rows);
    }

    public static class ParsedPayload{
        public String fileName;
        public String bankCode;
        public String cardTypeCode;
        public String cardNo;
        public java.util.List<java.util.List<String>> rows;
    }

    @GetMapping("/preview")
    @ResponseBody
    public List<com.finsight.domain.model.TransactionTemp> preview(@RequestParam("statementId") String statementId) {
        return statementFacade.preview(statementId);
    }

    @PostMapping("/commit")
    @ResponseBody
    public CommonResult commit(@RequestParam("statementId") String statementId) {
        return statementFacade.commit(statementId);
    }

    @GetMapping("/export")
    public void export(@RequestParam("statementId") String statementId, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=statement_preview.xlsx");
        java.util.List<java.util.List<String>> data = statementFacade.exportData(statementId);
        com.alibaba.excel.EasyExcel.write(response.getOutputStream()).sheet("Preview").doWrite(data);
    }
}
