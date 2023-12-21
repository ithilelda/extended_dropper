package top.ithilelda.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.ithilelda.ExtendedDropper;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "drop", at = @At("STORE"), ordinal = 0)
    private int modifyLootingLevel(int i, DamageSource source) {
        // only my dropper will have this DamageSource signature.
        if (source.isOf(DamageTypes.GENERIC) && source.getPosition() != null) {
            LivingEntity entity = (LivingEntity)(Object)this;
            i += getLootingLevel(entity.getWorld(), BlockPos.ofFloored(source.getPosition()));
            ExtendedDropper.LOGGER.info("dropper kill! modified looting level: " + i);
        }
        return i;
    }
    @Unique
    private int getLootingLevel(World world, BlockPos pos) {
        int result = 0;
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DropperBlockEntity dbe) {
                for (int i = 0; i < DropperBlockEntity.INVENTORY_SIZE; i++) {
                    int stackLootingLevel = EnchantmentHelper.getLevel(Enchantments.LOOTING, dbe.getStack(i));
                    if (stackLootingLevel > result) result = stackLootingLevel;
                }
            }
        }
        return result;
    }
}