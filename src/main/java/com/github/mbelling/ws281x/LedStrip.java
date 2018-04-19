package com.github.mbelling.ws281x;

public interface LedStrip {

    /**
     * Set the color of an individual pixel
     *
     * @param pixel The index of the pixel in the strip
     * @param red The red value (0 - 255)
     * @param green The green value (0 - 255)
     * @param blue The blue value (0 - 255)
     */
    void setPixel( int pixel, int red, int green, int blue );

    /**
     * Set the color of an individual pixel
     *
     * @param pixel The index of the pixel in the strip
     * @param color The color to set on the pixel
     */
    void setPixel( int pixel, Color color );

    /**
     * Set all LEDs in the strip to one color
     *
     * @param red The red value (0 - 255)
     * @param green The green value (0 - 255)
     * @param blue The blue value (0 - 255)
     */
    void setStrip( int red, int green, int blue );

    /**
     * Set all LEDs in the strip to one color
     *
     * @param color The color to set on the strip
     */
    void setStrip( Color color );

    /**
     * Render the current values to the physical light strip.
     *
     * This method needs to be called after any previous setPixel calls to make the lights change to those colors.
     */
    void render();

    /**
     * Set the brightness value from 0-255
     * @param brightness The brightnes to set the strip to
     */
    void setBrightness( int brightness );

    /**
     * Get the current brightness value of the strip
     * @return The current brightness value
     */
    int getBrightness();

    /**
     * Get the number of LEDs in this strip
     * @return The number of LEDs
     */
    int getLedsCount();


}
