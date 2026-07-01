package com.finsight.web.security;

import com.finsight.common.security.SecurityRoles;
import com.finsight.domain.model.BankCard;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> ("ROLE_" + SecurityRoles.ADMIN).equals(a));
    }

    public static BankCard maskCardIfNeeded(BankCard card) {
        if (card == null || isAdmin()) {
            return card;
        }
        card.setCardNo(com.finsight.common.security.SensitiveDataMasker.maskCardNumber(card.getCardNo()));
        return card;
    }
}
