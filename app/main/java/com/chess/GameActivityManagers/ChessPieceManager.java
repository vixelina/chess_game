package com.chess.GameActivityManagers;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.chess.GameActivity;
import com.chess.R;

import java.util.Map;

public class ChessPieceManager {
    private final GameActivity gameActivity;

    public ChessPieceManager(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public void animatePiecePlacement() {
        final int[][] pieceLayout = {
                // black pieces
                {0, 0, R.drawable.b_rook, 4},
                {0, 1, R.drawable.b_knight, 4},
                {0, 2, R.drawable.b_bishop, 4},
                {0, 3, R.drawable.b_queen, 4},
                {0, 4, R.drawable.b_king, 4},
                {0, 5, R.drawable.b_bishop, 4},
                {0, 6, R.drawable.b_knight, 4},
                {0, 7, R.drawable.b_rook, 4},

                // black pawns
                {1, 0, R.drawable.b_pawn, 8},
                {1, 1, R.drawable.b_pawn, 8},
                {1, 2, R.drawable.b_pawn, 8},
                {1, 3, R.drawable.b_pawn, 8},
                {1, 4, R.drawable.b_pawn, 8},
                {1, 5, R.drawable.b_pawn, 8},
                {1, 6, R.drawable.b_pawn, 8},
                {1, 7, R.drawable.b_pawn, 8},

                // white pawns
                {6, 0, R.drawable.w_pawn, 8},
                {6, 1, R.drawable.w_pawn, 8},
                {6, 2, R.drawable.w_pawn, 8},
                {6, 3, R.drawable.w_pawn, 8},
                {6, 4, R.drawable.w_pawn, 8},
                {6, 5, R.drawable.w_pawn, 8},
                {6, 6, R.drawable.w_pawn, 8},
                {6, 7, R.drawable.w_pawn, 8},

                // white pieces
                {7, 0, R.drawable.w_rook, 4},
                {7, 1, R.drawable.w_knight, 4},
                {7, 2, R.drawable.w_bishop, 4},
                {7, 3, R.drawable.w_queen, 4},
                {7, 4, R.drawable.w_king, 4},
                {7, 5, R.drawable.w_bishop, 4},
                {7, 6, R.drawable.w_knight, 4},
                {7, 7, R.drawable.w_rook, 4}
        };

        // place pieces at start
        for (int i = 0; i < pieceLayout.length; i++) {
            final int[] pieceInfo = pieceLayout[i];
            final long delay = i * GameActivity.PLACEMENT_DELAY;

            gameActivity.getHandler().postDelayed(() -> {
                int row = pieceInfo[0];
                int col = pieceInfo[1];
                int drawableId = pieceInfo[2];
                int padding = pieceInfo[3];

                // get square at position
                FrameLayout square = gameActivity.getBoardManager().getSquareAt(row, col);
                if (square != null) {
                    // add piece
                    placePiece(square, drawableId, padding);
                }
            }, delay);
        }
    }

    public void placePiece(FrameLayout square, int drawableId, int padding) {
        // create view for the piece
        ImageView piece = new ImageView(gameActivity);
        piece.setImageResource(drawableId);
        piece.setTag(drawableId);  // Set the tag for resource lookup
        piece.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        piece.setPadding(padding, padding, padding, padding);

        // remove existing piece
        for (int i = 0; i < square.getChildCount(); i++) {
            View child = square.getChildAt(i);
            if (child instanceof ImageView) {
                square.removeView(child);
                break;
            }
        }

        int insertIndex = 1; // place piece above background
        square.addView(piece, Math.min(insertIndex, square.getChildCount()));

        // listen for piece activity
        piece.setOnClickListener(v -> gameActivity.getMoveManager().handlePieceClick(piece, square));
    }

    public ImageView findPieceInSquare(FrameLayout square) {
        for (int i = 0; i < square.getChildCount(); i++) {
            View child = square.getChildAt(i);
            if (child instanceof ImageView) {
                return (ImageView) child;
            }
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public int getDrawableResourceId(ImageView imageView) {
        if (imageView == null) {
            return -1;
        }

        try {
            if (imageView.getTag() instanceof Integer) {
                return (Integer) imageView.getTag();
            }

            // search in map too, just in case
            android.graphics.drawable.Drawable drawable = imageView.getDrawable();
            for (Map.Entry<Integer, Integer> entry : gameActivity.getResourceIdMap().entrySet()) {
                if (drawable.equals(gameActivity.getResources().getDrawable(entry.getKey(), null))) {
                    return entry.getValue();
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public boolean isPieceWhite(ImageView pieceView) {
        if (pieceView == null) return false;

        Integer resourceId = (Integer) pieceView.getTag();
        if (resourceId == null) return false;

        // does resource name starts with "w_"
        String resourceName = gameActivity.getResources().getResourceEntryName(resourceId);
        return resourceName != null && resourceName.startsWith("w_");
    }
}