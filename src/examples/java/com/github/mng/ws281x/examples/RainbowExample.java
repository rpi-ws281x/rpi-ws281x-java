package com.github.mng.ws281x.examples;


import com.github.mbelling.ws281x.Color;
import com.github.mbelling.ws281x.LedStrip;
import com.github.mbelling.ws281x.LedStripType;
import com.github.mbelling.ws281x.Ws281xLedStrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RainbowExample {
    private static Logger log = LoggerFactory.getLogger(RainbowExample.class);

    //Generate rainbow colors across 0-255 positions.
    // taken from strandtest.py from the python rpi-ws281x project
    private static Color wheel(int pos) {
        if (pos < 85) {
            return new Color(pos * 3, 255 - pos * 3, 0);
        } else if (pos < 170) {
            pos -= 85;
            return new Color(255 - pos * 3, 0, pos * 3);
        } else {
            pos -= 170;
            return new Color(0, pos * 3, 255 - pos * 3);
        }
    }

    //Draw rainbow that fades across all pixels at once.
    // taken from strandtest.py from the python rpi-ws281x project
    private static void rainbow(LedStrip strip, int numPixels) throws InterruptedException {
        final int wait_ms = 20;
        final int iterations = 1;
        for (int j = 0; j < 256 * iterations; j++) {
            for (int i = 0; i < numPixels; i++) {
                strip.setPixel(i, wheel((i + j) & 255));
            }
            strip.render();
            Thread.sleep(wait_ms);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("STARTING RAINBOW EXAMPLE");
        log.info("received args: {}", (Object[]) args);
        int amount = Integer.parseInt(args[0]);
        var strip = new Ws281xLedStrip(amount, 10, 800000, 0, 255, 0, false, LedStripType.WS2811_STRIP_GRB, true);
        while (!Thread.interrupted()) {
            rainbow(strip, amount);
        }
        log.info("EXITING  CONSECUTIVE TURN-ON EXAMPLE BYE!");
    }
}
