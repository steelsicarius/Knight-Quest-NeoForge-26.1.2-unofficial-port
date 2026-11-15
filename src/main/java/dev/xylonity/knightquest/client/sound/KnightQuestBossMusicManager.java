package dev.xylonity.knightquest.client.sound;

import dev.xylonity.knightquest.KnightQuest;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Iterator;

/**
 * Port of KnightLib 1.21.1's {@code BossMusicManager}: keeps one global looping sound per
 * Netherman, started as soon as the boss exists (phase 1) and restarted whenever the
 * play condition (boss alive and listener within 64 blocks) holds again.
 */
@EventBusSubscriber(modid = KnightQuest.MOD_ID, value = Dist.CLIENT)
public final class KnightQuestBossMusicManager {

    private static final Int2ObjectMap<AbstractTickableSoundInstance> ACTIVE = new Int2ObjectOpenHashMap<>();

    private KnightQuestBossMusicManager() {
        ;;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        ACTIVE.values().removeIf(AbstractTickableSoundInstance::isStopped);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof NethermanEntity netherman)) {
                continue;
            }

            int id = entity.getId();
            if (ACTIVE.containsKey(id)) {
                continue;
            }

            if (!netherman.isAlive() || player.distanceToSqr(netherman) > 64 * 64) {
                continue;
            }

            NethermanBossMusicSound sound = new NethermanBossMusicSound(netherman);
            minecraft.getSoundManager().play(sound);
            ACTIVE.put(id, sound);
        }
    }

    public static void clear() {
        Iterator<AbstractTickableSoundInstance> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            AbstractTickableSoundInstance sound = it.next();
            if (sound instanceof NethermanBossMusicSound bossSound) {
                bossSound.shutdown();
            }
            it.remove();
        }
    }

}
