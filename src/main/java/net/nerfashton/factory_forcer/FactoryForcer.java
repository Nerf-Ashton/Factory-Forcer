package net.nerfashton.factory_forcer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(FactoryForcer.MOD_ID)
public class FactoryForcer {
    public static final String MOD_ID = "factory_forcer";

    public FactoryForcer(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, FactoryForcerConfig.SPEC);
    }

    @EventBusSubscriber
    public static class FactoryForcerEventHandler {
        public static final TagKey<Block> INDOOR_ONLY = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MOD_ID, "indoor_only"));
        public static final TagKey<Block> NATURE_BLOCKS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MOD_ID, "natural"));

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            BlockPos pos = event.getPos();

            Level level = player.level();
            if (level.isClientSide()) {
                return;
            }

            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                return;
            }

            if (event.getPlacedBlock().is(INDOOR_ONLY)) {
                BlockPos belowPos = pos.below();
                BlockState belowState = level.getBlockState(belowPos);

                if (belowState.is(NATURE_BLOCKS) && FactoryForcerConfig.REQUIRE_FOUNDATION.get()) {
                    cancelPlacement(event, player, pos, level, "message.factory_forcer.foundation_warning");
                    return;
                }

                int roofHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                if (pos.getY() >= roofHeight - 1) {
                    cancelPlacement(event, player, pos, level, "message.factory_forcer.outdoor_warning");
                }
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
                return;
            }

            BlockPos brokenPos = event.getPos();

            int roofHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, brokenPos.getX(), brokenPos.getZ());

            if (brokenPos.getY() < roofHeight - 1) {
                return;
            }

            for (int y = brokenPos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                BlockPos checkPos = new BlockPos(brokenPos.getX(), y, brokenPos.getZ());
                BlockState state = level.getBlockState(checkPos);

                if (state.isAir()) {
                    continue;
                }

                if (state.is(INDOOR_ONLY)) {
                    level.destroyBlock(checkPos, true);
                    System.out.println("Factory Forcer: block broken at: " + checkPos.getX() + " " + checkPos.getY() + " " + checkPos.getZ());

                    continue;
                }

                if (state.blocksMotion()) {
                    break;
                }
            }
        }
    }

    private static void cancelPlacement(BlockEvent.EntityPlaceEvent event, Player player, BlockPos pos, Level level, String message) {
        event.setCanceled(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        player.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem().copy());
        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(message), true);
    }
}
