package com.finsight.application.query;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.port.CategoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Normalizes {@link TransactionQuery} so list/count/report queries match the same ledger rows.
 */
@Service
public class TransactionQuerySupport {

    private final CategoryRepository categoryRepository;

    public TransactionQuerySupport(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void enrich(TransactionQuery query) {
        if (query == null) {
            return;
        }
        expandCategoryFilter(query);
    }

    /**
     * Expands a selected parent category to include its child category codes/ids.
     */
    void expandCategoryFilter(TransactionQuery query) {
        if (query.getConsumes() == null || query.getConsumes().length == 0) {
            return;
        }
        List<ConsumeCategory> all = categoryRepository.listActive();
        if (all == null || all.isEmpty()) {
            return;
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String raw : query.getConsumes()) {
            String selected = StringUtils.trimToEmpty(raw);
            if (selected.isEmpty()) {
                continue;
            }
            codes.add(selected);
            ConsumeCategory match = null;
            for (ConsumeCategory cat : all) {
                if (cat == null) {
                    continue;
                }
                if (selected.equals(StringUtils.trimToEmpty(cat.getCode()))
                        || selected.equals(StringUtils.trimToEmpty(cat.getId()))) {
                    match = cat;
                    break;
                }
            }
            if (match != null) {
                addCategoryKeys(codes, match);
                String parentCode = StringUtils.trimToEmpty(match.getCode());
                for (ConsumeCategory child : all) {
                    if (child == null) {
                        continue;
                    }
                    if (parentCode.equals(StringUtils.trimToEmpty(child.getParentId()))) {
                        addCategoryKeys(codes, child);
                    }
                }
            }
        }
        if (!codes.isEmpty()) {
            query.setConsumes(codes.toArray(new String[0]));
        }
    }

    private static void addCategoryKeys(Set<String> codes, ConsumeCategory cat) {
        if (StringUtils.isNotBlank(cat.getCode())) {
            codes.add(cat.getCode().trim());
        }
        if (StringUtils.isNotBlank(cat.getId())) {
            codes.add(cat.getId().trim());
        }
    }
}
