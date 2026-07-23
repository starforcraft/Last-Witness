package com.ultramega.lastwitness.events;

import com.ultramega.lastwitness.data.EchoOfPastData;
import com.ultramega.lastwitness.registry.ModDataComponents;
import com.ultramega.lastwitness.registry.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.ultramega.lastwitness.LastWitness.MODID;

@EventBusSubscriber(modid = MODID)
public final class EchoExtractionHandler {
    private EchoExtractionHandler() {
    }

    @SubscribeEvent
    public static void onItemEntityTick(final EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)
            || !(itemEntity.level() instanceof ServerLevel serverLevel)
            || !itemEntity.isAlive()) {
            return;
        }

        final ItemStack sourceStack = itemEntity.getItem();
        if (sourceStack.isEmpty() || sourceStack.is(ModItems.ECHO_OF_PAST.get())) {
            return;
        }

        final EchoOfPastData echo = sourceStack.get(ModDataComponents.ECHO_OF_PAST.get());
        if (echo == null || !isTouchingLava(itemEntity, serverLevel)) {
            return;
        }

        final ItemStack extractedEcho = new ItemStack(ModItems.ECHO_OF_PAST.get());
        extractedEcho.set(ModDataComponents.ECHO_OF_PAST.get(), echo);
        itemEntity.setItem(extractedEcho);
    }

    private static boolean isTouchingLava(final ItemEntity itemEntity, final ServerLevel level) {
        return itemEntity.isInLava() || level.getFluidState(itemEntity.blockPosition()).is(FluidTags.LAVA);
    }
}
