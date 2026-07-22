package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.registry.ModDataComponents;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EchoMarkerActive() implements ConditionalItemModelProperty {
    public static final MapCodec<EchoMarkerActive> MAP_CODEC = MapCodec.unit(new EchoMarkerActive());

    @Override
    public boolean get(final ItemStack stack,
                       @Nullable final ClientLevel level,
                       @Nullable final LivingEntity entity,
                       final int seed,
                       final ItemDisplayContext context) {
        return stack.has(ModDataComponents.ECHO_MARKED.get());
    }

    @Override
    public MapCodec<EchoMarkerActive> type() {
        return MAP_CODEC;
    }
}
