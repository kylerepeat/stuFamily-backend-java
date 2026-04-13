package com.stufamily.backend.adminquery.application.dto;

import java.util.List;

public record AdminFilterOptionsView(
    List<AdminSelectOptionView> productPublishStatuses,
    List<AdminSelectOptionView> weixinUserStatuses,
    List<AdminSelectOptionView> orderStatuses,
    List<AdminSelectOptionView> orderTypes,
    List<AdminSelectOptionView> familyCardStatuses
) {
}
