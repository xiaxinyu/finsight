package com.finsight.application.consume;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class DecisionTreeClassifier {
    private static final Logger log = LoggerFactory.getLogger(DecisionTreeClassifier.class);
    @Autowired
    private ConsumeRuleService ruleService;
    @Autowired
    private ConsumeCategoryService categoryService;

    private volatile List<ConsumeRule> rules;
    private final Map<String, ConsumeCategory> categoryMap = new ConcurrentHashMap<>();
    private final Map<String, List<RuleEntry>> equalsIndex = new ConcurrentHashMap<>();
    private final Map<String, List<RuleEntry>> tokenIndex = new ConcurrentHashMap<>();
    private volatile List<RuleEntry> regexEntries = new ArrayList<>();
    private volatile List<RuleEntry> containsPhrases = new ArrayList<>();

    public void reload(){
        rules = ruleService.listActive();
        equalsIndex.clear();
        tokenIndex.clear();
        regexEntries = new ArrayList<>();
        containsPhrases = new ArrayList<>();
        categoryMap.clear();
        for (ConsumeCategory c : categoryService.listAll()){
            if (c == null) continue;
            String idKey = c.getId();
            String codeKey = normCode(c.getCode());
            if (idKey != null) { categoryMap.put(idKey, c); }
            if (StringUtils.hasText(codeKey)) { categoryMap.put(codeKey, c); }
        }
        List<ConsumeCategory> cats = categoryService.listAll();
        if (cats != null){
            for (ConsumeCategory cat : cats){
                if (cat == null) continue;
                String cid = normCode(cat.getCode());
                if (!StringUtils.hasText(cid)) continue;
                List<String> toks = nameTokens(cat.getName());
                for(String tk : toks){
                    RuleEntry e = new RuleEntry();
                    e.categoryId = cid;
                    e.categoryName = cat.getName();
                    e.priority = -100;
                    e.type = "contains";
                    e.pattern = tk;
                    e.bankCode = "";
                    e.cardTypeCode = "";
                    tokenIndex.computeIfAbsent(tk, k -> new ArrayList<>()).add(e);
                }
            }
        }
        if (rules == null) return;
        for (ConsumeRule r : rules){
            String rid = s(r.getCategoryId());
            ConsumeCategory cat = categoryMap.get(rid);
            if (cat == null) cat = categoryMap.get(normCode(rid));
            if (cat == null) continue;
            String type = s(r.getPatternType()).toLowerCase();
            String pat = s(r.getPattern());
            RuleEntry e = new RuleEntry();
            e.categoryId = normCode(cat.getCode());
            e.categoryName = cat.getName();
            e.priority = r.getPriority() == null ? 0 : r.getPriority();
            e.type = type;
            e.pattern = pat;
            e.bankCode = s(r.getBankCode());
            e.cardTypeCode = s(r.getCardTypeCode());
            e.minAmount = r.getMinAmount() == null ? null : r.getMinAmount();
            e.maxAmount = r.getMaxAmount() == null ? null : r.getMaxAmount();
            e.startDate = r.getStartDate();
            e.endDate = r.getEndDate();
            if (!StringUtils.hasText(pat)) continue;
            if ("equals".equals(type)){
                equalsIndex.computeIfAbsent(pat, k -> new ArrayList<>()).add(e);
            } else if ("regex".equals(type)){
                try{ e.compiled = Pattern.compile(pat); regexEntries.add(e); }catch(Exception ignore){}
            } else {
                containsPhrases.add(e);
                for(String t : splitTokens(pat)){
                    if(StringUtils.hasText(t)){
                        tokenIndex.computeIfAbsent(t, k -> new ArrayList<>()).add(e);
                    }
                }
            }
        }
    }

    public Result classify(String narration, String bankCode, String cardTypeCode, Double amount, java.util.Date txnDate){
        if (rules == null) reload();
        String text = normalize(narration);
        if (!StringUtils.hasText(text)) return null;
        String b = s(bankCode).toLowerCase();
        String c = s(cardTypeCode).toLowerCase();

        RuleEntry bestEq = null;
        List<RuleEntry> eqs = equalsIndex.get(text);
        if (eqs != null && !eqs.isEmpty()){
            for(RuleEntry e : eqs){
                if(matchContext(e,b,c,amount,txnDate)){
                    if(bestEq==null || betterThan(e, bestEq)){
                        bestEq = e;
                    }
                }
            }
            if (bestEq != null) return toResult(bestEq);
        }

        RuleEntry bestRegex = null;
        for (RuleEntry e : regexEntries){
            if (!matchContext(e,b,c,amount,txnDate)) continue;
            try{
                if(e.compiled.matcher(text).find()){
                    if(bestRegex==null || betterThan(e, bestRegex)){
                        bestRegex = e;
                    }
                }
            }catch(Exception ignore){}
        }
        if (bestRegex != null) return toResult(bestRegex);

        RuleEntry bestContains = null;
        for (RuleEntry e : containsPhrases){
            if (!matchContext(e,b,c,amount,txnDate)) continue;
            try{
                String p = normalize(e.pattern);
                if(StringUtils.hasText(p) && text.contains(p)){
                    if(bestContains==null || betterThan(e, bestContains)){
                        bestContains = e;
                    }
                }
            }catch(Exception ignore){}
        }
        if (bestContains != null) return toResult(bestContains);

        Set<String> tokenSet = tokenizeSet(narration);
        Map<String, Integer> scores = new HashMap<>();
        Map<String, Integer> hits = new HashMap<>();
        Map<String, Integer> strongHits = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for(Map.Entry<String,List<RuleEntry>> en : tokenIndex.entrySet()){
            String token = en.getKey();
            if(seen.contains(token)) continue;
            if(tokenSet.contains(token) && !isStopword(token)){
                seen.add(token);
                for(RuleEntry e : en.getValue()){
                    if(!matchContext(e,b,c,amount,txnDate)) continue;
                    int s = scores.getOrDefault(e.categoryId, 0);
                    scores.put(e.categoryId, s + scoreForContains(e));
                    hits.put(e.categoryId, hits.getOrDefault(e.categoryId, 0) + 1);
                    if(token.length() >= 2){ strongHits.put(e.categoryId, strongHits.getOrDefault(e.categoryId, 0) + 1); }
                }
            }
        }
        String bestCatId = null;
        int bestScore = Integer.MIN_VALUE;
        for(Map.Entry<String,Integer> sc : scores.entrySet()){
            int hc = hits.getOrDefault(sc.getKey(), 0);
            int sh = strongHits.getOrDefault(sc.getKey(), 0);
            if(hc < 2 && sh < 1) continue;
            if(sc.getValue() > bestScore){ bestScore = sc.getValue(); bestCatId = sc.getKey(); }
        }
        if (bestCatId == null) return null;
        ConsumeCategory cat = categoryMap.get(bestCatId);
        if (cat == null) return null;
        Result r = new Result();
        r.id = cat.getCode();
        r.name = cat.getName();
        r.priority = 0;
        return r;
    }

    private Result toResult(RuleEntry e){
        ConsumeCategory cat = categoryMap.get(e.categoryId);
        if (cat == null) return null;
        Result r = new Result();
        r.id = cat.getCode();
        r.name = cat.getName();
        r.priority = e.priority;
        return r;
    }

    private boolean matchContext(RuleEntry e, String b, String c, Double amount, java.util.Date txnDate){
        if (StringUtils.hasText(b) && StringUtils.hasText(e.bankCode) && !e.bankCode.equalsIgnoreCase(b)) return false;
        if (StringUtils.hasText(c) && StringUtils.hasText(e.cardTypeCode) && !e.cardTypeCode.equalsIgnoreCase(c)) return false;
        if (amount != null){
            Double min = e.minAmount;
            Double max = e.maxAmount;
            double a = amount.doubleValue();
            if (min != null && a < min.doubleValue()) return false;
            if (max != null && a > max.doubleValue()) return false;
        }
        if (txnDate != null){
            java.util.Date s = e.startDate;
            java.util.Date eDate = e.endDate;
            long ts = txnDate.getTime();
            if (s != null && ts < s.getTime()) return false;
            if (eDate != null && ts > eDate.getTime()) return false;
        }
        return true;
    }

    private String normalize(String s){
        if (s == null) return null;
        String t = s.toLowerCase();
        t = t.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        t = t.replaceAll("\\s+"," ");
        return t;
    }

    private String s(String v){ return v == null ? "" : v.trim(); }

    private String normCode(String code){
        if(code == null) return null;
        String k = code.trim();
        k = k.replaceAll("[\u2012-\u2015\u2212\uFE58\uFE63\uFF0D]", "-");
        return k.toUpperCase();
    }

    private int scoreFor(RuleEntry e){
        int base = 0;
        String t = e.type == null ? "contains" : e.type.toLowerCase();
        if ("equals".equals(t)) base = 100;
        else if ("regex".equals(t)) base = 80;
        else base = 40;
        return base + e.priority;
    }

    private int scoreForContains(RuleEntry e){
        return 40 + e.priority;
    }
    
    private boolean betterThan(RuleEntry candidate, RuleEntry current){
        int cs = scoreFor(candidate);
        int os = scoreFor(current);
        if (cs != os) return cs > os;
        int cl = normalizedPatternLen(candidate);
        int ol = normalizedPatternLen(current);
        if (cl != ol) return cl > ol;
        int cx = specificity(candidate);
        int ox = specificity(current);
        if (cx != ox) return cx > ox;
        return false;
    }
    
    private int normalizedPatternLen(RuleEntry e){
        try{
            String p = normalize(e == null ? null : e.pattern);
            return p == null ? 0 : p.length();
        }catch(Exception ignore){
            return 0;
        }
    }
    
    private int specificity(RuleEntry e){
        if (e == null) return 0;
        int s = 0;
        if (StringUtils.hasText(e.bankCode)) s++;
        if (StringUtils.hasText(e.cardTypeCode)) s++;
        if (e.minAmount != null || e.maxAmount != null) s++;
        if (e.startDate != null || e.endDate != null) s++;
        return s;
    }

    private List<String> splitTokens(String pat){
        List<String> tokens = new ArrayList<>();
        for(String p : pat.split("\\|")){ String tp = p==null?"":p.trim(); if(StringUtils.hasText(tp)) tokens.add(tp.toLowerCase()); }
        return tokens;
    }
 
    private List<String> nameTokens(String name){
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(name)) return out;
        String txt = name.toLowerCase();
        txt = txt.replaceAll("[()（）\\[\\]{}“”\"'、，,/|]+", " ");
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<txt.length();i++){
            char ch = txt.charAt(i);
            if(Character.isLetterOrDigit(ch) || Character.isWhitespace(ch) || isCJK(ch)){
                sb.append(ch);
            }else{
                sb.append(' ');
            }
        }
        String norm = sb.toString().replaceAll("\\s+"," ").trim();
        for(String w : norm.split(" ")){
            String t = w==null?"":w.trim();
            if(!StringUtils.hasText(t)) continue;
            if(isAllDigits(t)) continue;
            if(t.length() < 2) continue;
            out.add(t);
        }
        if (isMostlyCJK(norm)){
            String c = norm.replaceAll("\\s+","");
            if (StringUtils.hasText(c)) out.add(c);
        }
        LinkedHashSet<String> uniq = new LinkedHashSet<>(out);
        return new ArrayList<>(uniq);
    }
 
    private boolean isCJK(char ch){
        Character.UnicodeBlock b = Character.UnicodeBlock.of(ch);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }
 
    private boolean isAllDigits(String s){
        for(int i=0;i<s.length();i++){ if(!Character.isDigit(s.charAt(i))) return false; }
        return s.length() > 0;
    }
 
    private boolean isMostlyCJK(String s){
        int cjk = 0, total = 0;
        for(int i=0;i<s.length();i++){
            char ch = s.charAt(i);
            if(Character.isWhitespace(ch)) continue;
            total++;
            if(isCJK(ch)) cjk++;
        }
        return total > 0 && cjk * 2 >= total;
    }
 
    private Set<String> tokenizeSet(String text){
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) return out;
        out.addAll(simpleEnglishTokens(text));
        out.addAll(simpleChineseTokens(text));
        out.addAll(jiebaTokensReflect(text));
        out.addAll(luceneTokensReflect(text));
        return out;
    }
 
    private Collection<String> simpleEnglishTokens(String text){
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        String lower = String.valueOf(text).toLowerCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[a-z]{2,}").matcher(lower);
        while(m.find()){ String w = m.group(); if(StringUtils.hasText(w)) ret.add(w); }
        return ret;
    }
 
    private Collection<String> simpleChineseTokens(String text){
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        String cleaned = String.valueOf(text).replaceAll("[()（）\\[\\]{}“”\"'、，,·:;|\\-]+", " ");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,}").matcher(cleaned);
        while(m.find()){
            String w = m.group();
            if(!StringUtils.hasText(w)) continue;
            ret.add(w);
            int n = w.length();
            int maxLen = Math.min(6, n);
            for(int len=2; len<=maxLen; len++){
                for(int i=0; i+len<=n; i++){
                    String sub = w.substring(i, i+len);
                    if(StringUtils.hasText(sub)){ ret.add(sub); }
                }
            }
        }
        return ret;
    }
 
    private Collection<String> jiebaTokensReflect(String text){
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        boolean hasCJK = false;
        for(int i=0;i<text.length();i++){ if(isCJK(text.charAt(i))){ hasCJK = true; break; } }
        if(!hasCJK) return ret;
        try{
            Class<?> segClass = Class.forName("com.huaban.analysis.jieba.JiebaSegmenter");
            Object seg = segClass.getConstructor().newInstance();
            Class<?> segModeClass = Class.forName("com.huaban.analysis.jieba.JiebaSegmenter$SegMode");
            Object search = java.lang.Enum.valueOf((Class<? extends java.lang.Enum>)segModeClass, "SEARCH");
            java.lang.reflect.Method process = segClass.getMethod("process", String.class, segModeClass);
            java.util.List<?> tokens = (java.util.List<?>)process.invoke(seg, text, search);
            for(Object tk : tokens){
                try{
                    java.lang.reflect.Field f = tk.getClass().getField("word");
                    String w = s(String.valueOf(f.get(tk))).toLowerCase();
                    if(w.length() >= 2){ ret.add(w); }
                }catch(Exception ignoreInner){}
            }
        }catch(Exception ignore){}
        return ret;
    }
 
    private Collection<String> luceneTokensReflect(String text){
        LinkedHashSet<String> ret = new LinkedHashSet<>();
        try{
            Class<?> analyzerClass = Class.forName("org.apache.lucene.analysis.standard.StandardAnalyzer");
            Object analyzer = analyzerClass.getConstructor().newInstance();
            Class<?> tokenStreamClass = Class.forName("org.apache.lucene.analysis.TokenStream");
            java.lang.reflect.Method tokenStreamMethod = analyzerClass.getMethod("tokenStream", String.class, java.io.Reader.class);
            Object ts = tokenStreamMethod.invoke(analyzer, "f", new java.io.StringReader(text));
            Class<?> attrClass = Class.forName("org.apache.lucene.analysis.tokenattributes.CharTermAttribute");
            java.lang.reflect.Method addAttr = tokenStreamClass.getMethod("addAttribute", Class.class);
            Object attr = addAttr.invoke(ts, attrClass);
            java.lang.reflect.Method reset = tokenStreamClass.getMethod("reset");
            reset.invoke(ts);
            java.lang.reflect.Method incrementToken = tokenStreamClass.getMethod("incrementToken");
            java.lang.reflect.Method toStringMethod = attrClass.getMethod("toString");
            while((Boolean)incrementToken.invoke(ts)){
                String w = String.valueOf(toStringMethod.invoke(attr));
                if(StringUtils.hasText(w) && w.length() >= 2){ ret.add(w.toLowerCase()); }
            }
            java.lang.reflect.Method end = tokenStreamClass.getMethod("end");
            java.lang.reflect.Method close = tokenStreamClass.getMethod("close");
            end.invoke(ts);
            close.invoke(ts);
        }catch(Exception ignore){}
        return ret;
    }

    private boolean isStopword(String token){
        if(token == null) return true;
        String t = token.trim().toLowerCase();
        if(t.length() <= 1) return true;
        String[] arr = {"公司","有限","集团","商户","外部","客户","支付宝","微信","银行","消费","交易","账单","充值","支付","订单","平台","服务","门店","分店","商店","商城"};
        for(String s : arr){ if(t.equals(s)) return true; }
        return false;
    }

    public static class Result{
        public String id;
        public String name;
        public int priority;
    }

    private static class RuleEntry{
        String categoryId;
        String categoryName;
        String type;
        String pattern;
        int priority;
        String bankCode;
        String cardTypeCode;
        Pattern compiled;
        Double minAmount;
        Double maxAmount;
        java.util.Date startDate;
        java.util.Date endDate;
    }

    public List<Result> classifyTopN(String narration, String bankCode, String cardTypeCode, Double amount, java.util.Date txnDate, int topN){
        if (rules == null) reload();
        String text = normalize(narration);
        if (!StringUtils.hasText(text)) return java.util.Collections.emptyList();
        String b = s(bankCode).toLowerCase();
        String c = s(cardTypeCode).toLowerCase();

        Map<String, Integer> aggScore = new HashMap<>();
        Map<String, Integer> aggPriority = new HashMap<>();
        Map<String, Integer> hitCount = new HashMap<>();
        Map<String, Integer> strongHitCount = new HashMap<>();
        Set<String> strongCats = new HashSet<>();

        List<RuleEntry> eqs = equalsIndex.get(text);
        if (eqs != null && !eqs.isEmpty()){
            for(RuleEntry e : eqs){
                if(!matchContext(e,b,c,amount,txnDate)) continue;
                int sc = scoreFor(e);
                int ps = aggScore.getOrDefault(e.categoryId, 0);
                int pp = aggPriority.getOrDefault(e.categoryId, Integer.MIN_VALUE);
                aggScore.put(e.categoryId, Math.max(ps, sc));
                aggPriority.put(e.categoryId, Math.max(pp, e.priority));
                strongCats.add(e.categoryId);
            }
        }

        for (RuleEntry e : regexEntries){
            if (!matchContext(e,b,c,amount,txnDate)) continue;
            try{
                if(e.compiled.matcher(text).find()){
                    int sc = scoreFor(e);
                    int ps = aggScore.getOrDefault(e.categoryId, 0);
                    int pp = aggPriority.getOrDefault(e.categoryId, Integer.MIN_VALUE);
                    aggScore.put(e.categoryId, Math.max(ps, sc));
                    aggPriority.put(e.categoryId, Math.max(pp, e.priority));
                    strongCats.add(e.categoryId);
                }
            }catch(Exception ignore){}
        }

        for (RuleEntry e : containsPhrases){
            if (!matchContext(e,b,c,amount,txnDate)) continue;
            try{
                String p = normalize(e.pattern);
                if(StringUtils.hasText(p) && text.contains(p)){
                    int sc = scoreFor(e);
                    int ps = aggScore.getOrDefault(e.categoryId, 0);
                    int pp = aggPriority.getOrDefault(e.categoryId, Integer.MIN_VALUE);
                    aggScore.put(e.categoryId, Math.max(ps, sc));
                    aggPriority.put(e.categoryId, Math.max(pp, e.priority));
                    strongCats.add(e.categoryId);
                }
            }catch(Exception ignore){}
        }

        Set<String> seen = new HashSet<>();
        Set<String> tokenSet = tokenizeSet(narration);
        for(Map.Entry<String,List<RuleEntry>> en : tokenIndex.entrySet()){
            String token = en.getKey();
            if(seen.contains(token)) continue;
            if(tokenSet.contains(token) && !isStopword(token)){
                seen.add(token);
                for(RuleEntry e : en.getValue()){
                    if(!matchContext(e,b,c,amount,txnDate)) continue;
                    int s = aggScore.getOrDefault(e.categoryId, 0);
                    aggScore.put(e.categoryId, s + scoreForContains(e));
                    int pp = aggPriority.getOrDefault(e.categoryId, Integer.MIN_VALUE);
                    aggPriority.put(e.categoryId, Math.max(pp, e.priority));
                    hitCount.put(e.categoryId, hitCount.getOrDefault(e.categoryId, 0) + 1);
                    if(token.length() >= 2){ strongHitCount.put(e.categoryId, strongHitCount.getOrDefault(e.categoryId, 0) + 1); }
                }
            }
        }

        List<Result> results = new ArrayList<>();
        for(String catId : aggScore.keySet()){
            int hc = hitCount.getOrDefault(catId, 0);
            int sh = strongHitCount.getOrDefault(catId, 0);
            if(hc < 2 && sh < 1 && !strongCats.contains(catId)) continue;
            ConsumeCategory cat = categoryMap.get(catId);
            if(cat == null) continue;
            Result r = new Result();
            r.id = cat.getCode();
            r.name = cat.getName();
            r.priority = aggPriority.getOrDefault(catId, 0);
            results.add(r);
        }

        results.sort((a,bRes) -> {
            int cp = Integer.compare(bRes.priority, a.priority);
            if(cp != 0) return cp;
            int as = aggScore.getOrDefault(a.id, 0);
            int bs = aggScore.getOrDefault(bRes.id, 0);
            int cs = Integer.compare(bs, as);
            if(cs != 0) return cs;
            return String.valueOf(a.name).compareToIgnoreCase(String.valueOf(bRes.name));
        });

        if (topN > 0 && results.size() > topN){
            return new ArrayList<>(results.subList(0, topN));
        }
        return results;
    }
 
    public List<String> tokens(String text){
        String input = text == null ? "" : text;
        java.util.Set<String> set = tokenizeSet(input);
        List<String> list = new ArrayList<>(set == null ? java.util.Collections.emptyList() : set);
        list.sort((a,b) -> {
            int la = a == null ? 0 : a.length();
            int lb = b == null ? 0 : b.length();
            int c = Integer.compare(lb, la);
            if(c != 0) return c;
            return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
        });
        int n = Math.min(list.size(), 12);
        return new ArrayList<>(list.subList(0, n));
    }
}
