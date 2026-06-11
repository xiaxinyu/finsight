package com.finsight.web.api.consume;

import com.finsight.domain.model.ConsumeRule;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.application.query.TransactionQuery;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/consume/rules")
public class ConsumeRuleController {
    private static final Logger log = LoggerFactory.getLogger(ConsumeRuleController.class);
    @Autowired
    private ConsumeRuleService ruleService;
    @Autowired
    private ClassificationService classificationService;
    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping
    public List<ConsumeRule> list(@RequestParam(value = "categoryId", required = false) String categoryId,
                                  @RequestParam(value = "tag", required = false) String tag,
                                  @RequestParam(value = "active", required = false) Integer active,
                                  @RequestParam(value = "includeInactive", required = false, defaultValue = "false") boolean includeInactive,
                                  @RequestParam(value = "includeInvalid", required = false, defaultValue = "false") boolean includeInvalid){
        LambdaQueryWrapper<ConsumeRule> qw = Wrappers.lambdaQuery();
        if (categoryId != null && !categoryId.trim().isEmpty()){
            qw.eq(ConsumeRule::getCategoryId, categoryId);
        }
        if (active != null){
            qw.eq(ConsumeRule::getActive, active);
        } else if (!includeInactive) {
            qw.eq(ConsumeRule::getActive, 1);
        }
        qw.orderByAsc(ConsumeRule::getPriority);
        
        List<ConsumeRule> list = ruleService.list(qw);
        ruleService.loadTags(list);

        if (!includeInvalid) {
            list = list.stream()
                    .filter(r -> r != null && StringUtils.isNotBlank(r.getPattern()))
                    .collect(Collectors.toList());
        }

        if (tag != null && !tag.trim().isEmpty()){
            String filterTag = tag.trim();
            return list.stream()
                .filter(r -> r.getTags() != null && r.getTags().contains(filterTag))
                .collect(Collectors.toList());
        }
        return list;
    }

    @PostMapping
    public ConsumeRule add(@RequestBody ConsumeRule rule){
        log.info("Try to add rule: {}", rule);
        try {
            rule.setId(com.finsight.common.util.StringTool.generateID());
            ruleService.save(rule);
            log.info("Rule added successfully: {}", rule.getId());
            classificationService.reload();
            return rule;
        } catch (Exception e) {
            log.error("Failed to add rule", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ConsumeRule update(@PathVariable("id") String id, @RequestBody ConsumeRule rule){
        log.info("Try to update rule id={}, body={}", id, rule);
        try {
            rule.setId(id);
            ruleService.updateById(rule);
            log.info("Rule updated successfully: {}", id);
            classificationService.reload();
            return rule;
        } catch (Exception e) {
            log.error("Failed to update rule " + id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id){
        ConsumeRule r = ruleService.getById(id);
        log.info("Delete rule id={}, categoryId={}, pattern={}, type={}, priority={}",
                r == null ? id : r.getId(),
                r == null ? null : r.getCategoryId(),
                r == null ? null : r.getPattern(),
                r == null ? null : r.getPatternType(),
                r == null ? null : r.getPriority());
        ruleService.removeById(id);
        classificationService.reload();
    }

    @PostMapping("/reload")
    public String reload(){
        classificationService.reload();
        return "ok";
    }
    
    @GetMapping("/suggest")
    public java.util.List<String> suggest(@RequestParam(value = "categoryId", required = false) String categoryId){
        java.util.List<String> out = new java.util.ArrayList<>();
        String id = categoryId == null ? "" : categoryId.trim();
        String base = id.toUpperCase();
        if(base.contains("EAT") || base.contains("FOOD") || base.contains("DINING") || base.contains("餐")){
            out.add("美团"); out.add("饿了么"); out.add("肯德基"); out.add("麦当劳"); out.add("星巴克"); out.add("外卖");
        }else if(base.contains("SHOP") || base.contains("MALL") || base.contains("购") || base.contains("网")){
            out.add("淘宝"); out.add("天猫"); out.add("京东"); out.add("拼多多"); out.add("唯品会");
        }else if(base.contains("TRAVEL") || base.contains("交通") || base.contains("出行")){
            out.add("滴滴"); out.add("高德"); out.add("地铁"); out.add("公交"); out.add("打车");
        }else if(base.contains("INVEST") || base.contains("投资")){
            out.add("基金"); out.add("股票"); out.add("证券"); out.add("支付宝理财"); out.add("银行理财");
        }else{
            out.add("超市"); out.add("便利店"); out.add("支付"); out.add("商城"); out.add("会员费");
        }
        return out;
    }
    
    @GetMapping("/recommend")
    public java.util.List<String> recommend(@RequestParam(value = "categoryId", required = false) String categoryId){
        java.util.List<String> out = new java.util.ArrayList<>();
        String id = categoryId == null ? "" : categoryId.trim();
        if(id.isEmpty()) return out;
        try{
            TransactionQuery q = new TransactionQuery();
            q.setConsumes(new String[]{id});
            Page page = new Page(1, 500);
            java.util.List<Transaction> list = transactionRepository.getTransactions(q, page);
            java.util.Map<String, Integer> freq = new java.util.HashMap<>();
            for(Transaction t : list){
                String desc = t.getTransactionDesc();
                java.util.List<String> tokens = classificationService.tokens(desc);
                if(tokens == null) continue;
                for(String s : tokens){
                    String k = org.apache.commons.lang3.StringUtils.trimToEmpty(s);
                    if(k.isEmpty()) continue;
                    if(k.length()<=1) continue;
                    Integer c = freq.get(k);
                    freq.put(k, c == null ? 1 : c + 1);
                }
            }
            java.util.List<java.util.Map.Entry<String,Integer>> arr = new java.util.ArrayList<>(freq.entrySet());
            arr.sort((a,b)->Integer.compare(b.getValue(), a.getValue()));
            int max = Math.min(20, arr.size());
            for(int i=0;i<max;i++){ out.add(arr.get(i).getKey()); }
        }catch(Exception e){
            log.warn("recommend failed for categoryId={}", categoryId, e);
        }
        return out;
    }
}
