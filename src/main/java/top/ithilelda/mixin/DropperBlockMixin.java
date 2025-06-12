package top.ithilelda.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.ithilelda.ExtendedDropper;

import java.util.List;
import java.util.Set;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {
	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/dispenser/DispenserBehavior;dispense(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"), method = "dispense")
	private ItemStack dispenseWithToolUsage(DispenserBehavior instance, BlockPointer blockPointer, ItemStack itemStack, Operation<ItemStack> original) {
		// we disable the vanilla behavior for tools and weapons.
		ServerWorld world = blockPointer.world();
		BlockPos facingPos = blockPointer.pos().offset(blockPointer.state().get(DispenserBlock.FACING));
		BlockState blockState = world.getBlockState(facingPos);
		// if there is a block in front, and we have the suitable mining tool, we try to use the tool to break it.
		if (!blockState.isAir() && itemStack.isSuitableFor(blockState) && !blockState.isIn(BlockTags.SAPLINGS)) {
			int itemDamage = EnchantmentHelper.getItemDamage(world, itemStack, itemStack.get(DataComponentTypes.TOOL).damagePerBlock());
			world.emitGameEvent(GameEvent.BLOCK_DESTROY, facingPos, GameEvent.Emitter.of(null, blockState));
			if (world.removeBlock(facingPos, false)) {
				blockState.getBlock().onBroken(world, facingPos, blockState);
				Block.dropStacks(blockState, world, facingPos, world.getBlockEntity(facingPos), null, itemStack);
			}
			itemStack.damage(itemDamage, world, null, null);
			return itemStack;
		// If we've got a weapon, we try to swing it.
		} else if (itemStack.get(DataComponentTypes.WEAPON) != null) {
			int itemDamage = EnchantmentHelper.getItemDamage(world, itemStack, itemStack.get(DataComponentTypes.WEAPON).itemDamagePerAttack());
			AttributeModifiersComponent attributeModifiers = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
			for(AttributeModifiersComponent.Entry modifier : attributeModifiers.modifiers()) {
				if (modifier.matches(EntityAttributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_MODIFIER_ID)) {
					float baseDamage = (float) modifier.modifier().value();
					List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, new Box(facingPos));
					for (LivingEntity target : entities) {
						DamageSource source = world.getDamageSources().generic();
						float trueDamage = EnchantmentHelper.getDamage(world, itemStack, target, source, baseDamage);
						target.damage(world, source, trueDamage);
						itemStack.damage(itemDamage, world, null, null);
						EnchantmentHelper.onTargetDamaged(world, target, source, itemStack, Item::getName);
					}
					break;
				}
			}
			return itemStack;
		}
		// otherwise we just continue with the original method.
		else {
			return original.call(instance, blockPointer, itemStack);
		}
	}
}