package com.github.mbelling.ws281x;

public class Color {
    private final int red;
    private final int green;
    private final int blue;

    public Color( long color ) {
        this.red = (short) (color >> 16) & 255;
        this.green = (short) (color >> 8) & 255;
        this.blue = (short) (color) & 255;
    }

    public Color( int red, int green, int blue ) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public long getColorBits() {
        return buildColour(red, green, blue);
    }

    public static long buildColour( int red, int green, int blue ) {
        return ( (short) red << 16 ) | ( (short) green << 8 ) | (short) blue;
    }

    // Predefined colors
    public final static Color WHITE = new Color(255, 255, 255);
    public final static Color LIGHT_GRAY = new Color(192, 192, 192);
    public final static Color GRAY = new Color(128, 128, 128);
    public final static Color DARK_GRAY = new Color(64, 64, 64);
    public final static Color BLACK = new Color(0, 0, 0);
    public final static Color RED = new Color(255, 0, 0);
    public final static Color PINK = new Color(255, 175, 175);
    public final static Color ORANGE = new Color(255, 200, 0);
    public final static Color YELLOW = new Color(255, 255, 0);
    public final static Color GREEN = new Color(0, 255, 0);
    public final static Color MAGENTA = new Color(255, 0, 255);
    public final static Color CYAN = new Color(0, 255, 255);
    public final static Color BLUE = new Color(0, 0, 255);
}
