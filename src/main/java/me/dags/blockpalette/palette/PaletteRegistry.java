package me.dags.blockpalette.palette;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import me.dags.blockpalette.color.ColorF;
import me.dags.blockpalette.color.ColorWheel;
import me.dags.blockpalette.color.Texture;
import me.dags.blockpalette.util.Config;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author dags <dags@dags.me>
 */
@SideOnly(Side.CLIENT)
public class PaletteRegistry {

    private static final IBlockState EMPTY_STATE = Blocks.AIR.getDefaultState();

    private final Map<String, List<Mapping>> textureVariants = new HashMap<>();
    private final Map<String, Mapping> mappings = new HashMap<>();
    private final ColorWheel colorWheel = new ColorWheel();
    private final Set<Block> blacklist;

    public PaletteRegistry() {
        this.blacklist = ImmutableSet.of(
                Blocks.COMMAND_BLOCK,
                Blocks.CHAIN_COMMAND_BLOCK,
                Blocks.REPEATING_COMMAND_BLOCK,
                Blocks.STRUCTURE_BLOCK,
                Blocks.STRUCTURE_VOID
        );
    }

    public ColorF getColor(ItemStack stack, ColorF def) {
        Texture texture = getTextureForStack(stack);
        return texture.isPresent() ? texture.getColor() : def;
    }

    public void buildPalettes() {
        for (Block block : Block.REGISTRY) {
            if (!blacklist.contains(block)) {
                Item item = Item.getItemFromBlock(block);
                if (item != Items.AIR) {
                    NonNullList<ItemStack> list = NonNullList.create();
                    block.getSubBlocks(CreativeTabs.SEARCH, list);
                    for (ItemStack stack : list) {
                        register(stack);
                    }
                }
            }
        }
    }

    public Palette getPalette(ItemStack itemStack) {
        if (itemStack == null || itemStack == ItemStack.EMPTY || Block.getBlockFromItem(itemStack.getItem()) == Blocks.AIR) {
            return Palette.EMPTY;
        }

        if (Config.match_textures) {
            return getVariantPalette(itemStack);
        }

        itemStack = itemStack.copy();
        itemStack.setCount(1);

        colorWheel.setAngle(Config.angle);
        colorWheel.setLeniency(Config.leniency);
        colorWheel.setGrayPoint(Config.gray_point);
        colorWheel.setAlphaPoint(Config.alpha_point);
        colorWheel.refresh();

        switch (Config.color_mode) {
            case COMPLIMENTARY:
                return getComplimentaryPalette(itemStack);
            case TRIAD:
                return getTriadPalette(itemStack);
            case TETRAD:
                return getTetradPalette(itemStack);
            case RAINBOW:
                return getRainbowPalette(itemStack);
            default:
                return getAdjacentPalette(itemStack);
        }
    }

    private Palette getColorPalette(ItemStack stack, List<PaletteItem> list) {
        Texture texture = getTextureForStack(stack);
        ColorF colorF = texture != null ? texture.getColor() : ColorF.EMPTY;
        return Palette.colorPalette(PaletteItem.of(stack, colorF), list);
    }

    public Palette getVariantPalette(ItemStack stack) {
        return getVariantPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getVariantPalette(ItemStack stack, Set<Mapping> filter) {
        List<Mapping> variants = getMatchingTexture(stack);
        List<PaletteItem> entries = statesToVariants(variants, filter);
        return Palette.texturePalette(PaletteItem.of(stack), entries);
    }

    public Palette getAdjacentPalette(ItemStack stack) {
        return getAdjacentPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getAdjacentPalette(ItemStack stack, Set<Mapping> filter) {
        Texture texture = getTextureForStack(stack);
        List<Texture> adjacent = colorWheel.getAdjacent(texture, Config.group_size);
        List<PaletteItem> entries = texturesToVariants(adjacent, filter);
        return getColorPalette(stack, entries);
    }

    public Palette getComplimentaryPalette(ItemStack stack) {
        return getComplimentaryPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getComplimentaryPalette(ItemStack stack, Set<Mapping> filter) {
        Texture texture = getTextureForStack(stack);
        List<Texture> complimentary = colorWheel.getComplimentary(texture, Config.group_size);
        List<PaletteItem> entries = texturesToVariants(complimentary, filter);
        return getColorPalette(stack, entries);
    }

    public Palette getTriadPalette(ItemStack stack) {
        return getTriadPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getTriadPalette(ItemStack stack, Set<Mapping> filter) {
        Texture texture = getTextureForStack(stack);
        List<Texture> triad = colorWheel.getTriad(texture, Config.group_size);
        List<PaletteItem> entries = texturesToVariants(triad, filter);
        return getColorPalette(stack, entries);
    }

    public Palette getTetradPalette(ItemStack stack) {
        return getTetradPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getTetradPalette(ItemStack stack, Set<Mapping> filter) {
        Texture texture = getTextureForStack(stack);
        List<Texture> tetrad = colorWheel.getTetrad(texture, Config.group_size);
        List<PaletteItem> entries = texturesToVariants(tetrad, filter);
        return getColorPalette(stack, entries);
    }

    public Palette getRainbowPalette(ItemStack stack) {
        return getRainbowPalette(stack, Sets.newHashSet(new Mapping(stack)));
    }

    public Palette getRainbowPalette(ItemStack stack, Set<Mapping> filter) {
        Texture texture = getTextureForStack(stack);
        List<Texture> rainbow = colorWheel.getRainbow(texture, Config.group_size);
        List<PaletteItem> entries = texturesToVariants(rainbow, filter);
        return getColorPalette(stack, entries);
    }

    private List<Mapping> getMatchingTexture(ItemStack itemStack) {
        String texture = getItemModel(itemStack).getParticleTexture().getIconName();
        List<Mapping> states = textureVariants.get(texture);
        return states != null ? states : Collections.<Mapping>emptyList();
    }

    private List<PaletteItem> statesToVariants(List<Mapping> variants, Set<Mapping> filter) {
        List<PaletteItem> results = new ArrayList<>();
        for (Mapping variant : variants) {
            if (filter.add(variant)) {
                ItemStack itemStack = variant.itemStack.copy();
                results.add(PaletteItem.of(itemStack, ColorF.EMPTY));
            }
        }
        return results;
    }

    private List<PaletteItem> texturesToVariants(List<Texture> textures, Set<Mapping> filter) {
        List<PaletteItem> results = new ArrayList<>();
        for (Texture texture : textures) {
            Mapping variant = mappings.get(texture.name);
            if (variant != EMPTY_STATE && filter.add(variant)) {
                ItemStack itemStack = variant.itemStack.copy();
                results.add(PaletteItem.of(itemStack, texture.getColor()));
            }
        }
        return results;
    }

    private Texture getTextureForStack(ItemStack itemStack) {
        String icon = getItemModel(itemStack).getParticleTexture().getIconName();
        return colorWheel.getTexture(icon);
    }

    private void register(ItemStack stack) {
        IBakedModel model = getModel(stack);
        if (!isValidModel(model)) {
            writeItemError("Unable to register model for Item: %s, Texture: %s", stack, model);
            return;
        }

        if (!hasParticleTexture(model)) {
            writeItemError("Unable to determine particle texture for Item: %s, Texture: %s", stack, model);
            return;
        }

        String iconName = model.getParticleTexture().getIconName();
        if (iconName == null) {
            writeItemError("Null particle texture!? Item: %s, Texture: %s", stack, model);
            return;
        }

        Mapping mapping = new Mapping(stack);
        Mapping current = mappings.get(iconName);
        if (mapping.overrides(current)) {
            mappings.put(iconName, mapping);
        }

        textureVariants.computeIfAbsent(iconName, k -> new ArrayList<>()).add(mapping);

        if (colorWheel.hasTexture(iconName)) {
            return;
        }

        Texture texture = PaletteRegistry.fromSprite(model.getParticleTexture());
        if (texture != null) {
            colorWheel.addTexture(texture);
        }
    }

    private static IBakedModel getModel(ItemStack itemStack) {
        IBakedModel blockModel = getBlockModel(itemStack);
        IBakedModel itemModel = getItemModel(itemStack);

        if (blockModel == missingModel()) {
            return itemModel;
        }

        if (!blockModel.getParticleTexture().getIconName().equals(itemModel.getParticleTexture().getIconName())) {
            return itemModel;
        }

        return blockModel;
    }

    private static IBakedModel getBlockModel(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemBlock) {
            IBlockState state = ((ItemBlock) itemStack.getItem()).getBlock().getStateFromMeta(itemStack.getMetadata());
            return ensure(Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state), missingModel());
        }
        return missingModel();
    }

    private static IBakedModel getItemModel(ItemStack itemStack) {
        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(itemStack);
        return ensure(model, missingModel());
    }

    private static IBakedModel ensure(IBakedModel model, IBakedModel missing) {
        if (model != null && model.getParticleTexture() != null && model.getParticleTexture().getIconName() != null) {
            return model;
        }
        return missing;
    }

    private static Texture fromSprite(TextureAtlasSprite sprite) {
        if (sprite.getFrameCount() > 0) {
            int[][] data = sprite.getFrameTextureData(0);
            if (data.length > 0) {
                int[] pixels = data[0];
                return new Texture(sprite.getIconName(), sprite.getIconWidth(), sprite.getIconHeight(), pixels);
            }
        }
        return null;
    }

    private static boolean isValidModel(@Nullable IBakedModel model) {
        return model != null && model != Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
    }

    private static boolean hasParticleTexture(@Nonnull IBakedModel model) {
        TextureAtlasSprite particle = model.getParticleTexture();
        return particle != null && !particle.getIconName().equals("missinno");
    }

    private static IBakedModel missingModel() {
        return Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
    }

    private static void writeItemError(String format, ItemStack stack, IBakedModel model) {
        String item;
        if (stack == null || stack.getItem() == null || stack.getItem() == Items.AIR) {
            item = "null";
        } else {
            item = String.format("%s (%s)", toString(stack.getItem().getRegistryName()), stack.getMetadata());
        }

        String modl;
        if (model == null || model.getParticleTexture() == null) {
            modl = "null";
        } else {
            modl = String.format("%s (%s)", toString(model.getParticleTexture().getIconName()), model.getClass());
        }

        System.err.println(String.format(format, item, modl));
    }

    private static String toString(@Nullable Object o) {
        return "" + o;
    }

    private static class Mapping {

        private final ItemStack itemStack;
        private final boolean mainTab;
        private final boolean isFullBlock;
        private final int hashCode;

        private Mapping(ItemStack stack) {
            Block block = Block.getBlockFromItem(stack.getItem());
            this.itemStack = stack;
            this.hashCode = itemStack.getItem().getUnlocalizedName(itemStack).hashCode();
            this.mainTab = itemStack.getItem().getCreativeTab() == CreativeTabs.BUILDING_BLOCKS;
            this.isFullBlock = block != Blocks.AIR && block.getDefaultState().isFullBlock();
        }

        private boolean overrides(Mapping other) {
            return other == null || (!other.mainTab && this.mainTab) || (!other.isFullBlock && this.isFullBlock);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof ItemStack) {
                ItemStack stack = (ItemStack) other;
                return stack.getItem().getUnlocalizedName(stack).hashCode() == hashCode();
            }
            return other.hashCode() == this.hashCode();
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}