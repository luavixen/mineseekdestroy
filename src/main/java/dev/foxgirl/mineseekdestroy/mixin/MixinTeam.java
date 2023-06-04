package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Team.class)
public abstract class MixinTeam {

    @Overwrite
    public MutableText decorateName(Text name) {
        var team = (Team) (Object) this;

        var context = Game.getGame().getContext();
        if (context != null) {
            var teamNew = context.getBaseTeam(team);
            if (teamNew != null && teamNew != team) team = teamNew;
        }

        var text = Text.empty()
            .append(team.getPrefix())
            .append(name)
            .append(team.getSuffix());

        var color = team.getColor();
        if (color != Formatting.RESET) {
            text.formatted(color);
        }

        return text;
    }

}
