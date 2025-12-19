package com.finsight.web.rest;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.web.rest.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<TreeNode> tree(){
        return consumeCategoryService.tree();
    }
}
