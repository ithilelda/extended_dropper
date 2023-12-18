package top.ithilelda.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.event.GameEvent;
import top.ithilelda.ExtendedDropper;

import java.util.List;

public class DropperActions {
    public static boolean BreakBlockWithTool(ServerWorld world, BlockState blockState, BlockPos pos, ItemStack tool) {
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(null, blockState));
        Block breakingBlock = blockState.getBlock();
        boolean result = world.removeBlock(pos, false);
        if (result) {
            breakingBlock.onBroken(world, pos, blockState);
            Block.dropStacks(blockState, world, pos, world.getBlockEntity(pos), null, tool);
        }
        boolean toolBroken = tool.damage(1, world.random, null);
        if (toolBroken) {
            tool.setCount(0);
        }
        return result;
    }
    public static void AttackEntityWithTool(ServerWorld world, BlockPos pos, ItemStack tool) {
        List<Entity> entities = world.getOtherEntities(null, new Box(pos));
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity target) {
                ExtendedDropper.LOGGER.info(tool.toString());
                float toolDamage = tool.isEmpty() ? 0.0f : 3.0f + ((ToolItem)tool.getItem()).getMaterial().getAttackDamage();
                toolDamage += EnchantmentHelper.getAttackDamage(tool, target.getGroup());
                target.damage(world.getDamageSources().generic(), toolDamage);
                int amount = tool.getItem() instanceof MiningToolItem ? 2 : 1;
                boolean toolBroken = tool.damage(amount, world.random, null);
                if (toolBroken) {
                    tool.setCount(0);
                }
            }
        }
    }
}
