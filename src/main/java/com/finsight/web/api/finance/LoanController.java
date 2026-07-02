package com.finsight.web.api.finance;

import com.finsight.application.finance.LoanService;
import com.finsight.domain.model.LoanTxnLink;
import com.finsight.web.api.dto.CommonResult;
import com.finsight.web.api.dto.LoanTxnLinkRequest;
import com.finsight.web.api.dto.LoanWriteRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public CommonResult list() {
        return CommonResult.success(loanService.listWithSummary());
    }

    @GetMapping("/{id}")
    public CommonResult get(@PathVariable String id) {
        return CommonResult.success(loanService.get(id));
    }

    @PostMapping
    public CommonResult create(@RequestBody LoanWriteRequest body) {
        return CommonResult.success(loanService.create(body));
    }

    @PutMapping("/{id}")
    public CommonResult update(@PathVariable String id, @RequestBody LoanWriteRequest body) {
        return CommonResult.success(loanService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public CommonResult delete(@PathVariable String id) {
        loanService.delete(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}/links")
    public CommonResult links(@PathVariable String id) {
        return CommonResult.success(loanService.listLinks(id));
    }

    @PostMapping("/{id}/links")
    public CommonResult addLink(@PathVariable String id, @RequestBody LoanTxnLinkRequest body) {
        LoanTxnLink link = loanService.linkTransaction(id, body.getTransactionId(), body.getLinkType());
        return CommonResult.success(link);
    }

    @DeleteMapping("/{id}/links/{transactionId}")
    public CommonResult removeLink(@PathVariable String id, @PathVariable String transactionId) {
        loanService.unlinkTransaction(id, transactionId);
        return CommonResult.success(null);
    }
}
