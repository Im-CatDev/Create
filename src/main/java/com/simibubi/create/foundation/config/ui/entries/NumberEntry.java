package com.simibubi.create.foundation.config.ui.entries;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.config.ui.ConfigTextField;

import net.createmod.catnip.utility.theme.Theme;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.TextStencilElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class NumberEntry<T extends Number> extends ValueEntry<T> {

	protected int minOffset = 0, maxOffset = 0;
	protected TextStencilElement minText = null, maxText = null;
	protected EditBox textField;

	@Nullable
	public static NumberEntry<? extends Number> create(Object type, String label, ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec spec) {
		if (type instanceof Integer) {
			return new IntegerEntry(label, (ForgeConfigSpec.ConfigValue<Integer>) value, spec);
		} else if (type instanceof Float) {
			return new FloatEntry(label, (ForgeConfigSpec.ConfigValue<Float>) value, spec);
		} else if (type instanceof Double) {
			return new DoubleEntry(label, (ForgeConfigSpec.ConfigValue<Double>) value, spec);
		}

		return null;
	}

	public NumberEntry(String label, ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec) {
		super(label, value, spec);
		textField = new ConfigTextField(Minecraft.getInstance().font, 0, 0, 200, 20);
		if (this instanceof IntegerEntry && annotations.containsKey("IntDisplay")) {
			String intDisplay = annotations.get("IntDisplay");
			int intValue = (Integer) getValue();
			String textValue;
			switch (intDisplay) {
				case "#":
					textValue = "#" + Integer.toHexString(intValue).toUpperCase(Locale.ROOT);
					break;
				case "0x":
					textValue = "0x" + Integer.toHexString(intValue).toUpperCase(Locale.ROOT);
					break;
				case "0b":
					textValue = "0b" + Integer.toBinaryString(intValue);
					break;
				default:
					textValue = String.valueOf(intValue);
			}
			textField.setValue(textValue);
		} else {
			textField.setValue(String.valueOf(getValue()));
		}
		textField.setTextColor(Theme.Key.TEXT.i());

		Object range = spec.getRange();
		try {
			Field minField = range.getClass().getDeclaredField("min");
			Field maxField = range.getClass().getDeclaredField("max");
			minField.setAccessible(true);
			maxField.setAccessible(true);
			T min = (T) minField.get(range);
			T max = (T) maxField.get(range);

			Font font = Minecraft.getInstance().font;
			if (min.doubleValue() > getTypeMin().doubleValue()) {
				TextComponent t = new TextComponent(formatBound(min) + " < ");
				minText = new TextStencilElement(font, t).centered(true, false);
				minText.withElementRenderer((ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0 ,0, height/2, height, width, Theme.Key.TEXT_DARKER.p()));
				minOffset = font.width(t);
			}
			if (max.doubleValue() < getTypeMax().doubleValue()) {
				TextComponent t = new TextComponent(" < " + formatBound(max));
				maxText = new TextStencilElement(font, t).centered(true, false);
				maxText.withElementRenderer((ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0 ,0, height/2, height, width, Theme.Key.TEXT_DARKER.p()));
				maxOffset = font.width(t);
			}
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException | NullPointerException ignored) {

		}

		textField.setResponder(s -> {
			try {
				T number = getParser().apply(s);
				if (!spec.test(number))
					throw new IllegalArgumentException();

				textField.setTextColor(Theme.Key.TEXT.i());
				setValue(number);

			} catch (IllegalArgumentException ignored) {
				textField.setTextColor(Theme.Key.BUTTON_FAIL.i());
			}
		});

		textField.moveCursorToStart();
		listeners.add(textField);
		onReset();
	}

	protected String formatBound(T bound) {
		String sci = String.format("%.2E", bound.doubleValue());
		String str = String.valueOf(bound);
		return sci.length() < str.length() ? sci : str;
	}

	protected abstract T getTypeMin();

	protected abstract T getTypeMax();

	protected abstract Function<String, T> getParser();

	@Override
	protected void setEditable(boolean b) {
		super.setEditable(b);
		textField.setEditable(b);
	}

	@Override
	public void onValueChange(T newValue) {
		super.onValueChange(newValue);

		try {
			T current = getParser().apply(textField.getValue());
			if (!current.equals(newValue)) {
				textField.setValue(String.valueOf(newValue));
			}
		} catch (IllegalArgumentException ignored) {}
	}

	@Override
	public void tick() {
		super.tick();
		textField.tick();
	}

	@Override
	public void render(PoseStack ms, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
		super.render(ms, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);

		textField.x = x + width - 82 - resetWidth;
		textField.y = y + 8;
		textField.setWidth(Math.min(width - getLabelWidth(width) - resetWidth - minOffset - maxOffset, 40));
		textField.setHeight(20);
		textField.render(ms, mouseX, mouseY, partialTicks);

		if (minText != null)
			minText
					.at(textField.x - minOffset, textField.y, 0)
					.withBounds(minOffset, textField.getHeight())
					.render(ms);

		if (maxText != null)
			maxText
					.at(textField.x + textField.getWidth(), textField.y, 0)
					.withBounds(maxOffset, textField.getHeight())
					.render(ms);
	}

	public static class IntegerEntry extends NumberEntry<Integer> {

		public IntegerEntry(String label, ForgeConfigSpec.ConfigValue<Integer> value, ForgeConfigSpec.ValueSpec spec) {
			super(label, value, spec);
		}

		@Override
		protected Integer getTypeMin() {
			return Integer.MIN_VALUE;
		}

		@Override
		protected Integer getTypeMax() {
			return Integer.MAX_VALUE;
		}

		@Override
		protected Function<String, Integer> getParser() {
			return (string) -> {
				if (string.startsWith("#")) {
					return Integer.parseUnsignedInt(string.substring(1), 16);
				} else if (string.startsWith("0x")) {
					return Integer.parseUnsignedInt(string.substring(2), 16);
				} else if (string.startsWith("0b")) {
					return Integer.parseUnsignedInt(string.substring(2), 2);
				} else {
					return Integer.parseInt(string);
				}
			};
		}
	}

	public static class FloatEntry extends NumberEntry<Float> {

		public FloatEntry(String label, ForgeConfigSpec.ConfigValue<Float> value, ForgeConfigSpec.ValueSpec spec) {
			super(label, value, spec);
		}

		@Override
		protected Float getTypeMin() {
			return -Float.MAX_VALUE;
		}

		@Override
		protected Float getTypeMax() {
			return Float.MAX_VALUE;
		}

		@Override
		protected Function<String, Float> getParser() {
			return Float::parseFloat;
		}
	}

	public static class DoubleEntry extends NumberEntry<Double> {

		public DoubleEntry(String label, ForgeConfigSpec.ConfigValue<Double> value, ForgeConfigSpec.ValueSpec spec) {
			super(label, value, spec);
		}

		@Override
		protected Double getTypeMin() {
			return (double) -Float.MAX_VALUE;
		}

		@Override
		protected Double getTypeMax() {
			return (double) Float.MAX_VALUE;
		}

		@Override
		protected Function<String, Double> getParser() {
			return Double::parseDouble;
		}
	}
}
