package com.finsight.web.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ClassificationTestResult {
    private String narration;
    private List<Hit> hits = new ArrayList<>();
    private String message;

    @Getter
    @Setter
    public static class Hit {
        private String categoryCode;
        private String categoryName;
    }
}
