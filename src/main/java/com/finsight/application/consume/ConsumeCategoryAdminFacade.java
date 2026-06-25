package com.finsight.application.consume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.domain.model.Category;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.domain.model.Transaction;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.web.api.dto.CollectionResult;
import com.finsight.application.analytics.ConfigVersionBump;
import com.finsight.application.classification.CategoryAliasService;
import com.finsight.application.classification.CategoryMergeSupport;
import com.finsight.application.classification.CategoryMergeSupport.MergeMode;
import com.finsight.application.classification.CategoryReportRoleInference;
import com.finsight.application.classification.CategoryReportRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsumeCategoryAdminFacade {

    private static final Logger log = LoggerFactory.getLogger(ConsumeCategoryAdminFacade.class);

    @Autowired
    private ConsumeCategoryService categoryService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private ConsumeRuleService consumeRuleService;

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private CategoryAliasService categoryAliasService;

    @Autowired
    private ConfigVersionBump configVersionBump;

    public CollectionResult<ConsumeCategory> list() {
        return list(false);
    }

    public CollectionResult<ConsumeCategory> list(boolean includeDeleted) {
        List<ConsumeCategory> list;
        if (includeDeleted) {
            LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
            qw.orderByAsc(ConsumeCategory::getLevel).orderByAsc(ConsumeCategory::getSortNo);
            list = categoryService.list(qw);
        } else {
            list = categoryService.listAll();
        }
        CollectionResult<ConsumeCategory> r = new CollectionResult<>();
        r.setRows(list);
        r.setTotal(list.size());
        return r;
    }

    public Category add(Category raw) {
        ConsumeCategory cat = ensureConsumeCategory(raw);
        if (cat.getParentId() == null || cat.getParentId().trim().isEmpty()) {
            cat.setLevel(1);
        } else {
            cat.setLevel(2);
        }
        autofillCodeAndSort(cat);
        if (cat.getTxnTypes() == null || cat.getTxnTypes().trim().isEmpty()) {
            cat.setTxnTypes("expense");
        }
        applyReportRole(cat);
        String genId = buildId(cat.getLevel(), cat.getCode(), cat.getParentId());
        cat.setId(genId);
        try {
            categoryService.save(cat);
        } catch (Exception ex) {
            if (isDuplicateError(ex)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Duplicate id/code: " + genId);
            }
            throw ex;
        }
        configVersionBump.bumpTaxonomy();
        return cat;
    }

    @Transactional(rollbackFor = Exception.class)
    public Category update(String id, Category raw, Boolean cascade) {
        ConsumeCategory cat = ensureConsumeCategory(raw);
        ConsumeCategory old = categoryService.getById(id);
        if (cat.getParentId() == null || cat.getParentId().trim().isEmpty()) {
            cat.setLevel(1);
        } else {
            cat.setLevel(2);
        }
        autofillCodeAndSort(cat);
        validateUniqueCode(cat.getCode(), id);
        applyReportRole(cat);
        String newId = buildId(cat.getLevel(), cat.getCode(), cat.getParentId());

        LambdaUpdateWrapper<ConsumeCategory> uwCat = Wrappers.lambdaUpdate();
        uwCat.eq(ConsumeCategory::getId, id);
        uwCat.set(ConsumeCategory::getId, newId)
                .set(ConsumeCategory::getCode, cat.getCode())
                .set(ConsumeCategory::getName, cat.getName())
                .set(ConsumeCategory::getLevel, cat.getLevel())
                .set(ConsumeCategory::getParentId, cat.getParentId())
                .set(ConsumeCategory::getSortNo, cat.getSortNo())
                .set(ConsumeCategory::getDeleted, cat.getDeleted())
                .set(ConsumeCategory::getTxnTypes, cat.getTxnTypes())
                .set(ConsumeCategory::getReportRole, cat.getReportRole());
        try {
            categoryService.update(null, uwCat);
        } catch (Exception ex) {
            if (isDuplicateError(ex)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Duplicate id/code: " + newId);
            }
            throw ex;
        }

        if (old != null && old.getLevel() != null && old.getLevel() == 1) {
            String oldCode = old.getCode();
            String newParentCode = cat.getCode();
            if (oldCode != null && newParentCode != null && !oldCode.equals(newParentCode)) {
                LambdaQueryWrapper<ConsumeCategory> qChildren = Wrappers.lambdaQuery();
                qChildren.eq(ConsumeCategory::getParentId, oldCode);
                List<ConsumeCategory> children = categoryService.list(qChildren);
                int affectedChildren = 0;
                for (ConsumeCategory ch : children) {
                    String oldChildId = ch.getId();
                    String childCode = ch.getCode();
                    String updatedChildCode = childCode;
                    if (childCode != null && childCode.startsWith(oldCode + "-")) {
                        updatedChildCode = newParentCode + childCode.substring(oldCode.length());
                    }
                    String newChildId = buildId(2, updatedChildCode, newParentCode);
                    LambdaUpdateWrapper<ConsumeCategory> uwChild = Wrappers.lambdaUpdate();
                    uwChild.eq(ConsumeCategory::getId, ch.getId())
                            .set(ConsumeCategory::getId, newChildId)
                            .set(ConsumeCategory::getCode, updatedChildCode)
                            .set(ConsumeCategory::getParentId, newParentCode)
                            .set(ConsumeCategory::getLevel, 2);
                    boolean ok = categoryService.update(null, uwChild);
                    if (ok) affectedChildren++;

                    if (Boolean.TRUE.equals(cascade)) {
                        LambdaUpdateWrapper<Transaction> uwChildTransaction = Wrappers.lambdaUpdate();
                        uwChildTransaction.set(Transaction::getConsumeCode, updatedChildCode)
                                .set(Transaction::getConsumeName, ch.getName())
                                .eq(Transaction::getConsumeID, oldChildId);
                        transactionMapper.update(null, uwChildTransaction);
                    }
                }
                log.info("Cascade update child categories: parentCode {} -> {}, affectedChildren={} (child codes adjusted if prefixed)", oldCode, newParentCode, affectedChildren);
            }
        }

        if (Boolean.TRUE.equals(cascade) && old != null) {
            String oldCode = old.getCode();
            String newCode = cat.getCode();
            String newName = cat.getName();
            if (oldCode != null && newCode != null && !oldCode.equals(newCode)) {
                LambdaUpdateWrapper<Transaction> uwById = Wrappers.lambdaUpdate();
                uwById.set(Transaction::getConsumeCode, newCode)
                        .set(Transaction::getConsumeName, newName)
                        .eq(Transaction::getConsumeID, id);
                int affectedById = transactionMapper.update(null, uwById);

                LambdaUpdateWrapper<Transaction> uw = Wrappers.lambdaUpdate();
                uw.set(Transaction::getConsumeID, newCode)
                        .set(Transaction::getConsumeCode, newCode)
                        .set(Transaction::getConsumeName, newName)
                        .eq(Transaction::getConsumeID, oldCode);
                int affectedByCode = transactionMapper.update(null, uw);

                LambdaUpdateWrapper<Transaction> uwByCodeCol = Wrappers.lambdaUpdate();
                uwByCodeCol.set(Transaction::getConsumeCode, newCode)
                        .set(Transaction::getConsumeName, newName)
                        .eq(Transaction::getConsumeCode, oldCode);
                int affectedByCodeCol = transactionMapper.update(null, uwByCodeCol);

                log.info("Cascade update transactions: id={}, oldCode {} -> newCode {}, affectedById={}, affectedByConsumeId={}, affectedByConsumeCodeCol={}",
                        id, oldCode, newCode, affectedById, affectedByCode, affectedByCodeCol);
            }
            syncAllTransactionConsumeCodes();
        }
        cat.setId(newId);
        configVersionBump.bumpTaxonomy();
        return cat;
    }

    public void delete(String id) {
        ConsumeCategory cat = categoryService.getById(id);
        long children = 0;
        if (id != null && !id.trim().isEmpty()) {
            LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
            qw.eq(ConsumeCategory::getParentId, cat == null ? null : cat.getCode());
            children = categoryService.count(qw);
        }
        log.info("Soft delete category id={}, name={}, code={}, children={}",
                cat == null ? id : cat.getId(),
                cat == null ? null : cat.getName(),
                cat == null ? null : cat.getCode(),
                children);
        LambdaUpdateWrapper<ConsumeCategory> uwDel = Wrappers.lambdaUpdate();
        uwDel.eq(ConsumeCategory::getId, id)
                .set(ConsumeCategory::getDeleted, 1);
        categoryService.update(null, uwDel);
        deactivateRulesForCategory(cat);
        classificationService.reload();
        configVersionBump.bumpTaxonomy();
    }

    private void deactivateRulesForCategory(ConsumeCategory cat) {
        if (cat == null) {
            return;
        }
        String code = cat.getCode();
        String cid = cat.getId();
        if ((code == null || code.isBlank()) && (cid == null || cid.isBlank())) {
            return;
        }
        LambdaUpdateWrapper<ConsumeRule> uw = Wrappers.lambdaUpdate();
        uw.and(w -> {
            if (code != null && !code.isBlank()) {
                w.eq(ConsumeRule::getCategoryId, code);
            }
            if (cid != null && !cid.isBlank()) {
                if (code != null && !code.isBlank()) {
                    w.or().eq(ConsumeRule::getCategoryId, cid);
                } else {
                    w.eq(ConsumeRule::getCategoryId, cid);
                }
            }
        });
        uw.set(ConsumeRule::getActive, 0);
        consumeRuleService.update(null, uw);
    }

    @Transactional(rollbackFor = Exception.class)
    public CollectionResult<String> migrate(String id, String toCode, boolean deleteAfter, boolean cascade) {
        ConsumeCategory src = categoryService.getById(id);
        if (src == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Source category not found: " + id);
        }
        String parentCode = src.getParentId() == null ? "" : src.getParentId();
        ConsumeCategory target;
        if (toCode != null && !toCode.trim().isEmpty()) {
            LambdaQueryWrapper<ConsumeCategory> q = Wrappers.lambdaQuery();
            q.eq(ConsumeCategory::getCode, toCode.trim());
            target = categoryService.getOne(q);
            if (target == null) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Target category not found by code: " + toCode);
            }
        } else {
            LambdaQueryWrapper<ConsumeCategory> qs = Wrappers.lambdaQuery();
            qs.eq(ConsumeCategory::getParentId, parentCode)
                    .orderByAsc(ConsumeCategory::getSortNo);
            List<ConsumeCategory> siblings = categoryService.list(qs);
            target = null;
            if (siblings != null && !siblings.isEmpty()) {
                Integer srcSort = src.getSortNo() == null ? -1 : src.getSortNo();
                for (ConsumeCategory s : siblings) {
                    if (s.getId().equals(src.getId())) continue;
                    Integer ss = s.getSortNo() == null ? Integer.MAX_VALUE : s.getSortNo();
                    if (ss > srcSort) {
                        target = s;
                        break;
                    }
                }
                if (target == null) {
                    for (ConsumeCategory s : siblings) {
                        if (!s.getId().equals(src.getId())) {
                            target = s;
                            break;
                        }
                    }
                }
            }
            if (target == null) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "No available target sibling to migrate under parent: " + parentCode);
            }
        }

        String tgtCode = target.getCode();
        String tgtName = target.getName();

        if (src.getCode() != null && src.getCode().equals(tgtCode)) {
            log.info("Skip migrate: source and target are identical. src[code={}]", src.getCode());
            CollectionResult<String> r = new CollectionResult<>();
            r.setRows(java.util.Arrays.asList("noop"));
            r.setTotal(1);
            return r;
        }

        MergeMode mode;
        try {
            mode = CategoryMergeSupport.resolveMode(src, target);
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        switch (mode) {
            case L1_INTO_L1 -> mergeL1IntoL1(src, target, deleteAfter);
            case L2_REPARENT_TO_L1 -> reparentL2ToL1(src, target);
            case L2_INTO_L2 -> mergeL2IntoL2(src, target, deleteAfter, cascade);
        }

        syncAllTransactionConsumeCodes();
        classificationService.reload();
        configVersionBump.bumpTaxonomy();
        CollectionResult<String> r = new CollectionResult<>();
        r.setRows(java.util.Arrays.asList("ok"));
        r.setTotal(1);
        return r;
    }

    private void mergeL1IntoL1(ConsumeCategory src, ConsumeCategory target, boolean deleteAfter) {
        String srcCode = src.getCode();
        String tgtCode = target.getCode();
        LambdaQueryWrapper<ConsumeCategory> qwChildren = Wrappers.lambdaQuery();
        qwChildren.eq(ConsumeCategory::getParentId, srcCode)
                .ne(ConsumeCategory::getDeleted, 1);
        List<ConsumeCategory> children = categoryService.list(qwChildren);
        int reparented = 0;
        for (ConsumeCategory child : children) {
            LambdaUpdateWrapper<ConsumeCategory> uwChild = Wrappers.lambdaUpdate();
            uwChild.eq(ConsumeCategory::getId, child.getId())
                    .set(ConsumeCategory::getParentId, tgtCode)
                    .set(ConsumeCategory::getLevel, 2);
            if (categoryService.update(null, uwChild)) {
                reparented++;
            }
        }
        log.info("L1 merge: reparented {} children from {} -> {}", reparented, srcCode, tgtCode);

        categoryAliasService.recordMergeAlias(src, target, "L1_MERGE");

        if (deleteAfter) {
            LambdaUpdateWrapper<ConsumeCategory> uwDel = Wrappers.lambdaUpdate();
            uwDel.eq(ConsumeCategory::getId, src.getId())
                    .set(ConsumeCategory::getDeleted, 1);
            categoryService.update(null, uwDel);
            deactivateRulesForCategory(src);
            log.info("Soft-deleted duplicate L1 after merge: id={}, code={}", src.getId(), srcCode);
        }
    }

    private void reparentL2ToL1(ConsumeCategory src, ConsumeCategory target) {
        LambdaUpdateWrapper<ConsumeCategory> uwMove = Wrappers.lambdaUpdate();
        uwMove.eq(ConsumeCategory::getId, src.getId())
                .set(ConsumeCategory::getParentId, target.getCode())
                .set(ConsumeCategory::getLevel, 2);
        categoryService.update(null, uwMove);
        log.info("Reparent L2: src[id={},code={}] -> parent {}", src.getId(), src.getCode(), target.getCode());
    }

    private void mergeL2IntoL2(ConsumeCategory src, ConsumeCategory target, boolean deleteAfter, boolean cascade) {
        String tgtCode = target.getCode();
        String tgtName = target.getName();
        int affected1 = 0, affected2 = 0, affected3 = 0, affected4 = 0, affected5 = 0, affected6 = 0;
        int rulesRemapped = 0;

        if (cascade) {
            LambdaUpdateWrapper<Transaction> uw1 = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw1, tgtCode, tgtName);
            uw1.eq(Transaction::getConsumeID, src.getId());
            affected1 = transactionMapper.update(null, uw1);

            LambdaUpdateWrapper<Transaction> uw2 = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw2, tgtCode, tgtName);
            uw2.eq(Transaction::getConsumeID, src.getCode());
            affected2 = transactionMapper.update(null, uw2);

            LambdaUpdateWrapper<Transaction> uw3 = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw3, tgtCode, tgtName);
            uw3.eq(Transaction::getConsumeCode, src.getCode());
            affected3 = transactionMapper.update(null, uw3);

            LambdaUpdateWrapper<Transaction> uw3b = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw3b, tgtCode, tgtName);
            uw3b.eq(Transaction::getCategoryCode, src.getCode());
            transactionMapper.update(null, uw3b);

            LambdaUpdateWrapper<Transaction> uw4 = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw4, tgtCode, tgtName);
            uw4.like(Transaction::getConsumeID, "/" + src.getCode());
            affected4 = transactionMapper.update(null, uw4);

            LambdaUpdateWrapper<Transaction> uw5 = Wrappers.lambdaUpdate();
            applyTransactionCategoryTarget(uw5, tgtCode, tgtName);
            uw5.like(Transaction::getConsumeID, "-" + src.getCode());
            affected5 = transactionMapper.update(null, uw5);

            if (src.getName() != null && !src.getName().trim().isEmpty()) {
                LambdaUpdateWrapper<Transaction> uw6 = Wrappers.lambdaUpdate();
                applyTransactionCategoryTarget(uw6, tgtCode, tgtName);
                uw6.eq(Transaction::getConsumeCode, src.getCode())
                        .eq(Transaction::getConsumeName, src.getName());
                affected6 = transactionMapper.update(null, uw6);
            }

            rulesRemapped = remapRulesToTarget(src, tgtCode);
        }

        log.info("Migrate L2 src[id={},code={}] -> target[code={},name={}], cascade={}, txnById={}, txnByConsumeId={}, "
                        + "txnByConsumeCode={}, likeSlash={}, likeDash={}, txnByName={}, rulesRemapped={}",
                src.getId(), src.getCode(), tgtCode, tgtName, cascade,
                affected1, affected2, affected3, affected4, affected5, affected6, rulesRemapped);

        categoryAliasService.recordMergeAlias(src, target, cascade ? "L2_MERGE_CASCADE" : "L2_MERGE");

        if (deleteAfter) {
            LambdaUpdateWrapper<ConsumeCategory> uwDel = Wrappers.lambdaUpdate();
            uwDel.eq(ConsumeCategory::getId, src.getId())
                    .set(ConsumeCategory::getDeleted, 1);
            categoryService.update(null, uwDel);
            if (!cascade) {
                deactivateRulesForCategory(src);
            }
            log.info("Soft-deleted source category after migration: id={}, code={}", src.getId(), src.getCode());
        }
    }

    private void applyTransactionCategoryTarget(LambdaUpdateWrapper<Transaction> uw, String tgtCode, String tgtName) {
        uw.set(Transaction::getConsumeID, tgtCode)
                .set(Transaction::getConsumeCode, tgtCode)
                .set(Transaction::getCategoryCode, tgtCode)
                .set(Transaction::getCategoryId, tgtCode);
        if (tgtName != null && !tgtName.isBlank()) {
            uw.set(Transaction::getConsumeName, tgtName)
                    .set(Transaction::getCategoryName, tgtName);
        }
    }

    private int remapRulesToTarget(ConsumeCategory src, String tgtCode) {
        String srcCode = src.getCode();
        String srcId = src.getId();
        if ((srcCode == null || srcCode.isBlank()) && (srcId == null || srcId.isBlank())) {
            return 0;
        }
        LambdaUpdateWrapper<ConsumeRule> uw = Wrappers.lambdaUpdate();
        uw.set(ConsumeRule::getCategoryId, tgtCode);
        uw.and(w -> {
            if (srcCode != null && !srcCode.isBlank()) {
                w.eq(ConsumeRule::getCategoryId, srcCode);
            }
            if (srcId != null && !srcId.isBlank()) {
                if (srcCode != null && !srcCode.isBlank()) {
                    w.or().eq(ConsumeRule::getCategoryId, srcId);
                } else {
                    w.eq(ConsumeRule::getCategoryId, srcId);
                }
            }
        });
        return consumeRuleService.update(null, uw) ? 1 : 0;
    }

    private String buildId(Integer level, String code, String parentId) {
        String c = (code == null) ? "" : code.trim();
        return c;
    }

    private void validateUniqueCode(String code, String currentId) {
        String c = (code == null) ? "" : code.trim();
        if (c.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeCategory::getCode, c);
        if (currentId != null && !currentId.trim().isEmpty()) {
            qw.ne(ConsumeCategory::getId, currentId.trim());
        }
        long cnt = categoryService.count(qw);
        if (cnt > 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Duplicate code: " + c + ", please choose another code.");
        }
    }

    private void syncAllTransactionConsumeCodes() {
        try {
            List<ConsumeCategory> all = categoryService.listAll();
            int total = 0;
            for (ConsumeCategory cc : all) {
                if (cc == null || cc.getId() == null || cc.getCode() == null) continue;
                LambdaUpdateWrapper<Transaction> uw = Wrappers.lambdaUpdate();
                uw.set(Transaction::getConsumeCode, cc.getCode())
                        .eq(Transaction::getConsumeID, cc.getId());
                total += transactionMapper.update(null, uw);
            }
            log.info("Sync all transactions consume_code done. affectedRows={}", total);
        } catch (Exception e) {
            log.warn("Sync all transactions consume_code failed: {}", e.getMessage());
        }
    }

    private boolean isDuplicateError(Throwable e) {
        while (e != null) {
            String cn = e.getClass().getName();
            if (cn.endsWith("DuplicateKeyException") ||
                    cn.endsWith("DataIntegrityViolationException") ||
                    cn.endsWith("SQLIntegrityConstraintViolationException")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private void autofillCodeAndSort(ConsumeCategory cat) {
        String parentCode = (cat.getParentId() == null) ? "" : cat.getParentId().trim();
        String code = (cat.getCode() == null) ? "" : cat.getCode().trim();
        Integer sortNo = cat.getSortNo();
        LambdaQueryWrapper<ConsumeCategory> qwSiblings = Wrappers.lambdaQuery();
        qwSiblings.eq(ConsumeCategory::getParentId, parentCode);
        List<ConsumeCategory> siblings = categoryService.list(qwSiblings);
        int nextSort = 1;
        for (ConsumeCategory s : siblings) {
            if (s.getSortNo() != null) nextSort = Math.max(nextSort, s.getSortNo() + 1);
        }
        if (sortNo == null || sortNo <= 0) {
            cat.setSortNo(nextSort);
        }
        if (code.isEmpty()) {
            String basePrefix;
            if (parentCode.isEmpty()) {
                String name = (cat.getName() == null) ? "" : cat.getName().trim();
                basePrefix = name.isEmpty() ? "CAT" : name.toUpperCase().replaceAll("\\s+", "_");
            } else {
                basePrefix = parentCode;
            }
            int next = 1;
            for (ConsumeCategory s : siblings) {
                String sc = s.getCode();
                if (sc == null) continue;
                if (sc.equals(basePrefix)) {
                    next = Math.max(next, 2);
                }
                if (sc.startsWith(basePrefix + "-")) {
                    String suf = sc.substring((basePrefix + "-").length());
                    try {
                        int n = Integer.parseInt(suf);
                        next = Math.max(next, n + 1);
                    } catch (Exception ignore) {
                    }
                }
            }
            String suffix = (next < 10) ? ("0" + next) : String.valueOf(next);
            cat.setCode(basePrefix + "-" + suffix);
        }
    }

    private static ConsumeCategory ensureConsumeCategory(Category raw) {
        if (raw instanceof ConsumeCategory cc) {
            return cc;
        }
        ConsumeCategory cc = new ConsumeCategory();
        cc.setId(raw.getId());
        cc.setParentId(raw.getParentId());
        cc.setCode(raw.getCode());
        cc.setName(raw.getName());
        cc.setLevel(raw.getLevel());
        cc.setSortNo(raw.getSortNo());
        cc.setDeleted(raw.getDeleted());
        cc.setTxnTypes(raw.getTxnTypes());
        cc.setReportRole(raw.getReportRole());
        cc.setVersion(raw.getVersion());
        cc.setCreatedAt(raw.getCreatedAt());
        cc.setUpdatedAt(raw.getUpdatedAt());
        cc.setCreatedBy(raw.getCreatedBy());
        cc.setUpdatedBy(raw.getUpdatedBy());
        return cc;
    }

    private static void applyReportRole(ConsumeCategory cat) {
        String explicit = CategoryReportRoles.normalize(cat.getReportRole());
        if (explicit != null) {
            cat.setReportRole(explicit);
            return;
        }
        String inferred = CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow(
                        cat.getCode(),
                        cat.getName(),
                        cat.getLevel() == null ? 0 : cat.getLevel(),
                        cat.getParentId(),
                        cat.getTxnTypes()))
                .orElse("other");
        cat.setReportRole(inferred);
    }
}

