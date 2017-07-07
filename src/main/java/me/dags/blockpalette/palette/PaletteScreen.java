package me.dags.blockpalette.palette;

import me.dags.blockpalette.color.ColorF;
import me.dags.blockpalette.color.ColorMode;
import me.dags.blockpalette.creative.PickMode;
import me.dags.blockpalette.gui.Animation;
import me.dags.blockpalette.gui.Hotbar;
import me.dags.blockpalette.gui.Settings;
import me.dags.blockpalette.gui.UI;
import me.dags.blockpalette.util.Config;
import me.dags.blockpalette.util.Pointer;
import me.dags.blockpalette.util.Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dags <dags@dags.me>
 */
public class PaletteScreen extends GuiScreen {

    private static final ResourceLocation BUTTON = new ResourceLocation("blockpalette", "textures/gui/button_mask.png");

    private final Settings paletteSettings = new Settings();
    private final Settings colorSettings = new Settings();
    private final Pointer<Boolean> showSettings = Pointer.of(Config.show_settings);
    private final Pointer<Boolean> matchMode = Pointer.of(Config.match_textures);
    private final Pointer<PickMode> pickMode = Pointer.of(Config.pick_mode);
    private final Pointer<Boolean> holdKey = Pointer.of(Config.hold_key);
    private final Pointer<Boolean> tooltips = Pointer.of(Config.show_tooltips);
    private final Pointer<Integer> highlightRed = Pointer.instant(Config.highlight_red);
    private final Pointer<Integer> highlightGreen = Pointer.instant(Config.highlight_green);
    private final Pointer<Integer> highlightBlue = Pointer.instant(Config.highlight_blue);
    private final Pointer<Float> highlightScale = Pointer.instant(Config.highlight_scale);

    private final Pointer<ColorMode> colorMode = Pointer.of(Config.color_mode);
    private final Pointer<Float> colorOpacity = Pointer.instant(Config.color_opacity);
    private final Pointer<Integer> colorAngle = Pointer.of(Config.angle);
    private final Pointer<Integer> colorGroupSize = Pointer.of(Config.group_size);
    private final Pointer<Float> colorLeniency = Pointer.of(Config.leniency);
    private final Pointer<Float> grayPoint = Pointer.of(Config.gray_point);
    private final Pointer<Float> alphaPoint = Pointer.of(Config.alpha_point);

    private final UI.AreaCycler<Boolean> toggleMode = new UI.AreaCycler<>(matchMode, new Boolean[]{true, false}, BUTTON);
    private final UI.AreaCycler<Boolean> toggleSettings = new UI.AreaCycler<>(showSettings, new Boolean[]{true, false}, BUTTON);
    private final List<GuiButton> buttons = new ArrayList<>();

    private final Hotbar hotbar;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            PaletteScreen.this.main.newPalette(PaletteScreen.this.main.getPalette().getCenter());
            refreshScreen();
        }
    };

    private final PaletteMain main;
    private final Minecraft minecraft;
    private int width = 0;
    private int height = 0;
    private boolean isCreativeOverlay = false;

    public PaletteScreen(PaletteMain main) {
        this.main = main;
        this.hotbar = new Hotbar(main.getPalette().getStackUnderMouse(), main.getPalette().getSelectedStack());

        this.minecraft = Minecraft.getMinecraft();

        this.paletteSettings.add(new UI.Cycler<>(pickMode, PickMode.values(), "palette.control.pickmode"), pickMode);
        this.paletteSettings.add(new UI.Cycler<>(holdKey, new Boolean[]{true, false}, "palette.control.hold"), "palette.tooltip.hold");
        this.paletteSettings.add(new UI.Cycler<>(tooltips, new Boolean[]{true, false}, "palette.control.tooltips"), "palette.tooltip.tooltips");
        this.paletteSettings.add(new UI.Label("palette.control.highlight.color.label", ColorF.rgb(255, 255, 255)));
        this.paletteSettings.add(new UI.IntSlider("palette.control.highlight.red", 0, 255, highlightRed),"palette.tooltip.highlight.color");
        this.paletteSettings.add(new UI.IntSlider("palette.control.highlight.green", 0, 255, highlightGreen), "palette.tooltip.highlight.color");
        this.paletteSettings.add(new UI.IntSlider("palette.control.highlight.blue", 0, 255, highlightBlue), "palette.tooltip.highlight.color");
        this.paletteSettings.add(new UI.Label("palette.control.highlight.scale.label", ColorF.rgb(255, 255, 255)));
        this.paletteSettings.add(new UI.FloatSlider("palette.control.highlight.scale", 1F, 1.5F, highlightScale), "palette.tooltip.highlight.scale");

        this.colorSettings.add(new UI.Label("palette.control.mode.label", 0xFFFFFF));
        this.colorSettings.add(new UI.Cycler<>(colorMode, ColorMode.values(), "%s"));
        this.colorSettings.add(new UI.FloatSlider("palette.control.opacity",0F, 1F, colorOpacity));

        this.colorSettings.add(new UI.Label("palette.control.settings.label", 0xFFFFFF));
        this.colorSettings.add(new UI.IntSlider("palette.control.angle", 0, 120, colorAngle), "palette.tooltip.angle");
        this.colorSettings.add(new UI.IntSlider("palette.control.groups", 1, 5, colorGroupSize), "palette.tooltip.groups");
        this.colorSettings.add(new UI.FloatSlider("palette.control.leniency", 0F, 1F, colorLeniency), "palette.tooltip.leniency");
        this.colorSettings.add(new UI.FloatSlider("palette.control.gray", 0F, 1F, grayPoint), "palette.tooltip.gray");
        this.colorSettings.add(new UI.FloatSlider("palette.control.alpha", 0F, 1F, alphaPoint), "palette.tooltip.alpha");
        this.colorSettings.add(new UI.Label("", 0xFFFFFF));
        this.colorSettings.add(new UI.Button("palette.control.refresh", refresh));

        this.buttons.add(toggleMode);
        this.buttons.add(toggleSettings);
    }

    @Override
    public void initGui() {
        resize();
        init();
        KeyBinding.setKeyBindState(main.show.getKeyCode(), true);
    }

    @Override
    public void onResize(Minecraft minecraft, int w, int h) {
        super.onResize(minecraft, w, h);
        resize();
        setupAnimations();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!listeningToKeyRelease() && !isCreativeOverlay && isMouseBind(mouseButton)) {
            minecraft.setIngameFocus();
            return;
        }

        paletteSettings.mousePressed(minecraft, mouseX, mouseY);
        colorSettings.mousePressed(minecraft, mouseX, mouseY);

        for (GuiButton button : buttons) {
            button.mousePressed(minecraft, mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);

        if (isMouseBind(mouseButton)) {
            if (listeningToKeyRelease()) {
                KeyBinding.setKeyBindState(main.show.getKeyCode(), false);
                return;
            }
        }

        if (mouseButton == 0 && Keyboard.isKeyDown(PaletteMain.switchKeyID) && main.getPalette().getStackUnderMouse().isPresent()) {
            main.newPalette(main.getPalette().getStackUnderMouse().get());
            refresh.run();
            return;
        }

        if (hotbar.mouseRelease(mouseX, mouseY)) {
            return;
        }

        main.getPalette().mouseRelease(mouseX, mouseY, mouseButton);

        paletteSettings.mouseReleased(mouseX, mouseY);
        colorSettings.mouseReleased(mouseX, mouseY);
        main.getPalette().setHighlightColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());
        main.getPalette().setHighlightRadius(highlightScale.get());

        for (GuiButton button : buttons) {
            button.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    public void keyTyped(char key, int code) throws IOException {
        super.keyTyped(key, code);
        hotbar.keyTyped(key, code);

        if (!isCreativeOverlay && code == main.show.getKeyCode()) {
            if (Config.pick_mode == PickMode.MOUSE || !Config.hold_key) {
                minecraft.setIngameFocus();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        hotbar.onClose();
        main.getPalette().close();
        main.newPalette(null);
        Config.save();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        KeyBinding.updateKeyBindState();

       // drawGradientRect(0, 0, this.width, this.height, 0x777777, -804253680);
       // Gui.drawRect(0, 0, width, height, 0x88000000);

        paletteSettings.draw(minecraft, mouseX, mouseY);
        colorSettings.draw(minecraft, mouseX, mouseY);

        hotbar.draw(mouseX, mouseY);
        main.getPalette().drawScreen(mouseX, mouseY);

        for (GuiButton button : buttons) {
            button.drawButton(minecraft, mouseX, mouseY);
        }

        if (Config.show_tooltips) {
            Render.cleanup();
            Render.beginTooltips();
            paletteSettings.drawTooltips(mouseX, mouseY);
            colorSettings.drawTooltips(mouseX, mouseY);
            Render.endTooltips();
        }

        if (listeningToKeyRelease() && keybindReleased()) {
            minecraft.setIngameFocus();
        }
    }

    private boolean listeningToKeyRelease() {
        return !isCreativeOverlay && Config.hold_key;
    }

    private boolean keybindReleased() {
        return !main.show.isKeyDown();
    }

    private boolean isMouseBind(int code) {
        return code - 100 == main.show.getKeyCode();
    }

    public void setCreativeOverlay(boolean overlay) {
        this.isCreativeOverlay = overlay;
    }

    private void refreshScreen() {
        ScaledResolution resolution = new ScaledResolution(minecraft);
        main.getPalette().resize(resolution.getScaledWidth(), resolution.getScaledHeight());
        main.getPalette().setHighlightColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());

        int centerX = resolution.getScaledWidth() / 2;
        int settingsWidth = centerX - main.getPalette().getWidth() - 10;
        paletteSettings.setSize(settingsWidth, height);
        colorSettings.setSize(settingsWidth, height);

        if (Config.show_settings && !paletteSettings.onScreen(width, height)) {
            paletteSettings.open();
        }
        if (!Config.match_textures && !colorSettings.onScreen(width, height)) {
            colorSettings.open();
        }
    }

    private void resize() {
        ScaledResolution resolution = new ScaledResolution(minecraft);
        width = resolution.getScaledWidth();
        height = resolution.getScaledHeight();
        main.getPalette().resize(resolution.getScaledWidth(), resolution.getScaledHeight());
        hotbar.init(width, height);

        int centerX = resolution.getScaledWidth() / 2;
        int centerY = resolution.getScaledHeight() / 2;
        int settingsWidth = centerX - main.getPalette().getWidth() - 10;

        paletteSettings.setSize(settingsWidth, height);
        paletteSettings.setPosition(-paletteSettings.getWidth(), 0);

        colorSettings.setPosition(width, 0);
        colorSettings.setSize(settingsWidth, height);

        toggleSettings.xPosition = centerX - 80 - 12;
        toggleSettings.yPosition = centerY - 78 - 12;
        toggleSettings.width = 24;
        toggleSettings.height = 24;

        toggleMode.xPosition = centerX - 30 - 12;
        toggleMode.yPosition = centerY - 106 - 12;
        toggleMode.width = 24;
        toggleMode.height = 24;

        setupAnimations();
    }

    private void setupAnimations() {
        int time = 10;

        int paletteStartX = -paletteSettings.getWidth();
        int paletteEndX = 0;
        int paletteTop = 0;
        Animation paletteOpen = new Animation(paletteSettings, time, paletteStartX, paletteTop, paletteEndX, paletteTop);
        Animation paletteClose = new Animation(paletteSettings, time, paletteEndX, paletteTop, paletteStartX, paletteTop);
        paletteSettings.setAnimations(paletteOpen, paletteClose);

        int colorStartX = width;
        int colorEndX = width - colorSettings.getWidth();
        int colorTop = 0;
        Animation colorOpen = new Animation(colorSettings, time, colorStartX, colorTop, colorEndX, colorTop);
        Animation colorClose = new Animation(colorSettings, time, colorEndX, colorTop, colorStartX, colorTop);
        colorSettings.setAnimations(colorOpen, colorClose);
    }

    private void init() {
        main.getPalette().setHighlightColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());
        // todo hotbar.setColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());

        if (Config.show_settings && !paletteSettings.onScreen(width, height)) {
            paletteSettings.open();
        }

        if (!Config.match_textures && !colorSettings.onScreen(width, height)) {
            colorSettings.open();
        }

        showSettings.setListener(new Pointer.Listener<Boolean>() {
            @Override
            public void onUpdate(Boolean value) {
                Config.show_settings = value;
                if (value && !paletteSettings.onScreen(width, height)) {
                    paletteSettings.open();
                } else if (!value && paletteSettings.onScreen(width, height)) {
                    paletteSettings.close();
                }
            }
        });

        matchMode.setListener(new Pointer.Listener<Boolean>() {
            @Override
            public void onUpdate(Boolean value) {
                Config.match_textures = value;
                refresh.run();
                if (Config.match_textures) {
                    if (colorSettings.onScreen(width, height)) {
                        colorSettings.close();
                    }
                } else {
                    colorSettings.open();
                }
            }
        });

        pickMode.setListener(new Pointer.Listener<PickMode>() {
            @Override
            public void onUpdate(PickMode value) {
                Config.pick_mode = value;
                if (isCreativeOverlay) {
                    GuiContainerCreative creative = new GuiContainerCreative(minecraft.thePlayer);
                    minecraft.displayGuiScreen(creative);
                }
            }
        });

        holdKey.setListener(new Pointer.Listener<Boolean>() {
            @Override
            public void onUpdate(Boolean value) {
                Config.hold_key = value;
            }
        });

        tooltips.setListener(new Pointer.Listener<Boolean>() {
            @Override
            public void onUpdate(Boolean value) {
                Config.show_tooltips = value;
            }
        });

        Pointer.Listener<Integer> colorListener = new Pointer.Listener<Integer>() {
            @Override
            public void onUpdate(Integer value) {
                main.getPalette().setHighlightColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());
                // todo hotbar.setColor(highlightRed.get(), highlightGreen.get(), highlightBlue.get());
                Config.highlight_red = highlightRed.get();
                Config.highlight_green = highlightGreen.get();
                Config.highlight_blue = highlightBlue.get();
            }
        };

        highlightRed.setListener(colorListener);
        highlightGreen.setListener(colorListener);
        highlightBlue.setListener(colorListener);

        highlightScale.setListener(new Pointer.Listener<Float>() {
            @Override
            public void onUpdate(Float value) {
                main.getPalette().setHighlightRadius(highlightScale.get());
                Config.highlight_scale = value;
            }
        });

        colorOpacity.setListener(new Pointer.Listener<Float>() {
            @Override
            public void onUpdate(Float value) {
                Config.color_opacity = value;
            }
        });

        colorMode.setListener(new Pointer.Listener<ColorMode>() {
            @Override
            public void onUpdate(ColorMode value) {
                Config.color_mode = value;
                refresh.run();
            }
        });

        colorAngle.setListener(new Pointer.Listener<Integer>() {
            @Override
            public void onUpdate(Integer value) {
                Config.angle = value;
                refresh.run();
            }
        });

        colorGroupSize.setListener(new Pointer.Listener<Integer>() {
            @Override
            public void onUpdate(Integer value) {
                Config.group_size = value;
                refresh.run();
            }
        });

        colorLeniency.setListener(new Pointer.Listener<Float>() {
            @Override
            public void onUpdate(Float value) {
                Config.leniency = value;
                refresh.run();
            }
        });

        grayPoint.setListener(new Pointer.Listener<Float>() {
            @Override
            public void onUpdate(Float value) {
                Config.gray_point = value;
                refresh.run();
            }
        });

        alphaPoint.setListener(new Pointer.Listener<Float>() {
            @Override
            public void onUpdate(Float value) {
                Config.alpha_point = value;
                refresh.run();
            }
        });
    }
}