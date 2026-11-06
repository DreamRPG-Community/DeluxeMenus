package com.extendedclip.deluxemenus.dupe.marker.impl;

import com.extendedclip.deluxemenus.dupe.marker.ItemMarker;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Keeps the upstream duplication guard active without the newer persistent-data API. */
public final class LegacyMenuItemMarker implements ItemMarker {
    private final String key;
    private final Method asNmsCopy;
    private final Method asBukkitCopy;
    private final Method getTag;
    private final Method setTag;
    private final Constructor<?> newTag;
    private final Method setBoolean;
    private final Method hasKey;
    private final Method remove;

    public LegacyMenuItemMarker(String key) {
        this.key = key;
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack");
            Class<?> stack = Class.forName("net.minecraft.server.v1_12_R1.ItemStack");
            Class<?> tag = Class.forName("net.minecraft.server.v1_12_R1.NBTTagCompound");
            asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
            asBukkitCopy = craft.getMethod("asBukkitCopy", stack);
            getTag = stack.getMethod("getTag");
            setTag = stack.getMethod("setTag", tag);
            newTag = tag.getConstructor();
            setBoolean = tag.getMethod("setBoolean", String.class, boolean.class);
            hasKey = tag.getMethod("hasKey", String.class);
            remove = tag.getMethod("remove", String.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("The legacy item marker requires Paper 1.12.2", e);
        }
    }

    @Override
    public ItemStack mark(ItemStack item) {
        return update(item, true);
    }

    @Override
    public ItemStack unmark(ItemStack item) {
        return update(item, false);
    }

    private ItemStack update(ItemStack item, boolean mark) {
        try {
            Object stack = asNmsCopy.invoke(null, item);
            Object tag = getTag.invoke(stack);
            if (tag == null && !mark) return item;
            if (tag == null) tag = newTag.newInstance();
            if (mark) setBoolean.invoke(tag, key, true);
            else remove.invoke(tag, key);
            setTag.invoke(stack, tag);
            return (ItemStack) asBukkitCopy.invoke(null, stack);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot update DeluxeMenus item marker", e);
        }
    }

    @Override
    public boolean isMarked(ItemStack item) {
        try {
            Object tag = getTag.invoke(asNmsCopy.invoke(null, item));
            return tag != null && (boolean) hasKey.invoke(tag, key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read DeluxeMenus item marker", e);
        }
    }
}
