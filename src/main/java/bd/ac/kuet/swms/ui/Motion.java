package bd.ac.kuet.swms.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class Motion {
    private Motion() { }

    public static void enter(Node node, double delayMs, double fromY) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
        slide.setFromY(fromY);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1));
        ParallelTransition both = new ParallelTransition(fade, slide);
        both.setDelay(Duration.millis(delayMs));
        both.play();
    }

    public static void staggerCards(Node page) {
        int index = 0;
        for (Node card : page.lookupAll(".card")) {
            if (!card.isVisible()) continue;
            enter(card, Math.min(index++, 10) * 45.0, 16);
        }
    }

    public static void slideIn(Node node, double fromX) {
        node.setTranslateX(fromX);
        TranslateTransition slide = new TranslateTransition(Duration.millis(360), node);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1));
        slide.play();
    }
}
