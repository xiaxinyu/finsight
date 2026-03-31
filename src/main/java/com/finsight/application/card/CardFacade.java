package com.finsight.application.card;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.KeyValue;
import com.finsight.web.restful.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardFacade {

    @Autowired
    private BankCardService bankCardService;

    public List<KeyValue> listNumbers(String bankCode, String cardTypeCode) {
        String b = bankCode == null ? "" : bankCode.trim().toUpperCase();
        String t = cardTypeCode == null ? "" : cardTypeCode.trim().toLowerCase();
        if (b.isEmpty() || t.isEmpty()) {
            return Collections.emptyList();
        }
        List<BankCard> cards = bankCardService.listByBankAndType(b, t);
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        return cards.stream().map(c -> {
            KeyValue kv = new KeyValue();
            kv.setKey(c.getCardNo());
            kv.setValue(displayName(c));
            return kv;
        }).collect(Collectors.toList());
    }

    public Map<String, String> cardName(String bankCode, String cardTypeCode, String cardNo) {
        String b = bankCode == null ? "" : bankCode.trim().toUpperCase();
        String t = cardTypeCode == null ? "" : cardTypeCode.trim().toLowerCase();
        String n = cardNo == null ? "" : cardNo.trim();
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("key", n);
        if (b.isEmpty() || t.isEmpty() || n.isEmpty()) {
            kv.put("value", "");
            return kv;
        }
        BankCard card = bankCardService.getByBankTypeNo(b, t, n);
        kv.put("value", card == null ? "" : safe(card.getCardName()));
        return kv;
    }

    public List<KeyValue> allCards() {
        List<KeyValue> result = new ArrayList<>();
        KeyValue all = new KeyValue();
        all.setKey("kong");
        all.setValue("All cards");
        result.add(all);

        List<BankCard> cards = bankCardService.listAllEnabled();
        if (cards == null || cards.isEmpty()) {
            return result;
        }
        result.addAll(cards.stream().map(c -> {
            KeyValue kv = new KeyValue();
            kv.setKey(c.getId());
            kv.setValue(displayName(c));
            return kv;
        }).collect(Collectors.toList()));
        return result;
    }

    public List<BankCard> listCards(String cardTypeCode) {
        LambdaQueryWrapper<BankCard> qw = Wrappers.lambdaQuery();
        qw.select(BankCard::getId, BankCard::getBankCode, BankCard::getCardTypeCode, BankCard::getCardNo, BankCard::getCardName, BankCard::getDeleted)
                .eq(BankCard::getDeleted, 0);
        if (cardTypeCode != null && !cardTypeCode.trim().isEmpty()) {
            qw.eq(BankCard::getCardTypeCode, cardTypeCode.trim().toLowerCase());
        }
        qw.orderByAsc(BankCard::getBankCode).orderByAsc(BankCard::getCardTypeCode).orderByAsc(BankCard::getCardNo);
        return bankCardService.list(qw);
    }

    public BankCard add(BankCard card) {
        String bankUpper = card.getBankCode() == null ? "" : card.getBankCode().trim().toUpperCase();
        String typeLower = card.getCardTypeCode() == null ? "" : card.getCardTypeCode().trim().toLowerCase();
        card.setBankCode(bankUpper);
        card.setCardTypeCode(typeLower);
        card.setId(generateCardId(bankUpper, typeLower));
        if (card.getDeleted() == null) {
            card.setDeleted(0);
        }
        if (card.getCreateUser() == null) {
            card.setCreateUser("system");
        }
        if (card.getUpdateUser() == null) {
            card.setUpdateUser("system");
        }
        if (card.getCreateTime() == null) {
            card.setCreateTime(new java.util.Date());
        }
        card.setUpdateTime(new java.util.Date());
        bankCardService.save(card);
        return card;
    }

    public BankCard update(String id, BankCard card) {
        card.setId(id);
        if (card.getDeleted() == null) {
            card.setDeleted(0);
        }
        if (card.getUpdateUser() == null) {
            card.setUpdateUser("system");
        }
        card.setUpdateTime(new java.util.Date());
        bankCardService.updateById(card);
        return card;
    }

    public void delete(String id) {
        BankCard c = bankCardService.getById(id);
        if (c != null) {
            c.setDeleted(1);
            bankCardService.updateById(c);
        }
    }

    public List<TreeNode> tree() {
        List<TreeNode> parents = new ArrayList<>();
        String[] types = new String[]{"credit", "debit"};
        String[] typeTexts = new String[]{"Credit", "Debit"};
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            String typeText = typeTexts[i];
            TreeNode p = new TreeNode();
            p.setId(type);
            p.setText(typeText);
            LambdaQueryWrapper<BankCard> qw = Wrappers.lambdaQuery();
            qw.select(BankCard::getId, BankCard::getBankCode, BankCard::getCardTypeCode, BankCard::getCardNo, BankCard::getCardName, BankCard::getDeleted)
                    .eq(BankCard::getCardTypeCode, type)
                    .eq(BankCard::getDeleted, 0)
                    .orderByAsc(BankCard::getBankCode)
                    .orderByAsc(BankCard::getCardNo);
            List<BankCard> children = bankCardService.list(qw);
            List<TreeNode> childNodes = children.stream().map(c -> {
                TreeNode n = new TreeNode();
                n.setId(c.getId());
                n.setText(displayName(c));
                return n;
            }).collect(Collectors.toList());
            p.setChildren(childNodes);
            if (!childNodes.isEmpty()) {
                p.setState("closed");
            }
            parents.add(p);
        }
        return parents;
    }

    private String displayName(BankCard c) {
        String name = safe(c.getCardName());
        if (!name.isEmpty()) {
            return name;
        }
        String bank = safe(c.getBankCode());
        String type = safe(c.getCardTypeCode());
        String no = safe(c.getCardNo());
        String masked = no.isEmpty() ? "" : (no.length() > 4 ? ("****" + no.substring(no.length() - 4)) : no);
        if ("credit".equalsIgnoreCase(type)) {
            return String.join(" ", bank, "Credit Card", masked).trim();
        }
        if ("debit".equalsIgnoreCase(type)) {
            return String.join(" ", bank, "Debit Card", masked).trim();
        }
        return String.join(" ", bank, masked).trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String generateCardId(String bankUpper, String typeLower) {
        String bankLower = bankUpper == null ? "" : bankUpper.toLowerCase();
        String tShort = "credit".equals(typeLower) ? "c" : ("debit".equals(typeLower) ? "d" : (typeLower == null || typeLower.isEmpty() ? "x" : typeLower.substring(0, 1)));
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^id-" + java.util.regex.Pattern.quote(bankLower) + "-" + java.util.regex.Pattern.quote(tShort) + "-(\\d+)$");
        LambdaQueryWrapper<BankCard> qw = Wrappers.lambdaQuery();
        qw.select(BankCard::getId).eq(BankCard::getBankCode, bankUpper).eq(BankCard::getCardTypeCode, typeLower);
        List<BankCard> rows = bankCardService.list(qw);
        int next = 1;
        for (BankCard r : rows) {
            String id = r.getId();
            if (id == null) continue;
            java.util.regex.Matcher m = p.matcher(id.trim());
            if (m.find()) {
                try {
                    next = Math.max(next, Integer.parseInt(m.group(1)) + 1);
                } catch (Exception ignore) {
                }
            }
        }
        String candidate;
        do {
            candidate = String.format("id-%s-%s-%03d", bankLower, tShort, next);
            next++;
        } while (bankCardService.getById(candidate) != null);
        return candidate;
    }
}

