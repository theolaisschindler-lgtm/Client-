package dev.amethyst.client.pvp;

import java.util.OptionalInt;

/** A hotbar loan is discarded as soon as somebody selects a different slot. */
final class WeaponSlot {
    private int previous = -1;
    private int selected = -1;

    void acquire(int previous, int selected) {
        this.previous = previous;
        this.selected = selected;
    }

    boolean active() { return selected >= 0; }
    boolean owns(int current) { return active() && current == selected; }

    OptionalInt release(int current, boolean restore) {
        OptionalInt result = restore && owns(current) ? OptionalInt.of(previous) : OptionalInt.empty();
        clear();
        return result;
    }

    void clear() { previous = selected = -1; }
}
