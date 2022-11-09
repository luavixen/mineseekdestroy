package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class StartingGameState extends GameState {

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

    private static void broadcast(GameContext context, Function<ServerPlayerEntity, Packet<?>> provider) {
        for (var player : context.playerManager.getPlayerList()) {
            player.networkHandler.sendPacket(provider.apply(player));
        }
    }
    private static void broadcast(GameContext context, Packet<?> packet) {
        broadcast(context, (player) -> packet);
    }

    private static void broadcastText(GameContext context, Text text) {
        broadcast(context, new OverlayMessageS2CPacket(text));
    }
    private static void broadcastPing(GameContext context) {
        broadcast(context, (player) -> new PlaySoundS2CPacket(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, player.getX(), player.getEyeY(), player.getZ(), 0.18F, 0.45F, 0L));
    }

    private final int ticksPreparing = (int) (Game.getGame().getRuleDouble(Game.RULE_PREPARING_DURATION) * 20.0);
    private final int ticksStarting = (int) (Game.getGame().getRuleDouble(Game.RULE_STARTING_DURATION) * 20.0);
    private final int ticksEffect = (int) (Game.getGame().getRuleDouble(Game.RULE_STARTING_EFFECT_DURATION) * 20.0);

    private int ticks = 0;
    private boolean flag0 = false;
    private boolean flag1 = true;

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        Game.getGame().sendInfo("Round starting...");

        broadcast(context, new TitleFadeS2CPacket(40, 80, 40));
        broadcast(context, new TitleS2CPacket(ScreenTexts.EMPTY));
        broadcast(context, new SubtitleS2CPacket(ScreenTexts.EMPTY));
        broadcastText(context, ScreenTexts.EMPTY);

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        int ticks = this.ticks++;

        if (ticks < ticksPreparing) {
            var time = toTime(ticksStarting);
            var text = formatRoundStarting(time, false);
            broadcastText(context, text);

            return null;
        }

        if (ticks < ticksPreparing + ticksStarting) {
            if (flag1) {
                flag1 = false;

                context.invisibilityService.executeSetEnabled(Game.CONSOLE_OPERATORS);
                context.barrierService.executeBlimpOpen(Game.CONSOLE_OPERATORS);

                var effect = new StatusEffectInstance(StatusEffects.SLOW_FALLING, ticksEffect);

                for (var player : context.getPlayers()) {
                    if (!player.isPlaying() || !player.isAlive()) continue;
                    var entity = player.getEntity();
                    if (entity != null) entity.addStatusEffect(effect);
                }
            }

            if (ticks % 20 == 0) {
                flag0 = !flag0;
                broadcastPing(context);
            }

            var time = toTimeRandomized((ticksPreparing + ticksStarting) - ticks);
            var text = formatRoundStarting(time, flag0);
            broadcastText(context, text);

            return null;
        }

        broadcastText(context, formatRoundStarted());
        broadcastPing(context);

        return new PlayingGameState();
    }

}
