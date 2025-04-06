package com.chess;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chess.GameActivityManagers.AnimationManager;
import com.chess.GameActivityManagers.BoardManager;
import com.chess.GameActivityManagers.ChessPieceManager;
import com.chess.GameActivityManagers.GameStateManager;
import com.chess.GameActivityManagers.MoveInfo;
import com.chess.GameActivityManagers.MoveManager;
import com.chess.GameActivityManagers.MoveValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static GameActivity instance;

    private boolean shouldResumeGame = false;
    private static final long REPLAY_MOVE_DELAY = 50; // 0.05s
    private static final long INITIAL_REPLAY_DELAY = 100; // 0.1s

    private boolean isWhiteTurn = true;
    private TableLayout chessboard;
    private FrameLayout selectedSquare = null;
    private ImageView selectedPiece = null;
    private View selectionBorder;
    private ValueAnimator borderAnimator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Stack<MoveInfo> moveHistory = new Stack<>();

    // square animations
    private final Map<FrameLayout, ValueAnimator> squareAnimators = new HashMap<>();

    // chessboard id map
    private final Map<Integer, Integer> resourceIdMap = new HashMap<>();

    // last move info
    private FrameLayout lastMoveFromSquare = null;
    private FrameLayout lastMoveToSquare = null;
    private TextView gameInfoTextView;
    private boolean gameOver = false;
    private String gameWinner = null;

    // managers
    private BoardManager boardManager;
    private MoveManager moveManager;
    private AnimationManager animationManager;
    private ChessPieceManager chessPieceManager;
    private MoveValidator moveValidator;

    // colors for the squares
    public static final int WHITE_COLOR = 0xFFFFFFFF;
    public static final int BLACK_COLOR = 0xFF000000;
    public static final long PLACEMENT_DELAY = 100;
    public static final int LIGHT_SQUARE_COLOR = 0xFFFFCE9E;
    public static final int DARK_SQUARE_COLOR = 0xFFD18B47;
    public static final int FROM_SQUARE_HIGHLIGHT = 0xFFE8F5E9; // Light green tint
    public static final int TO_SQUARE_HIGHLIGHT = 0xFFE1F5FE;   // Light blue tint

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent.getBooleanExtra("resume", false)) {
            shouldResumeGame = true;
        }

        initializeResourceIdMap();
        chessboard = findViewById(R.id.chessboard);

        // selection border
        selectionBorder = new View(this);
        selectionBorder.setBackgroundResource(R.drawable.selection_border);

        // managers
        initializeManagers();

        // game info
        gameInfoTextView = findViewById(R.id.gameInfo);
        setWhiteTurn(isWhiteTurn);

        // border animation
        animationManager.setupBorderAnimation();

        // undo button
        setupUndoButton();

        // menu button
        Button menuButton = findViewById(R.id.button_menu);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                // auto-save
                saveCurrentGame();
                finish();
            });
        }

        // chess piece placement related animations
        chessPieceManager.animatePiecePlacement();

        // click listeners for squares
        boardManager.setupChessboardClickListeners();

        // piece placement from resume
        long totalPlacementTime = 32 * PLACEMENT_DELAY;
        handler.postDelayed(this::onPiecePlacementComplete, totalPlacementTime + 100);
    }

    @SuppressLint("SetTextI18n")
    private void updateGameInfoText() {
        if (gameInfoTextView != null) {
            if (gameOver && gameWinner != null) {
                // show winner
                gameInfoTextView.setText(gameWinner + " won!");
            } else {
                // show turn
                String currentPlayer = isWhiteTurn ? "White" : "Black";
                gameInfoTextView.setText(currentPlayer + " to move!");
            }
        }
    }

    private void onPiecePlacementComplete() {
        if (shouldResumeGame) {
            handler.postDelayed(this::loadAndReplaySavedGame, INITIAL_REPLAY_DELAY);
        }
    }

    private void loadAndReplaySavedGame() {
        GameStateManager.GameState gameState = GameStateManager.loadGameState(this);
        if (gameState == null) {
            return;
        }

        // get saved moves as ArrayList
        ArrayList<MoveInfo> moves = new ArrayList<>(gameState.getMoveHistory());

        // replay moves
        for (int i = 0; i < moves.size(); i++) {
            final int moveIndex = i;
            final boolean isLastMove = (moveIndex == moves.size() - 1);

            handler.postDelayed(() -> {
                replayMove(moves.get(moveIndex), isLastMove);

                if (isLastMove) {
                    setWhiteTurn(gameState.isWhiteTurn());
                }
            }, i * REPLAY_MOVE_DELAY);
        }
    }

    private void replayMove(MoveInfo move, boolean shouldHighlight) {
        FrameLayout fromSquare = boardManager.getSquareAt(move.fromRow, move.fromCol);
        FrameLayout toSquare = boardManager.getSquareAt(move.toRow, move.toCol);

        if (fromSquare == null || toSquare == null) return;

        // find piece to move
        ImageView pieceToMove = chessPieceManager.findPieceInSquare(fromSquare);
        if (pieceToMove == null) return;

        // clear highlights
        animationManager.clearLastMoveHighlights();

        // add move to history
        moveHistory.push(move);

        // execute move
        moveManager.movePiece(pieceToMove, fromSquare, toSquare);

        // highlight
        if (shouldHighlight) {
            animationManager.highlightMove(move);
        }
    }

    private void initializeManagers() {
        boardManager = new BoardManager(this);
        moveManager = new MoveManager(this);
        animationManager = new AnimationManager(this);
        chessPieceManager = new ChessPieceManager(this);
        moveValidator = new MoveValidator(this);
    }

    private void initializeResourceIdMap() {
        int[] resourceIds = {
                R.drawable.w_pawn, R.drawable.w_rook, R.drawable.w_knight,
                R.drawable.w_bishop, R.drawable.w_queen, R.drawable.w_king,
                R.drawable.b_pawn, R.drawable.b_rook, R.drawable.b_knight,
                R.drawable.b_bishop, R.drawable.b_queen, R.drawable.b_king
        };

        for (int id : resourceIds) {
            resourceIdMap.put(id, id);
        }
    }

    private void setupUndoButton() {
        Button undoButton = findViewById(R.id.button_undo);
        if (undoButton != null) {
            undoButton.setOnClickListener(v ->
            {
                if (isGameOver()) {
                    return;
                }
                moveManager.undoLastMove();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // cancel all animations
        if (borderAnimator != null && borderAnimator.isRunning()) {
            borderAnimator.cancel();
        }

        // cancel square animations
        for (ValueAnimator animator : squareAnimators.values()) {
            if (animator.isRunning()) {
                animator.cancel();
            }
        }
        squareAnimators.clear();

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (instance == this) {
            instance = null;
        }

        // remove animations
        if (borderAnimator != null && borderAnimator.isRunning()) {
            borderAnimator.cancel();
        }

        for (ValueAnimator animator : squareAnimators.values()) {
            if (animator.isRunning()) {
                animator.cancel();
            }
        }
        squareAnimators.clear();

        handler.removeCallbacksAndMessages(null);
    }

    public void saveCurrentGame() {
        if (moveHistory.isEmpty()) {
            // nothing to save
            return;
        }

        // save current game state
        GameStateManager.saveGameState(this, moveHistory, isWhiteTurn);
    }

    public void setGameWinner(String winner) {
        this.gameWinner = winner;
        this.gameOver = true;
        updateGameInfoText();
    }

    // getters and setters

    public boolean isGameOver() {
        return gameOver;
    }

    public TableLayout getChessboard() {
        return chessboard;
    }

    public MoveValidator getMoveValidator() {
        return moveValidator;
    }

    public Handler getHandler() {
        return handler;
    }

    public Map<Integer, Integer> getResourceIdMap() {
        return resourceIdMap;
    }

    public Stack<MoveInfo> getMoveHistory() {
        return moveHistory;
    }

    public FrameLayout getSelectedSquare() {
        return selectedSquare;
    }

    public void setSelectedSquare(FrameLayout square) {
        this.selectedSquare = square;
    }

    public ImageView getSelectedPiece() {
        return selectedPiece;
    }

    public void setSelectedPiece(ImageView piece) {
        this.selectedPiece = piece;
    }

    public View getSelectionBorder() {
        return selectionBorder;
    }

    public ValueAnimator getBorderAnimator() {
        return borderAnimator;
    }

    public void setBorderAnimator(ValueAnimator animator) {
        this.borderAnimator = animator;
    }

    public Map<FrameLayout, ValueAnimator> getSquareAnimators() {
        return squareAnimators;
    }

    public FrameLayout getLastMoveFromSquare() {
        return lastMoveFromSquare;
    }

    public void setLastMoveFromSquare(FrameLayout square) {
        this.lastMoveFromSquare = square;
    }

    public FrameLayout getLastMoveToSquare() {
        return lastMoveToSquare;
    }

    public void setLastMoveToSquare(FrameLayout square) {
        this.lastMoveToSquare = square;
    }

    public BoardManager getBoardManager() {
        return boardManager;
    }

    public MoveManager getMoveManager() {
        return moveManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public ChessPieceManager getChessPieceManager() {
        return chessPieceManager;
    }

    public boolean getWhiteTurn() {
        return isWhiteTurn;
    }

    public void setWhiteTurn(boolean whiteTurn) {
        this.isWhiteTurn = whiteTurn;
        updateGameInfoText();
    }
}