package net.oxcodsnet.roadarchitect.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge builds the mod list via Stream#toList(), which is immutable on Java 21.
 * Sorting inside ModListScreen then fails, so we copy the stream result to a mutable list.
 */
@Pseudo
@Mixin(targets = "net.neoforged.neoforge.client.gui.ModListScreen", priority = 1100)
public abstract class ModListScreenMixin {
    @Shadow
    private List<?> mods;

    @Inject(method = "reloadMods", at = @At("RETURN"))
    private void roadarchitect$makeModsMutable(CallbackInfo ci) {
        if (mods == null || mods instanceof ArrayList<?>) {
            return;
        }
        mods = new ArrayList<>(mods);
    }
}
