-- Optional total term and manually recorded paid installments for loan cards.

ALTER TABLE fin_loan
    ADD COLUMN term_months INT NULL COMMENT 'Total loan term in months' AFTER monthly_payment,
    ADD COLUMN paid_installments INT NULL COMMENT 'Installments already paid (manual or historical)' AFTER term_months;
