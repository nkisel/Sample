package enigma;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import static enigma.EnigmaException.*;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Nick Kisel
 */
class Permutation {

    /** Set this Permutation to that specified by CYCLES, a string in the
     *  form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     *  is interpreted as a permutation in cycle notation.  Characters in the
     *  alphabet that are not included in any cycle map to themselves.
     *  Whitespace is ignored. */
    Permutation(String cycles, Alphabet alphabet) {
        _alphabet = alphabet;
        _derangement = true;
        _nextIndex = new HashMap<>(_alphabet.size());

        Matcher m = Pattern.compile("\\(([^)]+?)\\)").matcher(cycles);
        while (m.find()) {
            String ccl = m.group(1);
            for (int i = 0; i < ccl.length(); i++) {
                if (!_alphabet.contains(m.group(1).charAt(i))) {
                    throw error("Letter in cycle does not appear in alphabet");
                }
            }
            addCycle(ccl);
        }

        for (int i = 0; i < _alphabet.size(); i++) {
            if (!_nextIndex.containsKey(i)) {
                _derangement = false;
                _nextIndex.put(i, i);
            }
        }

        _inverseIndex = new HashMap<>(_alphabet.size());
        for (int i = 0; i < _nextIndex.size(); i++) {
            _inverseIndex.put(_nextIndex.get(i), i);
        }

    }

    /** Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     *  c0c1...cm. */
    private void addCycle(String cycle) {
        if (cycle == null || cycle.length() == 0) {
            return;
        } else if (cycle.length() == 1) {
            int alphaIndex = _alphabet.toInt(cycle.charAt(0));
            _nextIndex.put(alphaIndex, alphaIndex);
            _derangement = false;
        } else {
            for (int i = 1; i < cycle.length(); i++) {

                if (_nextIndex.getOrDefault(
                        _alphabet.toInt(cycle.charAt(i - 1)), null) != null) {
                    throw error("Letters may not "
                            + "appear in permutations more than once.");
                }

                _nextIndex.put(_alphabet.toInt(cycle.charAt(i - 1)),
                        _alphabet.toInt(cycle.charAt(i)));
            }

            if (_nextIndex.getOrDefault(_alphabet.toInt(
                    cycle.charAt(cycle.length() - 1)), null) != null) {
                throw error("Letters may not "
                        + "appear in permutations more than once.");
            }

            _nextIndex.put(_alphabet.toInt(cycle.charAt(cycle.length() - 1)),
                    _alphabet.toInt(cycle.charAt(0)));

        }
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return alphabet().size();
    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {
        return _nextIndex.get(wrap(p));
    }

    /** Return the result of applying the inverse of this permutation
     *  to  C modulo the alphabet size. */
    int invert(int c) {
        return _inverseIndex.get(wrap(c));
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        return alphabet().toChar(_nextIndex.get(alphabet().toInt(p)));
    }

    /** Return the result of applying the inverse of this permutation to C. */
    int invert(char c) {
        return alphabet().toChar(_inverseIndex.get(alphabet().toInt(c)));
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        return _derangement;
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;

    /** Return whether all letters in alphabet map to a different letter. */
    private boolean _derangement;

    /** Each key represents one index of ALPHABET and
     *  maps to the index of the next character.  */
    private HashMap<Integer, Integer> _nextIndex;

    /** An identical key-set to _NEXTINDEX whose values
     *  reverse the direction of permutation.  */
    private HashMap<Integer, Integer> _inverseIndex;

}
