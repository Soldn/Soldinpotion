package com.soldi.soldinoponiton.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class AnvilCombineListener implements Listener {

    private final Supplier<Boolean> combineEnabled;
    private final IntSupplier costSupplier;

    private static final Map<PotionType, PotionEffectType> TYPE_MAP = new EnumMap<>(PotionType.class);
    static {
        TYPE_MAP.put(PotionType.SPEED, PotionEffectType.SPEED);
        TYPE_MAP.put(PotionType.SLOWNESS, PotionEffectType.SLOW);
        TYPE_MAP.put(PotionType.STRENGTH, PotionEffectType.INCREASE_DAMAGE);
        TYPE_MAP.put(PotionType.INSTANT_HEAL, PotionEffectType.HEAL);
        TYPE_MAP.put(PotionType.INSTANT_DAMAGE, PotionEffectType.HARM);
        TYPE_MAP.put(PotionType.JUMP, PotionEffectType.JUMP);
        TYPE_MAP.put(PotionType.REGEN, PotionEffectType.REGENERATION);
        TYPE_MAP.put(PotionType.FIRE_RESISTANCE, PotionEffectType.FIRE_RESISTANCE);
        TYPE_MAP.put(PotionType.WATER_BREATHING, PotionEffectType.WATER_BREATHING);
        TYPE_MAP.put(PotionType.INVISIBILITY, PotionEffectType.INVISIBILITY);
        TYPE_MAP.put(PotionType.NIGHT_VISION, PotionEffectType.NIGHT_VISION);
        TYPE_MAP.put(PotionType.POISON, PotionEffectType.POISON);
        TYPE_MAP.put(PotionType.WEAKNESS, PotionEffectType.WEAKNESS);
        TYPE_MAP.put(PotionType.LUCK, PotionEffectType.LUCK);
        TYPE_MAP.put(PotionType.TURTLE_MASTER, PotionEffectType.DAMAGE_RESISTANCE);
        TYPE_MAP.put(PotionType.SLOW_FALLING, PotionEffectType.SLOW_FALLING);
    }

    public AnvilCombineListener(Supplier<Boolean> combineEnabled, IntSupplier costSupplier) {
        this.combineEnabled = combineEnabled;
        this.costSupplier = costSupplier;
    }

    private boolean isPotion(ItemStack i) {
        if (i == null) return false;
        Material t = i.getType();
        return t == Material.POTION || t == Material.SPLASH_POTION || t == Material.LINGERING_POTION;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        if (!combineEnabled.get()) return;

        AnvilInventory inv = e.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (!isPotion(left) || !isPotion(right)) return;
        if (!(left.getItemMeta() instanceof PotionMeta) || !(right.getItemMeta() instanceof PotionMeta)) return;

        PotionMeta lm = (PotionMeta) left.getItemMeta();
        PotionMeta rm = (PotionMeta) right.getItemMeta();

        PotionData ld = lm.getBasePotionData();
        PotionData rd = rm.getBasePotionData();
        if (ld == null || rd == null) return;
        if (!ld.getType().equals(rd.getType())) return;

        // Both must be level II (upgraded) and not extended
        if (!(ld.isUpgraded() && rd.isUpgraded()) || ld.isExtended() || rd.isExtended()) return;

        PotionEffectType baseType = TYPE_MAP.get(ld.getType());
        if (baseType == null) return; // unsupported

        // Create result: one potion with amplifier 2 (III level)
        ItemStack result = new ItemStack(left.getType(), 1);
        PotionMeta out = (PotionMeta) result.getItemMeta();
        if (out == null) return;

        // Keep same base visual type (I), but add custom effect amplifier 2
        out.setBasePotionData(new PotionData(ld.getType(), false, false));

        int durationTicks = 3600; // 3 minutes default
        for (PotionEffect pe : lm.getCustomEffects()) {
            if (pe.getType().equals(baseType)) {
                durationTicks = Math.max(durationTicks, pe.getDuration());
            }
        }

        out.addCustomEffect(new PotionEffect(baseType, durationTicks, 2, false, true, true), true);
        out.setDisplayName("§d" + prettify(ld.getType().name()) + " III");

        result.setItemMeta(out);
        e.setResult(result);
        inv.setRepairCost(costSupplier.getAsInt());
    }

    private String prettify(String name) {
        String lower = name.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
