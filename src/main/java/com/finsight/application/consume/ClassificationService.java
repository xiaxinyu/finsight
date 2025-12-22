package com.finsight.application.consume;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class ClassificationService {
    @Autowired
    private ConsumeRuleService ruleService;
    @Autowired
    private ConsumeCategoryService categoryService;
    @Autowired
    private DecisionTreeClassifier decisionTreeClassifier;

    private volatile List<ConsumeRule> rules;
    private final Map<String, ConsumeCategory> categoryMap = new ConcurrentHashMap<>();

    public void reload(){
        rules = ruleService.listActive();
        categoryMap.clear();
        for (ConsumeCategory c : categoryService.listAll()){
            if (c == null) continue;
            String idKey = c.getId();
            String codeKey = normCode(c.getCode());
            if (idKey != null) { categoryMap.put(idKey, c); }
            if (codeKey != null && !codeKey.isEmpty()) { categoryMap.put(codeKey, c); }
        }
        decisionTreeClassifier.reload();
    }

    public Result classify(String narration, String bankCode, String cardTypeCode, Double amount, java.util.Date txnDate){
        if (rules == null) reload();
        DecisionTreeClassifier.Result r = decisionTreeClassifier.classify(narration, bankCode, cardTypeCode, amount, txnDate);
        if (r == null) return null;
        Result res = new Result();
        res.id = r.id;
        res.name = r.name;
        return res;
    }

    public java.util.List<Result> classifyTopN(String narration, String bankCode, String cardTypeCode, Double amount, java.util.Date txnDate, int topN){
        if (rules == null) reload();
        java.util.List<DecisionTreeClassifier.Result> list = decisionTreeClassifier.classifyTopN(narration, bankCode, cardTypeCode, amount, txnDate, topN);
        java.util.List<Result> out = new java.util.ArrayList<>();
        if(list == null) list = java.util.Collections.emptyList();
        for(DecisionTreeClassifier.Result r : list){
            if(r == null) continue;
            Result res = new Result();
            res.id = r.id;
            res.name = r.name;
            out.add(res);
        }
        if(out.isEmpty()){
            ConsumeCategory fallback = null;
            ConsumeCategory c1 = categoryMap.get("OTHER-01");
            ConsumeCategory c2 = categoryMap.get("OTHER");
            fallback = c1 != null ? c1 : c2;
            if(fallback == null){
                for(ConsumeCategory c : categoryService.listAll()){
                    if(c == null) continue;
                    String code = normCode(c.getCode());
                    String name = c.getName() == null ? "" : c.getName().trim();
                    if("OTHER-01".equalsIgnoreCase(code) || "OTHER".equalsIgnoreCase(code) || "无法归类的支出".equals(name) || "Uncategorized".equalsIgnoreCase(name)){ fallback = c; break; }
                }
            }
            if(fallback != null){
                Result fr = new Result();
                fr.id = fallback.getCode();
                fr.name = fallback.getName();
                out.add(fr);
            } else {
                 // Try to load ANY category as fallback if critical specific ones missing
                 List<ConsumeCategory> all = categoryService.listAll();
                 if(!all.isEmpty()){
                     ConsumeCategory any = all.get(0);
                     Result fr = new Result();
                     fr.id = any.getCode();
                     fr.name = any.getName() + " (Fallback)";
                     out.add(fr);
                 }
            }
        }
        return out;
    }
 
    public java.util.List<String> tokens(String text){
        if (rules == null) reload();
        return decisionTreeClassifier.tokens(text);
    }

    private String normCode(String code){
        if(code == null) return null;
        String k = code.trim();
        k = k.replaceAll("[\u2012-\u2015\u2212\uFE58\uFE63\uFF0D]", "-");
        return k.toUpperCase();
    }

    private boolean match(String text, String pattern, String type){
        if (!StringUtils.hasText(pattern)) return false;
        String t = type == null ? "contains" : type.toLowerCase();
        if ("equals".equals(t)) return text.equals(pattern);
        if ("regex".equals(t)) return Pattern.compile(pattern).matcher(text).find();
        return text.contains(pattern);
    }

    private String normalize(String s){
        if (s == null) return null;
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    private String s(String v){
        return v == null ? "" : v.trim();
    }

    private int scoreFor(ConsumeRule r){
        int base = 0;
        String t = r.getPatternType() == null ? "contains" : r.getPatternType().toLowerCase();
        if ("equals".equals(t)) base = 100;
        else if ("regex".equals(t)) base = 80;
        else base = 60;
        int weight = r.getPriority() == null ? 0 : r.getPriority();
        int score = base + weight;
        return score;
    }

    public static class Result{
        public String id;
        public String name;
    }
}
