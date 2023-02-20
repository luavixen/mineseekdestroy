package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.Entity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeamMsgCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TeamMsgCommand.class)
public abstract class MixinTeamMsgCommand {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mineseekdestroy$onExecute(ServerCommandSource source, Entity entity, Team team, List<ServerPlayerEntity> recipients, SignedMessage message, CallbackInfo info) {
        if (!Game.getGame().getRuleBoolean(Game.RULE_MESSAGE_TEAM_ALLOWED)) {
            source.sendError(Text.of("Team messages are disabled"));
            info.cancel();
        } else if (Game.getGame().getRuleBoolean(Game.RULE_MESSAGE_TEAM_BROADCAST)) {
            var text = Text
                .literal("TM ")
                .append(source.getDisplayName())
                .append(" → ")
                .append(team.getDisplayName().copy().formatted(team.getColor()))
                .append(": ")
                .append(message.getContent());
            Game.CONSOLE_OPERATORS.sendInfo(text);
        }
    }

}
