package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Bill;
import com.finsight.infrastructure.mapper.BillMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillService {

    private final BillMapper billMapper;
    private final AuthenticationFacade authenticationFacade;

    public BillService(BillMapper billMapper, AuthenticationFacade authenticationFacade) {
        this.billMapper = billMapper;
        this.authenticationFacade = authenticationFacade;
    }

    public List<Bill> listEnabled() {
        return billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getEnabled, 1).eq(Bill::getDeleted, 0)
                .orderByAsc(Bill::getDueDay));
    }

    public Bill save(Bill bill) {
        if (bill.getId() == null || bill.getId().isBlank()) {
            bill.setId(UUID.randomUUID().toString());
            bill.setDeleted(0);
            bill.setCreateUser(authenticationFacade.getUserName());
            bill.setCreateTime(new Date());
            if (bill.getEnabled() == null) {
                bill.setEnabled(1);
            }
            billMapper.insert(bill);
        } else {
            bill.setUpdateUser(authenticationFacade.getUserName());
            bill.setUpdateTime(new Date());
            billMapper.updateById(bill);
        }
        return bill;
    }

    public List<Map<String, Object>> calendarNext30Days() {
        List<Bill> bills = listEnabled();
        Calendar cal = Calendar.getInstance();
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            for (Bill b : bills) {
                if (b.getDueDay() != null && b.getDueDay() == dayOfMonth) {
                    Map<String, Object> ev = new LinkedHashMap<>();
                    ev.put("date", cal.getTime());
                    ev.put("name", b.getName());
                    ev.put("amount", b.getAmount());
                    ev.put("billId", b.getId());
                    events.add(ev);
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return events;
    }
}
