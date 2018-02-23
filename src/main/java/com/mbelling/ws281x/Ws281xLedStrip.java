package com.mbelling.ws281x;

import com.mbelling.ws281x.jni.rpi_ws281x;
import com.mbelling.ws281x.jni.ws2811_channel_t;
import com.mbelling.ws281x.jni.ws2811_return_t;
import com.mbelling.ws281x.jni.ws2811_t;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mbelling.ws281x.jni.rpi_ws281xConstants;
import static com.mbelling.ws281x.jni.rpi_ws281xConstants.WS2811_STRIP_RGB;

/**
 * Basic class to interface with the rpi_ws281x native library
 */
public class Ws281xLedStrip {
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
    private int stripType;

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
     * @see #Ws281xLedStrip(int, int, int, int, int, int, boolean, int)
     */
    public Ws281xLedStrip() {
        // Default settings
        // TODO: move to constants file
        this(
                100,         // leds
                18,         // pin
                800000,     // freq hz
                10,          // dma
                255,        // brightness
                0,          // pwm channel
                false,       // invert
                WS2811_STRIP_RGB  // Strip type
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
     * @param stripType   The type of LED Strip {@link com.mbelling.ws281x.jni.rpi_ws281xConstants}
     */
    public Ws281xLedStrip( int ledsCount, int gpioPin, int frequencyHz, int dma, int brightness, int pwmChannel, boolean invert, int stripType ) {
        this.ledsCount = ledsCount;
        this.gpioPin = gpioPin;
        this.frequencyHz = frequencyHz;
        this.dma = dma;
        this.brightness = brightness;
        this.pwmChannel = pwmChannel;
        this.invert = invert;
        this.stripType = stripType;

        LOG.info( "LEDs count: {}, GPIO pin: {}, freq hZ: {}, DMA: {}, brightness: {}, pwm channel: {}, invert: {}",
                ledsCount, gpioPin, frequencyHz, dma, brightness, pwmChannel, invert
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
                } catch ( IOException e ) {
                    System.out.println( "Error loading library from classpath: " + e );
                    e.printStackTrace();

                    // Try load the usual way...
                    System.loadLibrary( LIB_NAME );
                    loaded.set( true );
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
            initChannel( channel, 0, 0, 0, false, WS2811_STRIP_RGB );
        }

        // Initialize current channel
        LOG.debug( "Initializing current channel..." );

        initChannel( currentChannel, ledsCount, gpioPin, brightness, invert, stripType );

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

    private long buildColour( int red, int green, int blue ) {
        return ( (short) red << 16 ) | ( (short) green << 8 ) | (short) blue;
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

    public int getStripType() {
        return stripType;
    }
}