package top.ithilelda.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.event.GameEvent;
import top.ithilelda.ExtendedDropper;

import java.util.List;

public class DropperActions {
    public static void BreakBlockWithTool(ServerWorld world, BlockState blockState, BlockPos pos, ItemStack tool) {
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(null, blockState));
        Block breakingBlock = blockState.getBlock();
        if (world.removeBlock(pos, false)) {
            breakingBlock.onBroken(world, pos, blockState);
            Block.dropStacks(blockState, world, pos, world.getBlockEntity(pos), null, tool);
        }
        if (tool.damage(1, world.random, null)) {
            tool.setCount(0);
        }
    }
    public static void AttackEntityWithSword(ServerWorld world, BlockPos targetPos, BlockPos dropperPos, ItemStack sword) {
        List<Entity> entities = world.getOtherEntities(null, new Box(targetPos));
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity target) {
                float swordDamage = sword.isEmpty() ? 0.0f : ((SwordItem)sword.getItem()).getAttackDamage();
                swordDamage += EnchantmentHelper.getAttackDamage(sword, target.getGroup());
                DamageSource source = new DamageSource(world.getDamageSources().registry.entryOf(DamageTypes.GENERIC), dropperPos.toCenterPos());
                target.damage(source, swordDamage);
                if (sword.damage(1, world.random, null)) {
                    sword.setCount(0);
                }
            }
        }
    }
}
