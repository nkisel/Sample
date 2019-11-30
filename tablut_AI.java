package tablut;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.lang.Math.*;

import static tablut.Square.BOARD_SIZE;
import static tablut.Square.sq;
import static tablut.Piece.*;

/** A Player that automatically generates moves.
 *  @author Nick Kisel
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        findMove(board(), maxDepth(board()),
                true, _myPiece == BLACK ? -1 : 1, -INFTY, INFTY);
        _controller.reportMove(_lastFoundMove);
        return _lastFoundMove.toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return the optimal move for me from the current position,
     *  assuming there is a move.
     *  Equivalent to a one-level search. */
    private Move findMove() {

        Board b = board();

        if (b.hasMove(b.turn())) {
            Move best = null;
            int bestValue = INFTY * (b.turn() == BLACK ? 1 : -1);
            for (Move move : b.legalMoves(b.turn())) {
                b.makeMove(move);
                int recent = staticScore(b);
                if (_myPiece == BLACK) {
                    if (bestValue > recent) {
                        bestValue = recent;
                        best = move;
                    }
                } else {
                    if (bestValue < recent) {
                        bestValue = recent;
                        best = move;
                    }
                }
                b.undo();
            }
            _lastFoundMove = best;
            return best;
        } else {
            return null;
        }

    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {

        if (depth == 0 || board.winner() != null) {
            int score = staticScore(board);
            return score;
        } else {
            List<Move> boardMoves = board.legalMoves(board.turn());
            ListIterator<Move> nextMove = boardMoves.listIterator();
            Move bestSoFar = null;

            int bestValue = -sense * (INFTY);

            while (nextMove.hasNext()) {
                Board nextPos = new Board(board);
                Move next = nextMove.next();
                nextPos.makeMove(next);
                int nextPosBestValue = findMove(nextPos, depth - 1,
                        false, -sense, alpha, beta);

                if (sense == 1) {
                    if (nextPosBestValue > bestValue) {
                        bestSoFar = next;
                        bestValue = nextPosBestValue;
                        alpha = max(alpha, nextPosBestValue);
                        if (beta <= alpha) {
                            break;
                        }
                    }

                } else {
                    if (nextPosBestValue < bestValue) {
                        bestSoFar = next;
                        bestValue = nextPosBestValue;
                        beta = min(beta, nextPosBestValue);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }

                if (saveMove) {
                    _lastFoundMove = bestSoFar;
                }
            }

            return bestValue;
        }

    }

    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        if (board.moveCount() < 16) {
            return 3;
        }
        List<Move> moves = board.legalMoves(board.turn());
        if (moves.size() > 3) {
            return 4;
        } else {
            return 5;
        }
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {

        if (board.winner() != null) {
            if (board.winner() == WHITE) {
                return WINNING_VALUE - board.moveCount();
            } else {
                return -WINNING_VALUE + board.moveCount();
            }
        } else {
            Square king = board.kingPosition();
            int totalScore = Math.min((int) (Math.pow(king.col() - 5, 2)
                    + Math.pow(king.row() - 5, 2)), 11);

            int winOptions = 0;
            for (int dir = 0; dir < 4; dir++) {
                if (clearPathToEdge(board, dir)) {
                    winOptions += 1;
                    totalScore += 5;
                }
                if (board.get(king.rookMove(dir, 1)) != EMPTY) {
                    totalScore -= 2;
                }
            }
            if (winOptions > 1) {
                return WILL_WIN_VALUE - (winOptions + 2 * board.moveCount());
            }

            Set<Square> whitePos = board.cachedLocations(WHITE);
            Set<Square> blackPos = board.cachedLocations(BLACK);

            int avgDistance = 0;
            for (Square black : blackPos) {
                avgDistance += (Math.pow(black.row() - king.row(), 2)
                        + Math.pow(black.col() - king.col(), 2));
            }

            return (totalScore + avgDistance)
                    + (whitePos.size()) - (blackPos.size() / 2);
        }
    }


    /** Return whether the king has a clear path to the edge in direction DIR
     * on this BOARD. */
    private boolean clearPathToEdge(Board board, int dir) {
        Square king = board.kingPosition();
        switch (dir) {
        case 0:
            return board.isUnblockedMove(king,
               sq(BOARD_SIZE - 1, king.row()));
        case 1:
            return board.isUnblockedMove(king, sq(0, king.row()));
        case 2:
            return board.isUnblockedMove(king,
                    sq(king.col(), BOARD_SIZE - 1));
        case 3:
            return board.isUnblockedMove(king, sq(king.col(), 0));
        default:
            return false;
        }
    }

}
