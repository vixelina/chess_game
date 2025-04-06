package com.chess.GameActivityManagers;

public class MoveInfo {
    public int fromRow, fromCol;
    public int toRow, toCol;
    public int pieceDrawableId;
    public int piecePadding;
    public int capturedPieceDrawableId;
    public int capturedPiecePadding;
    public boolean isFromSquareDark;
    public boolean isToSquareDark;
    public boolean wasPromotion = false;
    public Integer originalPieceId = null;
    public boolean wasCastling = false;
    public int rookFromCol = -1;
    public int rookToCol = -1;

    public MoveInfo(int fromRow, int fromCol, int toRow, int toCol,
                    int pieceDrawableId, int piecePadding,
                    int capturedPieceDrawableId, int capturedPiecePadding,
                    boolean isFromSquareDark, boolean isToSquareDark) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.pieceDrawableId = pieceDrawableId;
        this.piecePadding = piecePadding;
        this.capturedPieceDrawableId = capturedPieceDrawableId;
        this.capturedPiecePadding = capturedPiecePadding;
        this.isFromSquareDark = isFromSquareDark;
        this.isToSquareDark = isToSquareDark;
    }
}