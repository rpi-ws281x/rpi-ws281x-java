package com.github.mbelling.ws281x;

import com.github.mbelling.ws281x.jni.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.mbelling.ws281x.Color.buildColour;

/**
 * Basic class to interface with the rpi_ws281x native library
 */
public class Ws281xLedStrip implements LedStrip {
    private static final Logger LOG = LogManager.getLogger( Ws281xLedStrip.class );
    private static final String LIB_NAME = "ws281x";

    private static final AtomicBoolean loaded = new AtomicBoolean( false );

    // Settings
    private int ledsCount;
    private int gpioPin;
    private int frequencyHz;
    private int dma;
    private int brightness;
    private int pwmChannel;
    private boolean invert;
    private LedStripType stripType;
    private boolean clearOnExit;

    private ws2811_t leds;
    private ws2811_channel_t currentChannel;

    /**
     * Default constructor with the following settings:
     * <ul>
     * <li>ledCount = 100</li>
     * <li>gpioPin = 18</li>
     * <li>frequenchHz = 800000</li>
     * <li>dma = 10</li>
     * <li>brightness = 255</li>
     * <li>pwmChannel = 0</li>
     * <li>invert = false</li>
     * <li>stripType = WS2811_STRIP_RGB</li>
     * </ul>
     *
     * @see rpi_ws281xConstants
     * @see #Ws281xLedStrip(int, int, int, int, int, int, boolean, LedStripType, boolean)
     */
    public Ws281xLedStrip() {
        this(
                100,       // leds
                10,          // Using pin 10 to do SPI, which should allow non-sudo access
                800000,  // freq hz
                10,            // dma
                255,      // brightness
                0,      // pwm channel
                false,        // invert
                LedStripType.WS2811_STRIP_RGB,    // Strip type
                false    // clear on exit
        );
    }

    /**
     * Create an LED strip with the given settings
     *
     * @param ledsCount   The number of LEDs in the strip
     * @param gpioPin     The Raspberry Pi GPIO pin
     * @param frequencyHz Frequency of updates in Hertz
     * @param dma         DMA to use
     * @param brightness  Starting brightness for colors
     * @param pwmChannel  PWM Channel to use
     * @param invert      Whether or not to invert color values
     * @param stripType   The type of LED Strip {@link com.github.mbelling.ws281x.jni.rpi_ws281xConstants}
     * @param clearOnExit Clear LEDs on exit
     */
    public Ws281xLedStrip( int ledsCount, int gpioPin, int frequencyHz, int dma, int brightness, int pwmChannel,
            boolean invert, LedStripType stripType, boolean clearOnExit ) {
        this.ledsCount = ledsCount;
        this.gpioPin = gpioPin;
        this.frequencyHz = frequencyHz;
        this.dma = dma;
        this.brightness = brightness;
        this.pwmChannel = pwmChannel;
        this.invert = invert;
        this.stripType = stripType;
        this.clearOnExit = clearOnExit;

        LOG.info( "LEDs count: {}, GPIO pin: {}, freq hZ: {}, DMA: {}, brightness: {}, pwm channel: {}, invert: {}, "
                        + "strip type: {}, clear on exit: {}",
                ledsCount, gpioPin, frequencyHz, dma, brightness, pwmChannel, invert, stripType, clearOnExit
        );

        init();
    }

    /**
     * Set the color of an individual pixel
     *
     * @param pixel The index of the pixel in the strip
     * @param red The red value (0 - 255)
     * @param green The green value (0 - 255)
     * @param blue The blue value (0 - 255)
     */
    public synchronized void setPixel( int pixel, int red, int green, int blue ) {
        if ( leds != null ) {
            rpi_ws281x.ws2811_led_set( currentChannel, pixel, buildColour( red, green, blue ) );
        }
    }

    /**
     * Set the color of an individual pixel
     *
     * @param pixel The index of the pixel in the strip
     * @param color The color to set on the pixel
     */
    public synchronized void setPixel( int pixel, Color color ) {
        setPixel( pixel, color.getRed(), color.getGreen(), color.getBlue() );
    }

    /**
     * Set all LEDs in the strip to one color
     *
     * @param red The red value (0 - 255)
     * @param green The green value (0 - 255)
     * @param blue The blue value (0 - 255)
     */
    public synchronized void setStrip( int red, int green, int blue ) {
        for ( int i = 0; i < ledsCount; i++ ) {
            setPixel( i, red, green, blue );
        }
    }

    /**
     * Set all LEDs in the strip to one color
     *
     * @param color The color to set on the strip
     */
    public synchronized void setStrip( Color color ) {
        setStrip(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Get the color of a pixel
     *
     * @return The color of the pixel as a long
     */
    public synchronized long getPixel( int pixel ) {
        if ( leds != null ) {
            return rpi_ws281x.ws2811_led_get(currentChannel, pixel);
        }
        return 0;
    }


    /**
     * Render the current values to the physical light strip.
     *
     * This method needs to be called after any previous setPixel calls to make the lights change to those colors.
     */
    public synchronized void render() {
        if ( leds != null ) {
            rpi_ws281x.ws2811_render( leds );
        }
    }

    private static void initializeNative() {
        synchronized ( loaded ) {
            if ( !loaded.get() ) {
                try {
                    Path path = Files.createTempFile( "lib" + LIB_NAME, ".so" );
                    path.toFile().deleteOnExit();
                    Files.copy( Ws281xLedStrip.class.getResourceAsStream( "/lib" + LIB_NAME + ".so" ), path,
                            StandardCopyOption.REPLACE_EXISTING );
                    System.load( path.toString() );
                    loaded.set( true );
                    LOG.info("Native library loaded");
                } catch ( IOException e ) {
                    LOG.error( "Error loading library from classpath: ", e );

                    // Try load the usual way...
                    System.loadLibrary( LIB_NAME );
                    loaded.set( true );
                    LOG.info("Native library loaded");
                }
            }
        }
    }

    private void init() {

        initializeNative();
        Runtime.getRuntime().addShutdownHook( new Thread( Ws281xLedStrip.this::destroy, "WS281x Shutdown Handler" ) );

        // Create ref object for pwm channel (?)
        leds = new ws2811_t();

        // Fetch the channel we'll use
        LOG.debug( "Fetching current channel..." );
        currentChannel = rpi_ws281x.ws2811_channel_get( leds, pwmChannel );

        // Initialize all PWM channels
        LOG.debug( "Initializing all channels..." );

        ws2811_channel_t channel;
        for ( int i = 0; i < 2; i++ ) {
            channel = rpi_ws281x.ws2811_channel_get( leds, i );

            // Set all to zero (default)
            initChannel( channel, 0, 0, 0, false, convertLedTypeToNativeType(stripType) );
        }

        // Initialize current channel
        LOG.debug( "Initializing current channel..." );

        initChannel( currentChannel, ledsCount, gpioPin, brightness, invert, convertLedTypeToNativeType(stripType) );

        // Initialize LED driver/controller
        LOG.debug( "Initializing driver..." );

        leds.setFreq( frequencyHz );
        leds.setDmanum( dma );

        // Attempt to initialize driver
        LOG.debug( "Attempting to initialize driver..." );

        ws2811_return_t result = rpi_ws281x.ws2811_init( leds );

        if ( result.swigValue() != 0 ) {
            throw new RuntimeException( "Failed to setup lights driver, result code: " + result );
        }

        LOG.debug( "Initialization complete" );
    }

    private synchronized void destroy() {
        try {
            if ( leds != null ) {
                if (clearOnExit) {
                    setStrip(0, 0, 0);
                    render();
                }
                rpi_ws281x.ws2811_fini( leds );
                leds.delete();
                leds = null;
                currentChannel = null;
            }
        } catch ( Exception e ) {
            LOG.error( "Failed to dispose Ws281x LED controller", e );
        }
    }

    private void initChannel( ws2811_channel_t channel, int ledsCount, int gpioPin, int brightness, boolean invert, int stripType ) {
        if ( channel != null ) {
            channel.setCount( ledsCount );
            channel.setGpionum( gpioPin );
            channel.setInvert( invert ? 1 : 0 );
            channel.setBrightness( (short) brightness );
            channel.setStrip_type( stripType );
        }
    }

    public void setBrightness( int brightness ) {
        currentChannel.setBrightness( (short) brightness );
    }

    private int convertLedTypeToNativeType(LedStripType stripType) {
        switch (stripType) {
            case SK6812_STRIP_RGBW:
                return rpi_ws281xConstants.SK6812_STRIP_RGBW;
            case SK6812_STRIP_RBGW:
                return rpi_ws281xConstants.SK6812_STRIP_RBGW;
            case SK6812_STRIP_GRBW:
                return rpi_ws281xConstants.SK6812_STRIP_GRBW;
            case SK6812_STRIP_GBRW:
                return rpi_ws281xConstants.SK6812_STRIP_GBRW;
            case SK6812_STRIP_BRGW:
                return rpi_ws281xConstants.SK6812_STRIP_BRGW;
            case SK6812_STRIP_BGRW:
                return rpi_ws281xConstants.SK6812_STRIP_BGRW;
            case WS2811_STRIP_RGB:
                return rpi_ws281xConstants.WS2811_STRIP_RGB;
            case WS2811_STRIP_RBG:
                return rpi_ws281xConstants.WS2811_STRIP_RBG;
            case WS2811_STRIP_GRB:
                return rpi_ws281xConstants.WS2811_STRIP_GRB;
            case WS2811_STRIP_GBR:
                return rpi_ws281xConstants.WS2811_STRIP_GBR;
            case WS2811_STRIP_BRG:
                return rpi_ws281xConstants.WS2811_STRIP_BRG;
            case WS2811_STRIP_BGR:
                return rpi_ws281xConstants.WS2811_STRIP_BGR;
            default:
                throw new IllegalStateException("Unknown LED strip type: " + stripType);
        }
    }

    public int getLedsCount() {
        return ledsCount;
    }

    public int getGpioPin() {
        return gpioPin;
    }

    public int getFrequencyHz() {
        return frequencyHz;
    }

    public int getDma() {
        return dma;
    }

    public int getBrightness() {
        return brightness;
    }

    public int getPwmChannel() {
        return pwmChannel;
    }

    public boolean isInvert() {
        return invert;
    }

    public LedStripType getStripType() {
        return stripType;
    }
}
