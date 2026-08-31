package dev.pbenchants.client.gui;

/**
 * How a node reads at a glance. One value per colour in the design: green for
 * what you own, a breathing gold for what you can reach right now, dim grey for
 * what the tree has not opened yet, red for the half of a capstone pair your
 * choice locked out, and orange for what is still to be built.
 */
public enum NodeState {
	OWNED,
	AVAILABLE,
	LOCKED,
	BLOCKED,
	FUTURE;

	/** Frame colour of a node tile in this state. */
	public int frame() {
		return switch (this) {
			case OWNED -> SkillTreeStyle.GREEN;
			case AVAILABLE -> SkillTreeStyle.pulsingGold();
			case LOCKED -> SkillTreeStyle.BORDER;
			case BLOCKED -> SkillTreeStyle.BAD;
			case FUTURE -> SkillTreeStyle.SOON;
		};
	}

	/** Fill behind the icon. */
	public int background() {
		return switch (this) {
			case OWNED -> 0xFF16301C;
			case AVAILABLE -> 0xFF2B2412;
			case LOCKED -> 0xFF15181D;
			case BLOCKED -> 0xFF2C1618;
			case FUTURE -> 0xFF241E14;
		};
	}

	/** Colour of the node's name beside the icon. */
	public int labelColor() {
		return switch (this) {
			case OWNED -> SkillTreeStyle.GREEN;
			case AVAILABLE -> SkillTreeStyle.TEXT;
			case LOCKED -> SkillTreeStyle.DIM;
			case BLOCKED -> SkillTreeStyle.BAD;
			case FUTURE -> SkillTreeStyle.SOON;
		};
	}

	/** True when the icon should be veiled, because the node is out of reach. */
	public boolean dimmed() {
		return this == LOCKED || this == BLOCKED || this == FUTURE;
	}
}
