package dev.toolmastery.client;

import dev.toolmastery.client.gui.SkillTreeStyle;
import dev.toolmastery.client.gui.StorageResultWidget;
import dev.toolmastery.network.ArtisanActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The Ledger — one searchable page listing everything in every container in
 * reach, with counts and bearings.
 *
 * <p>Read-only by design: an index, not a remote inventory. You still have to
 * walk to the chest, which is the difference between a quality-of-life perk and
 * a wireless storage network.
 */
public class LedgerScreen extends Screen {
	private static final int MARGIN = 24;

	@Nullable
	private final Screen parent;

	private EditBox search;

	public LedgerScreen(@Nullable Screen parent) {
		super(Component.translatable("screen.toolmastery.ledger.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		search = new EditBox(font, MARGIN, MARGIN + 14, width - MARGIN * 2 - 60, 18,
			Component.translatable("screen.toolmastery.search"));
		search.setMaxLength(ArtisanActionPayload.MAX_QUERY);
		search.setHint(Component.translatable("screen.toolmastery.search.hint"));
		search.setResponder(query -> ClientPlayNetworking.send(ArtisanActionPayload.search(query)));
		addRenderableWidget(search);
		setInitialFocus(search);

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(width - MARGIN - 56, MARGIN + 14, 56, 18)
			.build());

		addRenderableWidget(new StorageResultWidget(MARGIN, MARGIN + 38, width - MARGIN * 2,
			height - MARGIN * 2 - 40, ClientArtisanState::searchResults));

		// Ask again on every rebuild: a resize must not leave a stale answer.
		ClientPlayNetworking.send(ArtisanActionPayload.search(search.getValue()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, SkillTreeStyle.BACKDROP);
		graphics.text(font, title, MARGIN, MARGIN, SkillTreeStyle.GOLD);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		ClientArtisanState.clearSearch();
		minecraft.setScreenAndShow(parent);
	}
}
