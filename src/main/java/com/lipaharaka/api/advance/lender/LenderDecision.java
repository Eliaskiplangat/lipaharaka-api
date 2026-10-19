package com.lipaharaka.api.advance.lender;

public record LenderDecision(boolean approved, String externalReferenceId, String reason) {
}
