package dev.pbenchants.perk;

/**
 * One flag for "the mod is already breaking blocks on this thread".
 *
 * <p>Every cascade in the mod breaks its extra blocks through
 * {@code gameMode.destroyBlock}, which fires the break event again — so without
 * a guard a Flat Earth swing inside a Diggy Diggy Hole pulse would 3x3 every
 * block the aura touches. Each cascade used to carry its own flag and check the
 * others by name, which is fine for three of them and quadratic for six.
 *
 * <p>The depth is a counter rather than a boolean so a nested {@code enter()}
 * cannot be released early by the inner {@code exit()}. Always in try/finally.
 */
public final class BreakGuard {
	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	private BreakGuard() {
	}

	/** True when a mod-driven break is already running: no cascade may start. */
	public static boolean busy() {
		return DEPTH.get() > 0;
	}

	public static void enter() {
		DEPTH.set(DEPTH.get() + 1);
	}

	public static void exit() {
		DEPTH.set(Math.max(0, DEPTH.get() - 1));
	}
}
