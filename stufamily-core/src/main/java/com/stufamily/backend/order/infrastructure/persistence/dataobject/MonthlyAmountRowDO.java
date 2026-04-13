package com.stufamily.backend.order.infrastructure.persistence.dataobject;

public class MonthlyAmountRowDO {
    private String month;
    private Long amountCents;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }
}
