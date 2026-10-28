package dev.amethyst.client.pvp;

/** Shares one combat interaction among manual input and enabled automation per client tick. */
public final class CombatActionBudget {
    private boolean used;

    public void reset() { used = false; }
    public boolean available() { return !used; }
    public void mark() { used = true; }
    public boolean claim() {
        if (used) return false;
        used = true;
        return true;
    }
}
