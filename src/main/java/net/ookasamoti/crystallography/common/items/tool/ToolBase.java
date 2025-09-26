package net.ookasamoti.crystallography.common.items.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ookasamoti.crystallography.common.items.crystals.Crystal;

import java.util.*;

public class ToolBase extends Item {
    private final int tier;
    private final List<ToolData> registeredTools;
    private int currentToolIndex;
    private final String toolType;
    private static final List<Item> validItems = new ArrayList<>();

    public ToolBase(Properties properties, String toolType, int tier) {
        super(properties);
        this.toolType = toolType;
        this.tier = tier;
        this.registeredTools = new ArrayList<>();
        this.currentToolIndex = 0;
    }

    public boolean canSetItem(Item item) {
        return validItems.contains(item) || item instanceof Crystal;
    }

    public static void addValidItems(List<Item> items) {
        validItems.addAll(items);
    }

    public boolean addItemToTool(Item item) {
        if (canSetItem(item) && registeredTools.size() < 8) {
            List<Item> crystals = new ArrayList<>();
            crystals.add(item);
            registeredTools.add(new ToolData(toolType, crystals, new HashMap<>()));
            return true;
        }
        return false;
    }

    public boolean registerTool(String toolCategory, List<Item> crystals, Map<Enchantment, Integer> enchantments) {
        if (registeredTools.size() < 12) {
            ToolData toolData = new ToolData(toolCategory, crystals, enchantments);
            registeredTools.add(toolData);
            return true;
        }
        return false;
    }

    public int getTier() { return tier; }

    public ToolData getCurrentTool() {
        if (!registeredTools.isEmpty()) {
            return registeredTools.get(currentToolIndex);
        }
        return null;
    }

    public void cycleTool(boolean forward) {
        if (forward) {
            currentToolIndex = (currentToolIndex + 1) % registeredTools.size();
        } else {
            currentToolIndex = (currentToolIndex - 1 + registeredTools.size()) % registeredTools.size();
        }
    }

    public String getToolType() {
        return toolType;
    }

    public record ToolData(String toolCategory, List<Item> crystals, Map<Enchantment, Integer> enchantments) {
    }
}
