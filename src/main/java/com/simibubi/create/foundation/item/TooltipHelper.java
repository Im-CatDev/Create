package com.simibubi.create.foundation.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.bridge.game.Language;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.content.contraptions.goggles.GogglesItem;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.item.ItemDescription.Palette;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.utility.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class TooltipHelper {

	public static final Map<String, ItemDescription> cachedTooltips = new HashMap<>();
	public static Language cachedLanguage;
	private static boolean gogglesMode;
	private static final Map<Item, Supplier<String>> tooltipReferrals = new HashMap<>();

	public static MutableComponent holdShift(Palette color, boolean highlighted) {
		return CreateLang.translateDirect("tooltip.holdForDescription", CreateLang.translateDirect("tooltip.keyShift")
			.withStyle(ChatFormatting.GRAY))
			.withStyle(ChatFormatting.DARK_GRAY);
	}

	public static void addHint(List<Component> tooltip, String hintKey, Object... messageParams) {
		Component spacing = IHaveGoggleInformation.componentSpacing;
		tooltip.add(spacing.plainCopy()
			.append(CreateLang.translateDirect(hintKey + ".title"))
			.withStyle(ChatFormatting.GOLD));
		Component hint = CreateLang.translateDirect(hintKey);
		List<Component> cutComponent = FontHelper.cutTextComponent(hint, ChatFormatting.GRAY, ChatFormatting.WHITE);
		for (Component component : cutComponent)
			tooltip.add(spacing.plainCopy()
				.append(component));
	}

	public static void referTo(ItemLike item, Supplier<? extends ItemLike> itemWithTooltip) {
		tooltipReferrals.put(item.asItem(), () -> itemWithTooltip.get()
			.asItem()
			.getDescriptionId());
	}

	public static void referTo(ItemLike item, String string) {
		tooltipReferrals.put(item.asItem(), () -> string);
	}

	//	public static List<ITextComponent> cutTextComponentOld(ITextComponent c, TextFormatting defaultColor,
//		TextFormatting highlightColor, int indent) {
//		IFormattableTextComponent lineStart = StringTextComponent.EMPTY.copy();
//		for (int i = 0; i < indent; i++)
//			lineStart.append(" ");
//		lineStart.formatted(defaultColor);
//
//		List<ITextComponent> lines = new ArrayList<>();
//		String rawText = getUnformattedDeepText(c);
//		String[] words = rawText.split(" ");
//		String word;
//		IFormattableTextComponent currentLine = lineStart.copy();
//
//		boolean firstWord = true;
//		boolean lastWord;
//
//		// Apply hard wrap
//		for (int i = 0; i < words.length; i++) {
//			word = words[i];
//			lastWord = i == words.length - 1;
//
//			if (!lastWord && !firstWord && getComponentLength(currentLine) + word.length() > maxCharsPerLine) {
//				lines.add(currentLine);
//				currentLine = lineStart.copy();
//				firstWord = true;
//			}
//
//			currentLine.append(new StringTextComponent((firstWord ? "" : " ") + word.replace("_", ""))
//				.formatted(word.matches("_([^_]+)_") ? highlightColor : defaultColor));
//			firstWord = false;
//		}
//
//		if (!firstWord) {
//			lines.add(currentLine);
//		}
//
//		return lines;
//	}

	private static void checkLocale() {
		Language currentLanguage = Minecraft.getInstance()
			.getLanguageManager()
			.getSelected();
		if (cachedLanguage != currentLanguage) {
			cachedTooltips.clear();
			cachedLanguage = currentLanguage;
		}
	}

	public static boolean hasTooltip(ItemStack stack, Player player) {
		checkLocale();

		boolean hasGoggles = GogglesItem.isWearingGoggles(player);

		if (hasGoggles != gogglesMode) {
			gogglesMode = hasGoggles;
			cachedTooltips.clear();
		}

		String key = getTooltipTranslationKey(stack);
		if (cachedTooltips.containsKey(key))
			return cachedTooltips.get(key) != ItemDescription.MISSING;
		return findTooltip(stack);
	}

	public static ItemDescription getTooltip(ItemStack stack) {
		checkLocale();
		String key = getTooltipTranslationKey(stack);
		if (cachedTooltips.containsKey(key)) {
			ItemDescription itemDescription = cachedTooltips.get(key);
			if (itemDescription != ItemDescription.MISSING)
				return itemDescription;
		}
		return null;
	}

	private static boolean findTooltip(ItemStack stack) {
		String key = getTooltipTranslationKey(stack);
		if (I18n.exists(key)) {
			cachedTooltips.put(key, buildToolTip(key, stack));
			return true;
		}
		cachedTooltips.put(key, ItemDescription.MISSING);
		return false;
	}

	private static ItemDescription buildToolTip(String translationKey, ItemStack stack) {
		AllSections module = AllSections.of(stack);
		ItemDescription tooltip = new ItemDescription(module.getTooltipPalette());
		String summaryKey = translationKey + ".summary";

		// Summary
		if (I18n.exists(summaryKey))
			tooltip = tooltip.withSummary(new TextComponent(I18n.get(summaryKey)));

		// Requirements
//		if (stack.getItem() instanceof BlockItem) {
//			BlockItem item = (BlockItem) stack.getItem();
//			if (item.getBlock() instanceof IRotate || item.getBlock() instanceof EngineBlock) {
//				tooltip = tooltip.withKineticStats(item.getBlock());
//			}
//		}

		// Behaviours
		for (int i = 1; i < 100; i++) {
			String conditionKey = translationKey + ".condition" + i;
			String behaviourKey = translationKey + ".behaviour" + i;
			if (!I18n.exists(conditionKey))
				break;
			if (i == 1)
				tooltip.getLinesOnShift()
					.add(new TextComponent(""));
			tooltip.withBehaviour(I18n.get(conditionKey), I18n.get(behaviourKey));
		}

		// Controls
		for (int i = 1; i < 100; i++) {
			String controlKey = translationKey + ".control" + i;
			String actionKey = translationKey + ".action" + i;
			if (!I18n.exists(controlKey))
				break;
			tooltip.withControl(I18n.get(controlKey), I18n.get(actionKey));
		}

		return tooltip.createTabs();
	}

	public static String getTooltipTranslationKey(ItemStack stack) {
		Item item = stack.getItem();
		if (tooltipReferrals.containsKey(item))
			return tooltipReferrals.get(item)
				.get() + ".tooltip";
		return item.getDescriptionId(stack) + ".tooltip";
	}

//	private static int getComponentLength(ITextComponent component) {
//		AtomicInteger l = new AtomicInteger();
//		TextProcessing.visitFormatted(component, Style.EMPTY, (s, style, charConsumer) -> {
//			l.getAndIncrement();
//			return true;
//		});
//		return l.get();
//	}

}
