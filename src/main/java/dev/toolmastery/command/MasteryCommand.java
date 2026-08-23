package dev.toolmastery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.GateRequirement;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTier;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The command interface to the skill system. This is also the dev-testing
 * surface until the GUI lands:
 *
 *   /mastery status <tree>
 *   /mastery unlock <tree>            unlock the next tier
 *   /mastery unlock <tree> <node>     unlock a node (XP levels + materials)
 *   /mastery enchant <tree> <node>    stamp it on the held item (XP levels, repeatable)
 *   /mastery debug add <tree> <counter> <amount>   (op only)
 */
public final class MasteryCommand {
	private static final SuggestionProvider<CommandSourceStack> TREE_IDS =
		(context, builder) -> SharedSuggestionProvider.suggest(SkillTrees.ALL.keySet(), builder);

	private static final SuggestionProvider<CommandSourceStack> NODE_IDS = (context, builder) -> {
		SkillTree tree = SkillTrees.byId(StringArgumentType.getString(context, "tree"));
		return tree == null
			? builder.buildFuture()
			: SharedSuggestionProvider.suggest(tree.nodes().keySet(), builder);
	};

	/** Only the nodes that actually have an enchant action. */
	private static final SuggestionProvider<CommandSourceStack> ENCHANT_NODE_IDS = (context, builder) -> {
		SkillTree tree = SkillTrees.byId(StringArgumentType.getString(context, "tree"));
		return tree == null
			? builder.buildFuture()
			: SharedSuggestionProvider.suggest(
				tree.nodes().values().stream().filter(SkillNode::enchantable).map(SkillNode::id).toList(), builder);
	};

	private MasteryCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mastery")
			.then(Commands.literal("status")
				.then(Commands.argument("tree", StringArgumentType.word())
					.suggests(TREE_IDS)
					.executes(context -> status(context.getSource(), StringArgumentType.getString(context, "tree")))))
			.then(Commands.literal("unlock")
				.then(Commands.argument("tree", StringArgumentType.word())
					.suggests(TREE_IDS)
					// bare: the next tier - with a node argument: that node
					.executes(context -> unlockTier(context.getSource(), StringArgumentType.getString(context, "tree")))
					.then(Commands.argument("node", StringArgumentType.word())
						.suggests(NODE_IDS)
						.executes(context -> unlockNode(
							context.getSource(),
							StringArgumentType.getString(context, "tree"),
							StringArgumentType.getString(context, "node"))))))
			.then(Commands.literal("enchant")
				.then(Commands.argument("tree", StringArgumentType.word())
					.suggests(TREE_IDS)
					.then(Commands.argument("node", StringArgumentType.word())
						.suggests(ENCHANT_NODE_IDS)
						.executes(context -> enchant(
							context.getSource(),
							StringArgumentType.getString(context, "tree"),
							StringArgumentType.getString(context, "node"))))))
			.then(Commands.literal("debug")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("maxall")
					.executes(context -> {
						ServerPlayer player = context.getSource().getPlayer();
						if (player == null) {
							return 0;
						}
						SkillService.maxAll(player);
						dev.toolmastery.network.ModNetworking.sendState(player);
						context.getSource().sendSystemMessage(Component.literal("All gates completed and all tiers unlocked.").withStyle(ChatFormatting.YELLOW));
						return 1;
					}))
				.then(Commands.literal("kit")
					.executes(context -> kit(context.getSource(), null, null))
					.then(Commands.argument("tree", StringArgumentType.word())
						.suggests(TREE_IDS)
						.executes(context -> kit(context.getSource(), StringArgumentType.getString(context, "tree"), null))
						.then(Commands.argument("enchant", StringArgumentType.word())
							.suggests((context, builder) -> {
								SkillTree tree = SkillTrees.byId(StringArgumentType.getString(context, "tree"));
								return tree == null
									? builder.buildFuture()
									: SharedSuggestionProvider.suggest(enchantsOf(tree).keySet(), builder);
							})
							.executes(context -> kit(
								context.getSource(),
								StringArgumentType.getString(context, "tree"),
								StringArgumentType.getString(context, "enchant"))))))
				.then(Commands.literal("unlockall")
					.executes(context -> {
						ServerPlayer player = context.getSource().getPlayer();
						if (player == null) {
							return 0;
						}
						SkillService.maxAll(player);
						SkillService.unlockAll(player);
						dev.toolmastery.network.ModNetworking.sendState(player);
						context.getSource().sendSystemMessage(Component.literal("Every node unlocked. Enchantments are unlocked only - stamp them on a tool with /mastery enchant, the skill screen, or /mastery debug kit.").withStyle(ChatFormatting.YELLOW));
						return 1;
					}))
				.then(Commands.literal("add")
					.then(Commands.argument("tree", StringArgumentType.word())
						.suggests(TREE_IDS)
						.then(Commands.argument("counter", StringArgumentType.word())
							.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(context -> debugAdd(
									context.getSource(),
									StringArgumentType.getString(context, "tree"),
									StringArgumentType.getString(context, "counter"),
									IntegerArgumentType.getInteger(context, "amount")))))))));
	}

	private static SkillTree treeOrFail(CommandSourceStack source, String treeId) {
		SkillTree tree = SkillTrees.byId(treeId);
		if (tree == null) {
			source.sendFailure(Component.literal("Unknown tree '" + treeId + "'. Available: " + String.join(", ", SkillTrees.ALL.keySet())));
		}
		return tree;
	}

	private static int status(CommandSourceStack source, String treeId) {
		SkillTree tree = treeOrFail(source, treeId);
		if (tree == null) {
			return 0;
		}
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return 0;
		}
		TreeProgress progress = SkillService.progress(player, tree);

		source.sendSystemMessage(Component.literal("— " + treeId + " — tiers unlocked: " + progress.unlockedTiers + "/" + tree.tiers().size())
			.withStyle(ChatFormatting.AQUA));

		int next = progress.unlockedTiers;
		if (next < tree.tiers().size()) {
			SkillTier tier = tree.tiers().get(next);
			source.sendSystemMessage(Component.literal("Next gate (tier " + (next + 1) + ", access " + tier.accessCost() + " lv):")
				.withStyle(ChatFormatting.GOLD));
			for (GateRequirement gate : tier.gates()) {
				int count = Math.min(progress.count(gate.id()), gate.target());
				boolean done = count >= gate.target();
				source.sendSystemMessage(Component.literal((done ? "  [done] " : "  [    ] ") + gate.displayName() + " " + count + "/" + gate.target())
					.withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			}
		}

		source.sendSystemMessage(Component.literal("Owned nodes:").withStyle(ChatFormatting.GOLD));
		for (SkillNode node : tree.nodes().values()) {
			if (progress.owns(node.id())) {
				source.sendSystemMessage(Component.literal("  * " + node.id()).withStyle(ChatFormatting.GREEN));
			}
		}
		return 1;
	}

	private static int unlockTier(CommandSourceStack source, String treeId) {
		SkillTree tree = treeOrFail(source, treeId);
		ServerPlayer player = source.getPlayer();
		if (tree == null || player == null) {
			return 0;
		}
		return report(source, SkillService.unlockNextTier(player, tree));
	}

	private static int unlockNode(CommandSourceStack source, String treeId, String nodeId) {
		SkillNode node = nodeOrFail(source, treeId, nodeId);
		ServerPlayer player = source.getPlayer();
		if (node == null || player == null) {
			return 0;
		}
		return report(source, SkillService.unlockNode(player, SkillTrees.byId(treeId), node));
	}

	private static int enchant(CommandSourceStack source, String treeId, String nodeId) {
		SkillNode node = nodeOrFail(source, treeId, nodeId);
		ServerPlayer player = source.getPlayer();
		if (node == null || player == null) {
			return 0;
		}
		return report(source, SkillService.enchantHeld(player, SkillTrees.byId(treeId), node));
	}

	@Nullable
	private static SkillNode nodeOrFail(CommandSourceStack source, String treeId, String nodeId) {
		SkillTree tree = treeOrFail(source, treeId);
		if (tree == null) {
			return null;
		}
		SkillNode node = tree.node(nodeId);
		if (node == null) {
			source.sendFailure(Component.literal("Unknown node '" + nodeId + "' in tree '" + treeId + "'."));
		}
		return node;
	}

	private static int debugAdd(CommandSourceStack source, String treeId, String counterId, int amount) {
		SkillTree tree = treeOrFail(source, treeId);
		ServerPlayer player = source.getPlayer();
		if (tree == null || player == null) {
			return 0;
		}
		SkillService.addCount(player, tree, counterId, amount);
		source.sendSystemMessage(Component.literal("Counter '" + counterId + "' += " + amount).withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	/**
	 * /mastery debug kit [tree] [enchant]
	 * - no args: full kit for every tree (one tool per tier)
	 * - tree: tier kit for that tree only
	 * - tree + enchant: one tool per level of that enchantment, nothing else
	 */
	private static int kit(CommandSourceStack source, @Nullable String treeId, @Nullable String enchantName) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return 0;
		}
		if (treeId == null) {
			giveTierTools(player, SkillTrees.PICKAXE, net.minecraft.world.item.Items.DIAMOND_PICKAXE, "Pickaxe");
			giveTierTools(player, SkillTrees.AXE, net.minecraft.world.item.Items.DIAMOND_AXE, "Axe");
			source.sendSystemMessage(Component.literal("Full kit delivered: one tool per tier of every tree.").withStyle(ChatFormatting.YELLOW));
			return 1;
		}
		SkillTree tree = treeOrFail(source, treeId);
		if (tree == null) {
			return 0;
		}
		net.minecraft.world.item.Item item = treeId.equals("axe")
			? net.minecraft.world.item.Items.DIAMOND_AXE
			: net.minecraft.world.item.Items.DIAMOND_PICKAXE;
		String label = treeId.substring(0, 1).toUpperCase() + treeId.substring(1);

		if (enchantName == null) {
			giveTierTools(player, tree, item, label);
			source.sendSystemMessage(Component.literal(label + " kit delivered: one tool per tier.").withStyle(ChatFormatting.YELLOW));
			return 1;
		}

		Integer maxLevel = enchantsOf(tree).get(enchantName);
		if (maxLevel == null) {
			source.sendFailure(Component.literal("Unknown enchantment '" + enchantName + "' for " + treeId
				+ ". Options: " + String.join(", ", enchantsOf(tree).keySet())));
			return 0;
		}
		dev.toolmastery.enchant.ModEnchantments.Grant reference = null;
		for (int level = 1; level <= maxLevel; level++) {
			net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
			for (dev.toolmastery.enchant.ModEnchantments.Grant grant : dev.toolmastery.enchant.ModEnchantments.NODE_GRANTS.values()) {
				if (grant.enchantment().identifier().getPath().equals(enchantName) && grant.level() == level) {
					reference = grant;
				}
			}
			if (reference == null) {
				continue;
			}
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
				Component.literal(prettyName(enchantName) + " " + roman(level)).withStyle(ChatFormatting.AQUA));
			dev.toolmastery.enchant.ModEnchantments.apply(player, stack, reference.enchantment(), level);
			if (!player.addItem(stack)) {
				player.drop(stack, false);
			}
		}
		source.sendSystemMessage(Component.literal(prettyName(enchantName) + " kit delivered: levels I-" + roman(maxLevel) + ".").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	/** Enchantment base names available in a tree, with their max level. */
	private static java.util.Map<String, Integer> enchantsOf(SkillTree tree) {
		java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
		for (SkillNode node : tree.nodes().values()) {
			dev.toolmastery.enchant.ModEnchantments.Grant grant = dev.toolmastery.enchant.ModEnchantments.NODE_GRANTS.get(node.id());
			if (grant != null) {
				result.merge(grant.enchantment().identifier().getPath(), grant.level(), Math::max);
			}
		}
		return result;
	}

	private static String prettyName(String id) {
		String[] words = id.split("_");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return builder.toString();
	}

	private static String roman(int level) {
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(level);
		};
	}

	private static void giveTierTools(ServerPlayer player, SkillTree tree, net.minecraft.world.item.Item item, String label) {
		for (int tierIndex = 0; tierIndex < tree.tiers().size(); tierIndex++) {
			net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
				Component.literal(label + " — Tier " + (tierIndex + 1)).withStyle(ChatFormatting.AQUA));

			// Cumulative best enchantment levels available up to this tier.
			java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>, Integer> best = new java.util.HashMap<>();
			for (SkillNode node : tree.nodes().values()) {
				if (node.tier() > tierIndex) {
					continue;
				}
				dev.toolmastery.enchant.ModEnchantments.Grant grant = dev.toolmastery.enchant.ModEnchantments.NODE_GRANTS.get(node.id());
				if (grant != null) {
					best.merge(grant.enchantment(), grant.level(), Math::max);
				}
			}
			best.forEach((enchantmentKey, level) ->
				dev.toolmastery.enchant.ModEnchantments.apply(player, stack, enchantmentKey, level));

			// Vanilla progression ladder so every tier tool feels the growth:
			// Efficiency I-V, Unbreaking I-III, and Fortune I-III on the pickaxe.
			// Tier 5 stops at Fortune III because Ancient Fortune, stamped on
			// above, is what pushes the effective level to IV.
			dev.toolmastery.enchant.ModEnchantments.apply(player, stack,
				net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY, tierIndex + 1);
			dev.toolmastery.enchant.ModEnchantments.apply(player, stack,
				net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, Math.min(tierIndex + 1, 3));
			if (item == net.minecraft.world.item.Items.DIAMOND_PICKAXE) {
				int fortune = switch (tierIndex) {
					case 2 -> 1;
					case 3 -> 2;
					case 4 -> 3;
					default -> 0;
				};
				if (fortune > 0) {
					dev.toolmastery.enchant.ModEnchantments.apply(player, stack,
						net.minecraft.world.item.enchantment.Enchantments.FORTUNE, fortune);
				}
			}

			if (!player.addItem(stack)) {
				player.drop(stack, false);
			}
		}
	}

	private static int report(CommandSourceStack source, SkillService.Result result) {
		switch (result) {
			case SkillService.Result.Ok ok -> source.sendSystemMessage(ok.message().copy().withStyle(ChatFormatting.GREEN));
			case SkillService.Result.Fail fail -> source.sendFailure(fail.message());
		}
		return result instanceof SkillService.Result.Ok ? 1 : 0;
	}
}
