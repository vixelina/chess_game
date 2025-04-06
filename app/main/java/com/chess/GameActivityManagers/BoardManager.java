package com.chess.GameActivityManagers;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TableRow;

import com.chess.GameActivity;

public class BoardManager {
    private final GameActivity gameActivity;

    public BoardManager(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public FrameLayout getSquareAt(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return null;
        }

        // get row
        View rowView = gameActivity.getChessboard().getChildAt(row);
        if (rowView instanceof TableRow) {
            TableRow tableRow = (TableRow) rowView;

            // get column
            View cellView = tableRow.getChildAt(col);
            if (cellView instanceof FrameLayout) {
                return (FrameLayout) cellView;
            }
        }

        return null;
    }

    public int[] getSquareCoordinates(FrameLayout square) {
        for (int i = 0; i < gameActivity.getChessboard().getChildCount(); i++) {
            View rowView = gameActivity.getChessboard().getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    if (row.getChildAt(j) == square) {
                        return new int[]{i, j}; // row, col
                    }
                }
            }
        }
        return null;
    }

    public boolean isSquareDark(int[] coords) {
        if (coords == null) return false;
        // determine dark squares
        return (coords[0] + coords[1]) % 2 == 1;
    }

    public void setupChessboardClickListeners() {
        for (int i = 0; i < gameActivity.getChessboard().getChildCount(); i++) {
            View rowView = gameActivity.getChessboard().getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cellView = row.getChildAt(j);
                    if (cellView instanceof FrameLayout) {
                        FrameLayout square = (FrameLayout) cellView;
                        square.setOnClickListener(v -> gameActivity.getMoveManager().handleSquareClick(square));
                    }
                }
            }
        }
    }
}