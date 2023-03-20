package se.attini.cli;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoadingIndicator {
    private static Timer timer;
    private static TimerTask timerTask;

    private final String text;
    private final boolean disableAnsiColor;

    private final static List<String> ANIMATION = List.of("", "#", "##", "###", "####", "#####", "######");


    public LoadingIndicator(String text, boolean disableAnsiColor) {
        this.text = text;
        this.disableAnsiColor = disableAnsiColor;
    }


    public void startSpinner() {


        if (disableAnsiColor) {
            System.out.println(text);
            return;
        }

        timer = new Timer();
        timerTask = new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                if (i == ANIMATION.size()) {
                    i = 0;
                }
                System.out.print("\033[2K");
                System.out.print("\r" + text + " \u001B[34m" + ANIMATION.get(i) + "\u001B[0m");
                i++;
            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 500L);

    }


    public void stopSpinner() {
        if (disableAnsiColor) {
            return;
        }
        timerTask.cancel();
        timer.cancel();
        System.out.print('\r');
        System.out.print("\033[2K");
        System.out.println(text);

    }
}
