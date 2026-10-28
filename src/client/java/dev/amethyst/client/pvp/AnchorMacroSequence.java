package dev.amethyst.client.pvp;

/** State transitions wait for observed blocks, rather than sending an entire packet sequence at once. */
public final class AnchorMacroSequence {
    public enum Step { IDLE, PLACE, WAIT_FOR_ANCHOR, CHARGE, WAIT_FOR_CHARGE, DETONATE }
    private Step step = Step.IDLE;
    private int delay;
    private int wait;
    private int age;

    public void begin(boolean anchorPresent, int charges, int actionDelay) {
        delay = Math.clamp(actionDelay, 1, 10);
        wait = 0;
        age = 0;
        step = anchorPresent ? (charges > 0 ? Step.DETONATE : Step.CHARGE) : Step.PLACE;
    }

    public void tick(boolean anchorPresent, int charges) {
        if (!isBusy()) return;
        if (++age > 80) { clear(); return; }
        if (wait > 0) wait--;
        if (step == Step.WAIT_FOR_ANCHOR && anchorPresent) {
            step = charges > 0 ? Step.DETONATE : Step.CHARGE;
        } else if (step == Step.WAIT_FOR_CHARGE && anchorPresent && charges > 0) {
            step = Step.DETONATE;
        } else if ((step == Step.CHARGE || step == Step.DETONATE || step == Step.WAIT_FOR_CHARGE)
                && !anchorPresent) {
            clear();
        } else if (step == Step.CHARGE && charges > 0) {
            step = Step.DETONATE;
        }
    }

    public Step nextAction() {
        return wait == 0 && (step == Step.PLACE || step == Step.CHARGE || step == Step.DETONATE)
                ? step : Step.IDLE;
    }

    public void actionResult(boolean consumed) {
        if (!consumed) { clear(); return; }
        step = switch (step) {
            case PLACE -> Step.WAIT_FOR_ANCHOR;
            case CHARGE -> Step.WAIT_FOR_CHARGE;
            default -> Step.IDLE;
        };
        wait = delay;
    }

    public boolean isBusy() { return step != Step.IDLE; }
    public void clear() { step = Step.IDLE; wait = 0; age = 0; }
}
