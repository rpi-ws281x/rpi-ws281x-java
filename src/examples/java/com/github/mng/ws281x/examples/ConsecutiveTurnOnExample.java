package com.github.mng.ws281x.examples;


import com.github.mbelling.ws281x.Color;
import com.github.mbelling.ws281x.LedStripType;
import com.github.mbelling.ws281x.Ws281xLedStrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsecutiveTurnOnExample {
    private static Logger log = LoggerFactory.getLogger(ConsecutiveTurnOnExample.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("STARTING CONSECUTIVE TURN-ON EXAMPLE");
        log.info("received args: {}", (Object[]) args);
        int amount = Integer.parseInt(args[0]);
        var strip = new Ws281xLedStrip(amount, 10, 800000, 0, 255, 0, false, LedStripType.WS2811_STRIP_GRB, true);
        int curr = 0;
        while (!Thread.interrupted()) {
            Color col;
            switch (curr % 3) {
                case 0:
                    col = Color.RED;
                    break;
                case 1:
                    col = Color.GREEN;
                    break;
                default:
                    col = Color.BLUE;
                    break;
            }
            strip.setPixel(curr++, col);
            if (curr > amount) {
                curr = 0;
                strip.setStrip(Color.BLACK);
            }
            log.info("rendered {}", curr);
            strip.render();
            Thread.sleep(1000);
        }
        log.info("EXITING  CONSECUTIVE TURN-ON EXAMPLE BYE!");
    }
}
