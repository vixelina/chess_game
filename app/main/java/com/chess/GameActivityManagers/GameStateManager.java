package com.chess.GameActivityManagers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Stack;

public class GameStateManager {
    private static final String PREF_NAME = "ChessGamePrefs";
    private static final String KEY_MOVE_HISTORY = "moveHistory";
    private static final String KEY_IS_WHITE_TURN = "isWhiteTurn";
    private static final String TAG = "GameStateManager";

    public static void saveGameState(Context context, Stack<MoveInfo> moveHistory, boolean isWhiteTurn) {
        if (moveHistory.isEmpty()) {
            Log.d(TAG, "No moves to save");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // convert move info stack to ArrayList
        ArrayList<MoveInfo> moveList = new ArrayList<>(moveHistory);

        // convert ArrayList to JSON
        Gson gson = new Gson();
        String movesJson = gson.toJson(moveList);

        // save data
        editor.putString(KEY_MOVE_HISTORY, movesJson);
        editor.putBoolean(KEY_IS_WHITE_TURN, isWhiteTurn);
        editor.apply();

        Log.d(TAG, "Game saved: " + moveList.size() + " moves, " +
                (isWhiteTurn ? "White's turn" : "Black's turn"));
    }

    public static boolean hasSavedGame(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String movesJson = prefs.getString(KEY_MOVE_HISTORY, null);
        return movesJson != null && !movesJson.isEmpty();
    }

    public static GameState loadGameState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String movesJson = prefs.getString(KEY_MOVE_HISTORY, null);
        boolean isWhiteTurn = prefs.getBoolean(KEY_IS_WHITE_TURN, true);

        if (movesJson == null || movesJson.isEmpty()) {
            Log.d(TAG, "No saved game found");
            return null;
        }

        // convert JSON to ArrayList
        Gson gson = new Gson();
        Type moveListType = new TypeToken<ArrayList<MoveInfo>>(){}.getType();
        ArrayList<MoveInfo> moveList = gson.fromJson(movesJson, moveListType);

        // convert ArrayList back to stack
        Stack<MoveInfo> moveHistory = new Stack<>();
        moveHistory.addAll(moveList);

        Log.d(TAG, "Game loaded: " + moveHistory.size() + " moves, " +
                (isWhiteTurn ? "White's turn" : "Black's turn"));

        return new GameState(moveHistory, isWhiteTurn);
    }

    // game state
    public static class GameState {
        private final Stack<MoveInfo> moveHistory;
        private final boolean isWhiteTurn;

        public GameState(Stack<MoveInfo> moveHistory, boolean isWhiteTurn) {
            this.moveHistory = moveHistory;
            this.isWhiteTurn = isWhiteTurn;
        }

        public Stack<MoveInfo> getMoveHistory() {
            return moveHistory;
        }

        public boolean isWhiteTurn() {
            return isWhiteTurn;
        }
    }
}