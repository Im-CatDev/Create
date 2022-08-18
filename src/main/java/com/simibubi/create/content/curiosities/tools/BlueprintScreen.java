package com.simibubi.create.content.curiosities.tools;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.logistics.item.filter.FilterScreenPacket;
import com.simibubi.create.content.logistics.item.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.container.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.element.PartialModelGuiElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BlueprintScreen extends AbstractSimiContainerScreen<BlueprintContainer> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public BlueprintScreen(BlueprintContainer container, Inventory inv, Component title) {
		super(container, inv, title);
		this.background = AllGuiTextures.BLUEPRINT;
	}

	@Override
	protected void init() {
		setWindowSize(background.getWidth(), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(1, 0);
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			contentsCleared();
			menu.sendClearPacket();
		});
		confirmButton = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			minecraft.player.closeContainer();
		});

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + background.getWidth(), y + background.getHeight() - 36, 56, 44));
	}

	@Override
	protected void renderBg(PoseStack ms, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.getHeight() + 4;
		renderPlayerInventory(ms, invX, invY);

		int x = leftPos;
		int y = topPos;

		background.render(ms, x, y, this);
		font.draw(ms, title, x + 15, y + 4, 0xFFFFFF);

		PartialModelGuiElement.of(AllBlockPartials.CRAFTING_BLUEPRINT_1x1).<GuiGameElement
			.GuiRenderBuilder>at(x + background.getWidth() + 20, y + background.getHeight() - 32, 0)
			.rotate(45, -45, 22.5f)
			.scale(40)
			.render(ms);
	}

	@Override
	protected void renderTooltip(PoseStack ms, int x, int y) {
		if (!menu.getCarried()
			.isEmpty() || this.hoveredSlot == null || this.hoveredSlot.hasItem()
			|| hoveredSlot.container == menu.playerInventory) {
			super.renderTooltip(ms, x, y);
			return;
		}
		renderComponentTooltip(ms, addToTooltip(new LinkedList<>(), hoveredSlot.getSlotIndex(), true), x, y, font);
	}

	@Override
	public List<Component> getTooltipFromItem(ItemStack stack) {
		List<Component> list = super.getTooltipFromItem(stack);
		if (hoveredSlot.container == menu.playerInventory)
			return list;
		return hoveredSlot != null ? addToTooltip(list, hoveredSlot.getSlotIndex(), false) : list;
	}

	private List<Component> addToTooltip(List<Component> list, int slot, boolean isEmptySlot) {
		if (slot < 0 || slot > 10)
			return list;

		if (slot < 9) {
			list.add(CreateLang.translateDirect("crafting_blueprint.crafting_slot")
				.withStyle(ChatFormatting.GOLD));
			if (isEmptySlot)
				list.add(CreateLang.translateDirect("crafting_blueprint.filter_items_viable")
					.withStyle(ChatFormatting.GRAY));

		} else if (slot == 9) {
			list.add(CreateLang.translateDirect("crafting_blueprint.display_slot")
				.withStyle(ChatFormatting.GOLD));
			if (!isEmptySlot)
				list.add(CreateLang
					.translateDirect(
						"crafting_blueprint." + (menu.contentHolder.inferredIcon ? "inferred" : "manually_assigned"))
					.withStyle(ChatFormatting.GRAY));

		} else if (slot == 10) {
			list.add(CreateLang.translateDirect("crafting_blueprint.secondary_display_slot")
				.withStyle(ChatFormatting.GOLD));
			if (isEmptySlot)
				list.add(CreateLang.translateDirect("crafting_blueprint.optional")
					.withStyle(ChatFormatting.GRAY));
		}

		return list;
	}

	@Override
	protected void containerTick() {
		if (!menu.contentHolder.isEntityAlive())
			menu.player.closeContainer();

		super.containerTick();

//		handleTooltips();
	}

//	protected void handleTooltips() {
//		List<IconButton> tooltipButtons = getTooltipButtons();
//
//		for (IconButton button : tooltipButtons) {
//			if (!button.getToolTip()
//				.isEmpty()) {
//				button.setToolTip(button.getToolTip()
//					.get(0));
//				button.getToolTip()
//					.add(TooltipHelper.holdShift(Palette.Yellow, hasShiftDown()));
//			}
//		}
//
//		if (hasShiftDown()) {
//			List<IFormattableTextComponent> tooltipDescriptions = getTooltipDescriptions();
//			for (int i = 0; i < tooltipButtons.size(); i++)
//				fillToolTip(tooltipButtons.get(i), tooltipDescriptions.get(i));
//		}
//	}

	protected void contentsCleared() {}

	protected void sendOptionUpdate(Option option) {
		AllPackets.channel.sendToServer(new FilterScreenPacket(option));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
