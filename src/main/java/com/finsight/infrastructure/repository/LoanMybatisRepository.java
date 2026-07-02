package com.finsight.infrastructure.repository;

import com.finsight.domain.model.Loan;
import com.finsight.domain.model.LoanTxnLink;
import com.finsight.domain.port.LoanRepository;
import com.finsight.infrastructure.mapper.LoanMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LoanMybatisRepository implements LoanRepository {

    private final LoanMapper mapper;

    public LoanMybatisRepository(LoanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Loan> listActive(String userId) {
        return mapper.listLoans(userId);
    }

    @Override
    public Optional<Loan> findById(String id, String userId) {
        Loan loan = mapper.findLoan(id, userId);
        return Optional.ofNullable(loan);
    }

    @Override
    public Loan save(Loan loan, String userId, String actor) {
        if (loan.getId() == null || loan.getId().isBlank()) {
            loan.setId(UUID.randomUUID().toString());
            loan.setUserId(userId);
            if (loan.getStatus() == null || loan.getStatus().isBlank()) {
                loan.setStatus("ACTIVE");
            }
            if (loan.getSortOrder() == null) {
                loan.setSortOrder(0);
            }
            mapper.insertLoan(loan, userId, actor);
        } else {
            loan.setUserId(userId);
            mapper.updateLoan(loan, actor);
        }
        return mapper.findLoan(loan.getId(), userId);
    }

    @Override
    public void softDelete(String id, String userId, String actor) {
        mapper.softDeleteLoan(id, userId, actor);
    }

    @Override
    public List<LoanTxnLink> listLinks(String loanId, String userId) {
        return mapper.listLoanLinks(loanId, userId);
    }

    @Override
    public LoanTxnLink addLink(LoanTxnLink link, String userId, String actor) {
        if (link.getId() == null || link.getId().isBlank()) {
            link.setId(UUID.randomUUID().toString());
        }
        link.setUserId(userId);
        mapper.insertLoanLink(link, userId, actor);
        return link;
    }

    @Override
    public void removeLink(String loanId, String transactionId, String userId) {
        mapper.deleteLoanLink(loanId, transactionId, userId);
    }
}
