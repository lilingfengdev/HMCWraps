package de.skyslycer.hmcwraps.serialization.inventory;

import de.skyslycer.hmcwraps.serialization.item.SerializableItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class InventoryItem extends SerializableItem implements IInventoryItem {

    private @Nullable HashMap<String, HashMap<String, List<String>>> actions;

    public InventoryItem(String id, String name, @Nullable Boolean glow, @Nullable List<String> lore, @Nullable List<String> flags,
            @Nullable Integer modelId, @Nullable Map<String, Integer> enchantments, @Nullable Integer amount,
            @Nullable  HashMap<String, HashMap<String, List<String>>> actions) {
        super(id, name, glow, lore, flags, modelId, enchantments, amount);
        this.actions = actions;
    }

    public InventoryItem() { }

    @Override
    @Nullable
    public HashMap<String, HashMap<String, List<String>>> getActions() {
        return actions;
    }

}
