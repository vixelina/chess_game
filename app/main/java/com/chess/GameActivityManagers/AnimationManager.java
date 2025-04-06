package com.chess.GameActivityManagers;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.chess.GameActivity;

public class AnimationManager {
    private final GameActivity gameActivity;

    public AnimationManager(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public void setupBorderAnimation() {
        ValueAnimator borderAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                GameActivity.WHITE_COLOR, GameActivity.BLACK_COLOR);
        borderAnimator.setDuration(1000);
        borderAnimator.setRepeatCount(ValueAnimator.INFINITE);
        borderAnimator.setRepeatMode(ValueAnimator.REVERSE);

        borderAnimator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            try {
                GradientDrawable border = (GradientDrawable) gameActivity.getSelectionBorder().getBackground();
                border.setStroke(4, color);
            } catch (Exception ignored) {}
        });

        gameActivity.setBorderAnimator(borderAnimator);
    }

    public void clearLastMoveHighlights() {
        // clear previous highlighting
        if (gameActivity.getLastMoveFromSquare() != null) {
            boolean isDark = gameActivity.getBoardManager().isSquareDark(
                    gameActivity.getBoardManager().getSquareCoordinates(gameActivity.getLastMoveFromSquare()));
            setStaticSquareColor(gameActivity.getLastMoveFromSquare(),
                    isDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR);
            gameActivity.setLastMoveFromSquare(null);
        }

        if (gameActivity.getLastMoveToSquare() != null) {
            boolean isDark = gameActivity.getBoardManager().isSquareDark(
                    gameActivity.getBoardManager().getSquareCoordinates(gameActivity.getLastMoveToSquare()));
            setStaticSquareColor(gameActivity.getLastMoveToSquare(),
                    isDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR);
            gameActivity.setLastMoveToSquare(null);
        }
    }

    public void highlightMove(MoveInfo move) {
        // get squares
        FrameLayout fromSquare = gameActivity.getBoardManager().getSquareAt(move.fromRow, move.fromCol);
        FrameLayout toSquare = gameActivity.getBoardManager().getSquareAt(move.toRow, move.toCol);

        if (fromSquare == null || toSquare == null) {
            return;
        }

        // get board colors
        boolean isFromDark = gameActivity.getBoardManager().isSquareDark(new int[]{move.fromRow, move.fromCol});
        boolean isToDark = gameActivity.getBoardManager().isSquareDark(new int[]{move.toRow, move.toCol});

        int fromBaseColor = isFromDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR;
        int toBaseColor = isToDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR;

        // set animated colors
        setSquareColor(fromSquare, fromBaseColor, GameActivity.FROM_SQUARE_HIGHLIGHT);
        setSquareColor(toSquare, toBaseColor, GameActivity.TO_SQUARE_HIGHLIGHT);

        gameActivity.setLastMoveFromSquare(fromSquare);
        gameActivity.setLastMoveToSquare(toSquare);
    }

    public void setStaticSquareColor(FrameLayout square, int color) {
        stopSquareAnimator(square);

        // non-animated color
        View backgroundView = square.getChildAt(0);
        if (backgroundView != null && !(backgroundView instanceof ImageView)) {
            backgroundView.setBackgroundColor(color);
        }
    }

    public void stopSquareAnimator(FrameLayout square) {
        ValueAnimator animator = gameActivity.getSquareAnimators().get(square);
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            gameActivity.getSquareAnimators().remove(square);
        }
    }

    public void setSquareColor(FrameLayout square, int fromColor, int toColor) {
        stopSquareAnimator(square);

        View backgroundView = square.getChildAt(0);
        if (backgroundView != null && !(backgroundView instanceof ImageView)) {
            // transition between colors
            ValueAnimator colorAnimator = ValueAnimator.ofArgb(fromColor, toColor);
            colorAnimator.setDuration(500);
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE);

            colorAnimator.addUpdateListener(animation -> {
                int animatedColor = (int) animation.getAnimatedValue();
                backgroundView.setBackgroundColor(animatedColor);
            });

            gameActivity.getSquareAnimators().put(square, colorAnimator);

            colorAnimator.start();
        }
    }
}