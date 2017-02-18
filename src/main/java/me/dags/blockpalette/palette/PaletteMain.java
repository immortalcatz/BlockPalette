package me.dags.blockpalette.palette;

import me.dags.blockpalette.gui.Palette;
import me.dags.blockpalette.gui.PaletteScreen;
import me.dags.blockpalette.util.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.io.File;

public class PaletteMain implements IResourceManagerReloadListener {

    public static final int selectKeyID = Keyboard.KEY_LCONTROL;

    public final KeyBinding show = new KeyBinding("key.blockpalette.open", Keyboard.getKeyIndex("C"), "Block Palette");
    private PaletteRegistry registry = new PaletteRegistry(this);
    private Palette palette = Palette.EMPTY;

    public PaletteRegistry getRegistry() {
        return registry;
    }

    public Palette getPalette() {
        return palette;
    }

    public void newPalette(ItemStack itemStack) {
        palette = registry.getPalette0(itemStack);
    }

    public void showPaletteScreen() {
        if (palette.isPresent()) {
            Minecraft.getMinecraft().displayGuiScreen(new PaletteScreen(this));
        }
    }

    public boolean isInventoryKey(int keyCode) {
        return keyCode == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        registry = new PaletteRegistry(this);
        registry.buildPalettes();
        palette = Palette.EMPTY;
    }

    public void onPreInit(File config) {
        Config.init(config);
    }

    public void onInit() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    public void onTick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != null && minecraft.currentScreen == null && Minecraft.isGuiEnabled()) {
            if (show.isPressed()) {
                newPalette(minecraft.thePlayer.getHeldItemMainhand());
                showPaletteScreen();
            }
        }
    }
}
