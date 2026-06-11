package com.finsight.domain.model;

/**
 * @deprecated use {@link ClassificationRule}.
 */
@Deprecated
public class ConsumeRule extends ClassificationRule {

    /** MyBatis-Plus service layer still persists {@link ConsumeRule}; API DTOs use {@link ClassificationRule}. */
    public static ConsumeRule from(ClassificationRule rule) {
        if (rule instanceof ConsumeRule legacy) {
            return legacy;
        }
        ConsumeRule legacy = new ConsumeRule();
        org.springframework.beans.BeanUtils.copyProperties(rule, legacy);
        return legacy;
    }
}
