package com.chess.GameActivityManagers;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chess.GameActivity;

public class MoveManager {
    private static final String DEBUG_TAG = "ChessDebug";
    private final GameActivity gameActivity;
    private boolean handlingCapture = false;

    public MoveManager(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public void handlePieceClick(ImageView piece, FrameLayout square) {
        if (gameActivity.isGameOver()) {
            return;
        }

        // get current parent of clicked piece
        ViewGroup currentParent = (ViewGroup) piece.getParent();
        FrameLayout currentSquare = (currentParent instanceof FrameLayout) ? (FrameLayout) currentParent : square;

        // DEBUG INFO
        Integer pieceId = (Integer) piece.getTag();
        String pieceName = pieceId != null ? gameActivity.getResources().getResourceEntryName(pieceId) : "unknown";
        boolean isPieceWhite = gameActivity.getChessPieceManager().isPieceWhite(piece);
        int[] coords = gameActivity.getBoardManager().getSquareCoordinates(currentSquare);

        Log.d(DEBUG_TAG, "PIECE CLICK: " + pieceName + " at " +
                (coords != null ? coords[0] + "," + coords[1] : "unknown") +
                " isPieceWhite=" + isPieceWhite +
                " isWhiteTurn=" + gameActivity.getWhiteTurn());

        // check if a piece is already selected
        if (gameActivity.getSelectedPiece() != null) {
            boolean isSelectedPieceWhite = gameActivity.getChessPieceManager().isPieceWhite(gameActivity.getSelectedPiece());
            boolean isClickedPieceWhite = gameActivity.getChessPieceManager().isPieceWhite(piece);

            Integer selectedPieceId = (Integer) gameActivity.getSelectedPiece().getTag();
            String selectedPieceName = selectedPieceId != null ?
                    gameActivity.getResources().getResourceEntryName(selectedPieceId) : "unknown";

            Log.d(DEBUG_TAG, "EXISTING SELECTION: " + selectedPieceName +
                    " isSelectedPieceWhite=" + isSelectedPieceWhite +
                    " isClickedPieceWhite=" + isClickedPieceWhite);

            // if clicked on opponent piece, treat as capture attempt
            if (isSelectedPieceWhite != isClickedPieceWhite &&
                    isSelectedPieceWhite == gameActivity.getWhiteTurn()) {

                Log.d(DEBUG_TAG, "CAPTURE ATTEMPT: Redirecting to handleSquareClick with CURRENT square");
                // use current square
                handleSquareClickCapture(gameActivity.getSelectedPiece(), gameActivity.getSelectedSquare(), currentSquare);
                return;
            }
        }

        // piece selection logic
        // check player turn
        if (isPieceWhite != gameActivity.getWhiteTurn()) {
            Log.d(DEBUG_TAG, "REJECTED: Not this player's turn");
            return;
        }

        if (gameActivity.getSelectedPiece() == piece) {
            Log.d(DEBUG_TAG, "DESELECT: Same piece clicked again");
            clearSelection();
        } else {
            clearSelection();
            Log.d(DEBUG_TAG, "SELECT: New piece selected");

            // use current parent for selection
            selectPiece(piece, currentSquare);
        }
    }

    // moving to non-empty squares
    private void handleSquareClickCapture(ImageView attackingPiece, FrameLayout fromSquare, FrameLayout targetSquare) {
        // get coordinates for move validation
        int[] fromCoords = gameActivity.getBoardManager().getSquareCoordinates(fromSquare);
        int[] toCoords = gameActivity.getBoardManager().getSquareCoordinates(targetSquare);

        if (fromCoords == null || toCoords == null) {
            Log.e(DEBUG_TAG, "ERROR: Could not get coordinates for capture attempt");
            return;
        }

        Log.d(DEBUG_TAG, "CAPTURE ATTEMPT: From " + fromCoords[0] + "," + fromCoords[1] +
                " To " + toCoords[0] + "," + toCoords[1]);

        // check if valid move
        boolean isValid = gameActivity.getMoveValidator().isValidMove(
                fromCoords[0], fromCoords[1],
                toCoords[0], toCoords[1],
                attackingPiece);

        Log.d(DEBUG_TAG, "CAPTURE VALIDATION: " + (isValid ? "VALID" : "INVALID"));

        if (!isValid) {
            return; // invalid
        }

        // clear previous move highlighting
        gameActivity.getAnimationManager().clearLastMoveHighlights();

        // original square colors
        boolean isFromSquareDark = gameActivity.getBoardManager().isSquareDark(fromCoords);
        boolean isToSquareDark = gameActivity.getBoardManager().isSquareDark(toCoords);

        // piece info
        int pieceDrawableId = gameActivity.getChessPieceManager().getDrawableResourceId(attackingPiece);
        int piecePadding = attackingPiece.getPaddingLeft();

        // target piece info
        ImageView targetPiece = gameActivity.getChessPieceManager().findPieceInSquare(targetSquare);
        int targetPieceDrawableId = -1;
        int targetPiecePadding = 0;

        if (targetPiece != null) {
            targetPieceDrawableId = gameActivity.getChessPieceManager().getDrawableResourceId(targetPiece);
            targetPiecePadding = targetPiece.getPaddingLeft();

            String targetPieceName = gameActivity.getResources().getResourceEntryName(targetPieceDrawableId);
            Log.d(DEBUG_TAG, "CAPTURING PIECE: " + targetPieceName);
        }

        // move info
        MoveInfo moveInfo = new MoveInfo(
                fromCoords[0], fromCoords[1],
                toCoords[0], toCoords[1],
                pieceDrawableId, piecePadding,
                targetPieceDrawableId, targetPiecePadding,
                isFromSquareDark, isToSquareDark
        );

        // add to history
        gameActivity.getMoveHistory().push(moveInfo);

        // execute
        movePiece(attackingPiece, fromSquare, targetSquare);

        // highlight
        gameActivity.getAnimationManager().highlightMove(moveInfo);

        // change player turn
        boolean oldTurn = gameActivity.getWhiteTurn();
        gameActivity.setWhiteTurn(!oldTurn);
        Log.d(DEBUG_TAG, "TURN CHANGED: " + (oldTurn ? "White" : "Black") +
                " -> " + (!oldTurn ? "White" : "Black"));
    }

    // moving to empty squares
    public void handleSquareClick(FrameLayout square) {
        if (gameActivity.isGameOver()) {
            return;
        }

        ImageView piece = gameActivity.getChessPieceManager().findPieceInSquare(square);

        if (piece != null) {
            handlePieceClick(piece, square);
        } else if (gameActivity.getSelectedPiece() != null) {
            // continue if piece color matches player turn
            boolean isSelectedPieceWhite = gameActivity.getChessPieceManager().isPieceWhite(gameActivity.getSelectedPiece());
            if (isSelectedPieceWhite != gameActivity.getWhiteTurn()) {
                return; // wrong player turn
            }

            // get coordinates for move validation
            int[] fromCoords = gameActivity.getBoardManager().getSquareCoordinates(gameActivity.getSelectedSquare());
            int[] toCoords = gameActivity.getBoardManager().getSquareCoordinates(square);

            if (fromCoords != null && toCoords != null) {
                // check if valid move
                if (!gameActivity.getMoveValidator().isValidMove(
                        fromCoords[0], fromCoords[1],
                        toCoords[0], toCoords[1],
                        gameActivity.getSelectedPiece())) {
                    return; // invalid
                }

                // valid - continue
                // clear highlighting
                gameActivity.getAnimationManager().clearLastMoveHighlights();

                // original board colors
                boolean isFromSquareDark = gameActivity.getBoardManager().isSquareDark(fromCoords);
                boolean isToSquareDark = gameActivity.getBoardManager().isSquareDark(toCoords);

                // get piece info
                int pieceDrawableId = gameActivity.getChessPieceManager().getDrawableResourceId(gameActivity.getSelectedPiece());
                int piecePadding = gameActivity.getSelectedPiece().getPaddingLeft();

                // check for captured piece
                ImageView capturedPiece = gameActivity.getChessPieceManager().findPieceInSquare(square);
                int capturedPieceDrawableId = -1;
                int capturedPiecePadding = 0;

                if (capturedPiece != null) {
                    capturedPieceDrawableId = gameActivity.getChessPieceManager().getDrawableResourceId(capturedPiece);
                    capturedPiecePadding = capturedPiece.getPaddingLeft();
                }

                // get move info
                MoveInfo moveInfo = new MoveInfo(
                        fromCoords[0], fromCoords[1],
                        toCoords[0], toCoords[1],
                        pieceDrawableId, piecePadding,
                        capturedPieceDrawableId, capturedPiecePadding,
                        isFromSquareDark, isToSquareDark
                );

                // add to history (for undo)
                gameActivity.getMoveHistory().push(moveInfo);

                // move piece
                movePiece(gameActivity.getSelectedPiece(), gameActivity.getSelectedSquare(), square);

                // highlight move
                gameActivity.getAnimationManager().highlightMove(moveInfo);

                // change player turn
                gameActivity.setWhiteTurn(!gameActivity.getWhiteTurn());
            }
        }
    }

    public void movePiece(ImageView piece, FrameLayout fromSquare, FrameLayout toSquare) {
        // get coords
        int[] fromCoords = gameActivity.getBoardManager().getSquareCoordinates(fromSquare);
        int[] toCoords = gameActivity.getBoardManager().getSquareCoordinates(toSquare);

        // special moves
        if (fromCoords != null && toCoords != null) {
            Integer pieceId = (Integer) piece.getTag();
            if (pieceId != null) {
                String pieceName = gameActivity.getResources().getResourceEntryName(pieceId);
                boolean isPawn = pieceName.endsWith("_pawn");
                boolean isWhitePawn = pieceName.startsWith("w_") && isPawn;

                // if castle
                if (pieceName.endsWith("_king") &&
                        fromCoords[0] == toCoords[0] &&
                        fromCoords[1] == 4 &&
                        (toCoords[1] == 6 || toCoords[1] == 2)) {

                    boolean isKingside = toCoords[1] == 6;
                    int rookFromCol = isKingside ? 7 : 0;
                    int rookToCol = isKingside ? 5 : 3;

                    // get rook
                    FrameLayout rookFromSquare = gameActivity.getBoardManager().getSquareAt(fromCoords[0], rookFromCol);
                    FrameLayout rookToSquare = gameActivity.getBoardManager().getSquareAt(fromCoords[0], rookToCol);

                    ImageView rookPiece = gameActivity.getChessPieceManager().findPieceInSquare(rookFromSquare);

                    if (rookPiece != null && rookToSquare != null) {
                        // move rook
                        rookFromSquare.removeView(rookPiece);

                        rookPiece.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        ));

                        rookToSquare.addView(rookPiece, Math.min(1, rookToSquare.getChildCount()));

                        // Mark this move as a castling in the move history
                        if (!gameActivity.getMoveHistory().isEmpty()) {
                            MoveInfo lastMove = gameActivity.getMoveHistory().peek();
                            lastMove.wasCastling = true;
                            lastMove.rookFromCol = rookFromCol;
                            lastMove.rookToCol = rookToCol;
                        }
                    }
                }

                // pawn promotion - unrelated to en passant
                if (isPawn) {
                    // is pawn on final rank
                    boolean reachedFinalRank = (isWhitePawn && toCoords[0] == 0) || (!isWhitePawn && toCoords[0] == 7);

                    if (reachedFinalRank) {
                        // store pawn Id - for undo
                        Integer originalPawnId = pieceId;

                        ImageView capturedPiece = gameActivity.getChessPieceManager().findPieceInSquare(toSquare);
                        boolean isKingCapture = false;
                        String capturedColor = null;

                        // bug fix - checking for king capture
                        if (capturedPiece != null) {
                            Integer capturedId = (Integer) capturedPiece.getTag();
                            if (capturedId != null) {
                                String capturedName = gameActivity.getResources().getResourceEntryName(capturedId);
                                isKingCapture = capturedName.endsWith("_king");
                                capturedColor = capturedName.startsWith("w_") ? "White" : "Black";
                            }
                        }

                        // remove pawn
                        fromSquare.removeView(piece);

                        // create queen
                        String colorPrefix = isWhitePawn ? "w_" : "b_";
                        int queenDrawableId = gameActivity.getResources().getIdentifier(
                                colorPrefix + "queen", "drawable", gameActivity.getPackageName());

                        // place queen
                        gameActivity.getChessPieceManager().placePiece(toSquare, queenDrawableId, 4);

                        // add to move history as promotion
                        if (!gameActivity.getMoveHistory().isEmpty()) {
                            MoveInfo lastMove = gameActivity.getMoveHistory().peek();
                            lastMove.wasPromotion = true;
                            lastMove.originalPieceId = originalPawnId;
                        }

                        // end game if king capture
                        if (isKingCapture && capturedColor != null) {
                            String winner = isWhitePawn ? "White" : "Black";
                            gameActivity.setGameWinner(winner);
                        }

                        return;
                    }
                }

                // check en passant
                if (isPawn &&
                        Math.abs(fromCoords[1] - toCoords[1]) == 1 && // move diagonal
                        Math.abs(fromCoords[0] - toCoords[0]) == 1 && // move one row
                        gameActivity.getChessPieceManager().findPieceInSquare(toSquare) == null) { // empty target

                    // remove captured pawn
                    FrameLayout capturedPawnSquare = gameActivity.getBoardManager().getSquareAt(fromCoords[0], toCoords[1]);
                    ImageView capturedPawn = gameActivity.getChessPieceManager().findPieceInSquare(capturedPawnSquare);

                    if (capturedPawn != null) {
                        capturedPawnSquare.removeView(capturedPawn);
                    }
                }
            }
        }

        ImageView capturedPiece = null;
        boolean kingCaptured = false;
        String capturedPieceName = null;

        for (int i = 0; i < toSquare.getChildCount(); i++) {
            View child = toSquare.getChildAt(i);
            if (child instanceof ImageView) {
                capturedPiece = (ImageView) child;

                // check if king capture
                Integer capturedPieceId = (Integer) capturedPiece.getTag();
                if (capturedPieceId != null) {
                    capturedPieceName = gameActivity.getResources().getResourceEntryName(capturedPieceId);
                    kingCaptured = capturedPieceName != null && capturedPieceName.endsWith("_king");

                    if (kingCaptured) {
                        System.out.println("KING CAPTURED: " + capturedPieceName);
                    }
                }

                break;
            }
        }

        fromSquare.removeView(piece);

        if (gameActivity.getSelectionBorder().getParent() != null) {
            ((ViewGroup)gameActivity.getSelectionBorder().getParent()).removeView(gameActivity.getSelectionBorder());
        }

        // remove any existing piece in target square (capture)
        for (int i = 0; i < toSquare.getChildCount(); i++) {
            View child = toSquare.getChildAt(i);
            if (child instanceof ImageView) {
                capturedPiece = (ImageView) child;
                toSquare.removeView(child);
                System.out.println("CAPTURE: " + gameActivity.getResources().getResourceEntryName((Integer)capturedPiece.getTag()));
                break;
            }
        }

        // add piece above background
        int insertIndex = 1; // background at index 0
        toSquare.addView(piece, Math.min(insertIndex, toSquare.getChildCount()));

        piece.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        piece.setPadding(8, 8, 8, 8);

        gameActivity.setSelectedPiece(null);
        gameActivity.setSelectedSquare(null);

        if (gameActivity.getBorderAnimator().isRunning()) {
            gameActivity.getBorderAnimator().cancel();
        }

        // if king capture
        if (kingCaptured) {
            // show winner
            if (capturedPieceName.startsWith("w_")) {
                gameActivity.setGameWinner("Black");
            } else {
                gameActivity.setGameWinner("White");
            }
        }
    }

    public void selectPiece(ImageView piece, FrameLayout square) {
        gameActivity.setSelectedPiece(piece);
        gameActivity.setSelectedSquare(square);

        if (gameActivity.getSelectionBorder().getParent() != null) {
            ((ViewGroup)gameActivity.getSelectionBorder().getParent()).removeView(gameActivity.getSelectionBorder());
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        square.addView(gameActivity.getSelectionBorder(), params);

        if (!gameActivity.getBorderAnimator().isStarted()) {
            gameActivity.getBorderAnimator().start();
        }
    }

    public void clearSelection() {
        if (gameActivity.getSelectedSquare() != null) {
            gameActivity.getSelectedSquare().removeView(gameActivity.getSelectionBorder());
            gameActivity.getBorderAnimator().cancel();
            gameActivity.setSelectedPiece(null);
            gameActivity.setSelectedSquare(null);
        }
    }

    public void undoLastMove() {
        if (gameActivity.getMoveHistory().isEmpty()) {
            return; // nothing to undo
        }

        // clear previous move highlights
        gameActivity.getAnimationManager().clearLastMoveHighlights();

        // remove last move from history
        MoveInfo lastMove = gameActivity.getMoveHistory().pop();

        // get involved squares
        FrameLayout fromSquare = gameActivity.getBoardManager().getSquareAt(lastMove.fromRow, lastMove.fromCol);
        FrameLayout toSquare = gameActivity.getBoardManager().getSquareAt(lastMove.toRow, lastMove.toCol);

        if (fromSquare == null || toSquare == null) {
            return; // should never trigger
        }

        // get moved piece
        ImageView movedPiece = gameActivity.getChessPieceManager().findPieceInSquare(toSquare);
        if (movedPiece == null) {
            return;
        }

        // remove piece from current square
        toSquare.removeView(movedPiece);

        // castle undo
        if (lastMove.wasCastling) {
            // get rook current square
            FrameLayout rookCurrentSquare = gameActivity.getBoardManager().getSquareAt(lastMove.fromRow, lastMove.rookToCol);

            // get rook original square
            FrameLayout rookOriginalSquare = gameActivity.getBoardManager().getSquareAt(lastMove.fromRow, lastMove.rookFromCol);

            // find rook
            ImageView rookPiece = gameActivity.getChessPieceManager().findPieceInSquare(rookCurrentSquare);

            if (rookPiece != null) {
                // remove rook from current pos
                rookCurrentSquare.removeView(rookPiece);

                // add rook to original pos
                rookOriginalSquare.addView(rookPiece, Math.min(1, rookOriginalSquare.getChildCount()));
            }
        }

        // promotion undo
        if (lastMove.wasPromotion && lastMove.originalPieceId != null) {
            // create new pawn
            ImageView pawnPiece = new ImageView(gameActivity);
            pawnPiece.setImageResource(lastMove.originalPieceId);
            pawnPiece.setTag(lastMove.originalPieceId);

            // set layout params for pawn
            pawnPiece.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            pawnPiece.setPadding(8, 8, 8, 8);

            // add listener for restored pawn
            pawnPiece.setOnClickListener(v -> handlePieceClick(pawnPiece, fromSquare));

            // add pawn back
            int insertIndex = 1;
            fromSquare.addView(pawnPiece, Math.min(insertIndex, fromSquare.getChildCount()));
        } else {
            // move piece back to original square
            int insertIndex = 1;
            fromSquare.addView(movedPiece, Math.min(insertIndex, fromSquare.getChildCount()));

            // reset layout parameters
            movedPiece.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            movedPiece.setPadding(
                    lastMove.piecePadding,
                    lastMove.piecePadding,
                    lastMove.piecePadding,
                    lastMove.piecePadding
            );
        }

        // if a piece was captured, restore it
        if (lastMove.capturedPieceDrawableId != -1) {
            // create new piece with captured piece info
            ImageView capturedPiece = getImageView(lastMove, toSquare);

            // add captured piece back to square
            int insertIndex = 1;
            toSquare.addView(capturedPiece, Math.min(insertIndex, toSquare.getChildCount()));
        }

        // restore original square colors
        gameActivity.getAnimationManager().setStaticSquareColor(fromSquare,
                lastMove.isFromSquareDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR);
        gameActivity.getAnimationManager().setStaticSquareColor(toSquare,
                lastMove.isToSquareDark ? GameActivity.DARK_SQUARE_COLOR : GameActivity.LIGHT_SQUARE_COLOR);

        // highlight previous move, if exists
        if (!gameActivity.getMoveHistory().isEmpty()) {
            MoveInfo previousMove = gameActivity.getMoveHistory().peek();
            gameActivity.getAnimationManager().highlightMove(previousMove);
        }

        // toggle player turn
        boolean currentTurn = gameActivity.getWhiteTurn();
        gameActivity.setWhiteTurn(!currentTurn);

        System.out.println("UNDO: Turn changed from " + (currentTurn ? "White" : "Black") +
                " to " + (!currentTurn ? "White" : "Black"));

        clearSelection();
    }

    @NonNull
    private ImageView getImageView(MoveInfo lastMove, FrameLayout toSquare) {
        ImageView capturedPiece = new ImageView(gameActivity);
        capturedPiece.setImageResource(lastMove.capturedPieceDrawableId);
        capturedPiece.setTag(lastMove.capturedPieceDrawableId);
        capturedPiece.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        capturedPiece.setPadding(
                lastMove.capturedPiecePadding,
                lastMove.capturedPiecePadding,
                lastMove.capturedPiecePadding,
                lastMove.capturedPiecePadding
        );

        // click listener
        capturedPiece.setOnClickListener(v -> handlePieceClick(capturedPiece, toSquare));
        return capturedPiece;
    }
}