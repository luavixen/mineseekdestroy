package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(MessageCommand.class)
public abstract class MixinMessageCommand {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void mineseekdestroy$onExecute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, SignedMessage message, CallbackInfo info) {
        if (!Game.getGame().getRuleBoolean(Game.RULE_MESSAGE_DIRECT_ALLOWED)) {
            source.sendError(Text.of("Direct messages are disabled"));
            info.cancel();
        } else if (Game.getGame().getRuleBoolean(Game.RULE_MESSAGE_DIRECT_BROADCAST)) {
            for (var target : targets) {
                var text = Text
                    .literal("DM ")
                    .append(source.getDisplayName())
                    .append(" → ")
                    .append(target.getDisplayName())
                    .append(": ")
                    .append(message.getContent());
                Game.CONSOLE_OPERATORS.sendInfo(text);
            }
        }
    }

}
