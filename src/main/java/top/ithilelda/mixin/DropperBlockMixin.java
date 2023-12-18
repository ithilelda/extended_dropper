package top.ithilelda.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.ithilelda.utils.DropperActions;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/dispenser/DispenserBehavior;dispense(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), method = "dispense")
	private ItemStack dispenseWithToolUsage(DispenserBehavior instance, BlockPointer blockPointer, ItemStack itemStack, Operation<ItemStack> original) {
		// we disable the vanilla behavior for tool items.
		if (itemStack.getItem() instanceof ToolItem) {
			ServerWorld world = blockPointer.getWorld();
			Direction direction = blockPointer.getBlockState().get(DispenserBlock.FACING);
			BlockPos facingPos = blockPointer.getPos().offset(direction);
			BlockState breakingBlockState = world.getBlockState(facingPos);
			// if there is a block in front and we have the suitable mining tool, we try to use the tool to break it.
			if (!breakingBlockState.isAir() && itemStack.getItem() instanceof MiningToolItem && itemStack.isSuitableFor(breakingBlockState)) {
				DropperActions.BreakBlockWithTool(world, breakingBlockState, facingPos, itemStack);
			}
			// If we've got a weapon, we try to swing it.
			else if (itemStack.getItem() instanceof SwordItem) {
				DropperActions.AttackEntityWithSword(world, facingPos, itemStack);
			}
			// otherwise, we do nothing. (note that tool items will never be dropped by dropper now).
			return itemStack;
		}
		// otherwise we just continue with the original method.
		else {
			return original.call(instance, blockPointer, itemStack);
		}
	}
}