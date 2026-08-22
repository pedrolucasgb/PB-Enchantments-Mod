package dev.toolmastery.client;

import dev.toolmastery.network.SkillActionPayload;
import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.skill.GateRequirement;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillTier;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The skill tree screen (default key: K). Layout mirrors the approved design:
 * class tabs on top (future classes disabled as "Coming soon"), tier columns
 * with node buttons, details panel on the right, player XP at the bottom.
 */
public class SkillTreeScreen extends Screen {
	// Palette (ARGB) — mirrors the design mockup.
	private static final int COLOR_BACKDROP = 0xE0101216;
	private static final int COLOR_PANEL = 0xFF191C22;
	private static final int COLOR_PANEL_BORDER = 0xFF2C313B;
	private static final int COLOR_TEXT = 0xFFE8E6DF;
	private static final int COLOR_MUTED = 0xFF9AA1AD;
	private static final int COLOR_XP = 0xFF7FE55F;
	private static final int COLOR_GOLD = 0xFFF2C94C;
	private static final int COLOR_LOCKED = 0xFF6C7280;

	private static final String[] COMING_SOON = {"Sword", "Bow", "Rod", "Armor", "Enchanter"};

	/** Fixed tab order: real trees first, then future classes. */
	private static final List<String> TREE_TABS = List.of("pickaxe", "axe");

	private String treeId = "pickaxe";
	@Nullable
	private String selectedNode;
	private int selectedTier = -1;

	private int panelX;
	private int treeTop;
	private Button actionButton;

	public SkillTreeScreen() {
		super(Component.translatable("screen.toolmastery.title"));
	}

	@Override
	protected void init() {
		ClientSkillState.setChangeListener(this::scheduleRebuild);
		rebuild();
	}

	/**
	 * Rebuilding replaces every widget, which must never happen while the
	 * screen is still dispatching the mouse event that triggered it — that
	 * eats or misroutes the click. Defer to the next client-thread slice.
	 */
	private void scheduleRebuild() {
		if (minecraft != null) {
			minecraft.execute(this::rebuild);
		}
	}

	@Override
	public void onClose() {
		ClientSkillState.setChangeListener(null);
		super.onClose();
	}

	private void rebuild() {
		clearWidgets();
		panelX = width - 148;

		// --- class tabs (wrap to a new row instead of overflowing the panel) ---
		int tabX = 8;
		int tabY = 18;
		for (String id : TREE_TABS) {
			if (tabX + 52 > panelX - 4) {
				tabX = 8;
				tabY += 18;
			}
			String label = (treeId.equals(id) ? "▶ " : "") + id.substring(0, 1).toUpperCase() + id.substring(1);
			Button tab = Button.builder(Component.literal(label), button -> {
					if (!treeId.equals(id)) {
						treeId = id;
						selectedNode = null;
						selectedTier = -1;
						scheduleRebuild();
					}
				})
				.bounds(tabX, tabY, 52, 16)
				.build();
			tab.setTooltip(Tooltip.create(Component.translatable("tree.toolmastery." + id)));
			addRenderableWidget(tab);
			tabX += 55;
		}
		for (String name : COMING_SOON) {
			if (tabX + 52 > panelX - 4) {
				tabX = 8;
				tabY += 18;
			}
			Button tab = Button.builder(Component.literal(name), button -> {
				})
				.bounds(tabX, tabY, 52, 16)
				.build();
			tab.active = false;
			tab.setTooltip(Tooltip.create(Component.translatable("screen.toolmastery.coming_soon")));
			addRenderableWidget(tab);
			tabX += 55;
		}
		treeTop = tabY + 22;

		// --- tier columns with node buttons ---
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		if (tree == null) {
			return;
		}
		int columns = tree.tiers().size();
		int colWidth = Math.min(86, (panelX - 16 - (columns - 1) * 4) / columns);
		int unlocked = state == null ? 0 : state.unlockedTiers();

		for (int tierIndex = 0; tierIndex < columns; tierIndex++) {
			int x = 8 + tierIndex * (colWidth + 4);
			boolean tierOpen = tierIndex < unlocked;

			final int capturedTier = tierIndex;
			String headerText = "T" + (tierIndex + 1) + (tierOpen ? " ✓" : " 🔒");
			Button header = Button.builder(Component.literal(headerText), button -> {
					selectedTier = capturedTier;
					selectedNode = null;
					scheduleRebuild();
				})
				.bounds(x, treeTop, colWidth, 14)
				.build();
			header.setTooltip(Tooltip.create(Component.translatable("tier.toolmastery." + treeId + "." + (tierIndex + 1))));
			addRenderableWidget(header);

			int y = treeTop + 18;
			for (SkillNode node : tree.nodes().values()) {
				if (node.tier() != tierIndex) {
					continue;
				}
				boolean owned = state != null && state.purchased().contains(node.id());
				Component label = Component.literal((owned ? "✓ " : "") + nodeName(node.id()));
				final String capturedNode = node.id();
				Button nodeButton = Button.builder(label, button -> {
						selectedNode = capturedNode;
						selectedTier = -1;
						scheduleRebuild();
					})
					.bounds(x, y, colWidth, 16)
					.build();
				if (!tierOpen && !owned) {
					// still clickable so the player can read what it does
					nodeButton.setTooltip(Tooltip.create(Component.translatable("screen.toolmastery.tier_locked")));
				}
				addRenderableWidget(nodeButton);
				y += 19;
			}
		}

		// --- action button in the details panel ---
		actionButton = Button.builder(actionLabel(), button -> performAction())
			.bounds(panelX + 6, height - 46, 136, 18)
			.build();
		refreshActionButton();
		addRenderableWidget(actionButton);

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(panelX + 6, height - 26, 136, 18)
			.build());
	}

	private Component actionLabel() {
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		if (tree == null || state == null) {
			return Component.translatable("screen.toolmastery.syncing");
		}
		if (selectedNode != null) {
			SkillNode node = tree.node(selectedNode);
			if (node != null) {
				if (state.purchased().contains(node.id())) {
					return Component.translatable("screen.toolmastery.owned");
				}
				return Component.translatable("screen.toolmastery.buy", node.cost());
			}
		}
		if (selectedTier >= 0) {
			if (selectedTier < state.unlockedTiers()) {
				return Component.translatable("screen.toolmastery.unlocked");
			}
			SkillTier tier = tree.tiers().get(selectedTier);
			return Component.translatable("screen.toolmastery.unlock", tier.accessCost());
		}
		return Component.translatable("screen.toolmastery.select_hint");
	}

	private void refreshActionButton() {
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		boolean enabled = false;
		if (tree != null && state != null) {
			if (selectedNode != null) {
				SkillNode node = tree.node(selectedNode);
				enabled = node != null && !state.purchased().contains(node.id());
			} else if (selectedTier >= 0) {
				enabled = selectedTier == state.unlockedTiers();
			}
		}
		actionButton.active = enabled;
		actionButton.setMessage(actionLabel());
	}

	private void performAction() {
		if (selectedNode != null) {
			ClientPlayNetworking.send(new SkillActionPayload(SkillActionPayload.Action.BUY_NODE, treeId, selectedNode));
		} else if (selectedTier >= 0) {
			ClientPlayNetworking.send(new SkillActionPayload(SkillActionPayload.Action.UNLOCK_TIER, treeId, ""));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, COLOR_BACKDROP);

		// details panel background
		graphics.fill(panelX, 36, width - 4, height - 4, COLOR_PANEL);
		graphics.outline(panelX, 36, width - 4 - panelX, height - 40, COLOR_PANEL_BORDER);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		graphics.text(font, title, 8, 6, COLOR_TEXT);
		if (minecraft != null && minecraft.player != null) {
			graphics.text(font,
				Component.translatable("screen.toolmastery.levels", minecraft.player.experienceLevel),
				panelX + 6, 6, COLOR_XP);
		}

		drawDetails(graphics);
	}

	private void drawDetails(GuiGraphicsExtractor graphics) {
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		int x = panelX + 6;
		int y = 42;
		int wrap = width - 12 - x;

		if (tree == null || state == null) {
			graphics.text(font, Component.translatable("screen.toolmastery.syncing"), x, y, COLOR_MUTED);
			return;
		}

		if (selectedNode != null) {
			SkillNode node = tree.node(selectedNode);
			if (node == null) {
				return;
			}
			graphics.text(font, Component.literal(nodeName(node.id())), x, y, COLOR_TEXT);
			y += 12;
			graphics.text(font, Component.literal(node.type().name().toLowerCase()), x, y, COLOR_GOLD);
			y += 12;
			boolean owned = state.purchased().contains(node.id());
			graphics.text(font,
				owned
					? Component.translatable("screen.toolmastery.owned")
					: Component.translatable("screen.toolmastery.cost", node.cost()),
				x, y, owned ? COLOR_XP : COLOR_MUTED);
			y += 12;
			if (node.requires() != null) {
				boolean has = state.purchased().contains(node.requires());
				graphics.text(font,
					Component.translatable("screen.toolmastery.requires", nodeName(node.requires())),
					x, y, has ? COLOR_XP : COLOR_LOCKED);
				y += 12;
			}
			if (node.exclusiveWith() != null) {
				graphics.text(font,
					Component.translatable("screen.toolmastery.exclusive", nodeName(node.exclusiveWith())),
					x, y, COLOR_LOCKED);
				y += 12;
			}
			y += 4;
			graphics.textWithWordWrap(font,
				Component.translatable("node.toolmastery." + baseId(node.id()) + ".desc"),
				x, y, wrap, COLOR_MUTED);
			return;
		}

		if (selectedTier >= 0) {
			SkillTier tier = tree.tiers().get(selectedTier);
			graphics.text(font,
				Component.translatable("tier.toolmastery." + treeId + "." + (selectedTier + 1)),
				x, y, COLOR_TEXT);
			y += 12;
			graphics.text(font, Component.translatable("screen.toolmastery.gate"), x, y, COLOR_GOLD);
			y += 12;
			for (GateRequirement gate : tier.gates()) {
				int count = Math.min(state.counters().getOrDefault(gate.id(), 0), gate.target());
				boolean done = count >= gate.target();
				String line = (done ? "✓ " : "□ ") + gate.id().replace('_', ' ') + " " + count + "/" + gate.target();
				graphics.text(font, Component.literal(line), x, y, done ? COLOR_XP : COLOR_MUTED);
				y += 11;
			}
			return;
		}

		graphics.textWithWordWrap(font, Component.translatable("screen.toolmastery.help"), x, y, wrap, COLOR_MUTED);
	}

	/** "dig_range_2" -> "Dig Range II"; "miners_magnet" -> localized node name. */
	private Component rawName(String base) {
		return Component.translatable("node.toolmastery." + base);
	}

	private String nodeName(String nodeId) {
		String base = baseId(nodeId);
		String name = rawName(base).getString();
		String suffix = nodeId.substring(base.length());
		if (suffix.startsWith("_")) {
			int rank = Integer.parseInt(suffix.substring(1));
			name += " " + switch (rank) {
				case 1 -> "I";
				case 2 -> "II";
				case 3 -> "III";
				default -> String.valueOf(rank);
			};
		}
		return name;
	}

	private static String baseId(String nodeId) {
		int lastUnderscore = nodeId.lastIndexOf('_');
		if (lastUnderscore > 0 && lastUnderscore == nodeId.length() - 2
			&& Character.isDigit(nodeId.charAt(nodeId.length() - 1))) {
			return nodeId.substring(0, lastUnderscore);
		}
		return nodeId;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
