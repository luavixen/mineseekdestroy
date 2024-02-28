package dev.foxgirl.mineseekdestroy.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringNbtReader.class)
public abstract class MixinStringNbtReader {

    @Shadow @Final
    private StringReader reader;

    @Unique
    private boolean mineseekdestroy$parseCompoundFlag = false;

    @Inject(method = "parseCompound", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$parseCompound(CallbackInfoReturnable<NbtCompound> info) throws CommandSyntaxException {
        if (!mineseekdestroy$parseCompoundFlag) {
            mineseekdestroy$parseCompoundFlag = true;
            try {
                info.setReturnValue(((StringNbtReader) (Object) this).parseCompound());
            } catch (CommandSyntaxException cause) {
                throw cause;
            } catch (Throwable cause) {
                String message = "StringNbtReader#parseCompound unexpected " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
                Game.LOGGER.warn(message);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, message);
            } finally {
                mineseekdestroy$parseCompoundFlag = false;
            }
        }
    }

    @Unique
    private boolean mineseekdestroy$parseElementFlag = false;

    @Inject(method = "parseElement", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$parseElement(CallbackInfoReturnable<NbtElement> info) throws CommandSyntaxException {
        if (!mineseekdestroy$parseElementFlag) {
            mineseekdestroy$parseElementFlag = true;
            try {
                info.setReturnValue(((StringNbtReader) (Object) this).parseElement());
            } catch (CommandSyntaxException cause) {
                throw cause;
            } catch (Throwable cause) {
                String message = "StringNbtReader#parseElement unexpected " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
                Game.LOGGER.warn(message);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, message);
            } finally {
                mineseekdestroy$parseElementFlag = false;
            }
        }
    }

}
