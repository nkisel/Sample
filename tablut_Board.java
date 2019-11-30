package tablut;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Stack;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;


/** The state of a Tablut Game.
 *  @author Nick Kisel
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }

        _undo = new Stack<>();
        _changes = new HashMap<>(SIZE * SIZE);
        _board = new HashMap<>(SIZE * SIZE);
        _whitePos = new HashSet<Square>();
        _blackPos = new HashSet<Square>();
        _repeatedPositions = new HashMap<>();

        for (Square sq : SQUARE_LIST) {
            _board.put(sq, model._board.get(sq));
        }

        this._moveCount = model.moveCount();
        this._winner = model.winner();
        this._kingPos = model.kingPosition();
        this._whitePos = pieceLocations(WHITE);
        this._blackPos = pieceLocations(BLACK);
        this._turn = model.turn();
        _undo.addAll(model._undo);
        this._limit = model._limit;
        this._repeatedPositions.putAll(model._repeatedPositions);
    }

    /** Clears the board to the initial position. */
    void init() {
        _undo = new Stack<>();
        _changes = new HashMap<>(SIZE * SIZE);
        _board = new HashMap<>(SIZE * SIZE);
        _whitePos = new
                HashSet<Square>(Arrays.asList(INITIAL_DEFENDERS));
        _blackPos = new
                HashSet<Square>(Arrays.asList(INITIAL_ATTACKERS));
        _repeatedPositions = new HashMap<>();

        _kingPos = THRONE;
        _limit = Integer.MAX_VALUE;

        for (Square s : SQUARE_LIST) {
            _board.put(s, EMPTY);
        }
        for (Square s : _whitePos) {
            _board.put(s, WHITE);
        }
        for (Square s : _blackPos) {
            _board.put(s, BLACK);
        }
        _board.put(THRONE, KING);
        _whitePos.add(_kingPos);

        _winner = null;
        _turn = BLACK;
        _moveCount = 0;
    }

    /** Set the move limit to N.  It is an error if 2*N <= moveCount(). */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            throw new IllegalArgumentException();
        } else {
            _limit = n;
        }
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position,
     *  or null if there is no winner yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (_repeatedPositions.containsKey(encodedBoard())
            && _repeatedPositions.get(encodedBoard()) == _turn) {
            _repeated = true;
            _winner = _turn;
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        return _kingPos;
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return _board.get(s);
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        assert (row >= 0) && (col >= 0)
                && (row < 10) && (col < 10);

        return _board.get(sq(col, row));
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(row - '1', col - 'a');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        _board.put(s, p);
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        _changes.put(s, get(s));
        put(p, s);
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        if (get(to) != EMPTY) {
            return false;
        }
        if (from.isRookMove(to)) {
            Square between = from;
            int steps = 1;
            while (between != to) {
                between = from.rookMove(from.direction(to), steps);
                if (get(between) != EMPTY) {
                    return false;
                }
                steps += 1;
            }
            return true;
        }
        return false;
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if (to == THRONE && get(from) != KING) {
            return false;
        }
        return isLegal(from) && isUnblockedMove(from, to);
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        assert isLegal(from, to);
        Piece player = _board.get(from);
        assert player.side() == _turn;
        assert KING == get(kingPosition());

        if (moveCount() > _limit) {
            _winner = _turn.opponent();
        }

        _repeatedPositions.put(encodedBoard(), _turn);
        _changes = new HashMap<Square, Piece>(6);

        revPut(EMPTY, from);
        revPut(player, to);
        if (player == KING) {
            _kingPos = to;
            if (to.isEdge()) {
                _winner = WHITE;
            }
        }

        for (int dir = 0; dir < 4; dir++) {
            Square companion = to.rookMove(dir, 2);
            if (companion != null
                    && (get(companion).side() == _turn
                    || companion == THRONE)) {
                capture(to, companion);
            }
        }

        _undo.push(_changes);
        _moveCount += 1;
        _turn = _turn.opponent();
        checkRepeated();

        if (!hasMove(turn())) {
            _winner = _turn.opponent();
        }
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square captureLocation = sq0.between(sq2);

        if (get(captureLocation) == KING) {
            if (captureLocation == THRONE) {
                if (get(NTHRONE) == BLACK && get(STHRONE) == BLACK
                        && get(WTHRONE) == BLACK && get(ETHRONE) == BLACK) {
                    revPut(EMPTY, THRONE);
                    _winner = BLACK;
                } else {
                    return;
                }

            } else if (captureLocation == NTHRONE
                    || captureLocation == WTHRONE
                    || captureLocation == ETHRONE
                    || captureLocation == STHRONE) {
                for (int direction = 0; direction < 4; direction++) {
                    if (!(get(captureLocation.rookMove(
                            direction, 1)) == BLACK
                            || (captureLocation.rookMove(
                                    direction, 1) == THRONE))) {
                        return;
                    }
                }
                revPut(EMPTY, captureLocation);
                _winner = BLACK;
            } else {
                if (get(sq0) == BLACK && get(sq2) == BLACK) {
                    revPut(EMPTY, captureLocation);
                    _winner = BLACK;
                }
            }
        } else if (get(sq0).opponent() == get(captureLocation).side()
                && ((get(sq0).side() == get(sq2).side())
                || (sq2 == THRONE && get(THRONE) == EMPTY))) {
            revPut(EMPTY, captureLocation);
        } else {
            if (get(captureLocation) == WHITE
                    && sq2 == THRONE && get(THRONE) == KING) {
                int occupied = 0;
                if (get(NTHRONE) == BLACK) {
                    occupied += 1;
                }
                if (get(ETHRONE) == BLACK) {
                    occupied += 1;
                }
                if (get(WTHRONE) == BLACK) {
                    occupied += 1;
                }
                if (get(STHRONE) == BLACK) {
                    occupied += 1;
                }
                if (occupied == 3) {
                    revPut(EMPTY, captureLocation);
                }
            }
        }
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
            _board.putAll(_undo.pop());
            _moveCount -= 1;
            _turn = _turn.opponent();
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        if (_repeated || _moveCount == 0) {
            return;
        } else {
            _repeatedPositions.remove(encodedBoard());
        }
        _repeated = false;
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        _undo.clear();
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        ArrayList<Move> moves = new ArrayList<>();
        for (Square loc : pieceLocations(side)) {
            for (int i = 0; i < 4; i++) {
                for (Move move : ROOK_MOVES[loc.index()][i]) {
                    if (isLegal(move)) {
                        moves.add(move);
                    } else if (move.to() == THRONE) {
                        continue;
                    } else {
                        break;
                    }

                }
            }
        }
        return moves;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {

        for (Square loc : pieceLocations(side)) {
            for (int i = 0; i < 4; i++) {
                for (Move move : ROOK_MOVES[loc.index()][i]) {
                    if (isLegal(move)) {
                        return true;
                    } else if (move.to() == THRONE) {
                        continue;
                    } else {
                        break;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        switch (side) {
        case WHITE:
            _whitePos = new HashSet<>();
            for (Square s : SQUARE_LIST) {
                if (get(s) == WHITE) {
                    _whitePos.add(s);
                }
            }
            _whitePos.add(kingPosition());
            return _whitePos;
        case BLACK:
            _blackPos = new HashSet<>();
            for (Square s : SQUARE_LIST) {
                if (get(s) == BLACK) {
                    _blackPos.add(s);
                }
            }
            return _blackPos;
        default:
            return null;
        }
    }

    /** Return the cached locations of all pieces on SIDE. */
    HashSet<Square> cachedLocations(Piece side) {
        switch (side) {
        case WHITE:
            return _whitePos;
        case BLACK:
            return _blackPos;
        default:
            return null;
        }
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;

    /** Number of moves allowed from each side. */
    private int _limit;

    /** A map between a square and the occupant piece for this board state. */
    private HashMap<Square, Piece> _board;

    /** Positions of white pieces on the board. */
    private HashSet<Square> _whitePos;

    /** Positions of black pieces on the board. */
    private HashSet<Square> _blackPos;

    /** King's position. */
    private Square _kingPos;

    /** Differences between the current board state and the previous one. */
    private Stack<HashMap<Square, Piece>> _undo;

    /** Stores all squares that may have been changed by the last move. */
    private HashMap<Square, Piece> _changes;

    /** Stores all boards (by string representation) that have been
     * encountered since the beginning of this game.. */
    private HashMap<String, Piece> _repeatedPositions;

}
