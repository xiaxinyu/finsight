package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.Loan;
import com.finsight.domain.model.LoanTxnLink;
import com.finsight.domain.model.LoanLenderYearFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface LoanMapper {

    List<Loan> listLoans(@Param("userId") String userId);

    Loan findLoan(@Param("id") String id, @Param("userId") String userId);

    int insertLoan(@Param("loan") Loan loan, @Param("userId") String userId, @Param("actor") String actor);

    int updateLoan(@Param("loan") Loan loan, @Param("actor") String actor);

    int softDeleteLoan(@Param("id") String id, @Param("userId") String userId, @Param("actor") String actor);

    List<LoanTxnLink> listLoanLinks(@Param("loanId") String loanId, @Param("userId") String userId);

    LoanTxnLink findLoanLink(@Param("loanId") String loanId,
                             @Param("transactionId") String transactionId,
                             @Param("userId") String userId);

    int insertLoanLink(@Param("link") LoanTxnLink link, @Param("userId") String userId, @Param("actor") String actor);

    int deleteLoanLink(@Param("loanId") String loanId,
                       @Param("transactionId") String transactionId,
                       @Param("userId") String userId);

    int deleteAllLoanLinks(@Param("loanId") String loanId, @Param("userId") String userId);

    BigDecimal sumActiveLoanOutstanding(@Param("userId") String userId);

    BigDecimal sumActiveLoanMonthlyPayment(@Param("userId") String userId);

    List<LoanLenderYearFlow> sumLoanLinkFlowByLenderYear(@Param("userId") String userId,
                                                           @Param("rangeStart") java.sql.Date rangeStart,
                                                           @Param("rangeEndExclusive") java.sql.Date rangeEndExclusive);
}
