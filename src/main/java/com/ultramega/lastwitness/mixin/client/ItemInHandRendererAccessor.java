package com.ultramega.lastwitness.mixin.client;


import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Accessor("mainHandItem")
    ItemStack lastWitness$getMainHandItem();

    @Accessor("mainHandItem")
    void lastWitness$setMainHandItem(ItemStack stack);

    @Accessor("offHandItem")
    ItemStack lastWitness$getOffHandItem();

    @Accessor("offHandItem")
    void lastWitness$setOffHandItem(ItemStack stack);
}
