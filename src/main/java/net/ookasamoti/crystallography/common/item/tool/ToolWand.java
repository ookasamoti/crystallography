package net.ookasamoti.crystallography.common.item.tool;

public class ToolWand extends ToolBase {
    public ToolWand(Properties props, int tier) {
        super(props, tier);
    }

    @Override
    public Kind getKind() { return Kind.WAND; }
}
