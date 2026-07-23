package com.ultramega.lastwitness.client;

import com.ultramega.lastwitness.network.ReplayPayload;
import com.ultramega.lastwitness.tracking.EntityReplayEvent;
import com.ultramega.lastwitness.tracking.EntitySnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.lastwitness.LastWitness.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class GhostReplayClient {
    private static final String GHOST_TAG = "lastwitness_ghost";
    private static final int END_HOLD_TICKS = 20;
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-1_000_000_000);
    private static final List<ActiveReplay> ACTIVE_GHOSTS = new ArrayList<>();
    private static ActiveReplay activeFirstPerson;
    private static EntitySnapshot localPlayerBeforeHud;

    private static ClientLevel activeLevel;

    private GhostReplayClient() {
    }

    public static void handlePayload(final ReplayPayload payload, final IPayloadContext context) {
        startReplay(payload.sourceEntityType(), payload.snapshots(), payload.entityEvents(), payload.firstPerson());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        restoreHudState();

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            clearReplays();
            activeLevel = null;
            return;
        }

        if (activeLevel != level) {
            clearReplays();
            activeLevel = level;
        }

        final Iterator<ActiveReplay> iterator = ACTIVE_GHOSTS.iterator();
        while (iterator.hasNext()) {
            final ActiveReplay replay = iterator.next();
            if (!replay.tick(level)) {
                replay.remove(level);
                iterator.remove();
            }
        }

        if (activeFirstPerson != null && !activeFirstPerson.tick(level)) {
            activeFirstPerson.remove(level);
            activeFirstPerson = null;
        }
    }

    @SubscribeEvent
    public static void onInteraction(final InputEvent.InteractionKeyMappingTriggered event) {
        if (activeFirstPerson != null) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult entityHit) || !isGhost(entityHit.getEntity())) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(final RenderGuiEvent.Pre event) {
        restoreHudState();

        final Minecraft minecraft = Minecraft.getInstance();
        if (activeFirstPerson == null
            || minecraft.player == null
            || !(activeFirstPerson.entity() instanceof Player)) {
            return;
        }

        localPlayerBeforeHud = EntitySnapshot.capture(minecraft.player);
        activeFirstPerson.hudSnapshot().loadInto(minecraft.player);
    }

    @SubscribeEvent
    public static void onRenderGuiPost(final RenderGuiEvent.Post event) {
        restoreHudState();
    }

    @SubscribeEvent
    public static void onRenderFramePost(final RenderFrameEvent.Post event) {
        restoreHudState();
    }

    private static void restoreHudState() {
        if (localPlayerBeforeHud == null) {
            return;
        }

        final EntitySnapshot snapshot = localPlayerBeforeHud;
        localPlayerBeforeHud = null;
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            snapshot.loadInto(player);
        }
    }

    private static void startReplay(final String sourceEntityType,
                                    final List<EntitySnapshot> snapshots,
                                    final List<EntityReplayEvent> entityEvents,
                                    final boolean firstPerson) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || snapshots.isEmpty()) {
            return;
        }

        final Identifier entityTypeId = Identifier.tryParse(sourceEntityType);
        if (entityTypeId == null) {
            return;
        }

        final Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityType.isEmpty()) {
            return;
        }

        final LivingEntity replayEntity = createReplayEntity(level, entityType.get());
        if (replayEntity == null) {
            return;
        }

        if (firstPerson && activeFirstPerson != null) {
            activeFirstPerson.remove(level);
            activeFirstPerson = null;
        }

        final int replayId = NEXT_ENTITY_ID.getAndDecrement();
        final UUID replayUuid = UUID.randomUUID();
        final List<ReplayFrame> replayFrames = buildReplayFrames(replayEntity, snapshots);
        applyInitialFrame(replayEntity, replayFrames.getFirst(), replayId, replayUuid, !firstPerson);

        final ActiveReplay replay = new ActiveReplay(
            replayEntity,
            replayFrames,
            entityEvents,
            replayId,
            replayUuid,
            !firstPerson,
            firstPerson ? minecraft.getCameraEntity() : null,
            firstPerson ? minecraft.options.getCameraType() : null
        );
        level.addEntity(replayEntity);
        replay.replayEventsThrough(0L);
        if (firstPerson) {
            activeFirstPerson = replay;
            replay.enforceCamera();
        } else {
            ACTIVE_GHOSTS.add(replay);
        }
        activeLevel = level;
    }

    private static LivingEntity createReplayEntity(final ClientLevel level, final EntityType<?> entityType) {
        if (entityType == EntityType.PLAYER) {
            return new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "Echo"));
        }

        final Entity created = entityType.create(level, EntitySpawnReason.COMMAND);
        return created instanceof LivingEntity living ? living : null;
    }

    private static List<ReplayFrame> buildReplayFrames(final LivingEntity replayEntity, final List<EntitySnapshot> snapshots) {
        final List<ReplayFrame> frames = new ArrayList<>(snapshots.size());
        for (final EntitySnapshot snapshot : snapshots) {
            snapshot.loadInto(replayEntity);
            frames.add(new ReplayFrame(snapshot, replayEntity.position()));
        }
        return List.copyOf(frames);
    }

    private static void applyInitialFrame(final LivingEntity replayEntity,
                                          final ReplayFrame frame,
                                          final int replayId,
                                          final UUID replayUuid,
                                          final boolean externalGhost) {
        frame.snapshot().loadInto(replayEntity);
        moveToPosition(replayEntity, frame.position());
        applyReplayState(replayEntity, replayId, replayUuid, externalGhost);
        replayEntity.setOldPosAndRot(replayEntity.position(), replayEntity.getYRot(), replayEntity.getXRot());
    }

    private static void applySnapshotState(final LivingEntity replayEntity,
                                           final ReplayFrame frame,
                                           final int replayId,
                                           final UUID replayUuid,
                                           final boolean externalGhost) {
        frame.snapshot().loadInto(replayEntity);
        applyReplayState(replayEntity, replayId, replayUuid, externalGhost);
    }

    private static void moveToPosition(final LivingEntity replayEntity, final Vec3 position) {
        replayEntity.snapTo(position.x(), position.y(), position.z(), replayEntity.getYRot(), replayEntity.getXRot());
    }

    private static Vec3 interpolatePosition(final ReplayFrame from,
                                            final ReplayFrame to,
                                            final long fromOffset,
                                            final long toOffset,
                                            final long elapsedTicks) {
        final long duration = Math.max(1L, toOffset - fromOffset);
        final double progress = Math.clamp((double) (elapsedTicks - fromOffset) / duration, 0.0D, 1.0D);
        return from.position().lerp(to.position(), progress);
    }

    private static void applyReplayState(final LivingEntity replayEntity, final int replayId, final UUID replayUuid, final boolean externalGhost) {
        replayEntity.setId(replayId);
        replayEntity.setUUID(replayUuid);
        if (externalGhost) {
            replayEntity.addTag(GHOST_TAG);
        } else {
            replayEntity.removeTag(GHOST_TAG);
        }
        replayEntity.setSilent(true);
        replayEntity.setInvulnerable(true);
        if (!replayEntity.isAlive()) {
            replayEntity.setHealth(1.0F);
        }
        replayEntity.setInvisible(false);
        replayEntity.setGlowingTag(externalGhost);
        replayEntity.setCustomNameVisible(false);
        replayEntity.setNoGravity(true);
        replayEntity.noPhysics = true;
        replayEntity.setDeltaMovement(Vec3.ZERO);
        if (replayEntity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static boolean isGhost(final Entity entity) {
        return entity.entityTags().contains(GHOST_TAG);
    }

    private static void clearReplays() {
        restoreHudState();
        if (activeLevel != null) {
            for (final ActiveReplay ghost : ACTIVE_GHOSTS) {
                ghost.remove(activeLevel);
            }
            if (activeFirstPerson != null) {
                activeFirstPerson.remove(activeLevel);
            }
        }
        ACTIVE_GHOSTS.clear();
        activeFirstPerson = null;
    }

    private record ReplayFrame(EntitySnapshot snapshot, Vec3 position) {
    }

    private static final class ActiveReplay {
        private final LivingEntity entity;
        private final List<ReplayFrame> frames;
        private final List<EntityReplayEvent> entityEvents;
        private final int replayId;
        private final UUID replayUuid;
        private final boolean externalGhost;
        private final Entity previousCameraEntity;
        private final CameraType previousCameraType;
        private final long firstGameTime;
        private int nextFrame;
        private int nextEntityEvent;
        private long elapsedTicks;
        private int endHoldTicks;

        private ActiveReplay(final LivingEntity entity,
                             final List<ReplayFrame> frames,
                             final List<EntityReplayEvent> entityEvents,
                             final int replayId,
                             final UUID replayUuid,
                             final boolean externalGhost,
                             final Entity previousCameraEntity,
                             final CameraType previousCameraType) {
            this.entity = entity;
            this.frames = List.copyOf(frames);
            this.entityEvents = List.copyOf(entityEvents);
            this.replayId = replayId;
            this.replayUuid = replayUuid;
            this.externalGhost = externalGhost;
            this.previousCameraEntity = previousCameraEntity;
            this.previousCameraType = previousCameraType;
            this.firstGameTime = this.frames.getFirst().snapshot().gameTime();
            this.nextFrame = 1;
            this.elapsedTicks = 1L;
        }

        private LivingEntity entity() {
            return this.entity;
        }

        private EntitySnapshot hudSnapshot() {
            return this.frames.get(Math.max(0, this.nextFrame - 1)).snapshot();
        }

        private boolean tick(final ClientLevel level) {
            if (this.entity.isRemoved() || this.entity.level() != level) {
                return false;
            }

            final Vec3 previousPosition = this.entity.position();
            final float previousYRot = this.entity.getYRot();
            final float previousXRot = this.entity.getXRot();

            while (this.nextFrame < this.frames.size() && this.frameOffset(this.nextFrame) <= this.elapsedTicks) {
                applySnapshotState(this.entity, this.frames.get(this.nextFrame), this.replayId, this.replayUuid, this.externalGhost);
                this.nextFrame++;
            }

            final ReplayFrame currentFrame = this.frames.get(this.nextFrame - 1);
            final Vec3 targetPosition;
            if (this.nextFrame < this.frames.size()) {
                targetPosition = interpolatePosition(
                    currentFrame,
                    this.frames.get(this.nextFrame),
                    this.frameOffset(this.nextFrame - 1),
                    this.frameOffset(this.nextFrame),
                    this.elapsedTicks
                );
            } else {
                targetPosition = currentFrame.position();
            }

            moveToPosition(this.entity, targetPosition);
            this.entity.setOldPosAndRot(previousPosition, previousYRot, previousXRot);
            applyReplayState(this.entity, this.replayId, this.replayUuid, this.externalGhost);
            this.replayEventsThrough(this.elapsedTicks);
            this.entity.calculateEntityAnimation(this.entity instanceof FlyingAnimal);
            this.enforceCamera();

            if (this.nextFrame < this.frames.size() || this.nextEntityEvent < this.entityEvents.size()) {
                this.elapsedTicks++;
                return true;
            }

            return this.endHoldTicks++ < END_HOLD_TICKS;
        }

        private void enforceCamera() {
            if (this.externalGhost) {
                return;
            }

            final Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            if (minecraft.getCameraEntity() != this.entity) {
                minecraft.setCameraEntity(this.entity);
            }
        }

        private void replayEventsThrough(final long elapsedTicks) {
            while (this.nextEntityEvent < this.entityEvents.size() && this.entityEventOffset(this.nextEntityEvent) <= elapsedTicks) {
                this.entity.handleEntityEvent(this.entityEvents.get(this.nextEntityEvent).eventByte());
                this.nextEntityEvent++;
            }
        }

        private long frameOffset(final int index) {
            return Math.max(0L, this.frames.get(index).snapshot().gameTime() - this.firstGameTime);
        }

        private long entityEventOffset(final int index) {
            return Math.max(0L, this.entityEvents.get(index).gameTime() - this.firstGameTime);
        }

        private void remove(final ClientLevel level) {
            if (!this.externalGhost) {
                restoreHudState();
                final Minecraft minecraft = Minecraft.getInstance();
                if (this.previousCameraType != null) {
                    minecraft.options.setCameraType(this.previousCameraType);
                }

                final Entity fallback = minecraft.player;
                final Entity camera = this.previousCameraEntity != null
                    && !this.previousCameraEntity.isRemoved()
                    && this.previousCameraEntity.level() == minecraft.level
                    ? this.previousCameraEntity
                    : fallback;
                minecraft.setCameraEntity(camera);
            }

            if (!this.entity.isRemoved()) {
                level.removeEntity(this.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
