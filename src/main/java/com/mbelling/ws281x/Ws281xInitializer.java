package com.mbelling.ws281x;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Ws281xInitializer {

    private static final String LIB_NAME = "ws281x";
	private static Boolean loaded = Boolean.FALSE;

	private static final Logger LOG = LogManager.getLogger(Ws281xInitializer.class);

	// Settings
	private int ledsCount;
	private int gpioPin;
	private int frequencyHz;
	private int dma;
	private int brightness;
	private int pwmChannel;
	private boolean invert;

	// Messy JNI instances
	ws2811_t leds;
	ws2811_channel_t currentChannel;

	public Ws281xInitializer()
	{
		// Default settings
		// TODO: move to constants file
		this(
				59,         // leds
				18,         // pin
				800000,     // freq hz
				5,          // dma
				255,        // brightness
				0,          // pwm channel
				false       // invert
		);
	}

	public Ws281xLedController(int ledsCount, int gpioPin, int frequencyHz, int dma, int brightness, int pwmChannel, boolean invert)
	{
		this.ledsCount = ledsCount;
		this.gpioPin = gpioPin;
		this.frequencyHz = frequencyHz;
		this.dma = dma;
		this.brightness = brightness;
		this.pwmChannel = pwmChannel;
		this.invert = invert;

		LOG.info("LEDs count: {}, GPIO pin: {}, freq hZ: {}, DMA: {}, brightness: {}, pwm channel: {}, invert: {}",
				ledsCount, gpioPin, frequencyHz, dma, brightness, pwmChannel, invert
		);

		init();
	}

	private static void initializeNative() {
		synchronized (loaded) {
			com.mbelling.ws281x.rpi_ws281x.ws2811_init(ws2811_t)
			if (!loaded) {
				try {
					Path path = Files.createTempFile("lib" + LIB_NAME, ".so");
					path.toFile().deleteOnExit();
					Files.copy(Ws281xInitializer.class.getResourceAsStream("/lib" + LIB_NAME + ".so"), path,
							StandardCopyOption.REPLACE_EXISTING);
					System.load(path.toString());
					loaded = Boolean.TRUE;
				} catch (IOException e) {
					System.out.println("Error loading library from classpath: " + e);
					e.printStackTrace();

					// Try load the usual way...
					System.loadLibrary(LIB_NAME);
					loaded = Boolean.TRUE;
				}

				Runtime.getRuntime().addShutdownHook(new Thread(WS281xNative::terminate, "WS281x Shutdown Handler"));
			}
		}
	}

	private void init()
	{
		initializeNative();

		// Create ref object for pwm channel (?)
		leds = new ws2811_t();

		// Fetch the channel we'll use
		LOG.debug("Fetching current channel...");
		currentChannel = rpi_ws281x.ws2811_channel_get(leds, pwmChannel);

		// Initialize all PWM channels
		LOG.debug("Initializing all channels...");

		ws2811_channel_t channel;
		for (int i = 0; i < 2; i++)
		{
			channel = rpi_ws281x.ws2811_channel_get(leds, i);

			// Set all to zero (default)
			initChannel(channel, 0, 0, 0, false);
		}

		// Initialize current channel
		LOG.debug("Initializing current channel...");

		initChannel(currentChannel, ledsCount, gpioPin, brightness, invert);

		// Initialize LED driver/controller
		LOG.debug("Initializing driver...");

		leds.setFreq(frequencyHz);
		leds.setDmanum(dma);

		// Attempt to initialize driver
		LOG.debug("Attempting to initialize driver...");

		ws2811_return_t result = rpi_ws281x.ws2811_init(leds);

		if (result.swigValue() != 0)
		{
			throw new RuntimeException("Failed to setup lights driver, result code: " + result);
		}

//		LOG.debug("Setting LEDs to off...");
//
//		// Set strip to off
//		setStrip(0, 0, 0);
//		render();

		LOG.debug("Initialization complete");
	}

	private void initChannel(ws2811_channel_t channel, int ledsCount, int gpioPin, int brightness, boolean invert)
	{
		channel.setCount(ledsCount);
		channel.setGpionum(gpioPin);
		channel.setInvert(invert ? 1 : 0);
		channel.setBrightness((short)brightness);
	}

	public synchronized void setStrip(int red, int green, int blue)
	{
		for (int i = 0; i < ledsCount; i++)
		{
			setPixel(i, red, green, blue);
		}
	}

	public synchronized void setPixel(int pixel, int red, int green, int blue)
	{
		if (leds != null)
		{
			rpi_ws281x.ws2811_led_set(currentChannel, pixel, buildColour(red, green, blue));
		}
	}

	public synchronized void render()
	{
		if (leds != null)
		{
			rpi_ws281x.ws2811_render(leds);
		}
	}

	public synchronized void destroy()
	{
		try
		{
			if (leds != null)
			{
				rpi_ws281x.ws2811_fini(leds);
				leds.delete();
				leds = null;
				currentChannel = null;
			}
		}
		catch (Exception e)
		{
			LOG.error("Failed to dispose Ws281x LED controller", e);
		}
	}

	private long buildColour(int red, int green, int blue)
	{
		return ((short) red << 16) | ((short) green << 8) | (short) blue;
	}

	public int getLedsCount()
	{
		return ledsCount;
	}

	public int getGpioPin()
	{
		return gpioPin;
	}

	public int getFrequencyHz()
	{
		return frequencyHz;
	}

	public int getDma()
	{
		return dma;
	}

	public int getBrightness()
	{
		return brightness;
	}

	public int getPwmChannel()
	{
		return pwmChannel;
	}

	public boolean isInvert()
	{
		return invert;
	}
}