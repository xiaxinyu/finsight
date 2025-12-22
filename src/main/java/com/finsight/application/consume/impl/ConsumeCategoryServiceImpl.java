package com.finsight.application.consume.impl;

import com.finsight.infrastructure.mapper.ConsumeCategoryMapper;
import com.finsight.web.restful.model.TreeNode;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.application.consume.ConsumeCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ConsumeCategoryServiceImpl extends ServiceImpl<ConsumeCategoryMapper, ConsumeCategory> implements ConsumeCategoryService {
    @Override
    public List<ConsumeCategory> listAll() {
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.ne(ConsumeCategory::getDeleted, 1)
          .orderByAsc(ConsumeCategory::getLevel)
          .orderByAsc(ConsumeCategory::getSortNo);
        return super.list(qw);
    }

    @Override
    public List<TreeNode> tree() {
        List<ConsumeCategory> list = listAll();
        List<ConsumeCategory> roots = list.stream()
                .filter(c -> {
                    Integer lv = c.getLevel();
                    String p = c.getParentId();
                    return (lv != null && lv == 1) || (p == null || p.trim().isEmpty());
                })
                .collect(Collectors.toList());
        Map<String, List<ConsumeCategory>> byParent = list.stream()
                .filter(c -> c.getLevel() != null && c.getLevel() == 2)
                .collect(Collectors.groupingBy(c -> {
                    String p = c.getParentId();
                    return p == null ? "" : p.trim();
                }));
        List<TreeNode> result = new ArrayList<>();
        for (ConsumeCategory r : roots) {
            result.add(build(r, byParent));
        }
        return result;
    }

    @Override
    public void ensureDefaults() {
        ensureRoot("INVEST", "投资", 95, "expense");
        java.util.Set<String> existingChildCodes = new java.util.LinkedHashSet<>();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.finsight.domain.model.ConsumeCategory> qwChild = com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery();
        qwChild.eq(com.finsight.domain.model.ConsumeCategory::getParentId, "INVEST")
               .ne(com.finsight.domain.model.ConsumeCategory::getDeleted, 1);
        for (ConsumeCategory cc : super.list(qwChild)) {
            if (cc != null && cc.getCode() != null) {
                existingChildCodes.add(cc.getCode().trim());
            }
        }
        if (!existingChildCodes.contains("INVEST-01")) {
            ensureLeaf("INVEST-01", "基金", "INVEST", 1, "expense");
        }
        if (!existingChildCodes.contains("INVEST-02")) {
            ensureLeaf("INVEST-02", "股票", "INVEST", 2, "expense");
        }
        if (!existingChildCodes.contains("INVEST-OTHER")) {
            ensureLeaf("INVEST-OTHER", "其它消费", "INVEST", 99, "expense");
        }
        ensureRoot("OTHER", "其他类别", 99, "expense");
        ensureLeaf("OTHER-01", "无法归类的支出", "OTHER", 1, "expense");
    }

    private void ensureRoot(String code, String name, Integer sortNo, String txnTypes){
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeCategory::getCode, code).ne(ConsumeCategory::getDeleted, 1);
        if (super.count(qw) > 0) { return; }
        ConsumeCategory c = new ConsumeCategory();
        c.setId(code);
        c.setCode(code);
        c.setName(name);
        c.setLevel(1);
        c.setParentId(null);
        c.setSortNo(sortNo);
        c.setDeleted(0);
        c.setTxnTypes(txnTypes);
        super.save(c);
    }

    private void ensureLeaf(String code, String name, String parentCode, Integer sortNo, String txnTypes){
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeCategory::getCode, code).ne(ConsumeCategory::getDeleted, 1);
        if (super.count(qw) > 0) { return; }
        ConsumeCategory c = new ConsumeCategory();
        c.setId(code);
        c.setCode(code);
        c.setName(name);
        c.setLevel(2);
        c.setParentId(parentCode);
        c.setSortNo(sortNo);
        c.setDeleted(0);
        c.setTxnTypes(txnTypes);
        super.save(c);
    }

    private TreeNode build(ConsumeCategory cat, Map<String, List<ConsumeCategory>> byParent){
        TreeNode n = new TreeNode();
        n.setId(cat.getCode());
        String txt = cat.getName();
        if (txt == null || txt.trim().isEmpty()) { txt = cat.getCode(); }
        n.setText(txt);
        String parentKey = cat.getCode() == null ? "" : cat.getCode().trim();
        List<ConsumeCategory> children = new ArrayList<>(byParent.getOrDefault(parentKey, Collections.emptyList()));
        children.sort((a, b) -> {
            Integer sa = a.getSortNo();
            Integer sb = b.getSortNo();
            if (sa == null && sb == null) return 0;
            if (sa == null) return 1;
            if (sb == null) return -1;
            return Integer.compare(sa, sb);
        });
        if (!children.isEmpty()){
            List<TreeNode> cs = new ArrayList<>();
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            for (ConsumeCategory c : children){
                if (c == null) continue;
                if (c.getLevel() != null && c.getLevel() != 2) continue; // enforce two levels
                if (cat.getCode() != null && cat.getCode().equals(c.getCode())) continue; // avoid self-loop by duplicate code
                String cid = c.getCode() == null ? null : c.getCode().trim();
                if (cid == null || seen.contains(cid)) continue;
                seen.add(cid);
                TreeNode cn = new TreeNode();
                cn.setId(cid);
                String ct = c.getName();
                if (ct == null || ct.trim().isEmpty()) { ct = cid; }
                cn.setText(ct);
                cs.add(cn);
            }
            if (!cs.isEmpty()){
                n.setChildren(cs);
                n.setState("closed");
            }
        }
        return n;
    }
}
