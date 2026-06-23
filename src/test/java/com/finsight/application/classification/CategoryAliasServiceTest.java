package com.finsight.application.classification;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.CategoryAlias;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.infrastructure.mapper.CategoryAliasMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryAliasServiceTest {

    @Mock
    private CategoryAliasMapper aliasMapper;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private CategoryAliasService service;

    @BeforeEach
    void setUp() {
        service = new CategoryAliasService(aliasMapper, authenticationFacade);
        when(authenticationFacade.getUserName()).thenReturn("tester");
    }

    @Test
    void recordMergeAlias_persistsSourceCodeOnTarget() {
        ConsumeCategory source = new ConsumeCategory();
        source.setId("SHOP-02");
        source.setCode("SHOP-02");
        source.setName("Legacy shop");

        ConsumeCategory target = new ConsumeCategory();
        target.setId("SHOPPING-02");
        target.setCode("SHOPPING-02");
        target.setName("Shopping misc");

        service.recordMergeAlias(source, target, "L2_MERGE");

        ArgumentCaptor<CategoryAlias> captor = ArgumentCaptor.forClass(CategoryAlias.class);
        verify(aliasMapper).insert(captor.capture());
        CategoryAlias saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("SHOPPING-02", saved.getCategoryId());
        assertEquals("SHOP-02", saved.getAliasCode());
        assertEquals("Legacy shop", saved.getAliasName());
        assertEquals("L2_MERGE", saved.getReason());
        assertEquals("tester", saved.getCreatedBy());
    }
}
