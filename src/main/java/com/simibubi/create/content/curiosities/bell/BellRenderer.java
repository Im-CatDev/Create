package com.simibubi.create.content.curiosities.bell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import net.createmod.catnip.utility.math.AngleHelper;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

public class BellRenderer<TE extends AbstractBellTileEntity> extends SafeTileEntityRenderer<TE> {

	public BellRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(TE te, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		BlockState state = te.getBlockState();
		Direction facing = state.getValue(BellBlock.FACING);
		BellAttachType attachment = state.getValue(BellBlock.ATTACHMENT);

		SuperByteBuffer bell = CachedBufferer.partial(te.getBellModel(), state);

		if (te.isRinging)
			bell.rotateCentered(te.ringDirection.getCounterClockWise(), getSwingAngle(te.ringingTicks + partialTicks));

		float rY = AngleHelper.horizontalAngle(facing);
		if (attachment == BellAttachType.SINGLE_WALL || attachment == BellAttachType.DOUBLE_WALL)
			rY += 90;
		bell.rotateCentered(Direction.UP, AngleHelper.rad(rY));

		bell.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutout()));
	}

	public static float getSwingAngle(float time) {
		float t = time / 1.5f;
		return 1.2f * Mth.sin(t / (float) Math.PI) / (2.5f + t / 3.0f);
	}

}
