package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.util.Broadcast;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

public class StartingGameState extends GameState {

    @Override
    public @NotNull String getName() {
        return "starting";
    }

    private static Text formatRoundStarting(double time, boolean blink) {
        return Text
            .literal("ROUND STARTING IN T-" + new DecimalFormat("00.0000").format(Math.max(time, 0.0)))
            .formatted(blink ? Formatting.YELLOW : Formatting.RED);
    }
    private static Text formatRoundStarted() {
        return Text.literal("ROUND STARTED! KILL!").formatted(Formatting.LIGHT_PURPLE);
    }

    private static double toTime(int ticks) {
        return ((double) ticks) / 20.0;
    }
    private static double toTimeRandomized(int ticks) {
        return ((double) ticks + ThreadLocalRandom.current().nextDouble()) / 20.0;
    }

    private static void broadcast(Packet<?> packet) {
        Broadcast.send(packet);
    }
    private static void broadcastText(Text text) {
        Broadcast.send(new OverlayMessageS2CPacket(text));
    }
    private static void broadcastPing() {
        Broadcast.sendSoundPing();
    }

    private final int ticksPreparing = (int) (Rules.getPreparingDuration() * 20.0);
    private final int ticksStarting = (int) (Rules.getStartingDuration() * 20.0);
    private final int ticksEffect = (int) (Rules.getStartingEffectDuration() * 20.0);

    private int ticks = 0;
    private boolean flagBlink = false;
    private boolean flagReady = true;

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        context.game.sendInfo("Round starting...");

        broadcast(new TitleFadeS2CPacket(40, 80, 40));
        broadcast(new TitleS2CPacket(ScreenTexts.EMPTY));
        broadcast(new SubtitleS2CPacket(ScreenTexts.EMPTY));
        broadcastText(ScreenTexts.EMPTY);

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        int ticks = this.ticks++;

        if (ticks < ticksPreparing) {
            var time = toTime(ticksStarting);
            var text = formatRoundStarting(time, false);
            broadcastText(text);

            return null;
        }

        if (ticks < ticksPreparing + ticksStarting) {
            if (flagReady) {
                flagReady = false;

                context.invisibilityService.executeSetEnabled(Game.CONSOLE_OPERATORS);
                context.barrierService.executeBlimpOpen(Game.CONSOLE_OPERATORS);

                for (var player : context.getPlayersNormal()) {
                    if (player.isSpectator() || !player.isAlive()) continue;
                    var entity = player.getEntity();
                    if (entity != null) {
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, ticksEffect));
                    }
                }
            }

            if (ticks % 20 == 0) {
                flagBlink = !flagBlink;
                broadcastPing();
            }

            var time = toTimeRandomized((ticksPreparing + ticksStarting) - ticks);
            var text = formatRoundStarting(time, flagBlink);
            broadcastText(text);

            return null;
        }

        broadcastText(formatRoundStarted());
        broadcastPing();

        return new PlayingGameState();
    }

}
