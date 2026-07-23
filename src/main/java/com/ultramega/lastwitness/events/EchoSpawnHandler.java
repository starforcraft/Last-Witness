package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.registry.ModAttachments;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class EchoSpawnHandler {
    private EchoSpawnHandler() {
    }

    public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity livingEntity) || livingEntity.hasData(ModAttachments.CARRIES_ECHO)) {
            return;
        }
        // When Server Players join, they load from disk, so skip the check for them
        if (event.loadedFromDisk() && !(livingEntity instanceof Player)) {
            return;
        }

        final boolean carriesEcho = event.getLevel().getRandom().nextDouble() < Config.ECHO_SPAWN_CHANCE.get();
        livingEntity.setData(ModAttachments.CARRIES_ECHO, carriesEcho);
    }
}
