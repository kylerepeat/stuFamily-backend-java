package com.stufamily.backend.home.application.dto;

import java.util.List;

public record AdminParentMessageDetailView(
    AdminParentMessageView root,
    List<AdminParentMessageNodeView> nodes
) {
}

