package com.nstut.buildinggadgetsextra.clienttest;

/**
 * Loader- and Minecraft-independent state machine for real-client multitool UI round trips.
 * Version adapters drive actual screen widgets and read the synchronized client stack.
 */
public final class ClientRangeRoundTripScenario {
    public static final String ENABLE_PROPERTY = "bge.clientIntegrationTest";
    public static final int START_RANGE = 1;
    public static final int TARGET_RANGE = 7;
    public static final int EXCHANGE_INITIAL_RANGE = 2;
    public static final int EXCHANGE_TARGET_RANGE = 4;
    public static final int COPY_START_X_TARGET = 1;
    public static final int DESTRUCTION_LEFT_TARGET = 1;
    public static final int DESTRUCTION_DEPTH_TARGET = 1;
    public static final int TIMEOUT_TICKS = 1200;

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

        default boolean supportsExtendedStateRoundTrip() { return false; }
        default int clientActiveToolOrdinal() { return -1; }
        default void switchToBuildSubmenu() { throw new UnsupportedOperationException(); }
        default void switchToExchangeSubmenu() { throw new UnsupportedOperationException(); }
        default void switchToCopySubmenu() { throw new UnsupportedOperationException(); }
        default void switchToDestructionSubmenu() { throw new UnsupportedOperationException(); }
        default boolean isCopyScreenOpen() { return false; }
        default void openCopyConfig() { throw new UnsupportedOperationException(); }
        default int visibleCopyStartX() { return Integer.MIN_VALUE; }
        default void clickCopyStartXPlus() { throw new UnsupportedOperationException(); }
        default void confirmCopyConfig() { throw new UnsupportedOperationException(); }
        default int clientCopyStartX() { return Integer.MIN_VALUE; }
        default boolean isDestructionScreenOpen() { return false; }
        default void openDestructionConfig() { throw new UnsupportedOperationException(); }
        default int visibleDestructionLeft() { return Integer.MIN_VALUE; }
        default int visibleDestructionDepth() { return Integer.MIN_VALUE; }
        default void clickDestructionLeftPlus() { throw new UnsupportedOperationException(); }
        default void clickDestructionDepthPlus() { throw new UnsupportedOperationException(); }
        default int clientDestructionLeft() { return Integer.MIN_VALUE; }
        default int clientDestructionDepth() { return Integer.MIN_VALUE; }
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
        SWITCH_EXCHANGE,
        VERIFY_EXCHANGE_INITIAL,
        CLICK_EXCHANGE_RANGE,
        WAIT_EXCHANGE_SYNC,
        SWITCH_BUILD_AGAIN,
        VERIFY_BUILD_RESTORE,
        SWITCH_EXCHANGE_AGAIN,
        VERIFY_EXCHANGE_RESTORE,
        SWITCH_COPY,
        WAIT_COPY_PROFILE,
        OPEN_COPY_CONFIG,
        EDIT_COPY_START,
        VERIFY_COPY_EDIT,
        WAIT_COPY_SYNC,
        OPEN_RADIAL_FOR_DESTRUCTION,
        SWITCH_DESTRUCTION,
        WAIT_DESTRUCTION_PROFILE,
        OPEN_DESTRUCTION_CONFIG,
        EDIT_DESTRUCTION,
        VERIFY_DESTRUCTION_EDIT,
        WAIT_DESTRUCTION_SYNC,
        REOPEN_DESTRUCTION_RADIAL,
        SWITCH_DESTRUCTION_AGAIN,
        REOPEN_DESTRUCTION_CONFIG,
        VERIFY_DESTRUCTION_REOPEN,
        DONE
    }

    private final Adapter adapter;
    private Phase phase = Phase.WAIT_FOR_ITEM;
    private int ticks;
    private int clicks;
    private int exchangeClicks;
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
                    if (!adapter.supportsExtendedStateRoundTrip()) {
                        succeed("ui->c2s->server->s2c->ui range round trip persisted " + TARGET_RANGE);
                        return;
                    }
                    advance(Phase.SWITCH_EXCHANGE, 1);
                    break;
                case SWITCH_EXCHANGE:
                    adapter.switchToExchangeSubmenu();
                    advance(Phase.VERIFY_EXCHANGE_INITIAL, 2);
                    break;
                case VERIFY_EXCHANGE_INITIAL:
                    require(adapter.isMultitoolScreenOpen(), "multitool screen closed while switching to exchange profile");
                    if (adapter.clientActiveToolOrdinal() != 1 || adapter.clientRange() != EXCHANGE_INITIAL_RANGE) return;
                    if (adapter.visibleScreenRange() < 0) return;
                    require(adapter.visibleScreenRange() == EXCHANGE_INITIAL_RANGE,
                            "exchange submenu was built from stale profile state; visible=" + adapter.visibleScreenRange()
                                    + " client=" + adapter.clientRange());
                    advance(Phase.CLICK_EXCHANGE_RANGE, 0);
                    break;
                case CLICK_EXCHANGE_RANGE:
                    if (exchangeClicks < EXCHANGE_TARGET_RANGE - EXCHANGE_INITIAL_RANGE) {
                        adapter.clickRangePlus();
                        exchangeClicks++;
                        phaseDelay = 1;
                        return;
                    }
                    require(adapter.visibleScreenRange() == EXCHANGE_TARGET_RANGE,
                            "exchange range widget did not reach " + EXCHANGE_TARGET_RANGE);
                    advance(Phase.WAIT_EXCHANGE_SYNC, 0);
                    break;
                case WAIT_EXCHANGE_SYNC:
                    if (adapter.clientRange() != EXCHANGE_TARGET_RANGE) return;
                    advance(Phase.SWITCH_BUILD_AGAIN, 1);
                    break;
                case SWITCH_BUILD_AGAIN:
                    adapter.switchToBuildSubmenu();
                    advance(Phase.VERIFY_BUILD_RESTORE, 2);
                    break;
                case VERIFY_BUILD_RESTORE:
                    if (adapter.clientActiveToolOrdinal() != 0 || adapter.clientRange() != TARGET_RANGE) return;
                    if (adapter.visibleScreenRange() < 0) return;
                    require(adapter.visibleScreenRange() == TARGET_RANGE,
                            "build profile range was not restored; visible=" + adapter.visibleScreenRange());
                    advance(Phase.SWITCH_EXCHANGE_AGAIN, 1);
                    break;
                case SWITCH_EXCHANGE_AGAIN:
                    adapter.switchToExchangeSubmenu();
                    advance(Phase.VERIFY_EXCHANGE_RESTORE, 2);
                    break;
                case VERIFY_EXCHANGE_RESTORE:
                    if (adapter.clientActiveToolOrdinal() != 1 || adapter.clientRange() != EXCHANGE_TARGET_RANGE) return;
                    if (adapter.visibleScreenRange() < 0) return;
                    require(adapter.visibleScreenRange() == EXCHANGE_TARGET_RANGE,
                            "exchange profile range was not restored; visible=" + adapter.visibleScreenRange());
                    advance(Phase.SWITCH_COPY, 1);
                    break;
                case SWITCH_COPY:
                    adapter.switchToCopySubmenu();
                    advance(Phase.WAIT_COPY_PROFILE, 2);
                    break;
                case WAIT_COPY_PROFILE:
                    if (adapter.clientActiveToolOrdinal() != 2) return;
                    advance(Phase.OPEN_COPY_CONFIG, 1);
                    break;
                case OPEN_COPY_CONFIG:
                    adapter.openCopyConfig();
                    advance(Phase.EDIT_COPY_START, 2);
                    break;
                case EDIT_COPY_START:
                    require(adapter.isCopyScreenOpen(), "copy coordinate screen did not open");
                    require(adapter.visibleCopyStartX() == 0,
                            "copy Start X should begin as relative zero; visible=" + adapter.visibleCopyStartX());
                    adapter.clickCopyStartXPlus();
                    advance(Phase.VERIFY_COPY_EDIT, 1);
                    break;
                case VERIFY_COPY_EDIT:
                    require(adapter.visibleCopyStartX() == COPY_START_X_TARGET,
                            "copy Start X edit was consumed/reset before Confirm; visible=" + adapter.visibleCopyStartX());
                    adapter.confirmCopyConfig();
                    advance(Phase.WAIT_COPY_SYNC, 1);
                    break;
                case WAIT_COPY_SYNC:
                    if (adapter.clientCopyStartX() != COPY_START_X_TARGET) return;
                    advance(Phase.OPEN_RADIAL_FOR_DESTRUCTION, 1);
                    break;
                case OPEN_RADIAL_FOR_DESTRUCTION:
                    adapter.openMultitoolScreen();
                    advance(Phase.SWITCH_DESTRUCTION, 2);
                    break;
                case SWITCH_DESTRUCTION:
                    adapter.switchToDestructionSubmenu();
                    advance(Phase.WAIT_DESTRUCTION_PROFILE, 2);
                    break;
                case WAIT_DESTRUCTION_PROFILE:
                    if (adapter.clientActiveToolOrdinal() != 4) return;
                    advance(Phase.OPEN_DESTRUCTION_CONFIG, 1);
                    break;
                case OPEN_DESTRUCTION_CONFIG:
                    adapter.openDestructionConfig();
                    advance(Phase.EDIT_DESTRUCTION, 2);
                    break;
                case EDIT_DESTRUCTION:
                    require(adapter.isDestructionScreenOpen(), "destruction configuration screen did not open");
                    require(adapter.visibleDestructionLeft() == 0 && adapter.visibleDestructionDepth() == 0,
                            "destruction test profile did not start at zero; left=" + adapter.visibleDestructionLeft()
                                    + " depth=" + adapter.visibleDestructionDepth());
                    adapter.clickDestructionLeftPlus();
                    adapter.clickDestructionDepthPlus();
                    advance(Phase.VERIFY_DESTRUCTION_EDIT, 1);
                    break;
                case VERIFY_DESTRUCTION_EDIT:
                    require(adapter.visibleDestructionLeft() == DESTRUCTION_LEFT_TARGET,
                            "destruction Left widget did not retain edit; visible=" + adapter.visibleDestructionLeft());
                    require(adapter.visibleDestructionDepth() == DESTRUCTION_DEPTH_TARGET,
                            "destruction Depth widget did not retain edit; visible=" + adapter.visibleDestructionDepth());
                    advance(Phase.WAIT_DESTRUCTION_SYNC, 0);
                    break;
                case WAIT_DESTRUCTION_SYNC:
                    if (adapter.clientDestructionLeft() != DESTRUCTION_LEFT_TARGET
                            || adapter.clientDestructionDepth() != DESTRUCTION_DEPTH_TARGET) return;
                    adapter.closeScreen();
                    advance(Phase.REOPEN_DESTRUCTION_RADIAL, 2);
                    break;
                case REOPEN_DESTRUCTION_RADIAL:
                    adapter.openMultitoolScreen();
                    advance(Phase.SWITCH_DESTRUCTION_AGAIN, 2);
                    break;
                case SWITCH_DESTRUCTION_AGAIN:
                    adapter.switchToDestructionSubmenu();
                    if (adapter.clientActiveToolOrdinal() != 4) return;
                    advance(Phase.REOPEN_DESTRUCTION_CONFIG, 1);
                    break;
                case REOPEN_DESTRUCTION_CONFIG:
                    adapter.openDestructionConfig();
                    advance(Phase.VERIFY_DESTRUCTION_REOPEN, 2);
                    break;
                case VERIFY_DESTRUCTION_REOPEN:
                    require(adapter.isDestructionScreenOpen(), "destruction screen did not reopen");
                    require(adapter.clientDestructionLeft() == DESTRUCTION_LEFT_TARGET,
                            "destruction Left reverted on the synchronized client stack");
                    require(adapter.clientDestructionDepth() == DESTRUCTION_DEPTH_TARGET,
                            "destruction Depth reverted on the synchronized client stack");
                    require(adapter.visibleDestructionLeft() == DESTRUCTION_LEFT_TARGET,
                            "reopened destruction UI lost Left; visible=" + adapter.visibleDestructionLeft());
                    require(adapter.visibleDestructionDepth() == DESTRUCTION_DEPTH_TARGET,
                            "reopened destruction UI lost Depth; visible=" + adapter.visibleDestructionDepth());
                    succeed("range profile isolation, copy selection editing, and destruction persistence round trips passed");
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

    private void succeed(String detail) {
        phase = Phase.DONE;
        adapter.pass(detail);
    }

    private void fail(String detail, Throwable error) {
        phase = Phase.DONE;
        adapter.fail(detail, error);
    }
}
