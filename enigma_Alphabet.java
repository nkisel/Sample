package enigma;

import java.util.ArrayList;
import static enigma.EnigmaException.*;

/** An alphabet of encodable characters.  Provides a mapping from characters
 *  to and from indices into the alphabet.
 *  @author Nick Kisel
 */
class Alphabet {

    /** A new alphabet containing CHARS.  Character number #k has index
     *  K (numbering from 0). No character may be duplicated. */
    Alphabet(String chars) {
        this._alphabet = new ArrayList<Character>();
        for (int i = 0; i < chars.length(); i++) {
            if (!_alphabet.contains(chars.charAt(i))) {
                _alphabet.add(chars.charAt(i));
            } else {
                throw error(String.format(
                        "Found a duplicated character %c.", chars.charAt(i)));
            }
        }

        if (_alphabet.contains('*')
                || _alphabet.contains('(')
                || _alphabet.contains(')')) {
            throw error("Alphabet may not contain *, (, or ).");
        }

        _size = _alphabet.size();
    }

    /** A default alphabet of all upper-case characters. */
    Alphabet() {
        this("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    /** Returns the size of the alphabet. */
    int size() {
        return _size;
    }

    /** Returns true if preprocess(CH) is in this alphabet. */
    boolean contains(char ch) {
        return _alphabet.contains(ch);
    }

    /** Returns character number INDEX in the alphabet, where
     *  0 <= INDEX < size(). */
    char toChar(int index) {
        return (_alphabet.get(index));
    }

    /** Returns the index of character preprocess(CH), which must be in
     *  the alphabet. This is the inverse of toChar(). */
    int toInt(char ch) {
        return _alphabet.indexOf(ch);
    }

    /** The ordered list of all characters in the alphabet. */
    private java.util.ArrayList<Character> _alphabet;

    /** The number of characters in the alphabet. */
    private int _size;

}
