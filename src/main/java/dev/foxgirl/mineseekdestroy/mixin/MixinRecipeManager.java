package dev.foxgirl.mineseekdestroy.mixin;

import com.google.gson.JsonElement;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class MixinRecipeManager {

    @Inject(method = "apply", at = @At("HEAD"))
    private void mineseekdestroy$hookApply(Map<Identifier, JsonElement> map, ResourceManager manager, Profiler profiler, CallbackInfo info) {
        map.remove(new Identifier("minecraft", "bone_meal_from_bone_block"));
    }

}
