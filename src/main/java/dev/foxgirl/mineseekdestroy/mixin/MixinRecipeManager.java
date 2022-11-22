package dev.foxgirl.mineseekdestroy.mixin;

import com.google.gson.Gson;
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
    private void mineseekdestroy$hookApply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo info) {
        var jsonString = (
            """
            {
              "type": "minecraft:crafting_shaped",
              "key": {
                "E": {
                  "item": "minecraft:egg"
                }
              },
              "pattern": [
                "EE ",
                "EE ",
                "   "
              ],
              "result": {
                "item": "minecraft:bone_block",
                "count": 1
              }
            }
            """
        );
        var jsonValue = new Gson().fromJson(jsonString, JsonElement.class);
        map.put(new Identifier("mineseekdestroy", "egg_block"), jsonValue);
    }

}
