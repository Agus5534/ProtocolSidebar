package me.catcoder.sidebar.text;

import java.util.List;

import lombok.NonNull;
import lombok.experimental.Delegate;

/**
 * Text sliding animation. WIP
 *
 * @author CatCoder
 */
public class TextSlideAnimation extends TextIterator {

    private final String text;

    @Delegate(types = {FrameIterator.class})
    private final FrameIterator frameIterator;

    public TextSlideAnimation(@NonNull String text) {
        this.text = text;
        this.frameIterator = new FrameIterator(createAnimationFrames());
    }

    private List<TextFrame> createAnimationFrames() {
        // TODO: 17.11.2022 contributions are welcome :)
        throw new UnsupportedOperationException("Unsupported yet");
    }

}
