package com.soldi.soldinoponiton.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.Objects;

public class PotionStackListener implements Listener {

    private final int maxStack;

    public PotionStackListener(int maxStack) {
        this.maxStack = maxStack;
    }

    private boolean isPotion(Material mat) {
        return mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION;
    }

    private boolean samePotion(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (!isPotion(a.getType())) return false;
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        if (!(am instanceof PotionMeta) || !(bm instanceof PotionMeta)) return false;

        PotionMeta ap = (PotionMeta) am;
        PotionMeta bp = (PotionMeta) bm;

        // Compare base potion data
        if (!Objects.equals(ap.getBasePotionData(), bp.getBasePotionData())) return false;
        // Compare custom effects
        if (!Objects.equals(ap.getCustomEffects(), bp.getCustomEffects())) return false;
        // Compare custom name/lore to avoid mixing differently named potions
        if (!Objects.equals(am.getDisplayName(), bm.getDisplayName())) return false;
        if (!Objects.equals(am.getLore(), bm.getLore())) return false;

        return true;
    }

    private int getAmount(ItemStack it) {
        return it == null ? 0 : it.getAmount();
    }

    private int transfer(ItemStack from, ItemStack to) {
        // returns moved amount
        int canMove = Math.min(getAmount(from), Math.max(0, maxStack - getAmount(to)));
        if (canMove <= 0) return 0;
        to.setAmount(getAmount(to) + canMove);
        from.setAmount(getAmount(from) - canMove);
        return canMove;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        // Merge when clicking a potion over another potion of the same kind
        if (cursor != null && current != null && isPotion(cursor.getType()) && samePotion(cursor, current)) {
            if (getAmount(current) >= maxStack) return;
            if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.RIGHT) {
                transfer(cursor, current);
                e.setCancelled(true);
                e.setCurrentItem(current);
                e.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
            }
        }

        // Shift-click merge: move potion into existing stacks in the destination inventory
        if (current != null && isPotion(current.getType()) && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
            Inventory dest = e.getView().getBottomInventory();
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(dest)) {
                dest = e.getView().getTopInventory();
            }
            ItemStack moving = current.clone();
            int toMove = moving.getAmount();
            // Try fill existing stacks first
            for (int i = 0; i < dest.getSize() && toMove > 0; i++) {
                ItemStack slot = dest.getItem(i);
                if (slot != null && samePotion(slot, moving) && slot.getAmount() < maxStack) {
                    int moved = Math.min(toMove, maxStack - slot.getAmount());
                    slot.setAmount(slot.getAmount() + moved);
                    toMove -= moved;
                    dest.setItem(i, slot);
                }
            }
            // Then empty slots
            for (int i = 0; i < dest.getSize() && toMove > 0; i++) {
                ItemStack slot = dest.getItem(i);
                if (slot == null || slot.getType() == Material.AIR) {
                    ItemStack place = moving.clone();
                    place.setAmount(Math.min(maxStack, toMove));
                    dest.setItem(i, place);
                    toMove -= place.getAmount();
                }
            }
            if (toMove != moving.getAmount()) {
                e.setCancelled(true);
                if (toMove <= 0) {
                    e.getClickedInventory().setItem(e.getSlot(), null);
                } else {
                    ItemStack left = moving.clone();
                    left.setAmount(toMove);
                    e.getClickedInventory().setItem(e.getSlot(), left);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        ItemStack cursor = e.getOldCursor();
        if (cursor == null || !isPotion(cursor.getType())) return;
        // Prevent vanilla from splitting into 1s, we'll handle stacking via clicks
        e.setCancelled(true);
    }
}
