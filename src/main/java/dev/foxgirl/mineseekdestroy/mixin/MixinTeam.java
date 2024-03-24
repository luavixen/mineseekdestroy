package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Objects;

@Mixin(Team.class)
public abstract class MixinTeam {

    @Overwrite
    public MutableText decorateName(Text name) {
        var team = (Team) (Object) this;

        var context = Game.getGame().getContext();
        if (context != null) {
            var gameTeam = context.getGameTeam(team);
            if (gameTeam != null && !Objects.equals(gameTeam.getTeamNameDead(), team.getName())) {
                var teamNew = context.getBaseTeam(team);
                if (teamNew != null) team = teamNew;
            }
        }

        return name.copy().formatted(team.getColor());
    }

}
