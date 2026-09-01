package net.filemaid.util.ui.notification;

public enum Direction {
    CENTER(0, 0, 0.5, 0.5, 0),
    NORTH(0, -1, 0.5, 0.0, 1),
    NORTH_EAST(1, -1, 1.0, 0.0, 2),
    EAST(1, 0, 1.0, 0.5, 3),
    SOUTH_EAST(1, 1, 1.0, 1.0, 4),
    SOUTH(0, 1, 0.5, 1.0, 5),
    SOUTH_WEST(-1, 1, 0.0, 1.0, 6),
    WEST(-1, 0, 0.0, 0.5, 7),
    NORTH_WEST(-1, -1, 0.0, 0.0, 8);

    public final int vx;
    public final int vy;
    public final double ax;
    public final double ay;
    public final int swingConstant;

    private Direction(int n2, int n3, double d, double d2, int n4) {
        this.vx = n2;
        this.vy = n3;
        this.ax = d;
        this.ay = d2;
        this.swingConstant = n4;
    }
}

