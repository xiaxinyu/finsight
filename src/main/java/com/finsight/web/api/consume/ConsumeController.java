package com.finsight.web.api.consume;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.web.api.dto.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consume")
public class ConsumeController {
    @Autowired
    private ConsumeCategoryService consumeCategoryService;

    @GetMapping("/tree")
    public List<TreeNode> tree(@RequestParam(value = "txnType", required = false) String txnType){
        return consumeCategoryService.tree(txnType);
    }
}
