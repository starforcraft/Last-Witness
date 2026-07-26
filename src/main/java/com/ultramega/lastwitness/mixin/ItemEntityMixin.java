package com.ultramega.lastwitness.mixin;

import com.ultramega.lastwitness.events.EchoItemDeletionHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onDestroyed("
        + "Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void lastwitness$removeCompletedEchoWhenDestroyed(final ServerLevel level,
                                                              final DamageSource source,
                                                              final float damage,
                                                              final CallbackInfoReturnable<Boolean> cir) {
        EchoItemDeletionHandler.onItemEntityDestroyed((ItemEntity) (Object) this);
    }
}
