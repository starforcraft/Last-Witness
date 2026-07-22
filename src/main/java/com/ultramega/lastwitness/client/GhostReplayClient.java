package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.network.GhostReplayPayload;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostReplayClient {
    private static final String GHOST_TAG = "lastwitness_ghost";
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-1_000_000_000);
    private static final List<ActiveGhost> ACTIVE_GHOSTS = new ArrayList<>();

    private static ClientLevel activeLevel;

    private GhostReplayClient() {
    }

    public static void handlePayload(final GhostReplayPayload payload, final IPayloadContext context) {
        startReplay(payload);
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;

        if (level == null) {
            clearGhosts();
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clearGhosts();
            activeLevel = level;
        }

        final Iterator<ActiveGhost> iterator = ACTIVE_GHOSTS.iterator();
        while (iterator.hasNext()) {
            final ActiveGhost replay = iterator.next();
            if (!replay.tick(level)) {
                replay.remove(level);
                iterator.remove();
            }
        }
    }

    public static void onInteraction(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult entityHit) || !isGhost(entityHit.getEntity())) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private static void startReplay(final GhostReplayPayload payload) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || payload.snapshots().isEmpty()) {
            return;
        }

        final Identifier entityTypeId = Identifier.tryParse(payload.sourceEntityType());
        if (entityTypeId == null) {
            return;
        }

        final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityType.isEmpty()) {
            return;
        }

        final Entity created = entityType.get().create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof LivingEntity ghost)) {
            return;
        }

        ghost.setId(NEXT_ENTITY_ID.getAndDecrement());
        ghost.setUUID(UUID.randomUUID());
        ghost.addTag(GHOST_TAG);
        ghost.setSilent(true);
        ghost.setInvulnerable(true);
        ghost.setInvisible(false);
        ghost.setGlowingTag(true);
        ghost.setCustomNameVisible(false);
        ghost.noPhysics = true;
        ghost.setDeltaMovement(Vec3.ZERO);
        if (ghost instanceof Mob mob) {
            mob.setNoAi(true);
        }

        final List<EntitySnapshot> replayFrames = anchorAtEchoItem(payload);
        final EntitySnapshot firstFrame = replayFrames.getFirst();
        applyInitialFrame(ghost, firstFrame);
        level.addEntity(ghost);

        ACTIVE_GHOSTS.add(new ActiveGhost(ghost, replayFrames));
        activeLevel = level;
    }


    /**
     * Re-centers the historical path so its final frame ends at the echo item.
     * This keeps the manifestation attached to the item even if water or an
     * explosion moved the untouched drop after the source entity died.
     */
    private static List<EntitySnapshot> anchorAtEchoItem(final GhostReplayPayload payload) {
        final EntitySnapshot finalFrame = payload.snapshots().getLast();
        final double offsetX = payload.anchorX() - finalFrame.x();
        final double offsetY = payload.anchorY() - finalFrame.y();
        final double offsetZ = payload.anchorZ() - finalFrame.z();

        return payload.snapshots().stream()
            .map(frame -> new EntitySnapshot(
                frame.gameTime(),
                frame.x() + offsetX,
                frame.y() + offsetY,
                frame.z() + offsetZ,
                frame.yRot(),
                frame.xRot(),
                frame.bodyYRot(),
                frame.headYRot(),
                frame.pose(),
                frame.health(),
                frame.onGround(),
                frame.sprinting(),
                frame.swimming(),
                frame.fallFlying(),
                frame.usingItem()
            ))
            .toList();
    }

    private static void applyInitialFrame(final LivingEntity ghost, final EntitySnapshot frame) {
        ghost.snapTo(frame.x(), frame.y(), frame.z(), frame.yRot(), frame.xRot());
        ghost.setOldPosAndRot(new Vec3(frame.x(), frame.y(), frame.z()), frame.yRot(), frame.xRot());
        applyFrameState(ghost, frame);
    }

    private static void applyFrame(final LivingEntity ghost, final EntitySnapshot frame) {
        ghost.setOldPosAndRot(ghost.position(), ghost.getYRot(), ghost.getXRot());
        ghost.setPos(frame.x(), frame.y(), frame.z());
        ghost.setYRot(frame.yRot());
        ghost.setXRot(frame.xRot());
        applyFrameState(ghost, frame);
    }

    private static void applyFrameState(final LivingEntity ghost, final EntitySnapshot frame) {
        ghost.setYBodyRot(frame.bodyYRot());
        ghost.setYHeadRot(frame.headYRot());
        ghost.setPose(parsePose(frame.pose()));
        ghost.setOnGround(frame.onGround());
        ghost.setSprinting(frame.sprinting());
        ghost.setSwimming(frame.swimming());
        ghost.setDeltaMovement(Vec3.ZERO);
        ghost.noPhysics = true;
        ghost.setInvisible(false);
        ghost.setGlowingTag(true);
    }

    private static Pose parsePose(final String poseName) {
        try {
            return Pose.valueOf(poseName);
        } catch (final IllegalArgumentException ignored) {
            return Pose.STANDING;
        }
    }

    private static boolean isGhost(final Entity entity) {
        return entity.entityTags().contains(GHOST_TAG);
    }

    private static void clearGhosts() {
        if (activeLevel != null) {
            for (final ActiveGhost ghost : ACTIVE_GHOSTS) {
                ghost.remove(activeLevel);
            }
        }
        ACTIVE_GHOSTS.clear();
    }

    private static final class ActiveGhost {
        private final LivingEntity entity;
        private final List<EntitySnapshot> snapshots;
        private int nextFrame;

        private ActiveGhost(final LivingEntity entity, final List<EntitySnapshot> snapshots) {
            this.entity = entity;
            this.snapshots = List.copyOf(snapshots);
            this.nextFrame = 1;
        }

        private boolean tick(final ClientLevel level) {
            if (this.entity.isRemoved() || this.entity.level() != level || this.nextFrame >= this.snapshots.size()) {
                return false;
            }

            applyFrame(this.entity, this.snapshots.get(this.nextFrame));
            this.nextFrame++;
            return true;
        }

        private void remove(final ClientLevel level) {
            if (!this.entity.isRemoved()) {
                level.removeEntity(this.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
