package com.simibubi.create.content.logistics.block.belts.tunnel;

import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.utility.VecHelper;
import net.minecraft.world.phys.Vec3;

public class BrassTunnelFilterSlot extends ValueBoxTransform.Sided {

	@Override
	protected Vec3 getSouthLocation() {
		return VecHelper.voxelSpace(8, 13, 15.5f);
	}

}
