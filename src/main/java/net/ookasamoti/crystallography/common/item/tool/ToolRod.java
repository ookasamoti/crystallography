package net.ookasamoti.crystallography.common.item.tool;

public class ToolRod extends ToolBase {
    public ToolRod(Properties props, int tier) {
        super(props, tier);
    }

    @Override
    public Kind getKind() { return Kind.ROD; }
}
