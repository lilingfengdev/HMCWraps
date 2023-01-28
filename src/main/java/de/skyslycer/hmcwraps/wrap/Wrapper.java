package de.skyslycer.hmcwraps.wrap;

import de.skyslycer.hmcwraps.HMCWraps;
import de.skyslycer.hmcwraps.events.ItemUnwrapEvent;
import de.skyslycer.hmcwraps.events.ItemWrapEvent;
import de.skyslycer.hmcwraps.serialization.IWrap;
import de.skyslycer.hmcwraps.serialization.IWrapValues;
import de.skyslycer.hmcwraps.serialization.wrap.Wrap.WrapValues;
import de.skyslycer.hmcwraps.util.PlayerUtil;
import de.skyslycer.hmcwraps.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.UUID;

public class Wrapper implements IWrapper {

    private final HMCWraps plugin;

    private final NamespacedKey physicalKey;
    private final NamespacedKey wrapIdKey;
    private final NamespacedKey playerKey;
    private final NamespacedKey physicalUnwrapperKey;
    private final NamespacedKey physicalWrapperKey;
    private final NamespacedKey originalModelIdKey;
    private final NamespacedKey originalColorKey;

    public Wrapper(HMCWraps plugin) {
        this.plugin = plugin;
        physicalKey = new NamespacedKey(plugin, "wrap-physical");
        wrapIdKey = new NamespacedKey(plugin, "wrap-id");
        playerKey = new NamespacedKey(plugin, "wrap-player");
        physicalUnwrapperKey = new NamespacedKey(plugin, "unwrapper");
        physicalWrapperKey = new NamespacedKey(plugin, "wrapper");
        originalModelIdKey = new NamespacedKey(plugin, "original-model-id");
        originalColorKey = new NamespacedKey(plugin, "original-color");
    }

    @Override
    public IWrap getWrap(ItemStack item) {
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        var data = container.get(wrapIdKey, PersistentDataType.STRING);
        if (data == null || data.equals("-")) {
            return null;
        }
        return plugin.getWraps().get(data);
    }

    @Override
    public ItemStack setWrap(@Nullable IWrap wrap, ItemStack item, boolean physical, Player player, boolean giveBack) {
        var event = new ItemWrapEvent(wrap, item, physical, player, giveBack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return item;
        }
        return setWrapPrivate(event.getWrap(), event.getItem(), event.isPhysical(), event.getPlayer(), event.isGiveBack());
    }

    private ItemStack setWrapPrivate(@Nullable IWrap wrap, ItemStack item, boolean physical, Player player, boolean giveBack) {
        var editing = item.clone();
        var currentWrap = getWrap(editing);
        if (isPhysical(editing) && currentWrap != null && currentWrap.getPhysical().isPresent() && currentWrap.getPhysical().get().isKeepAfterUnwrap()
                && giveBack) {
            PlayerUtil.give(player, setPhysicalWrapper(currentWrap.getPhysical().get().toItem(plugin, player), currentWrap));
        }
        var originalData = getOriginalData(item);
        var meta = editing.getItemMeta();
        var originalModelId = -1;
        Color originalColor = null;
        if (meta.hasCustomModelData()) {
            originalModelId = meta.getCustomModelData();
        }
        meta.getPersistentDataContainer().set(wrapIdKey, PersistentDataType.STRING, wrap == null ? "-" : wrap.getUuid());
        meta.setCustomModelData(wrap == null ? originalData.modelId() : wrap.getModelId());
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            originalColor = leatherMeta.getColor();
            leatherMeta.setColor(wrap == null ? originalData.color() : wrap.getColor());
            editing.setItemMeta(leatherMeta);
        } else {
            editing.setItemMeta(meta);
        }
        editing = setPhysical(editing.clone(), physical);
        if (wrap == null || currentWrap != null) {
            return editing;
        }
        return setOriginalData(editing, new WrapValues(originalModelId, originalColor));
    }

    @Override
    public ItemStack removeWrap(ItemStack target, Player player, boolean giveBack) {
        var event = new ItemUnwrapEvent(target, player, getWrap(target), giveBack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return target;
        }
        return removeWrapPrivate(event.getItem(), event.getPlayer(), event.isGiveBack());
    }

    private ItemStack removeWrapPrivate(ItemStack item, Player player, boolean giveBack) {
        var currentWrap = getWrap(item);
        if (currentWrap == null) {
            return item;
        }
        return setWrapPrivate(null, item, false, player, giveBack);
    }

    @Override
    public ItemStack setPhysicalUnwrapper(ItemStack item) {
        var editing = item.clone();
        var meta = editing.getItemMeta();
        meta.getPersistentDataContainer().set(physicalUnwrapperKey, PersistentDataType.BYTE, (byte) 1);
        editing.setItemMeta(meta);
        return editing;
    }

    @Override
    public boolean isPhysicalUnwrapper(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(physicalUnwrapperKey, PersistentDataType.BYTE);
    }

    @Override
    public ItemStack setPhysicalWrapper(ItemStack item, IWrap wrap) {
        var editing = item.clone();
        var meta = editing.getItemMeta();
        meta.getPersistentDataContainer().set(physicalWrapperKey, PersistentDataType.STRING, wrap.getUuid());
        editing.setItemMeta(meta);
        return editing;
    }

    @Override
    public String getPhysicalWrapper(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(physicalWrapperKey, PersistentDataType.STRING);
    }

    @Override
    public IWrapValues getOriginalData(ItemStack item) {
        int modelData = -1;
        try {
            modelData = getOriginalModelId(item);
        } catch (Exception ignored) {
            Bukkit.getLogger().warning("Failed to get original model data for " + item.getType() + "! Data may not be a number.");
        }
        Color color = null;
        try {
            color = getOriginalColor(item);
        } catch (Exception ignored) {
            Bukkit.getLogger().warning("Failed to get original color for " + item.getType() + "! Data may not be a correct color format.");
        }
        return new WrapValues(modelData, color);
    }

    private int getOriginalModelId(ItemStack item) {
        var meta = item.getItemMeta();
        var modelData = -1;
        var modelDataSettings = plugin.getConfiguration().getPreservation().getModelId();
        if (modelDataSettings.isOriginalEnabled()) {
            var data = meta.getPersistentDataContainer().get(originalModelIdKey, PersistentDataType.INTEGER);
            if (data != null) {
                modelData = data;
            }
        } else if (modelDataSettings.isDefaultEnabled()) {
            var map = modelDataSettings.getDefaults();
            if (map.containsKey(item.getType().toString())) {
                modelData = Integer.parseInt(map.get(item.getType().toString()));
            }
            for (String key : map.keySet()) {
                if (plugin.getCollectionHelper().getMaterials(key).contains(item.getType())) {
                    modelData = Integer.parseInt(map.get(key));
                }
            }
        }
        return modelData;
    }

    private Color getOriginalColor(ItemStack item) {
        var colorSettings = plugin.getConfiguration().getPreservation().getColor();
        Color color = null;
        var meta = item.getItemMeta();
        if (colorSettings.isOriginalEnabled()) {
            var data = meta.getPersistentDataContainer().get(originalColorKey, PersistentDataType.INTEGER);
            if (data != null) {
                color = Color.fromRGB(data);
            }
        } else if (colorSettings.isDefaultEnabled()) {
            var map = colorSettings.getDefaults();
            if (map.containsKey(item.getType().toString())) {
                color = StringUtil.colorFromString(map.get(item.getType().toString()));
            }
            for (String key : map.keySet()) {
                if (plugin.getCollectionHelper().getMaterials(key).contains(item.getType())) {
                    color = StringUtil.colorFromString(map.get(key));
                }
            }
        }
        return color;
    }

    @Override
    public ItemStack setOriginalData(ItemStack item, IWrapValues wrapValues) {
        var editing = item.clone();
        var meta = editing.getItemMeta();
        meta.getPersistentDataContainer().set(originalModelIdKey, PersistentDataType.INTEGER, wrapValues.modelId());
        if (wrapValues.color() != null) {
            meta.getPersistentDataContainer().set(originalColorKey, PersistentDataType.INTEGER, wrapValues.color().asRGB());
        }
        editing.setItemMeta(meta);
        return editing;
    }

    @Override
    public UUID getOwningPlayer(ItemStack item) {
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        var data = container.get(playerKey, PersistentDataType.STRING);
        if (data == null) {
            return null;
        }
        return UUID.fromString(data);
    }

    @Override
    public ItemStack setOwningPlayer(ItemStack item, UUID uuid) {
        var editing = item.clone();
        var meta = editing.getItemMeta();
        meta.getPersistentDataContainer().set(playerKey, PersistentDataType.STRING, uuid.toString());
        editing.setItemMeta(meta);
        return editing;
    }

    @Override
    public boolean isOwningPlayer(ItemStack item, Player player) {
        var uuid = getOwningPlayer(item);
        if (uuid == null) {
            return false;
        }
        return player.getUniqueId().equals(uuid);
    }

    @Override
    public boolean isPhysical(ItemStack item) {
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        var data = container.get(physicalKey, PersistentDataType.BYTE);
        if (data == null) {
            return false;
        }
        return data.intValue() > 0;
    }

    @Override
    public ItemStack setPhysical(ItemStack item, boolean physical) {
        var editing = item.clone();
        var meta = editing.getItemMeta();
        meta.getPersistentDataContainer().set(physicalKey, PersistentDataType.BYTE, physical ? (byte) 1 : (byte) 0);
        editing.setItemMeta(meta);
        return editing;
    }

    @Override
    public boolean isValidModelId(ItemStack item, IWrap wrap) {
        var modelData = -1;
        if (getWrap(item) != null) {
            modelData = getOriginalModelId(item);
        } else if (item.getItemMeta().hasCustomModelData()) {
            modelData = item.getItemMeta().getCustomModelData();
        }
        return (wrap.getModelIdExclude() == null || !wrap.getModelIdExclude().contains(modelData)) && (wrap.getModelIdInclude() == null || wrap.getModelIdInclude().contains(modelData));
    }

}
