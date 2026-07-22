package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.Config;
import com.ultramega.lastwitness.registry.ModAttachments;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class EchoSpawnHandler {
    private EchoSpawnHandler() {
    }

    public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity livingEntity) || livingEntity.hasData(ModAttachments.CARRIES_ECHO)) {
            return;
        }

        final boolean carriesEcho = event.getLevel().getRandom().nextDouble() < Config.ECHO_SPAWN_CHANCE.get();
        livingEntity.setData(ModAttachments.CARRIES_ECHO, carriesEcho);
    }
}
