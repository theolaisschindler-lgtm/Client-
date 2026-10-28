package dev.amethyst.client.pvp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatActionBudgetTest {
    @Test void aManualAttackBlocksAllFollowingModulesUntilTheNextTick() {
        var budget = new CombatActionBudget();
        budget.mark();
        assertFalse(budget.claim());
        assertFalse(budget.available());
        budget.reset();
        assertTrue(budget.claim());
    }

    @Test void onlyTheFirstCompetingModuleClaimsATick() {
        var budget = new CombatActionBudget();
        assertTrue(budget.claim());
        assertFalse(budget.claim());
        assertFalse(budget.claim());
        budget.reset();
        assertTrue(budget.available());
    }
}
