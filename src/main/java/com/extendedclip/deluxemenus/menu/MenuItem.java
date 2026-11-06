package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.hooks.ItemHook;
import com.extendedclip.deluxemenus.menu.options.HeadType;
import com.extendedclip.deluxemenus.menu.options.LoreAppendMode;
import com.extendedclip.deluxemenus.menu.options.MenuItemOptions;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.ItemUtils;
import com.extendedclip.deluxemenus.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.extendedclip.deluxemenus.utils.Constants.INVENTORY_ITEM_ACCESSORS;
import static com.extendedclip.deluxemenus.utils.Constants.PLACEHOLDER_PREFIX;
import static com.extendedclip.deluxemenus.utils.Constants.STACK_PREFIX;

public class MenuItem {

    private final DeluxeMenus plugin;
    private final MenuItemOptions options;

    public MenuItem(@NotNull final DeluxeMenus plugin, @NotNull final MenuItemOptions options) {
        this.plugin = plugin;
        this.options = options;
    }

    public static ItemStack base64ToItemStack(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            dataInput.close();
            Object object = dataInput.readObject();
            if (object instanceof ItemStack) {
                return (ItemStack) object;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    public ItemStack getItemStack(@NotNull final MenuHolder holder) {
        final Player viewer = holder.getViewer();

        ItemStack itemStack = null;
        int amount = 1;

        String stringMaterial = this.options.material();
        String lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ROOT);

        if (ItemUtils.isPlaceholderOption(lowercaseStringMaterial)) {
            stringMaterial = holder.setPlaceholdersAndArguments(stringMaterial.substring(PLACEHOLDER_PREFIX.length()));
            lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ENGLISH);
        }
        if (ItemUtils.isItemStackOption(lowercaseStringMaterial)) {
            stringMaterial = holder.setPlaceholdersAndArguments(stringMaterial.substring(STACK_PREFIX.length()));
            ItemStack base64Item = base64ToItemStack(stringMaterial);
            if (base64Item != null) {
                itemStack = base64Item;
                amount = itemStack.getAmount();
                lowercaseStringMaterial = itemStack.getType().toString().toLowerCase(Locale.ENGLISH);
            }
        }


        if (ItemUtils.isPlayerItem(lowercaseStringMaterial)) {
            final ItemStack playerItem = INVENTORY_ITEM_ACCESSORS.get(lowercaseStringMaterial).apply(viewer.getInventory());

            if (playerItem == null || playerItem.getType() == Material.AIR) {
                return new ItemStack(Material.AIR);
            }

            itemStack = playerItem.clone();
            amount = playerItem.getAmount();
        }

        final int temporaryAmount = amount;

        final String finalMaterial = lowercaseStringMaterial;
        final ItemHook pluginHook = plugin.getItemHooks().values()
            .stream()
            .filter(x -> finalMaterial.startsWith(x.getPrefix()))
            .findFirst()
            .orElse(null);

        if (pluginHook != null) {
            itemStack = pluginHook.getItem(
                    viewer,
                    holder.setPlaceholdersAndArguments(stringMaterial.substring(pluginHook.getPrefix().length()))
            );
        }

        if (ItemUtils.isWaterBottle(stringMaterial)) {
            itemStack = ItemUtils.createWaterBottles(amount);
        }

        // The item is neither a water bottle nor plugin hook item
        if (itemStack == null) {
            final Material material = Material.getMaterial(stringMaterial.toUpperCase(Locale.ROOT));
            if (material == null) {
                plugin.debug(
                        DebugLevel.HIGHEST,
                        Level.WARNING,
                        "Material: " + stringMaterial + " is not valid! Setting to Stone."
                );
                itemStack = new ItemStack(Material.STONE, temporaryAmount);
            } else {
                itemStack = new ItemStack(material, temporaryAmount);
            }
        }

        if (ItemUtils.isBanner(itemStack.getType())) {
            final BannerMeta meta = (BannerMeta) itemStack.getItemMeta();
            if (meta != null) {
                if (!this.options.bannerMeta().isEmpty()) {
                    meta.setPatterns(this.options.bannerMeta());
                }
                itemStack.setItemMeta(meta);
            }
        }

        if (ItemUtils.isShield(itemStack.getType())) {
            final BlockStateMeta blockStateMeta = (BlockStateMeta) itemStack.getItemMeta();

            if (blockStateMeta != null) {
                final Banner banner = (Banner) blockStateMeta.getBlockState();
                if (this.options.baseColor().isPresent()) {
                    banner.setBaseColor(this.options.baseColor().get());
                    banner.update();
                    blockStateMeta.setBlockState(banner);
                }
                if (!this.options.bannerMeta().isEmpty()) {
                    banner.setPatterns(this.options.bannerMeta());
                    banner.update();
                    blockStateMeta.setBlockState(banner);
                }

                itemStack.setItemMeta(blockStateMeta);
            }
        }

        if (ItemUtils.hasPotionMeta(itemStack)) {
            final PotionMeta meta = (PotionMeta) itemStack.getItemMeta();

            if (meta != null) {
                if (this.options.rgb().isPresent()) {
                    final Color color = parseRGBColor(holder.setPlaceholdersAndArguments(this.options.rgb().get()));
                    if (color != null) {
                        meta.setColor(color);
                    }
                }

                if (!this.options.potionEffects().isEmpty()) {
                    for (PotionEffect effect : this.options.potionEffects()) {
                        meta.addCustomEffect(effect, true);
                    }
                }

                itemStack.setItemMeta(meta);
            }
        }

        if (itemStack.getType() == Material.AIR) {
            return itemStack;
        }

        if (this.options.damage().isPresent()) {
            final String parsedDamage = holder.setPlaceholdersAndArguments(this.options.damage().get());
            try {
                int damage = Integer.parseInt(parsedDamage);
                if (damage >= 0) {
                    itemStack.setDurability((short) damage);
                }
            } catch (final NumberFormatException exception) {
                plugin.printStacktrace(
                        "Invalid damage found: " + parsedDamage + ".",
                        exception
                );
            }
        }

        if (this.options.amount() != -1) {
            amount = this.options.amount();
        }

        if (this.options.dynamicAmount().isPresent()) {
            try {
                final int dynamicAmount = (int) Double.parseDouble(holder.setPlaceholdersAndArguments(this.options.dynamicAmount().get()));
                amount = Math.max(dynamicAmount, 1);
            } catch (final NumberFormatException ignored) {
            }
        }

        int maxStackSize = itemStack.getMaxStackSize();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }

        itemStack.setAmount(amount);

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        if (this.options.displayName().isPresent()) {
            final String displayName = holder.setPlaceholdersAndArguments(this.options.displayName().get());
            itemMeta.setDisplayName(StringUtils.legacy(StringUtils.colorItemText(displayName, plugin.getGeneralConfig().suppressDefaultItalics())));
        }

        List<Component> lore = new ArrayList<>();
        // Preserve existing lore for modes which combine the item and menu descriptions.
        List<Component> itemLore = itemMeta.hasLore() ? itemMeta.getLore().stream().map(StringUtils::color).toList() : List.of();
        // Ensures backwards compatibility with how hooked items are currently handled
        LoreAppendMode mode = this.options.loreAppendMode().orElse(LoreAppendMode.OVERRIDE);
        if (!this.options.hasLore() && this.options.loreAppendMode().isEmpty()) mode = LoreAppendMode.IGNORE;
        switch (mode) {
            case IGNORE: // DM lore is not added at all
                lore.addAll(itemLore);
                break;
            case TOP: // DM lore is added at the top
                lore.addAll(getMenuItemLore(holder, this.options.lore()));
                lore.addAll(itemLore);
                break;
            case BOTTOM: // DM lore is bottom at the bottom
                lore.addAll(itemLore);
                lore.addAll(getMenuItemLore(holder, this.options.lore()));
                break;
            case OVERRIDE: // Lore from DM overrides the lore from the item
                lore.addAll(getMenuItemLore(holder, this.options.lore()));
                break;
        }

        itemMeta.setLore(lore.stream().map(StringUtils::legacy).toList());

        if (this.options.unbreakable()) {
            itemMeta.setUnbreakable(true);
        }

        if (itemMeta instanceof LeatherArmorMeta && this.options.rgb().isPresent()) {
            final LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemMeta;

            final Color color = parseRGBColor(holder.setPlaceholdersAndArguments(this.options.rgb().get()));
            if (color != null) {
                leatherArmorMeta.setColor(color);
            } else {
                plugin.debug(
                        DebugLevel.HIGHEST,
                        Level.WARNING,
                        "Invalid rgb colors found for leather armor: " + this.options.rgb().get()
                );
            }

            itemStack.setItemMeta(leatherArmorMeta);
        } else if (itemMeta instanceof FireworkEffectMeta && this.options.rgb().isPresent()) {
            final FireworkEffectMeta fireworkEffectMeta = (FireworkEffectMeta) itemMeta;
            final Color color = parseRGBColor(holder.setPlaceholdersAndArguments(this.options.rgb().get()));
            if (color != null) {
                fireworkEffectMeta.setEffect(FireworkEffect.builder().withColor(color).build());
            } else {
                plugin.debug(
                        DebugLevel.HIGHEST,
                        Level.WARNING,
                        "Invalid RGB color found for firework or firework star: " + this.options.rgb().get()
                );
            }
            itemStack.setItemMeta(fireworkEffectMeta);
        } else if (itemMeta instanceof EnchantmentStorageMeta && !this.options.enchantments().isEmpty()) {
            final EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) itemMeta;
            for (final Map.Entry<Enchantment, Integer> entry : this.options.enchantments().entrySet()) {
                final boolean result = enchantmentStorageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                if (!result) {
                    plugin.debug(
                            DebugLevel.HIGHEST,
                            Level.INFO,
                            "Failed to add enchantment " + entry.getKey().getName() + " to item " + itemStack.getType()
                    );
                }
            }
            itemStack.setItemMeta(enchantmentStorageMeta);
        } else {
            itemStack.setItemMeta(itemMeta);
        }

        if (!(itemMeta instanceof EnchantmentStorageMeta) && !this.options.enchantments().isEmpty()) {
            this.options.enchantments().forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
        }

        if (!this.options.itemFlags().isEmpty()) {
            for (final ItemFlag flag : this.options.itemFlags()) {
                itemMeta.addItemFlags(flag);

            }
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Checks if the string is a head item. The check is case-insensitive.
     * Head items are:
     * <ul>
     * <li>"head-{player-name}" (a simple named player head, supports placeholders. eg. "head-%player_name% or head-extendedclip")</li>
     * <li>"texture-{texture-url}" (a head with a custom texture specified by a texture url. eg. "texture-93a728ad8d31486a7f9aad200edb373ea803d1fc5fd4321b2e2a971348234443")</li>
     * <li>"basehead-{base64-encoded-texture-url}" (a head with a custom texture specified by a base64 encoded texture url)</li>
     * <li>"hdb-{hdb-head-id}" (a head with a custom texture specified by a <a href="https://www.spigotmc.org/resources/14280/">HeadDatabase</a> id)</li>
     * </ul>
     *
     * @param material The string to check
     * @return true if the string is a head item, false otherwise
     */
    private boolean isHeadItem(@NotNull final String material) {
        final Optional<HeadType> headType = HeadType.parseHeadType(material);
        headType.ifPresent(this.options::headType);
        return headType.isPresent();
    }

    private @NotNull Optional<ItemStack> getItemFromHook(String hookName, String... args) {
        return plugin.getItemHook(hookName).map(itemHook -> itemHook.getItem(args));
    }

    protected List<Component> getMenuItemLore(@NotNull final MenuHolder holder, @NotNull final List<String> lore) {
        final boolean suppressDefaultItalics = plugin.getGeneralConfig().suppressDefaultItalics();

        return lore.stream()
                .map(holder::setPlaceholdersAndArguments)
                .map(line -> line.split("\n"))
                .flatMap(Arrays::stream)
                .map(line -> line.split("\\\\n"))
                .flatMap(Arrays::stream)
                .map(line -> StringUtils.colorItemText(line, suppressDefaultItalics))
                .collect(Collectors.toList());
    }

    private @Nullable Color parseRGBColor(@NotNull final String input) {
        final Color color = StringUtils.parseRGBColor(input);
        if (color == null) {
            plugin.debug(
                    DebugLevel.HIGHEST,
                    Level.WARNING,
                    "Invalid RGB color found: " + input
            );
        }
        return color;
    }

    public @NotNull MenuItemOptions options() {
        return options;
    }
}
