package com.carddemo.integration;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.carddemo.service.InterestCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FinancialProcessingIntegrationTest {

    @Autowired
    private InterestCalculationService interestCalculationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionCategoryBalanceRepository tcatBalRepository;

    @Autowired
    private DiscountGroupRepository discountGroupRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String ACCOUNT_ID_1 = "00000000001";
    private static final String ACCOUNT_ID_2 = "00000000002";
    private static final String CARD_NUMBER_1 = "4111111111111111";
    private static final String CARD_NUMBER_2 = "4222222222222222";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        tcatBalRepository.deleteAll();
        discountGroupRepository.deleteAll();
        cardXrefRepository.deleteAll();
        accountRepository.deleteAll();

        Account account1 = new Account();
        account1.setAccountId(ACCOUNT_ID_1);
        account1.setActiveStatus("Y");
        account1.setCurrentBalance(new BigDecimal("5000.00"));
        account1.setCreditLimit(new BigDecimal("10000.00"));
        account1.setCurrentCycleCreditAmount(new BigDecimal("200.00"));
        account1.setCurrentCycleDebitAmount(new BigDecimal("300.00"));
        account1.setGroupId("GOLD");
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setAccountId(ACCOUNT_ID_2);
        account2.setActiveStatus("Y");
        account2.setCurrentBalance(new BigDecimal("3000.00"));
        account2.setCreditLimit(new BigDecimal("8000.00"));
        account2.setCurrentCycleCreditAmount(new BigDecimal("100.00"));
        account2.setCurrentCycleDebitAmount(new BigDecimal("150.00"));
        account2.setGroupId("SILVER");
        accountRepository.save(account2);

        CardXref xref1 = new CardXref();
        xref1.setCardNumber(CARD_NUMBER_1);
        xref1.setAccountId(ACCOUNT_ID_1);
        xref1.setCustomerId("000000001");
        cardXrefRepository.save(xref1);

        CardXref xref2 = new CardXref();
        xref2.setCardNumber(CARD_NUMBER_2);
        xref2.setAccountId(ACCOUNT_ID_2);
        xref2.setCustomerId("000000002");
        cardXrefRepository.save(xref2);

        DiscountGroup goldRate = new DiscountGroup();
        goldRate.setGroupId("GOLD");
        goldRate.setTypeCode("01");
        goldRate.setCategoryCode("001");
        goldRate.setInterestRate(new BigDecimal("12.0000"));
        discountGroupRepository.save(goldRate);

        DiscountGroup goldRate2 = new DiscountGroup();
        goldRate2.setGroupId("GOLD");
        goldRate2.setTypeCode("02");
        goldRate2.setCategoryCode("002");
        goldRate2.setInterestRate(new BigDecimal("18.0000"));
        discountGroupRepository.save(goldRate2);

        DiscountGroup defaultRate = new DiscountGroup();
        defaultRate.setGroupId("DEFAULT");
        defaultRate.setTypeCode("01");
        defaultRate.setCategoryCode("001");
        defaultRate.setInterestRate(new BigDecimal("24.0000"));
        discountGroupRepository.save(defaultRate);

        TransactionCategoryBalance bal1 = new TransactionCategoryBalance();
        bal1.setAccountId(ACCOUNT_ID_1);
        bal1.setTypeCode("01");
        bal1.setCategoryCode("001");
        bal1.setBalance(new BigDecimal("2000.00"));
        tcatBalRepository.save(bal1);

        TransactionCategoryBalance bal2 = new TransactionCategoryBalance();
        bal2.setAccountId(ACCOUNT_ID_1);
        bal2.setTypeCode("02");
        bal2.setCategoryCode("002");
        bal2.setBalance(new BigDecimal("1500.00"));
        tcatBalRepository.save(bal2);

        TransactionCategoryBalance bal3 = new TransactionCategoryBalance();
        bal3.setAccountId(ACCOUNT_ID_2);
        bal3.setTypeCode("01");
        bal3.setCategoryCode("001");
        bal3.setBalance(new BigDecimal("1000.00"));
        tcatBalRepository.save(bal3);
    }

    @Nested
    class InterestCalculation {

        @Test
        void shouldCalculateInterestUsingGroupRate() {
            interestCalculationService.calculateMonthlyInterest();

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID_1).orElseThrow();

            BigDecimal interest1 = new BigDecimal("2000.00").multiply(new BigDecimal("12.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            BigDecimal interest2 = new BigDecimal("1500.00").multiply(new BigDecimal("18.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            BigDecimal totalInterest = interest1.add(interest2);

            assertThat(updated.getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("5000.00").add(totalInterest));
        }

        @Test
        void shouldFallbackToDefaultGroupWhenNotFound() {
            interestCalculationService.calculateMonthlyInterest();

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID_2).orElseThrow();

            BigDecimal interest = new BigDecimal("1000.00").multiply(new BigDecimal("24.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);

            assertThat(updated.getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("3000.00").add(interest));
        }

        @Test
        void shouldResetCycleAmountsToZero() {
            interestCalculationService.calculateMonthlyInterest();

            Account updated1 = accountRepository.findByAccountId(ACCOUNT_ID_1).orElseThrow();
            assertThat(updated1.getCurrentCycleCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(updated1.getCurrentCycleDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);

            Account updated2 = accountRepository.findByAccountId(ACCOUNT_ID_2).orElseThrow();
            assertThat(updated2.getCurrentCycleCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(updated2.getCurrentCycleDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldGenerateSystemTransactionRecords() {
            interestCalculationService.calculateMonthlyInterest();

            List<Transaction> systemTxns = transactionRepository.findAll().stream()
                .filter(t -> "01".equals(t.getTypeCode()) && "05".equals(t.getCategoryCode()))
                .toList();

            assertThat(systemTxns).hasSize(2);
        }

        @Test
        void shouldPopulateSystemTransactionFieldsCorrectly() {
            interestCalculationService.calculateMonthlyInterest();

            Transaction sysTxn = transactionRepository.findAll().stream()
                .filter(t -> "01".equals(t.getTypeCode()) && "05".equals(t.getCategoryCode()))
                .filter(t -> t.getDetails() != null && t.getDetails().contains(ACCOUNT_ID_1))
                .findFirst()
                .orElseThrow();

            assertThat(sysTxn.getTypeCode()).isEqualTo("01");
            assertThat(sysTxn.getCategoryCode()).isEqualTo("05");
            assertThat(sysTxn.getSource()).isEqualTo("System");
            assertThat(sysTxn.getDetails()).contains(ACCOUNT_ID_1);
            assertThat(sysTxn.getCardNumber()).isEqualTo(CARD_NUMBER_1);
            assertThat(sysTxn.getTransactionId()).isNotNull();
            assertThat(sysTxn.getTransactionId()).hasSize(16);
            assertThat(sysTxn.getTimestamp()).isNotNull();
            assertThat(sysTxn.getTimestamp()).matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d+");
        }

        @Test
        void shouldCalculateZeroInterestForZeroRate() {
            DiscountGroup zeroRate = new DiscountGroup();
            zeroRate.setGroupId("GOLD");
            zeroRate.setTypeCode("03");
            zeroRate.setCategoryCode("003");
            zeroRate.setInterestRate(BigDecimal.ZERO);
            discountGroupRepository.save(zeroRate);

            TransactionCategoryBalance zeroBal = new TransactionCategoryBalance();
            zeroBal.setAccountId(ACCOUNT_ID_1);
            zeroBal.setTypeCode("03");
            zeroBal.setCategoryCode("003");
            zeroBal.setBalance(new BigDecimal("5000.00"));
            tcatBalRepository.save(zeroBal);

            BigDecimal balanceBefore = accountRepository.findByAccountId(ACCOUNT_ID_1).orElseThrow().getCurrentBalance();

            interestCalculationService.calculateMonthlyInterest();

            BigDecimal interest1 = new BigDecimal("2000.00").multiply(new BigDecimal("12.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            BigDecimal interest2 = new BigDecimal("1500.00").multiply(new BigDecimal("18.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID_1).orElseThrow();
            assertThat(updated.getCurrentBalance())
                .isEqualByComparingTo(balanceBefore.add(interest1).add(interest2));
        }

        @Test
        void shouldProcessMultipleAccountsIndependently() {
            interestCalculationService.calculateMonthlyInterest();

            Account acct1 = accountRepository.findByAccountId(ACCOUNT_ID_1).orElseThrow();
            Account acct2 = accountRepository.findByAccountId(ACCOUNT_ID_2).orElseThrow();

            assertThat(acct1.getCurrentBalance()).isNotEqualTo(new BigDecimal("5000.00"));
            assertThat(acct2.getCurrentBalance()).isNotEqualTo(new BigDecimal("3000.00"));
            assertThat(acct1.getCurrentBalance()).isNotEqualTo(acct2.getCurrentBalance());
        }

        @Test
        void shouldUseTransactionIdWithDatePrefix() {
            interestCalculationService.calculateMonthlyInterest();

            Transaction sysTxn = transactionRepository.findAll().stream()
                .filter(t -> "01".equals(t.getTypeCode()))
                .findFirst()
                .orElseThrow();

            assertThat(sysTxn.getTransactionId()).hasSize(16);
            String datePrefix = sysTxn.getTransactionId().substring(0, 8);
            assertThat(datePrefix).matches("\\d{8}");
        }
    }
}
