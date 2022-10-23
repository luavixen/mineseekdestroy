package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class PlayingGameState extends GameState {

    @Override
    public void onRespawn(@Nullable GameContext context, ServerPlayerEntity oldPlayerEntity, ServerPlayerEntity newPlayerEntity, boolean alive) {
        if (context == null) return;

        var player = context.getPlayer(newPlayerEntity);
        if (player.isPlaying() && !player.isLiving()) {
            player.teleport(Game.POSITION_BLIMP);
        }
    }

    @Override
    public boolean allowDeath(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return true;

        var player = context.getPlayer(playerEntity);
        if (player.isPlaying() && player.isAlive()) {
            player.setAlive(false);
            player.countDeath();

            if (damageSource.getAttacker() instanceof ServerPlayerEntity playerAttackerEntity) {
                var playerAttacker = context.getPlayer(playerAttackerEntity);
                if (player.getTeam() == GameTeam.PLAYER_BLACK) {
                    playerAttacker.countKill();
                    playerAttacker.countKill();
                } else {
                    playerAttacker.countKill();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return true;
    }

}
