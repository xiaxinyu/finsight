package com.finsight.domain.port;

import com.finsight.domain.model.Bill;

import java.util.List;

public interface BillRepository {

    List<Bill> listEnabled(String userId);

    Bill save(Bill bill, String userId);

    boolean hasAnyForUser(String userId);
}
