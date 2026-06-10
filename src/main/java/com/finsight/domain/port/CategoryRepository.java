package com.finsight.domain.port;

import com.finsight.domain.model.ConsumeCategory;

import java.util.List;

public interface CategoryRepository {

    ConsumeCategory findById(String id);

    ConsumeCategory findByCode(String code);

    List<ConsumeCategory> listActive();
}
