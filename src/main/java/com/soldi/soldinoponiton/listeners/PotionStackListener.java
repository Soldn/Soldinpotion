package com.soldi.soldinoponiton.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PotionStackListener implements Listener {

    private final int maxStack;

    public PotionStackListener(int maxStack) {
        this.maxStack = maxStack;
    }

    private boolean isPotion(Material m) {
        return m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION;
    }

    // 1) Stack ONLY when picking up from the ground
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        ItemStack drop = e.getItem().getItemStack();
        if (drop == null || !isPotion(drop.getType())) return;

        PlayerInventory inv = player.getInventory();
        int remaining = drop.getAmount();

        // Fill existing similar stacks up to maxStack
        for (int slot = 0; slot < inv.getSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null) continue;
            if (!isPotion(stack.getType())) continue;
            if (!stack.isSimilar(drop)) continue;
            int canAdd = Math.min(maxStack - stack.getAmount(), remaining);
            if (canAdd <= 0) continue;
            stack.setAmount(stack.getAmount() + canAdd);
            remaining -= canAdd;
        }

        // Create new stacks in empty slots (up to maxStack each)
        for (int slot = 0; slot < inv.getSize() && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack != null) continue;
            int toPut = Math.min(maxStack, remaining);
            ItemStack clone = drop.clone();
            clone.setAmount(toPut);
            inv.setItem(slot, clone);
            remaining -= toPut;
        }

        // If we handled all items, cancel default pickup and remove the ground item
        if (remaining <= 0) {
            e.setCancelled(true);
            e.getItem().remove();
        } else {
            // Otherwise, update ground item amount and cancel default logic
            e.setCancelled(true);
            drop.setAmount(remaining);
            e.getItem().setItemStack(drop);
        }
    }

    // 2) BLOCK stacking/merging inside inventory & placing into mini-crafting grid
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        Inventory clickedInv = e.getClickedInventory();

        // disallow any potion in player 2x2 crafting grid
        if (clickedInv != null && e.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            if (e.getSlotType() == SlotType.CRAFTING || e.getSlotType() == SlotType.RESULT) {
                if ((current != null && isPotion(current.getType())) || (cursor != null && isPotion(cursor.getType()))) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // prevent double-click "collect to cursor" for potions
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && cursor != null && isPotion(cursor.getType())) {
            e.setCancelled(true);
            return;
        }

        // prevent shift-click auto moves for potions
        if ((e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
             e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) &&
            current != null && isPotion(current.getType())) {
            e.setCancelled(true);
            return;
        }

        // prevent merging two similar potion stacks by manual place
        if (current != null && cursor != null &&
            isPotion(current.getType()) && isPotion(cursor.getType()) &&
            current.isSimilar(cursor)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        ItemStack cursor = e.getOldCursor();
        if (cursor != null && isPotion(cursor.getType())) {
            // Block dragging potions (prevents hidden merges)
            e.setCancelled(true);
        }
    }
}
