package com.finsight.application.classification;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.CategoryAlias;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.infrastructure.mapper.CategoryAliasMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CategoryAliasService {

    private final CategoryAliasMapper aliasMapper;
    private final AuthenticationFacade authenticationFacade;

    public CategoryAliasService(CategoryAliasMapper aliasMapper, AuthenticationFacade authenticationFacade) {
        this.aliasMapper = aliasMapper;
        this.authenticationFacade = authenticationFacade;
    }

    public void recordMergeAlias(ConsumeCategory source, ConsumeCategory target, String reason) {
        if (source == null || target == null) {
            return;
        }
        String aliasCode = StringUtils.trimToNull(source.getCode());
        if (aliasCode == null) {
            return;
        }
        String targetRef = StringUtils.defaultIfBlank(target.getId(), target.getCode());
        if (targetRef == null) {
            return;
        }
        CategoryAlias row = new CategoryAlias();
        row.setId(UUID.randomUUID().toString().replace("-", ""));
        row.setCategoryId(targetRef);
        row.setAliasCode(aliasCode);
        row.setAliasName(StringUtils.trimToNull(source.getName()));
        row.setReason(StringUtils.defaultIfBlank(reason, "MERGE"));
        String user = authenticationFacade.getUserName();
        if (user != null && !user.isBlank()) {
            row.setCreatedBy(user);
            row.setUpdatedBy(user);
        }
        aliasMapper.insert(row);
    }
}
