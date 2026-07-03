package com.finsight.infrastructure.repository;

import com.finsight.domain.model.Bill;
import com.finsight.domain.port.BillRepository;
import com.finsight.infrastructure.mapper.FinPlanningMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BillMybatisRepository implements BillRepository {

    private final FinPlanningMapper mapper;

    public BillMybatisRepository(FinPlanningMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Bill> listEnabled(String userId) {
        return mapper.listBills(userId).stream()
                .filter(b -> b.getEnabled() == null || b.getEnabled() == 1)
                .toList();
    }

    @Override
    public Bill save(Bill bill, String userId) {
        if (bill.getId() == null || bill.getId().isBlank()) {
            bill.setId(UUID.randomUUID().toString());
            if (bill.getEnabled() == null) {
                bill.setEnabled(1);
            }
            if (bill.getDeleted() == null) {
                bill.setDeleted(0);
            }
            mapper.insertBill(bill, userId);
        } else {
            mapper.updateBill(bill);
        }
        return bill;
    }

    @Override
    public boolean hasAnyForUser(String userId) {
        return mapper.countBillsForUser(userId) > 0;
    }

    @Override
    public void softDelete(String billId, String userId) {
        mapper.softDeleteBill(billId, userId);
    }
}
