package dev.toolmastery.client;

import dev.toolmastery.enchant.EnchantCompat;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.network.SkillActionPayload;
import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.skill.GateRequirement;
import dev.toolmastery.skill.MaterialCost;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillTier;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The skill tree screen (default key: K). Layout mirrors the approved design:
 * class tabs on top (future classes disabled as "Coming soon"), tier columns
 * with node buttons, details panel on the right, player XP at the bottom.
 *
 * <p>A node offers up to two purchases, and neither happens on a single click:
 * <b>Unlock</b> (XP levels + materials, once) and <b>Enchant</b> (whole XP
 * levels, repeatable, stamps the enchantment on the held item). Pressing either
 * swaps the details panel for a confirmation card that spells out exactly what
 * the player is about to buy — the enchanting-table promise for an unlock, the
 * held item and its compatibility for an enchant — with Confirm and Cancel.
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
	private static final int COLOR_COMING_SOON = 0xFFFFA94D;
	private static final int COLOR_BAD = 0xFFE86B6B;

	private static final String[] COMING_SOON = {"Sword", "Bow", "Rod", "Armor"};

	/** Fixed tab order: real trees first, then future classes. */
	private static final List<String> TREE_TABS = List.of("pickaxe", "axe", "enchanter");

	/** Tab button width — wide enough for the longest class name. */
	private static final int TAB_WIDTH = 58;

	/** Which purchase is waiting for a Confirm click. */
	private enum Pending {
		NONE,
		UNLOCK_TIER,
		UNLOCK_NODE,
		ENCHANT_NODE
	}

	private String treeId = "pickaxe";
	@Nullable
	private String selectedNode;
	private int selectedTier = -1;
	private Pending pending = Pending.NONE;

	private int panelX;
	private int treeTop;

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

	private void select(@Nullable String nodeId, int tier) {
		selectedNode = nodeId;
		selectedTier = tier;
		pending = Pending.NONE;
		scheduleRebuild();
	}

	private void rebuild() {
		clearWidgets();
		panelX = width - 148;

		// --- class tabs (wrap to a new row instead of overflowing the panel) ---
		int tabX = 8;
		int tabY = 18;
		for (String id : TREE_TABS) {
			if (tabX + TAB_WIDTH > panelX - 4) {
				tabX = 8;
				tabY += 18;
			}
			String label = (treeId.equals(id) ? "▶ " : "") + id.substring(0, 1).toUpperCase() + id.substring(1);
			Button tab = Button.builder(Component.literal(label), button -> {
					if (!treeId.equals(id)) {
						treeId = id;
						select(null, -1);
					}
				})
				.bounds(tabX, tabY, TAB_WIDTH, 16)
				.build();
			tab.setTooltip(Tooltip.create(Component.translatable("tree.toolmastery." + id)));
			addRenderableWidget(tab);
			tabX += TAB_WIDTH + 3;
		}
		for (String name : COMING_SOON) {
			if (tabX + TAB_WIDTH > panelX - 4) {
				tabX = 8;
				tabY += 18;
			}
			Button tab = Button.builder(Component.literal(name), button -> {
				})
				.bounds(tabX, tabY, TAB_WIDTH, 16)
				.build();
			tab.active = false;
			tab.setTooltip(Tooltip.create(Component.translatable("screen.toolmastery.coming_soon")));
			addRenderableWidget(tab);
			tabX += TAB_WIDTH + 3;
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
			Button header = Button.builder(Component.literal(headerText), button -> select(null, capturedTier))
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
				Component label = Component.literal(owned ? "✓ " : "").append(node.displayName());
				final String capturedNode = node.id();
				Button nodeButton = Button.builder(label, button -> select(capturedNode, -1))
					.bounds(x, y, colWidth, 16)
					.build();
				if (!node.implemented() && !owned) {
					// ships in a future update — readable (click for details), not unlockable
					nodeButton.setTooltip(Tooltip.create(Component.translatable("screen.toolmastery.coming_soon")));
				} else if (!tierOpen && !owned) {
					// still clickable so the player can read what it does
					nodeButton.setTooltip(Tooltip.create(Component.translatable("screen.toolmastery.tier_locked")));
				}
				addRenderableWidget(nodeButton);
				y += 19;
			}
		}

		buildActionButtons(tree, state);

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(panelX + 6, height - 26, 136, 18)
			.build());
	}

	// ---------- actions ----------

	private void buildActionButtons(SkillTree tree, @Nullable SkillStatePayload.TreeState state) {
		int primaryY = height - 66;
		int secondaryY = height - 46;

		if (pending != Pending.NONE) {
			addRenderableWidget(Button.builder(Component.translatable("screen.toolmastery.confirm"), button -> confirm())
				.bounds(panelX + 6, primaryY, 136, 18)
				.build());
			addRenderableWidget(Button.builder(Component.translatable("screen.toolmastery.cancel"), button -> {
					pending = Pending.NONE;
					scheduleRebuild();
				})
				.bounds(panelX + 6, secondaryY, 136, 18)
				.build());
			return;
		}

		if (state == null) {
			addDisabled(Component.translatable("screen.toolmastery.syncing"), primaryY);
			return;
		}

		SkillNode node = selectedNode == null ? null : tree.node(selectedNode);
		if (node != null) {
			boolean owned = state.purchased().contains(node.id());
			Component unlockLabel = owned
				? Component.translatable("screen.toolmastery.unlocked")
				: Component.translatable("screen.toolmastery.unlock_node", node.unlockCost());
			if (!owned && node.implemented()) {
				addRenderableWidget(Button.builder(unlockLabel, button -> {
						pending = Pending.UNLOCK_NODE;
						scheduleRebuild();
					})
					.bounds(panelX + 6, primaryY, 136, 18)
					.build());
			} else {
				addDisabled(node.implemented() ? unlockLabel : Component.translatable("screen.toolmastery.coming_soon"), primaryY);
			}

			if (node.enchantable()) {
				Component enchantLabel = Component.translatable("screen.toolmastery.enchant_node", node.enchantCost());
				if (owned && enchantProblem(node) == null) {
					addRenderableWidget(Button.builder(enchantLabel, button -> {
							pending = Pending.ENCHANT_NODE;
							scheduleRebuild();
						})
						.bounds(panelX + 6, secondaryY, 136, 18)
						.build());
				} else {
					Button disabled = addDisabled(enchantLabel, secondaryY);
					Component why = owned ? enchantProblem(node) : Component.translatable("screen.toolmastery.enchant_needs_unlock");
					if (why != null) {
						disabled.setTooltip(Tooltip.create(why));
					}
				}
			}
			return;
		}

		if (selectedTier >= 0) {
			SkillTier tier = tree.tiers().get(selectedTier);
			if (selectedTier < state.unlockedTiers()) {
				addDisabled(Component.translatable("screen.toolmastery.unlocked"), primaryY);
			} else if (selectedTier == state.unlockedTiers()) {
				addRenderableWidget(Button.builder(Component.translatable("screen.toolmastery.unlock", tier.accessCost()),
						button -> {
							pending = Pending.UNLOCK_TIER;
							scheduleRebuild();
						})
					.bounds(panelX + 6, primaryY, 136, 18)
					.build());
			} else {
				addDisabled(Component.translatable("screen.toolmastery.unlock", tier.accessCost()), primaryY);
			}
			return;
		}

		addDisabled(Component.translatable("screen.toolmastery.select_hint"), primaryY);
	}

	private Button addDisabled(Component label, int y) {
		Button button = Button.builder(label, ignored -> {
			})
			.bounds(panelX + 6, y, 136, 18)
			.build();
		button.active = false;
		return addRenderableWidget(button);
	}

	private void confirm() {
		switch (pending) {
			case UNLOCK_TIER ->
				ClientPlayNetworking.send(new SkillActionPayload(SkillActionPayload.Action.UNLOCK_TIER, treeId, ""));
			case UNLOCK_NODE -> {
				if (selectedNode != null) {
					ClientPlayNetworking.send(
						new SkillActionPayload(SkillActionPayload.Action.UNLOCK_NODE, treeId, selectedNode));
				}
			}
			case ENCHANT_NODE -> {
				if (selectedNode != null) {
					ClientPlayNetworking.send(
						new SkillActionPayload(SkillActionPayload.Action.ENCHANT_NODE, treeId, selectedNode));
				}
			}
			case NONE -> {
				// nothing pending
			}
		}
		pending = Pending.NONE;
		scheduleRebuild();
	}

	/**
	 * The same compatibility verdict the server will reach, computed locally so
	 * the button can be greyed out with the reason attached before any level is
	 * spent. Null means the enchant would go through.
	 */
	@Nullable
	private Component enchantProblem(SkillNode node) {
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
		if (player == null || grant == null) {
			return Component.translatable("screen.toolmastery.syncing");
		}
		Holder<Enchantment> holder = ModEnchantments.holder(player, grant.enchantment());
		if (holder == null) {
			return Component.translatable("screen.toolmastery.syncing");
		}
		return EnchantCompat.problem(player.getMainHandItem(), holder, grant.level());
	}

	// ---------- rendering ----------

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

	/**
	 * Word-wraps text to the details panel width instead of letting long lines
	 * escape the panel. Returns the y just below the last drawn line.
	 */
	private int wrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int lineSpacing) {
		int wrap = width - 12 - x;
		for (FormattedCharSequence line : font.split(text, wrap)) {
			graphics.text(font, line, x, y, color);
			y += lineSpacing;
		}
		return y;
	}

	private void drawDetails(GuiGraphicsExtractor graphics) {
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		int x = panelX + 6;
		int y = 42;

		if (tree == null || state == null) {
			graphics.text(font, Component.translatable("screen.toolmastery.syncing"), x, y, COLOR_MUTED);
			return;
		}
		if (pending != Pending.NONE) {
			drawConfirmation(graphics, tree, x, y);
			return;
		}
		if (selectedNode != null) {
			SkillNode node = tree.node(selectedNode);
			if (node != null) {
				drawNode(graphics, node, state, x, y);
			}
			return;
		}
		if (selectedTier >= 0) {
			drawTier(graphics, tree, state, x, y);
			return;
		}
		graphics.textWithWordWrap(font, Component.translatable("screen.toolmastery.help"),
			x, y, width - 12 - x, COLOR_MUTED);
	}

	private void drawNode(GuiGraphicsExtractor graphics, SkillNode node, SkillStatePayload.TreeState state, int x, int y) {
		int wrap = width - 12 - x;
		y = wrappedText(graphics, node.displayName(), x, y, COLOR_TEXT, 12);
		graphics.text(font, Component.literal(node.type().name().toLowerCase()), x, y, COLOR_GOLD);
		y += 12;
		if (!node.implemented()) {
			// highlighted badge — the full description still renders below
			y = wrappedText(graphics, Component.translatable("screen.toolmastery.coming_soon_badge"),
				x, y, COLOR_COMING_SOON, 12);
		}

		boolean owned = state.purchased().contains(node.id());
		y = wrappedText(graphics,
			owned
				? Component.translatable("screen.toolmastery.unlocked")
				: Component.translatable("screen.toolmastery.unlock_cost", node.unlockCost()),
			x, y, owned ? COLOR_XP : COLOR_MUTED, 12);
		if (!owned) {
			y = drawMaterials(graphics, node, x, y);
		}
		if (node.enchantable()) {
			y = wrappedText(graphics, Component.translatable("screen.toolmastery.enchant_cost", node.enchantCost()),
				x, y, COLOR_MUTED, 12);
		}
		if (node.requires() != null) {
			boolean has = state.purchased().contains(node.requires());
			y = wrappedText(graphics,
				Component.translatable("screen.toolmastery.requires", SkillNode.displayName(node.requires())),
				x, y, has ? COLOR_XP : COLOR_LOCKED, 12);
		}
		if (node.exclusiveWith() != null) {
			y = wrappedText(graphics,
				Component.translatable("screen.toolmastery.exclusive", SkillNode.displayName(node.exclusiveWith())),
				x, y, COLOR_LOCKED, 12);
		}
		y += 4;
		// Keyed on the full node id, not the family: every rank describes only
		// what that rank does, so Dig Range II does not recite I and III too.
		graphics.textWithWordWrap(font,
			Component.translatable("node.toolmastery." + node.id() + ".desc"),
			x, y, wrap, COLOR_MUTED);
	}

	/** The have/need checklist for a node's unlock materials. */
	private int drawMaterials(GuiGraphicsExtractor graphics, SkillNode node, int x, int y) {
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		for (MaterialCost material : node.materials()) {
			int held = player == null ? 0 : material.held(player);
			boolean done = held >= material.count();
			Component line = Component.literal(done ? "✓ " : "□ ")
				.append(material.label())
				.append(done ? Component.empty() : Component.literal(" (" + held + ")"));
			y = wrappedText(graphics, line, x, y, done ? COLOR_XP : COLOR_MUTED, 11);
		}
		return y;
	}

	private void drawTier(GuiGraphicsExtractor graphics, SkillTree tree, SkillStatePayload.TreeState state, int x, int y) {
		SkillTier tier = tree.tiers().get(selectedTier);
		y = wrappedText(graphics,
			Component.translatable("tier.toolmastery." + treeId + "." + (selectedTier + 1)),
			x, y, COLOR_TEXT, 12);
		graphics.text(font, Component.translatable("screen.toolmastery.gate"), x, y, COLOR_GOLD);
		y += 12;
		for (GateRequirement gate : tier.gates()) {
			int count = Math.min(state.counters().getOrDefault(gate.id(), 0), gate.target());
			boolean done = count >= gate.target();
			String line = (done ? "✓ " : "□ ") + gate.displayName() + " " + count + "/" + gate.target();
			y = wrappedText(graphics, Component.literal(line), x, y, done ? COLOR_XP : COLOR_MUTED, 11);
		}
	}

	/**
	 * The card behind Unlock/Enchant: what this click costs, what it changes,
	 * and — for an enchant — which item is about to receive it.
	 */
	private void drawConfirmation(GuiGraphicsExtractor graphics, SkillTree tree, int x, int y) {
		int wrap = width - 12 - x;
		SkillNode node = selectedNode == null ? null : tree.node(selectedNode);

		if (pending == Pending.UNLOCK_TIER) {
			SkillTier tier = tree.tiers().get(Math.max(selectedTier, 0));
			y = wrappedText(graphics, Component.translatable("screen.toolmastery.confirm.tier_title",
				Component.translatable("tier.toolmastery." + treeId + "." + (selectedTier + 1))), x, y, COLOR_GOLD, 12);
			y += 2;
			graphics.textWithWordWrap(font,
				Component.translatable("screen.toolmastery.confirm.tier_body", tier.accessCost()),
				x, y, wrap, COLOR_MUTED);
			return;
		}
		if (node == null) {
			return;
		}

		if (pending == Pending.UNLOCK_NODE) {
			y = wrappedText(graphics, Component.translatable("screen.toolmastery.confirm.unlock_title",
				node.displayName()), x, y, COLOR_GOLD, 12);
			y = wrappedText(graphics, Component.translatable("screen.toolmastery.unlock_cost", node.unlockCost()),
				x, y, COLOR_MUTED, 12);
			y = drawMaterials(graphics, node, x, y);
			y += 2;
			ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
			String bodyKey;
			if (!node.enchantable()) {
				bodyKey = "screen.toolmastery.confirm.unlock_body_passive";
			} else if (grant != null && ModEnchantments.TABLE_POOL.contains(grant.enchantment())) {
				bodyKey = "screen.toolmastery.confirm.unlock_body_enchantment";
			} else {
				bodyKey = "screen.toolmastery.confirm.unlock_body_capstone";
			}
			graphics.textWithWordWrap(font,
				Component.translatable(bodyKey, node.displayName(), node.enchantCost()), x, y, wrap, COLOR_MUTED);
			return;
		}

		// ENCHANT_NODE
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		Component held = player == null ? Component.empty() : player.getMainHandItem().getHoverName();
		y = wrappedText(graphics, Component.translatable("screen.toolmastery.confirm.enchant_title",
			node.displayName()), x, y, COLOR_GOLD, 12);
		y = wrappedText(graphics, Component.translatable("screen.toolmastery.confirm.enchant_target", held),
			x, y, COLOR_TEXT, 12);
		y = wrappedText(graphics, Component.translatable("screen.toolmastery.enchant_cost", node.enchantCost()),
			x, y, COLOR_MUTED, 12);
		Component problem = enchantProblem(node);
		if (problem != null) {
			y = wrappedText(graphics, problem, x, y, COLOR_BAD, 11);
		}
		y += 2;
		graphics.textWithWordWrap(font, Component.translatable("screen.toolmastery.confirm.enchant_body"),
			x, y, wrap, COLOR_MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
