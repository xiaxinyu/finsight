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
                .set(ConsumeCategory::getTxnTypes, cat.getTxnTypes());
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

        if (target.getLevel() != null && target.getLevel() == 1) {
            LambdaUpdateWrapper<ConsumeCategory> uwMove = Wrappers.lambdaUpdate();
            uwMove.eq(ConsumeCategory::getId, src.getId())
                    .set(ConsumeCategory::getParentId, tgtCode)
                    .set(ConsumeCategory::getLevel, 2);
            categoryService.update(null, uwMove);
            log.info("Reparent category: src[id={},code={}] -> parent {}", src.getId(), src.getCode(), tgtCode);
        } else {
            int affected1 = 0, affected2 = 0, affected3 = 0, affected4 = 0, affected5 = 0, affected6 = 0;
            if (cascade) {
                LambdaUpdateWrapper<Transaction> uw1 = Wrappers.lambdaUpdate();
                uw1.set(Transaction::getConsumeID, tgtCode)
                        .set(Transaction::getConsumeCode, tgtCode)
                        .set(Transaction::getConsumeName, tgtName)
                        .eq(Transaction::getConsumeID, src.getId());
                affected1 = transactionMapper.update(null, uw1);

                LambdaUpdateWrapper<Transaction> uw2 = Wrappers.lambdaUpdate();
                uw2.set(Transaction::getConsumeID, tgtCode)
                        .set(Transaction::getConsumeCode, tgtCode)
                        .set(Transaction::getConsumeName, tgtName)
                        .eq(Transaction::getConsumeID, src.getCode());
                affected2 = transactionMapper.update(null, uw2);

                LambdaUpdateWrapper<Transaction> uw3 = Wrappers.lambdaUpdate();
                uw3.set(Transaction::getConsumeCode, tgtCode)
                        .set(Transaction::getConsumeName, tgtName)
                        .eq(Transaction::getConsumeCode, src.getCode());
                affected3 = transactionMapper.update(null, uw3);

                LambdaUpdateWrapper<Transaction> uw4 = Wrappers.lambdaUpdate();
                uw4.set(Transaction::getConsumeCode, tgtCode)
                        .set(Transaction::getConsumeName, tgtName)
                        .like(Transaction::getConsumeID, "/" + src.getCode());
                affected4 = transactionMapper.update(null, uw4);

                LambdaUpdateWrapper<Transaction> uw5 = Wrappers.lambdaUpdate();
                uw5.set(Transaction::getConsumeCode, tgtCode)
                        .set(Transaction::getConsumeName, tgtName)
                        .like(Transaction::getConsumeID, "-" + src.getCode());
                affected5 = transactionMapper.update(null, uw5);

                if (src.getName() != null && !src.getName().trim().isEmpty()) {
                    LambdaUpdateWrapper<Transaction> uw6 = Wrappers.lambdaUpdate();
                    uw6.set(Transaction::getConsumeCode, tgtCode)
                            .eq(Transaction::getConsumeName, src.getName());
                    affected6 = transactionMapper.update(null, uw6);
                }
            }
            log.info("Migrate transactions from src[id={},code={}] to target[code={},name={}], affected rows: byId={}, byConsumeId={}, byConsumeCodeCol={}, likeSlash={}, likeDash={}, byName={}",
                    src.getId(), src.getCode(), tgtCode, tgtName, affected1, affected2, affected3, affected4, affected5, affected6);

            if (deleteAfter) {
                LambdaUpdateWrapper<ConsumeCategory> uwDel = Wrappers.lambdaUpdate();
                uwDel.eq(ConsumeCategory::getId, src.getId())
                        .set(ConsumeCategory::getDeleted, 1);
                categoryService.update(null, uwDel);
                log.info("Soft-deleted source category after migration: id={}, code={}", src.getId(), src.getCode());
            }
        }
        syncAllTransactionConsumeCodes();
        CollectionResult<String> r = new CollectionResult<>();
        r.setRows(java.util.Arrays.asList("ok"));
        r.setTotal(1);
        return r;
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
        cc.setVersion(raw.getVersion());
        cc.setCreatedAt(raw.getCreatedAt());
        cc.setUpdatedAt(raw.getUpdatedAt());
        cc.setCreatedBy(raw.getCreatedBy());
        cc.setUpdatedBy(raw.getUpdatedBy());
        return cc;
    }
}

