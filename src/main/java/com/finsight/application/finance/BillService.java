package com.finsight.application.finance;

import com.finsight.domain.model.Bill;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillService {

    private final PlanningPreferencesGateway planningGateway;

    public BillService(PlanningPreferencesGateway planningGateway) {
        this.planningGateway = planningGateway;
    }

    public List<Bill> listEnabled() {
        return planningGateway.enabledBills();
    }

    public Bill save(Bill bill) {
        return planningGateway.saveBill(bill);
    }

    public void delete(String billId) {
        planningGateway.deleteBill(billId);
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
