package com.nstut.buildinggadgetsextra.cut;

import com.direwolf20.buildinggadgets.common.blocks.EffectBlock;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.TileSupport;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.CutAnimationPolicy;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Adapts BG2's queued render-block cut to the native 1.16 EffectBlock.
 * EffectBlock already renders a captured state shrinking to air over 20 ticks.
 */
@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyCutScheduler {
    private static final int EFFECT_LIFETIME_TICKS = 20;
    private static final int DROP_GUARD_MARGIN_TICKS = 5;
    private static final List<CutJob> JOBS = new ArrayList<>();
    private static final List<DropGuard> DROP_GUARDS = new ArrayList<>();

    private LegacyCutScheduler() {
    }

    public static void schedule(ServerWorld world, Region region, List<BlockPos> positions) {
        List<BlockPos> ordered = new ArrayList<>(positions);
        ordered.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());

        Deque<CutBlock> blocks = new ArrayDeque<>(ordered.size());
        for (BlockPos pos : ordered) {
            blocks.addLast(new CutBlock(pos.immutable(), TileSupport.createBlockData(world, pos)));
        }

        DropGuard guard = new DropGuard(world, region,
                world.getGameTime() + EFFECT_LIFETIME_TICKS + DROP_GUARD_MARGIN_TICKS);
        DROP_GUARDS.add(guard);
        JOBS.add(new CutJob(world, blocks, CutAnimationPolicy.blocksPerTick(blocks.size()), guard));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<CutJob> jobs = JOBS.iterator();
        while (jobs.hasNext()) {
            CutJob job = jobs.next();
            for (int i = 0; i < job.amountPerTick && !job.blocks.isEmpty(); i++) {
                CutBlock cut = job.blocks.removeFirst();
                // BG2 explicitly removes block entities before installing its
                // temporary render block, preventing containers from spilling.
                job.world.removeBlockEntity(cut.pos);
                EffectBlock.spawnEffectBlock(job.world, cut.pos, cut.data,
                        EffectBlock.Mode.REMOVE, false);
            }
            if (job.blocks.isEmpty()) {
                job.guard.expiresAt = job.world.getGameTime()
                        + EFFECT_LIFETIME_TICKS + DROP_GUARD_MARGIN_TICKS;
                jobs.remove();
            }
        }

        DROP_GUARDS.removeIf(guard -> guard.world.getGameTime() > guard.expiresAt);
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof ItemEntity) || event.getWorld().isClientSide()) return;

        ItemEntity item = (ItemEntity) event.getEntity();
        // Player-thrown items carry a thrower/owner and must remain untouched.
        // Vanilla block/container drops have neither.
        if (item.getThrower() != null || item.getOwner() != null) return;
        BlockPos itemPos = event.getEntity().blockPosition();
        for (DropGuard guard : DROP_GUARDS) {
            if (guard.world == event.getWorld() && guard.region.contains(itemPos)) {
                // Dependency updates (torch support, door halves, inventories,
                // etc.) are part of the cut and must never duplicate as drops.
                event.setCanceled(true);
                return;
            }
        }
    }

    private static final class CutBlock {
        private final BlockPos pos;
        private final BlockData data;

        private CutBlock(BlockPos pos, BlockData data) {
            this.pos = pos;
            this.data = data;
        }
    }

    private static final class CutJob {
        private final ServerWorld world;
        private final Deque<CutBlock> blocks;
        private final int amountPerTick;
        private final DropGuard guard;

        private CutJob(ServerWorld world, Deque<CutBlock> blocks, int amountPerTick, DropGuard guard) {
            this.world = world;
            this.blocks = blocks;
            this.amountPerTick = amountPerTick;
            this.guard = guard;
        }
    }

    private static final class DropGuard {
        private final ServerWorld world;
        private final Region region;
        private long expiresAt;

        private DropGuard(ServerWorld world, Region region, long expiresAt) {
            this.world = world;
            this.region = region;
            this.expiresAt = expiresAt;
        }
    }
}
