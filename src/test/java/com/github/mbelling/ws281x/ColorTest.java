package com.github.mbelling.ws281x;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorTest {
    @Test
    public void testBitShifting() {
        Color color = new Color( 64, 128, 192 );

        long longValue = color.getColorBits();

        Color newColor = new Color( longValue );

        assertEquals( 64, newColor.getRed() );
        assertEquals( 128, newColor.getGreen() );
        assertEquals( 192, newColor.getBlue() );
    }
}