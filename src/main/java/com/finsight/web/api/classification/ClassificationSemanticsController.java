package com.finsight.web.api.classification;

import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/classification/semantics")
public class ClassificationSemanticsController {

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return FinanceSemanticsCatalog.catalogPayload();
    }
}
