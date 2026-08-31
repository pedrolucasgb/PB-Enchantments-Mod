package dev.pbenchants.client;

import dev.pbenchants.client.gui.ClassTabWidget;
import dev.pbenchants.client.gui.NodeState;
import dev.pbenchants.client.gui.SkillNodeWidget;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.client.gui.TierHeaderWidget;
import dev.pbenchants.enchant.EnchantCompat;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.network.SkillActionPayload;
import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.skill.GateRequirement;
import dev.pbenchants.skill.MaterialCost;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillTier;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The skill tree screen (default key: K), drawn as a tree rather than a list of
 * buttons: class tabs across the top, one column per tier headed by the tier's
 * own name, nodes as item icons in a frame whose colour is their state, and
 * connector lines running from a rank to the rank that unlocks it. The details
 * panel sits on the right, the player's XP bar along the bottom mirroring the
 * HUD.
 *
 * <p>A node offers up to two purchases, and neither happens on a single click:
 * <b>Unlock</b> (XP levels + materials, once) and <b>Enchant</b> (whole XP
 * levels, repeatable, stamps the enchantment on the held item). Pressing either
 * swaps the details panel for a confirmation card that spells out exactly what
 * the player is about to buy — the enchanting-table promise for an unlock, the
 * held item and its compatibility for an enchant — with Confirm and Cancel.
 *
 * <p>Nothing here is written per class: the tabs come from
 * {@link SkillTrees#ORDER}, the tier names and icons off the tree and its
 * nodes. A new class or node appears correctly without this file changing.
 */
public class SkillTreeScreen extends Screen {
	private static final int MARGIN = 6;
	private static final int TITLE_BAR = 16;
	private static final int COLUMN_GAP = 6;

	/**
	 * A tier column never shrinks below this. Up to five tiers every tree fits
	 * the window and this never binds; the seven-tier trees — Sword and Armor —
	 * would have to squeeze a node tile down to unreadable to fit, so they
	 * scroll sideways instead.
	 */
	private static final int MIN_COLUMN_WIDTH = 92;

	private static final int MAX_COLUMN_WIDTH = 112;

	/** Height of the horizontal scrollbar, and the room reserved under the tree for it. */
	private static final int SCROLLBAR_HEIGHT = 4;
	private static final int SCROLLBAR_ROOM = 8;

	/** Pixels one notch of the wheel moves the tree. */
	private static final int SCROLL_STEP = 24;

	/** How far a wrapped gate line is indented under its checkbox. */
	private static final int GATE_INDENT = 8;

	/** Which purchase is waiting for a Confirm click. */
	private enum Pending {
		NONE,
		UNLOCK_TIER,
		UNLOCK_NODE,
		ENCHANT_NODE
	}

	private String treeId = SkillTrees.ORDER.getFirst().id();
	@Nullable
	private String selectedNode;
	private int selectedTier = -1;
	private Pending pending = Pending.NONE;

	private int panelX;
	private int panelWidth;
	private int treeRight;
	private int treeTop;
	private int treeBottom;
	private int columnWidth;

	/**
	 * How far the tree is scrolled sideways, in pixels, and the most it can be.
	 * Kept across rebuilds so buying a node does not throw the player back to
	 * tier 1, and clamped on every layout in case the window got wider.
	 */
	private int scrollX;
	private int maxScrollX;
	private boolean draggingScrollbar;

	/**
	 * The tier headers and node tiles, with the x each would sit at unscrolled.
	 * They are {@code addWidget} rather than {@code addRenderableWidget} so this
	 * screen can draw them itself inside a scissor — otherwise a column scrolled
	 * half out of view would paint over the details panel.
	 */
	private record Positioned(AbstractWidget widget, int baseX) {
	}

	private final List<Positioned> treeWidgets = new ArrayList<>();

	/** Node id to the widget drawing it, so the connectors know where to run. */
	private final Map<String, SkillNodeWidget> nodeWidgets = new LinkedHashMap<>();

	public SkillTreeScreen() {
		super(Component.translatable("screen.pbenchants.title"));
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

	// ---------- layout ----------

	private void rebuild() {
		clearWidgets();
		nodeWidgets.clear();
		treeWidgets.clear();

		panelWidth = width >= 400 ? 152 : 134;
		panelX = width - panelWidth - MARGIN;
		treeRight = panelX - MARGIN;

		treeTop = buildTabs() + 6;
		treeBottom = height - 20;

		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		// The bar takes its room out of the tree rather than out of the window,
		// so a five-tier tree is laid out exactly as it always was.
		if (tree != null && overflowOf(tree) > 0) {
			treeBottom -= SCROLLBAR_ROOM;
		}
		if (tree != null) {
			buildTree(tree, state);
			buildActionButtons(tree, state);
		}

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(panelX + 6, height - 26, panelWidth - 12, 18)
			.build());
	}

	/**
	 * Class tabs. Every class gets a labelled tab when the row has room for
	 * one; on a narrow window they all shrink to icon-only instead, because a
	 * second and third row of tabs would eat the height the tree needs. The
	 * name is still one hover away either way.
	 */
	private int buildTabs() {
		int x = MARGIN;
		int y = TITLE_BAR + 3;
		boolean compact = !tabsFitLabelled();
		for (SkillTree tree : SkillTrees.ORDER) {
			Component label = tree.shortName();
			int tabWidth = compact ? ClassTabWidget.ICON_ONLY_WIDTH : ClassTabWidget.widthFor(font, label);
			if (x + tabWidth > treeRight && x > MARGIN) {
				x = MARGIN;
				y += ClassTabWidget.HEIGHT + 2;
			}
			String id = tree.id();
			ClassTabWidget tab = new ClassTabWidget(x, y, tabWidth, tree.iconStack(), label,
				treeId.equals(id), () -> {
					if (!treeId.equals(id)) {
						treeId = id;
						select(null, -1);
					}
				});
			tab.setTooltip(Tooltip.create(tree.displayName()));
			addRenderableWidget(tab);
			x += tabWidth + 2;
		}
		for (SkillTrees.PlannedTree planned : SkillTrees.PLANNED) {
			Component label = Component.literal(planned.name());
			int tabWidth = compact ? ClassTabWidget.ICON_ONLY_WIDTH : ClassTabWidget.widthFor(font, label);
			if (x + tabWidth > treeRight && x > MARGIN) {
				x = MARGIN;
				y += ClassTabWidget.HEIGHT + 2;
			}
			ClassTabWidget tab = new ClassTabWidget(x, y, tabWidth, new ItemStack(planned.icon()), label,
				false, () -> {
				});
			tab.active = false;
			tab.setTooltip(Tooltip.create(Component.translatable("screen.pbenchants.coming_soon")));
			addRenderableWidget(tab);
			x += tabWidth + 2;
		}
		return y + ClassTabWidget.HEIGHT;
	}

	/** Whether one row holds every tab with its name on it. */
	private boolean tabsFitLabelled() {
		int total = MARGIN;
		for (SkillTree tree : SkillTrees.ORDER) {
			total += ClassTabWidget.widthFor(font, tree.shortName()) + 2;
		}
		for (SkillTrees.PlannedTree planned : SkillTrees.PLANNED) {
			total += ClassTabWidget.widthFor(font, Component.literal(planned.name())) + 2;
		}
		return total <= treeRight;
	}

	/** The width one tier column wants, before anyone asks whether they all fit. */
	private int columnWidthFor(SkillTree tree) {
		int columns = tree.tiers().size();
		int fit = (treeRight - MARGIN - (columns - 1) * COLUMN_GAP) / columns;
		return Math.clamp(fit, MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH);
	}

	/** Pixels of tree that do not fit the viewport, and so have to be scrolled to. */
	private int overflowOf(SkillTree tree) {
		int columns = tree.tiers().size();
		int content = columns * columnWidthFor(tree) + (columns - 1) * COLUMN_GAP;
		return Math.max(0, content - (treeRight - MARGIN));
	}

	/** One column per tier: a header, then the tier's nodes stacked under it. */
	private void buildTree(SkillTree tree, @Nullable SkillStatePayload.TreeState state) {
		int columns = tree.tiers().size();
		columnWidth = columnWidthFor(tree);
		maxScrollX = overflowOf(tree);
		scrollX = Math.clamp(scrollX, 0, maxScrollX);
		int unlocked = state == null ? 0 : state.unlockedTiers();

		int tallest = 1;
		for (int tier = 0; tier < columns; tier++) {
			tallest = Math.max(tallest, tree.nodesInTier(tier).size());
		}
		int nodesTop = treeTop + TierHeaderWidget.HEIGHT + 6;
		int pitch = Math.clamp((treeBottom - nodesTop) / tallest, 20, SkillTreeStyle.NODE_PITCH);
		int nodeHeight = Math.min(SkillTreeStyle.NODE_HEIGHT, pitch - 4);

		for (int tier = 0; tier < columns; tier++) {
			int x = MARGIN + tier * (columnWidth + COLUMN_GAP);
			TierHeaderWidget.State headerState = tier < unlocked
				? TierHeaderWidget.State.OPEN
				: tier == unlocked ? TierHeaderWidget.State.NEXT : TierHeaderWidget.State.LOCKED;
			TierHeaderWidget header = new TierHeaderWidget(x, treeTop, columnWidth, tier, tree.tierName(tier),
				headerState, index -> select(null, index));
			header.selected(selectedTier == tier);
			header.setTooltip(Tooltip.create(tierTooltip(tree, tier, state)));
			addWidget(header);
			treeWidgets.add(new Positioned(header, x));

			int y = nodesTop;
			for (SkillNode node : tree.nodesInTier(tier)) {
				NodeState nodeState = stateOf(node, state);
				SkillNodeWidget widget = new SkillNodeWidget(x, y, columnWidth, nodeHeight, node, nodeState,
					picked -> select(picked.id(), -1));
				widget.selected(node.id().equals(selectedNode));
				widget.setTooltip(Tooltip.create(nodeTooltip(node, nodeState, state)));
				addWidget(widget);
				treeWidgets.add(new Positioned(widget, x));
				nodeWidgets.put(node.id(), widget);
				y += pitch;
			}
		}
		applyScroll();
	}

	/**
	 * Puts every tree widget where the current scroll says it belongs, and
	 * hides the ones that have left the viewport.
	 *
	 * <p>The hiding is not cosmetic — the scissor stops a scrolled-out column
	 * being <em>drawn</em> over the details panel, but not from being clicked
	 * through it. {@code visible} is what vanilla's widgets check before they
	 * take a click, so this is the one flag that closes both.
	 */
	private void applyScroll() {
		for (Positioned positioned : treeWidgets) {
			AbstractWidget widget = positioned.widget();
			int x = positioned.baseX() - scrollX;
			widget.setX(x);
			widget.visible = x + widget.getWidth() > MARGIN - 3 && x < treeRight + 3;
		}
	}

	private void scrollTo(int target) {
		int clamped = Math.clamp(target, 0, maxScrollX);
		if (clamped != scrollX) {
			scrollX = clamped;
			applyScroll();
		}
	}

	private boolean overTree(double mouseX, double mouseY) {
		return mouseX >= MARGIN - 3 && mouseX <= treeRight + 3
			&& mouseY >= treeTop - 3 && mouseY <= treeBottom + SCROLLBAR_ROOM;
	}

	private int scrollbarTop() {
		return treeBottom + 4;
	}

	/** Width of the scrollbar thumb: the share of the tree that is on screen. */
	private int thumbWidth() {
		int track = treeRight - MARGIN;
		int content = track + maxScrollX;
		return Math.max(16, track * track / Math.max(1, content));
	}

	private int thumbX() {
		int travel = treeRight - MARGIN - thumbWidth();
		return MARGIN + (maxScrollX == 0 ? 0 : travel * scrollX / maxScrollX);
	}

	/** Drags the thumb so its centre lands under the cursor. */
	private void dragScrollbarTo(double mouseX) {
		int travel = treeRight - MARGIN - thumbWidth();
		if (travel <= 0) {
			return;
		}
		double offset = mouseX - MARGIN - thumbWidth() / 2.0;
		scrollTo((int) Math.round(offset * maxScrollX / travel));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		// Either axis scrolls it: a plain wheel is all most people have, and a
		// tree that only moves sideways for a trackpad would be a trap.
		if (maxScrollX > 0 && overTree(mouseX, mouseY)) {
			double notches = deltaX != 0 ? deltaX : deltaY;
			scrollTo(scrollX - (int) Math.round(notches * SCROLL_STEP));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (maxScrollX > 0 && event.y() >= scrollbarTop() - 2
			&& event.y() <= scrollbarTop() + SCROLLBAR_HEIGHT + 2
			&& event.x() >= MARGIN && event.x() <= treeRight) {
			draggingScrollbar = true;
			dragScrollbarTo(event.x());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingScrollbar) {
			dragScrollbarTo(event.x());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingScrollbar = false;
		return super.mouseReleased(event);
	}

	/** The colour a node wears in the tree, and the reason behind it. */
	private NodeState stateOf(SkillNode node, @Nullable SkillStatePayload.TreeState state) {
		if (state != null && state.purchased().contains(node.id())) {
			return NodeState.OWNED;
		}
		if (!node.implemented()) {
			return NodeState.FUTURE;
		}
		if (state == null) {
			return NodeState.LOCKED;
		}
		if (node.blockedBy(state.purchased()::contains) != null) {
			return NodeState.BLOCKED;
		}
		if (node.tier() >= state.unlockedTiers()
			|| (node.requires() != null && !state.purchased().contains(node.requires()))) {
			return NodeState.LOCKED;
		}
		return NodeState.AVAILABLE;
	}

	private Component nodeTooltip(SkillNode node, NodeState nodeState, @Nullable SkillStatePayload.TreeState state) {
		MutableComponent tip = Component.empty().append(node.displayName())
			.append(Component.literal("\n"))
			.append(SkillTreeStyle.typeName(node.type())
				.withColor(SkillTreeStyle.typeColor(node.type()) & 0xFFFFFF));
		if (nodeState == NodeState.OWNED) {
			if (node.pveOnly()) {
				tip.append(Component.literal("\n"))
					.append(Component.translatable("screen.pbenchants.pve_only").withColor(0xE8A45F));
			}
			tip.append(Component.literal("\n"))
				.append(Component.translatable("screen.pbenchants.unlocked").withColor(0x5FBF4F));
			return tip;
		}
		if (node.pveOnly()) {
			tip.append(Component.literal("\n"))
				.append(Component.translatable("screen.pbenchants.pve_only").withColor(0xE8A45F));
		}
		tip.append(Component.literal("\n"))
			.append(Component.translatable("screen.pbenchants.unlock_cost", node.unlockCost())
				.withColor(0x9AA1AD));
		Component blocker = state == null ? null : unlockProblem(node, state);
		if (blocker != null) {
			tip.append(Component.literal("\n")).append(blocker.copy().withColor(0xE86B6B));
		}
		return tip;
	}

	private Component tierTooltip(SkillTree tree, int tier, @Nullable SkillStatePayload.TreeState state) {
		MutableComponent tip = Component.empty().append(tree.tierName(tier)).append(Component.literal("\n"));
		int unlocked = state == null ? 0 : state.unlockedTiers();
		if (tier < unlocked) {
			return tip.append(Component.translatable("screen.pbenchants.unlocked").withColor(0x5FBF4F));
		}
		return tip.append(Component.translatable("screen.pbenchants.unlock_cost_tier",
			tree.tiers().get(tier).accessCost()).withColor(0x9AA1AD));
	}

	// ---------- actions ----------

	private void buildActionButtons(SkillTree tree, @Nullable SkillStatePayload.TreeState state) {
		int primaryY = height - 68;
		int secondaryY = height - 48;
		int buttonWidth = panelWidth - 12;

		if (pending != Pending.NONE) {
			addRenderableWidget(Button.builder(Component.translatable("screen.pbenchants.confirm"), button -> confirm())
				.bounds(panelX + 6, primaryY, buttonWidth, 18)
				.build());
			addRenderableWidget(Button.builder(Component.translatable("screen.pbenchants.cancel"), button -> {
					pending = Pending.NONE;
					scheduleRebuild();
				})
				.bounds(panelX + 6, secondaryY, buttonWidth, 18)
				.build());
			return;
		}

		if (state == null) {
			addDisabled(Component.translatable("screen.pbenchants.syncing"), primaryY);
			return;
		}

		SkillNode node = selectedNode == null ? null : tree.node(selectedNode);
		if (node != null) {
			boolean owned = state.purchased().contains(node.id());
			Component unlockLabel = owned
				? Component.translatable("screen.pbenchants.unlocked")
				: Component.translatable("screen.pbenchants.unlock_node", node.unlockCost());
			Component unlockBlocker = owned ? null : unlockProblem(node, state);
			if (!owned && unlockBlocker == null) {
				addRenderableWidget(Button.builder(unlockLabel, button -> {
						pending = Pending.UNLOCK_NODE;
						scheduleRebuild();
					})
					.bounds(panelX + 6, primaryY, buttonWidth, 18)
					.build());
			} else {
				Component blockedLabel = node.implemented()
					? Component.translatable("screen.pbenchants.locked")
					: Component.translatable("screen.pbenchants.coming_soon");
				Button disabled = addDisabled(owned ? unlockLabel : blockedLabel, primaryY);
				if (unlockBlocker != null) {
					disabled.setTooltip(Tooltip.create(unlockBlocker));
				}
			}

			if (node.enchantable()) {
				Component enchantLabel = Component.translatable("screen.pbenchants.enchant_node", node.enchantCost());
				if (owned && enchantProblem(node) == null) {
					addRenderableWidget(Button.builder(enchantLabel, button -> {
							pending = Pending.ENCHANT_NODE;
							scheduleRebuild();
						})
						.bounds(panelX + 6, secondaryY, buttonWidth, 18)
						.build());
				} else {
					Button disabled = addDisabled(enchantLabel, secondaryY);
					Component why = owned ? enchantProblem(node) : Component.translatable("screen.pbenchants.enchant_needs_unlock");
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
				addDisabled(Component.translatable("screen.pbenchants.unlocked"), primaryY);
			} else if (selectedTier == state.unlockedTiers()) {
				addRenderableWidget(Button.builder(Component.translatable("screen.pbenchants.unlock", tier.accessCost()),
						button -> {
							pending = Pending.UNLOCK_TIER;
							scheduleRebuild();
						})
					.bounds(panelX + 6, primaryY, buttonWidth, 18)
					.build());
			} else {
				// Tiers open strictly in order, so this one is not even a choice yet.
				Button disabled = addDisabled(Component.translatable("screen.pbenchants.unlock", tier.accessCost()), primaryY);
				disabled.setTooltip(Tooltip.create(
					Component.translatable("screen.pbenchants.tier_needs_previous", state.unlockedTiers() + 1)));
			}
			return;
		}

		addDisabled(Component.translatable("screen.pbenchants.select_hint"), primaryY);
	}

	private Button addDisabled(Component label, int y) {
		Button button = Button.builder(label, ignored -> {
			})
			.bounds(panelX + 6, y, panelWidth - 12, 18)
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
	 * Why this node cannot be unlocked yet, or null when it can. Mirrors the
	 * structural half of {@code SkillService.unlockNode} — the part that is
	 * about the shape of the tree rather than about your wallet: tiers open in
	 * order, and a rank needs the rank below it.
	 *
	 * <p>Running cost (materials and XP levels) is deliberately left out. The
	 * server still checks it and nothing is spent on a failed attempt, and the
	 * details panel already shows the material checklist — greying the button
	 * for "come back with more coal" would hide the difference between
	 * <em>not yet affordable</em> and <em>not yet reachable</em>, which is
	 * exactly the distinction this is here to make.
	 */
	@Nullable
	private Component unlockProblem(SkillNode node, SkillStatePayload.TreeState state) {
		if (!node.implemented()) {
			return Component.translatable("screen.pbenchants.coming_soon");
		}
		if (node.tier() >= state.unlockedTiers()) {
			return Component.translatable("screen.pbenchants.unlock_needs_tier", node.tier() + 1);
		}
		if (node.requires() != null && !state.purchased().contains(node.requires())) {
			return Component.translatable("screen.pbenchants.unlock_needs_node",
				SkillNode.displayName(node.requires()));
		}
		String blocker = node.blockedBy(state.purchased()::contains);
		if (blocker != null) {
			return Component.translatable("screen.pbenchants.unlock_blocked_by",
				SkillNode.displayName(blocker));
		}
		// The end of a tree: everything else in it, first. Asked of the synced
		// snapshot with the same method the server uses, so the button greys out
		// with the real number rather than a guess.
		SkillTree tree = SkillTrees.byId(treeId);
		if (node.requiresAll() && tree != null) {
			int missing = tree.missingForCompletion(state.purchased(), node.id());
			if (missing > 0) {
				return Component.translatable("screen.pbenchants.unlock_needs_all", missing);
			}
		}
		return null;
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
			return Component.translatable("screen.pbenchants.syncing");
		}
		Holder<Enchantment> holder = ModEnchantments.holder(player, grant.enchantment());
		if (holder == null) {
			return Component.translatable("screen.pbenchants.syncing");
		}
		return EnchantCompat.problem(player.getMainHandItem(), holder, grant.level());
	}

	// ---------- rendering ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, SkillTreeStyle.BACKDROP);

		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);

		drawTitleBar(graphics, tree);
		drawTreeCanvas(graphics, tree, state, mouseX, mouseY, delta);
		drawScrollbar(graphics);
		SkillTreeStyle.panel(graphics, panelX, TITLE_BAR + 3, panelWidth, height - TITLE_BAR - 7,
			SkillTreeStyle.PANEL, SkillTreeStyle.BORDER);

		// Widgets (tabs, buttons) draw over the frame; the details text goes on
		// last, clipped to its panel.
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		drawDetails(graphics);
		drawXpBar(graphics);
	}

	private void drawTitleBar(GuiGraphicsExtractor graphics, @Nullable SkillTree tree) {
		graphics.fill(0, 0, width, TITLE_BAR, SkillTreeStyle.PANEL_DEEP);
		graphics.fill(0, TITLE_BAR, width, TITLE_BAR + 1, SkillTreeStyle.BORDER);
		graphics.text(font, title, MARGIN, 4, SkillTreeStyle.GOLD);
		if (tree != null) {
			String name = SkillTreeStyle.trim(font, tree.displayName().getString(),
				Math.max(40, treeRight - MARGIN - font.width(title) - 12));
			graphics.text(font, name, treeRight - font.width(name), 4, SkillTreeStyle.MUTED);
		}
	}

	/** The tree background: the column strips, then the prerequisite wiring. */
	private void drawTreeCanvas(GuiGraphicsExtractor graphics, @Nullable SkillTree tree,
			@Nullable SkillStatePayload.TreeState state, int mouseX, int mouseY, float delta) {
		if (tree == null) {
			return;
		}
		SkillTreeStyle.panel(graphics, MARGIN - 3, treeTop - 3, treeRight - MARGIN + 6,
			treeBottom - treeTop + 6, SkillTreeStyle.PANEL_DEEP, SkillTreeStyle.BORDER);

		// Everything inside the frame is scrolled and clipped to it: the column
		// strips, the wiring, and the tiles themselves. The tree widgets are
		// drawn here rather than with the rest because this is the only place
		// the scissor is up — a column halfway off the edge has to be cut, not
		// painted across the details panel.
		graphics.enableScissor(MARGIN - 3, treeTop - 3, treeRight + 3, treeBottom + 3);
		int unlocked = state == null ? 0 : state.unlockedTiers();
		for (int tier = 0; tier < tree.tiers().size(); tier++) {
			int x = MARGIN + tier * (columnWidth + COLUMN_GAP) - scrollX;
			graphics.fill(x - 2, treeTop - 1, x + columnWidth + 2, treeBottom + 1,
				tier < unlocked ? SkillTreeStyle.COLUMN_OPEN : SkillTreeStyle.COLUMN_LOCKED);
		}
		drawConnectors(graphics, state);
		for (Positioned positioned : treeWidgets) {
			positioned.widget().extractRenderState(graphics, mouseX, mouseY, delta);
		}
		graphics.disableScissor();
	}

	/**
	 * The horizontal scrollbar, drawn only when there is something off screen —
	 * which today means the seven-tier trees, and any tree at all in a narrow
	 * window. The thumb is as wide a share of the track as the viewport is of
	 * the tree, so its size says how much you are not seeing.
	 */
	private void drawScrollbar(GuiGraphicsExtractor graphics) {
		if (maxScrollX <= 0) {
			return;
		}
		int top = scrollbarTop();
		graphics.fill(MARGIN, top, treeRight, top + SCROLLBAR_HEIGHT, SkillTreeStyle.PANEL_DEEP);
		int thumb = thumbX();
		graphics.fill(thumb, top, thumb + thumbWidth(), top + SCROLLBAR_HEIGHT,
			draggingScrollbar ? SkillTreeStyle.GOLD : SkillTreeStyle.BORDER_LIT);
	}

	/**
	 * The lines that make a rank chain read as a chain: Melt I to II to III.
	 * A prerequisite one column back is wired with a Z through the gutter
	 * between the columns; one in the same column — a node that needs the top
	 * rank of its own tier — routes around the left edge instead.
	 */
	private void drawConnectors(GuiGraphicsExtractor graphics, @Nullable SkillStatePayload.TreeState state) {
		for (SkillNodeWidget target : nodeWidgets.values()) {
			String requires = target.node().requires();
			if (requires == null) {
				continue;
			}
			SkillNodeWidget source = nodeWidgets.get(requires);
			if (source == null) {
				continue;
			}
			boolean satisfied = state != null && state.purchased().contains(requires);
			int color = satisfied ? SkillTreeStyle.GREEN : SkillTreeStyle.BORDER_LIT;
			int fromY = source.connectorY();
			int toY = target.connectorY();
			int toX = target.getX();

			if (source.getRight() <= toX) {
				int fromX = source.getRight();
				int mid = (fromX + toX) / 2;
				horizontal(graphics, fromX, mid, fromY, color);
				vertical(graphics, mid, fromY, toY, color);
				horizontal(graphics, mid, toX, toY, color);
			} else {
				int gutter = toX - 3;
				horizontal(graphics, gutter, source.getX(), fromY, color);
				vertical(graphics, gutter, fromY, toY, color);
				horizontal(graphics, gutter, toX, toY, color);
			}
			// A stub where the line lands, so the direction of the chain reads.
			graphics.fill(toX - 2, toY - 1, toX, toY + 2, color);
		}
	}

	private void horizontal(GuiGraphicsExtractor graphics, int x1, int x2, int y, int color) {
		graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2), y + 1, color);
	}

	private void vertical(GuiGraphicsExtractor graphics, int x, int y1, int y2, int color) {
		graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color);
	}

	/** The player's experience, drawn the way the HUD draws it. */
	private void drawXpBar(GuiGraphicsExtractor graphics) {
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		if (player == null) {
			return;
		}
		int barX = MARGIN;
		int barWidth = treeRight - MARGIN;
		int barY = height - 14;
		SkillTreeStyle.progressBar(graphics, barX, barY, barWidth, 11, player.experienceProgress,
			SkillTreeStyle.XP_GREEN);

		// The level rides on the bar rather than above it: the HUD has the whole
		// screen to breathe in, this strip has eleven pixels.
		String level = Component.translatable("screen.pbenchants.levels", player.experienceLevel).getString();
		SkillTreeStyle.outlinedText(graphics, font, level,
			barX + (barWidth - font.width(level)) / 2, barY + 2, SkillTreeStyle.TEXT);
	}

	/**
	 * Word-wraps text to the details panel width instead of letting long lines
	 * escape the panel. Returns the y just below the last drawn line.
	 */
	private int wrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int lineSpacing) {
		for (FormattedCharSequence line : font.split(text, panelWidth - 12)) {
			graphics.text(font, line, x, y, color);
			y += lineSpacing;
		}
		return y;
	}

	private void drawDetails(GuiGraphicsExtractor graphics) {
		SkillTree tree = SkillTrees.byId(treeId);
		SkillStatePayload.TreeState state = ClientSkillState.tree(treeId);
		int x = panelX + 6;
		int y = TITLE_BAR + 9;

		// Everything below belongs to the buttons: clip, so a long description
		// stops at the edge instead of running under them.
		graphics.enableScissor(panelX + 1, TITLE_BAR + 4, panelX + panelWidth - 1, height - 72);
		if (tree == null || state == null) {
			graphics.text(font, Component.translatable("screen.pbenchants.syncing"), x, y, SkillTreeStyle.MUTED);
		} else if (pending != Pending.NONE) {
			drawConfirmation(graphics, tree, x, y);
		} else if (selectedNode != null) {
			SkillNode node = tree.node(selectedNode);
			if (node != null) {
				drawNode(graphics, node, state, x, y);
			}
		} else if (selectedTier >= 0) {
			drawTier(graphics, tree, state, x, y);
		} else {
			graphics.textWithWordWrap(font, Component.translatable("screen.pbenchants.help"),
				x, y, panelWidth - 12, SkillTreeStyle.MUTED);
		}
		graphics.disableScissor();
	}

	private void drawNode(GuiGraphicsExtractor graphics, SkillNode node, SkillStatePayload.TreeState state, int x, int y) {
		int wrap = panelWidth - 12;
		graphics.item(node.iconStack(), x, y);
		int afterTitle = y;
		for (FormattedCharSequence line : font.split(node.displayName(), wrap - 20)) {
			graphics.text(font, line, x + 20, afterTitle + 4, SkillTreeStyle.TEXT);
			afterTitle += 10;
		}
		y = Math.max(afterTitle, y + 18) + 2;

		SkillTreeStyle.badge(graphics, font, SkillTreeStyle.typeName(node.type()), x, y,
			SkillTreeStyle.typeColor(node.type()));
		y += 15;

		if (!node.implemented()) {
			// highlighted badge — the full description still renders below
			y = wrappedText(graphics, Component.translatable("screen.pbenchants.coming_soon_badge"),
				x, y, SkillTreeStyle.SOON, 11) + 2;
		}

		boolean owned = state.purchased().contains(node.id());
		y = wrappedText(graphics,
			owned
				? Component.translatable("screen.pbenchants.unlocked")
				: Component.translatable("screen.pbenchants.unlock_cost", node.unlockCost()),
			x, y, owned ? SkillTreeStyle.GREEN : SkillTreeStyle.MUTED, 11);
		if (!owned) {
			y = drawMaterials(graphics, node, x, y);
		}
		if (node.enchantable()) {
			y = wrappedText(graphics, Component.translatable("screen.pbenchants.enchant_cost", node.enchantCost()),
				x, y, SkillTreeStyle.MUTED, 11);
		}
		if (node.requires() != null) {
			boolean has = state.purchased().contains(node.requires());
			y = wrappedText(graphics,
				Component.translatable("screen.pbenchants.requires", SkillNode.displayName(node.requires())),
				x, y, has ? SkillTreeStyle.GREEN : SkillTreeStyle.BAD, 11);
		}
		for (String exclusive : node.exclusiveWith()) {
			y = wrappedText(graphics,
				Component.translatable("screen.pbenchants.exclusive", SkillNode.displayName(exclusive)),
				x, y, SkillTreeStyle.DIM, 11);
		}
		y += 4;
		// Keyed on the full node id, not the family: every rank describes only
		// what that rank does, so Dig Range II does not recite I and III too.
		graphics.textWithWordWrap(font,
			Component.translatable("node.pbenchants." + node.id() + ".desc"),
			x, y, wrap, SkillTreeStyle.MUTED);
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
			y = wrappedText(graphics, line, x, y, done ? SkillTreeStyle.GREEN : SkillTreeStyle.MUTED, 11);
		}
		return y;
	}

	private void drawTier(GuiGraphicsExtractor graphics, SkillTree tree, SkillStatePayload.TreeState state, int x, int y) {
		SkillTier tier = tree.tiers().get(selectedTier);
		boolean open = selectedTier < state.unlockedTiers();
		y = wrappedText(graphics, tree.tierName(selectedTier), x, y, SkillTreeStyle.TEXT, 11);
		y = wrappedText(graphics,
			open
				? Component.translatable("screen.pbenchants.unlocked")
				: Component.translatable("screen.pbenchants.unlock_cost_tier", tier.accessCost()),
			x, y, open ? SkillTreeStyle.GREEN : SkillTreeStyle.MUTED, 13);

		graphics.text(font, Component.translatable("screen.pbenchants.gate"), x, y, SkillTreeStyle.GOLD);
		y += 12;
		for (GateRequirement gate : tier.gates()) {
			int count = Math.min(state.counters().getOrDefault(gate.id(), 0), gate.target());
			boolean done = count >= gate.target();
			int color = done ? SkillTreeStyle.GREEN : SkillTreeStyle.MUTED;
			Component line = Component.literal((done ? "✓ " : "□ ") + gate.displayName()
				+ " " + count + "/" + gate.target());
			// A gate name too long for the panel wraps; the continuation is
			// indented under the checkbox so the list still reads as a list.
			boolean first = true;
			for (FormattedCharSequence part : font.split(line, panelWidth - 12 - GATE_INDENT)) {
				graphics.text(font, part, first ? x : x + GATE_INDENT, y, color);
				y += 10;
				first = false;
			}
			SkillTreeStyle.progressBar(graphics, x, y, panelWidth - 12, 3,
				(float) count / gate.target(), done ? SkillTreeStyle.GREEN : SkillTreeStyle.GOLD);
			y += 7;
		}
	}

	/**
	 * The card behind Unlock/Enchant: what this click costs, what it changes,
	 * and — for an enchant — which item is about to receive it.
	 */
	private void drawConfirmation(GuiGraphicsExtractor graphics, SkillTree tree, int x, int y) {
		int wrap = panelWidth - 12;
		SkillNode node = selectedNode == null ? null : tree.node(selectedNode);

		if (pending == Pending.UNLOCK_TIER) {
			int tierIndex = Math.max(selectedTier, 0);
			SkillTier tier = tree.tiers().get(tierIndex);
			y = wrappedText(graphics, Component.translatable("screen.pbenchants.confirm.tier_title",
				tree.tierName(tierIndex)), x, y, SkillTreeStyle.GOLD, 11);
			y += 2;
			graphics.textWithWordWrap(font,
				Component.translatable("screen.pbenchants.confirm.tier_body", tier.accessCost()),
				x, y, wrap, SkillTreeStyle.MUTED);
			return;
		}
		if (node == null) {
			return;
		}

		if (pending == Pending.UNLOCK_NODE) {
			y = wrappedText(graphics, Component.translatable("screen.pbenchants.confirm.unlock_title",
				node.displayName()), x, y, SkillTreeStyle.GOLD, 11);
			y = wrappedText(graphics, Component.translatable("screen.pbenchants.unlock_cost", node.unlockCost()),
				x, y, SkillTreeStyle.MUTED, 11);
			y = drawMaterials(graphics, node, x, y);
			y += 2;
			ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
			String bodyKey;
			if (!node.enchantable()) {
				bodyKey = "screen.pbenchants.confirm.unlock_body_passive";
			} else if (grant != null && ModEnchantments.TABLE_POOL.contains(grant.enchantment())) {
				bodyKey = "screen.pbenchants.confirm.unlock_body_enchantment";
			} else {
				bodyKey = "screen.pbenchants.confirm.unlock_body_capstone";
			}
			graphics.textWithWordWrap(font,
				Component.translatable(bodyKey, node.displayName(), node.enchantCost()), x, y, wrap,
				SkillTreeStyle.MUTED);
			return;
		}

		// ENCHANT_NODE
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		Component held = player == null ? Component.empty() : player.getMainHandItem().getHoverName();
		y = wrappedText(graphics, Component.translatable("screen.pbenchants.confirm.enchant_title",
			node.displayName()), x, y, SkillTreeStyle.GOLD, 11);
		y = wrappedText(graphics, Component.translatable("screen.pbenchants.confirm.enchant_target", held),
			x, y, SkillTreeStyle.TEXT, 11);
		y = wrappedText(graphics, Component.translatable("screen.pbenchants.enchant_cost", node.enchantCost()),
			x, y, SkillTreeStyle.MUTED, 11);
		Component problem = enchantProblem(node);
		if (problem != null) {
			y = wrappedText(graphics, problem, x, y, SkillTreeStyle.BAD, 11);
		}
		y += 2;
		graphics.textWithWordWrap(font, Component.translatable("screen.pbenchants.confirm.enchant_body"),
			x, y, wrap, SkillTreeStyle.MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
