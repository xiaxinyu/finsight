package com.finsight.application.consume;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.web.api.dto.TreeNode;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ConsumeCategoryService extends IService<ConsumeCategory> {
    List<ConsumeCategory> listAll();
    List<TreeNode> tree();
    List<TreeNode> tree(String txnType);
    void ensureDefaults();
}
