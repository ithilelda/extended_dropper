package top.ithilelda.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/dispenser/DispenserBehavior;dispense(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), method = "dispense")
	private ItemStack dispenseWithToolUsage(DispenserBehavior instance, BlockPointer blockPointer, ItemStack itemStack, Operation<ItemStack> original) {
		// we only perform actions if we have chosen a valid tool.
		ServerWorld world = blockPointer.world();
		Direction direction = blockPointer.state().get(DispenserBlock.FACING);
		BlockPos facingPos = blockPointer.pos().offset(direction);
		BlockState breakingBlockState = world.getBlockState(facingPos);
		if (itemStack.isSuitableFor(breakingBlockState)) {
			Block breakingBlock = breakingBlockState.getBlock();
			// usually we call onBreak, but the player is null, so we do not spawn particles nor trigger piglins, just emit events.
			world.emitGameEvent(GameEvent.BLOCK_DESTROY, facingPos, GameEvent.Emitter.of(null, breakingBlockState));
			boolean bl = world.removeBlock(facingPos, false);
			if (bl) {
				breakingBlock.onBroken(world, facingPos, breakingBlockState);
				Block.dropStacks(breakingBlockState, world, facingPos, world.getBlockEntity(facingPos), null, itemStack);
			}
			boolean toolBroken = itemStack.damage(1, world.random, null);
			if (toolBroken) {
				itemStack.setCount(0);
			}
			return itemStack;
		}
		// otherwise we just continue with the original method.
		else {
			return original.call(instance, blockPointer, itemStack);
		}
	}
}