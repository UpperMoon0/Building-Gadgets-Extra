package com.nstut.buildinggadgetsextra.clienttest;

/**
 * Loader- and Minecraft-independent state machine for the real-client range round-trip test.
 * Version adapters must drive the actual screen widgets and read the synchronized client stack.
 */
public final class ClientRangeRoundTripScenario {
    public static final String ENABLE_PROPERTY = "bge.clientIntegrationTest";
    public static final int START_RANGE = 1;
    public static final int TARGET_RANGE = 7;
    public static final int TIMEOUT_TICKS = 600;

    public interface Adapter {
        boolean hasReadyMultitool();
        int clientRange();
        void openMultitoolScreen();
        boolean isMultitoolScreenOpen();
        int visibleScreenRange();
        void enterBuildSubmenu();
        void clickRangePlus();
        void closeScreen();
        void pass(String detail);
        void fail(String detail, Throwable error);
    }

    private enum Phase {
        WAIT_FOR_ITEM,
        OPEN_SCREEN,
        ENTER_BUILD,
        CLICK_RANGE,
        WAIT_FOR_SYNC,
        CLOSE_SCREEN,
        REOPEN_SCREEN,
        VERIFY_REOPEN,
        DONE
    }

    private final Adapter adapter;
    private Phase phase = Phase.WAIT_FOR_ITEM;
    private int ticks;
    private int clicks;
    private int phaseDelay;

    public ClientRangeRoundTripScenario(Adapter adapter) {
        this.adapter = adapter;
    }

    public void tick() {
        if (phase == Phase.DONE) return;
        try {
            ticks++;
            if (ticks > TIMEOUT_TICKS) {
                fail("timeout phase=" + phase + " clientRange=" + safeClientRange(), null);
                return;
            }
            if (phaseDelay > 0) {
                phaseDelay--;
                return;
            }

            switch (phase) {
                case WAIT_FOR_ITEM:
                    if (!adapter.hasReadyMultitool()) return;
                    require(adapter.clientRange() == START_RANGE,
                            "client setup range must start at " + START_RANGE + " but was " + adapter.clientRange());
                    advance(Phase.OPEN_SCREEN, 1);
                    break;
                case OPEN_SCREEN:
                    adapter.openMultitoolScreen();
                    advance(Phase.ENTER_BUILD, 2);
                    break;
                case ENTER_BUILD:
                    require(adapter.isMultitoolScreenOpen(), "multitool screen did not open");
                    if (adapter.visibleScreenRange() < 0) {
                        adapter.enterBuildSubmenu();
                        advance(Phase.CLICK_RANGE, 2);
                    } else {
                        advance(Phase.CLICK_RANGE, 0);
                    }
                    break;
                case CLICK_RANGE:
                    require(adapter.isMultitoolScreenOpen(), "screen closed while changing range");
                    if (clicks < TARGET_RANGE - START_RANGE) {
                        adapter.clickRangePlus();
                        clicks++;
                        phaseDelay = 1;
                        return;
                    }
                    require(adapter.visibleScreenRange() == TARGET_RANGE,
                            "real range widget did not reach " + TARGET_RANGE + "; visible=" + adapter.visibleScreenRange());
                    advance(Phase.WAIT_FOR_SYNC, 0);
                    break;
                case WAIT_FOR_SYNC:
                    if (adapter.clientRange() != TARGET_RANGE) return;
                    advance(Phase.CLOSE_SCREEN, 2);
                    break;
                case CLOSE_SCREEN:
                    adapter.closeScreen();
                    advance(Phase.REOPEN_SCREEN, 2);
                    break;
                case REOPEN_SCREEN:
                    adapter.openMultitoolScreen();
                    advance(Phase.VERIFY_REOPEN, 2);
                    break;
                case VERIFY_REOPEN:
                    require(adapter.isMultitoolScreenOpen(), "multitool screen did not reopen");
                    if (adapter.visibleScreenRange() < 0) {
                        adapter.enterBuildSubmenu();
                        phaseDelay = 2;
                        return;
                    }
                    require(adapter.clientRange() == TARGET_RANGE,
                            "synchronized client stack reverted after reopen; clientRange=" + adapter.clientRange());
                    require(adapter.visibleScreenRange() == TARGET_RANGE,
                            "reopened UI did not read synchronized range; visible=" + adapter.visibleScreenRange());
                    phase = Phase.DONE;
                    adapter.pass("ui->c2s->server->s2c->ui range round trip persisted " + TARGET_RANGE);
                    break;
                default:
                    break;
            }
        } catch (Throwable error) {
            fail("exception phase=" + phase + " message=" + error.getMessage(), error);
        }
    }

    private void advance(Phase next, int delay) {
        phase = next;
        phaseDelay = delay;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private int safeClientRange() {
        try {
            return adapter.clientRange();
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private void fail(String detail, Throwable error) {
        phase = Phase.DONE;
        adapter.fail(detail, error);
    }
}
