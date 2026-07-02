package com.finsight.domain.port;

import com.finsight.domain.model.Loan;
import com.finsight.domain.model.LoanTxnLink;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    List<Loan> listActive(String userId);

    Optional<Loan> findById(String id, String userId);

    Loan save(Loan loan, String userId, String actor);

    void softDelete(String id, String userId, String actor);

    List<LoanTxnLink> listLinks(String loanId, String userId);

    Optional<LoanTxnLink> findLink(String loanId, String transactionId, String userId);

    LoanTxnLink addLink(LoanTxnLink link, String userId, String actor);

    void removeLink(String loanId, String transactionId, String userId);

    void removeAllLinks(String loanId, String userId);
}
