package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Team.class)
public abstract class MixinTeam {

    @Inject(
        method = "decorateName(Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;",
        at = @At("HEAD"), cancellable = true
    )
    private void mineseekdestroy$hookDecorateName(Text name, CallbackInfoReturnable<MutableText> info) {
        var context = Game.getGame().getContext();
        if (context != null) {
            Team teamOld = (Team) (Object) this;
            Team teamNew;

            if (teamOld == context.teamDuelDamaged) teamNew = context.teamDuel;
            else if (teamOld == context.teamWardenDamaged) teamNew = context.teamWarden;
            else if (teamOld == context.teamBlackDamaged) teamNew = context.teamBlack;
            else if (teamOld == context.teamYellowDamaged) teamNew = context.teamYellow;
            else if (teamOld == context.teamBlueDamaged) teamNew = context.teamBlue;
            else return;

            info.setReturnValue(teamNew.decorateName(name));
        }
    }

}
