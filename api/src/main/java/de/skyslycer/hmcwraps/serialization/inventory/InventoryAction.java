package de.skyslycer.hmcwraps.serialization.inventory;

import de.skyslycer.hmcwraps.IHMCWraps;
import de.skyslycer.hmcwraps.messages.Messages;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.entity.Player;

public enum InventoryAction {

    SCROLL_FORTH,
    SCROLL_BACK,
    NEXT_PAGE,
    PREVIOUS_PAGE,
    UNWRAP,
    CLOSE;

    /**
     * Add the specified action to a GuiItem.
     *
     * @param item The GuiItem
     * @param gui The GUI
     * @param action The action to add
     * @param plugin The plugin
     */
    public static void add(GuiItem item, PaginatedGui gui, InventoryAction action, IHMCWraps plugin) {
        switch (action) {
            case SCROLL_FORTH, NEXT_PAGE -> item.setAction(event -> gui.next());
            case SCROLL_BACK, PREVIOUS_PAGE -> item.setAction(event -> gui.previous());
            case CLOSE -> item.setAction(event -> event.getWhoClicked().closeInventory());
            case UNWRAP -> item.setAction(event -> {
                System.out.println("1");
                var wrap = plugin.getWrapper().getWrap(event.getWhoClicked().getInventory().getItemInMainHand());
                event.getWhoClicked().getInventory().setItemInMainHand(plugin.getWrapper().removeWrap(
                        event.getWhoClicked().getInventory().getItemInMainHand(), (Player) event.getWhoClicked(), true));
                System.out.println("2");
                event.getWhoClicked().getOpenInventory().close();
                plugin.getMessageHandler().send(event.getWhoClicked(), Messages.REMOVE_WRAP);
                System.out.println("3");
                if (wrap != null) {
                    System.out.println("4");
                    plugin.getActionHandler().pushUnwrap(wrap, (Player) event.getWhoClicked());
                    System.out.println("5");
                }
            });
        }
    }

}