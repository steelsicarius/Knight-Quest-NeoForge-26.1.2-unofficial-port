package dev.xylonity.knightquest.client.sound;

import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.registry.KnightQuestSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Port of KnightLib 1.21.1's {@code AbstractLoopSound} boss-music behaviour for the Netherman.
 * The track is a global (non-positional, {@code relative} + {@code Attenuation.NONE}) looping
 * RECORDS stream that fades in over 40 ticks and fades out and stops when the boss dies or the
 * listener gets further than 64 blocks, exactly like KnightLib's {@code IBossMusicProvider}
 * defaults ({@code getMusicRangeSqr() == 64 * 64}, {@code getFadeIn() == getFadeOut() == 40}).
 * The manager restarts it as soon as the condition holds again, so the music effectively
 * follows the boss fight instead of staying anchored to the summoning point.
 */
public class NethermanBossMusicSound extends AbstractTickableSoundInstance {

    private static final double MUSIC_RANGE_SQR = 64 * 64;
    private static final int FADE_TICKS = 40;

    private final NethermanEntity boss;
    private int fadeInCounter;
    private int fadeOutCounter;
    private boolean fadingOut;

    public NethermanBossMusicSound(NethermanEntity boss) {
        super(KnightQuestSounds.THE_ARCHITECT_OF_CHAOS.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.boss = boss;
        this.looping = true;
        this.relative = true;
        this.volume = 0F;
        this.pitch = 1F;
        this.attenuation = Attenuation.NONE;
        this.fadeInCounter = FADE_TICKS;
        this.fadeOutCounter = FADE_TICKS;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    /**
     * Public stop handle for the manager ({@link #stop()} is protected in 26.1.2;
     * KnightLib reached it through a mixin accessor).
     */
    public void shutdown() {
        this.stop();
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean shouldPlay = this.boss.isAlive() && player.distanceToSqr(this.boss) <= MUSIC_RANGE_SQR;

        if (!shouldPlay && !this.fadingOut) {
            this.fadingOut = true;
            this.fadeOutCounter = FADE_TICKS;
        }

        if (this.fadingOut) {
            this.volume = (float) this.fadeOutCounter / FADE_TICKS;
            if (--this.fadeOutCounter <= 0) {
                this.stop();
            }
        } else if (this.fadeInCounter > 0) {
            this.volume = 1F - (float) this.fadeInCounter-- / FADE_TICKS;
        } else {
            this.volume = 1F;
        }
    }

}
