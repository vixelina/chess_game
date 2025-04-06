package com.chess.GameActivityManagers;

import android.widget.FrameLayout;
import android.widget.ImageView;

import com.chess.GameActivity;

public class MoveValidator {
    private final GameActivity gameActivity;

    public MoveValidator(GameActivity gameActivity) {
        this.gameActivity = gameActivity;
    }

    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, ImageView piece) {
        ChessPieceManager pieceManager = gameActivity.getChessPieceManager();
        BoardManager boardManager = gameActivity.getBoardManager();

        // check if target location contains same color piece
        FrameLayout targetSquare = boardManager.getSquareAt(toRow, toCol);
        ImageView targetPiece = pieceManager.findPieceInSquare(targetSquare);

        // cannot move to a square with a piece of the same color
        boolean isSourceWhite = pieceManager.isPieceWhite(piece);
        if (targetPiece != null) {
            boolean isTargetWhite = pieceManager.isPieceWhite(targetPiece);
            if (isSourceWhite == isTargetWhite) {
                return false; // Can't capture your own color
            }
        }

        // get piece type from resource name
        Integer pieceResourceId = (Integer) piece.getTag();
        if (pieceResourceId == null) return false;

        String resourceName = gameActivity.getResources().getResourceEntryName(pieceResourceId);
        if (resourceName == null) return false;

        // remove color prefix to get piece type
        String pieceType = resourceName.substring(2); // After "w_" or "b_"

        // check move validity based on piece type
        boolean isCapture = (targetPiece != null);
        switch (pieceType) {
            case "pawn":
                return isValidPawnMove(fromRow, fromCol, toRow, toCol, isSourceWhite, isCapture);
            case "rook":
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case "knight":
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case "bishop":
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case "queen":
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case "king":
                return isValidKingMove(fromRow, fromCol, toRow, toCol);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite, boolean isCapture) {
        // determine direction based on color
        int direction = isWhite ? -1 : 1;  // white pawns move up (-1), black pawns move down (+1)

        // check diagonal movement (potential capture)
        if (Math.abs(fromCol - toCol) == 1) {
            if ((toRow - fromRow) == direction) { // diagonal move
                if (isCapture) {
                    return true; // normal capture
                } else {
                    // en passant
                    return isValidEnPassant(fromRow, fromCol, toRow, toCol, isWhite);
                }
            }
            return false;
        }

        // non diagonal move handling:

        // cannot capture forwards
        if (isCapture) {
            return false;
        }

        // only straight forward movement
        if (fromCol != toCol) {
            return false;
        }

        // first move - can move 1 or 2 squares
        boolean isFirstMove = (isWhite && fromRow == 6) || (!isWhite && fromRow == 1);

        if (toRow - fromRow == direction) {
            return true;  // moving 1 square is always valid if space is empty
        }

        if (isFirstMove && toRow - fromRow == 2 * direction) {
            // check that both squares are empty for a 2-square move
            int middleRow = fromRow + direction;
            FrameLayout middleSquare = gameActivity.getBoardManager().getSquareAt(middleRow, fromCol);
            return gameActivity.getChessPieceManager().findPieceInSquare(middleSquare) == null;
        }

        return false;
    }

    private boolean isValidEnPassant(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite) {
        // Check move history
        if (gameActivity.getMoveHistory().isEmpty()) {
            return false;
        }

        MoveInfo lastMove = gameActivity.getMoveHistory().peek();

        // 1. Check if last move was a pawn
        int lastPieceId = lastMove.pieceDrawableId;

        String lastPieceName = gameActivity.getResources().getResourceEntryName(lastPieceId);
        if (!lastPieceName.endsWith("_pawn")) {
            return false;
        }

        // 2. Check if it was opponent's pawn (opposite color)
        boolean lastMoveWasWhite = lastPieceName.startsWith("w_");
        if (lastMoveWasWhite == isWhite) {
            return false; // Same color pawn
        }

        // 3. Check if it was a two-square move
        int startRow = lastMoveWasWhite ? 6 : 1;
        if (lastMove.fromRow != startRow || Math.abs(lastMove.fromRow - lastMove.toRow) != 2) {
            return false;
        }

        // 4. Check if our pawn is on an adjacent column to the opponent's pawn
        if (Math.abs(fromCol - lastMove.toCol) != 1) {
            return false;
        }

        // 5. Check if our pawn is on the same row as the opponent's pawn
        if (fromRow != lastMove.toRow) {
            return false;
        }

        // 6. Check if we're moving to the square behind the opponent's pawn
        int behindRow = lastMove.toRow + (lastMoveWasWhite ? 1 : -1);
        return toRow == behindRow && toCol == lastMove.toCol;
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        // horizontally or vertically
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        // check if path is clear
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        // knight moves in L-shape - 2 squares in one direction and 1 square perpendicular
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        // bishop moves diagonally
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) {
            return false;
        }

        // check if path is clear
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        // queen combines rook and bishop moves
        return isValidRookMove(fromRow, fromCol, toRow, toCol) ||
                isValidBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        // check for castle
        if (fromRow == toRow && fromCol == 4) {
            if (toCol == 6 || toCol == 2) {
                return isValidCastle(fromRow, fromCol, toCol);
            }
        }

        // normal king move
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);

        return rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0);
    }

    private boolean isValidCastle(int fromRow, int fromCol, int toCol) {
        // verify piece
        FrameLayout fromSquare = gameActivity.getBoardManager().getSquareAt(fromRow, fromCol);
        ImageView kingPiece = gameActivity.getChessPieceManager().findPieceInSquare(fromSquare);
        if (kingPiece == null) return false;

        // get king color
        boolean isWhite = gameActivity.getChessPieceManager().isPieceWhite(kingPiece);

        // verify king
        boolean isCorrectKingRow = (isWhite && fromRow == 7) || (!isWhite && fromRow == 0);
        if (!isCorrectKingRow || fromCol != 4) return false;

        // check castle
        boolean isKingside = toCol == 6;
        boolean isQueenside = toCol == 2;

        if (!isKingside && !isQueenside) return false;

        // find rook
        int rookCol = isKingside ? 7 : 0;
        FrameLayout rookSquare = gameActivity.getBoardManager().getSquareAt(fromRow, rookCol);
        ImageView rookPiece = gameActivity.getChessPieceManager().findPieceInSquare(rookSquare);

        // verify correct rook color
        if (rookPiece == null || gameActivity.getChessPieceManager().isPieceWhite(rookPiece) != isWhite) {
            return false;
        }

        // verify is rook
        Integer rookResourceId = (Integer) rookPiece.getTag();
        if (rookResourceId == null) return false;

        String rookResourceName = gameActivity.getResources().getResourceEntryName(rookResourceId);
        if (!rookResourceName.endsWith("_rook")) return false;

        // check if pieces have moved
        if (hasPieceMoved(fromRow, fromCol) || hasPieceMoved(fromRow, rookCol)) {
            return false;
        }

        // check if path is clear
        int startCol = Math.min(fromCol, rookCol) + 1;
        int endCol = Math.max(fromCol, rookCol) - 1;

        for (int col = startCol; col <= endCol; col++) {
            FrameLayout squareBetween = gameActivity.getBoardManager().getSquareAt(fromRow, col);
            if (gameActivity.getChessPieceManager().findPieceInSquare(squareBetween) != null) {
                return false; // path blocked
            }
        }

        return true;
    }

    private boolean hasPieceMoved(int row, int col) {
        for (MoveInfo move : gameActivity.getMoveHistory()) {
            if (move.fromRow == row && move.fromCol == col) {
                return true; // piece has moved
            }
        }
        return false;
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        // determine direction
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        // check all squares along the path except the destination
        while (currentRow != toRow || currentCol != toCol) {
            FrameLayout square = gameActivity.getBoardManager().getSquareAt(currentRow, currentCol);
            if (gameActivity.getChessPieceManager().findPieceInSquare(square) != null) {
                return false;  // path is blocked
            }

            currentRow += rowStep;
            currentCol += colStep;
        }

        return true;  // path is clear
    }
}