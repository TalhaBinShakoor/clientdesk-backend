package com.clientdesk.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionDemoDataGuardTest {

    @Test
    void allowsProductionStartupWhenDemoDataIsAbsent() {
        assertDoesNotThrow(() -> ProductionDemoDataGuard.rejectDemoData(false, false));
    }

    @Test
    void refusesProductionStartupWhenDemoOrganizationExists() {
        assertThrows(
                IllegalStateException.class,
                () -> ProductionDemoDataGuard.rejectDemoData(true, false));
    }

    @Test
    void refusesProductionStartupWhenDemoUserExists() {
        assertThrows(
                IllegalStateException.class,
                () -> ProductionDemoDataGuard.rejectDemoData(false, true));
    }
}
