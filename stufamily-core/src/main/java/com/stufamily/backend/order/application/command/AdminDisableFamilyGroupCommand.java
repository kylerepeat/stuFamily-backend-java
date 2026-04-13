package com.stufamily.backend.order.application.command;

public record AdminDisableFamilyGroupCommand(
    String orderNo,
    Long operatorUserId
) {
}
