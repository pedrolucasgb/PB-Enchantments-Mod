package dev.pbenchants.enchant;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.skill.TreeSwitch;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The librarian's three PB offers, as code.
 *
 * <p>On 26.x these are data: {@code data/toolmastery/villager_trade/librarian/1/*.json}
 * appended to the {@code librarian/level_1} trade tag. 1.21.1 has no data-driven
 * trades, so the same three offers are added to the level-1 librarian pool
 * through Fabric's {@link TradeOfferHelper}. A librarian still draws two
 * level-1 trades from the merged pool and keeps them for life, exactly as
 * before.
 *
 * <p>Each offer mirrors its JSON: an emerald price of a flat surcharge plus
 * vanilla's rank-scaled enchanted-book price (what {@code enchant_randomly}
 * with {@code include_additional_cost_component} adds on 26.x: {@code 2 +
 * rand(5 + 10 * level) + 3 * level}, doubled for {@code #double_trade_price}),
 * capped at a stack of 64; a book as the second cost; 3 uses, 10 villager XP
 * and a 0.2 price multiplier. An enchantment whose tree is switched off on
 * this server is never picked — if that leaves nothing, the listing yields no
 * offer and the villager simply rolls something else.
 */
public final class LibrarianTrades {
	private static final TagKey<Enchantment> TRADE_POOL =
		TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(PBEnchants.DATA_NS, "trade_pool"));

	private LibrarianTrades() {
	}

	public static void register() {
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, factories -> {
			// emerald_and_book_mastery_book: any book from the trade pool.
			factories.add(new BookForEmeralds(24, registry -> registry.getTag(TRADE_POOL)
				.map(set -> set.stream().toList()).orElse(List.of())));
			// emerald_and_book_indestructible
			factories.add(new BookForEmeralds(32, registry -> single(registry, ModEnchantments.INDESTRUCTIBLE)));
			// emerald_and_book_slipstream
			factories.add(new BookForEmeralds(24, registry -> single(registry, ModEnchantments.SLIPSTREAM)));
		});
	}

	private static List<Holder<Enchantment>> single(Registry<Enchantment> registry, ResourceKey<Enchantment> key) {
		return registry.getHolder(key).<List<Holder<Enchantment>>>map(List::of).orElse(List.of());
	}

	private record BookForEmeralds(int surcharge, Function<Registry<Enchantment>, List<Holder<Enchantment>>> options)
		implements VillagerTrades.ItemListing {

		@Nullable
		@Override
		public MerchantOffer getOffer(Entity trader, RandomSource random) {
			Registry<Enchantment> registry = trader.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
			List<Holder<Enchantment>> allowed = new ArrayList<>();
			for (Holder<Enchantment> holder : options.apply(registry)) {
				if (TreeSwitch.enchantmentAllowed(holder)) {
					allowed.add(holder);
				}
			}
			if (allowed.isEmpty()) {
				return null;
			}
			Holder<Enchantment> enchantment = allowed.get(random.nextInt(allowed.size()));
			int level = Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
			int bookPrice = 2 + random.nextInt(5 + level * 10) + 3 * level;
			if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
				bookPrice *= 2;
			}
			int price = Math.min(64, surcharge + bookPrice);
			ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
			return new MerchantOffer(new ItemCost(Items.EMERALD, price), Optional.of(new ItemCost(Items.BOOK)),
				book, 3, 10, 0.2F);
		}
	}
}
