package enigma;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Collection;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author Nick Kisel
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;
        _numRotors = numRotors;
        _pawls = pawls;
        _allRotors = new HashMap<>();
        _installed = new ArrayList<>();
        for (Rotor rotor : allRotors) {
            _allRotors.put(rotor.name(), rotor);
        }
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return _numRotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _pawls;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        if (rotors.length != numRotors()) {
            throw error(String.format("Machine of %d rotors "
                    + "cannot fit %d rotors", numRotors(), rotors.length));
        }

        _installed.clear();

        int pawlCount = 0;
        for (String rotorName : rotors) {
            Rotor rtr = _allRotors.getOrDefault(rotorName, null);

            if (rtr == null) {
                throw error(String.format("Rotor name %s "
                        + "does not appear in the configuration.", rotorName));

            } else if (rtr.name().toUpperCase().equals(rotors[0].toUpperCase())
                    && !rtr.reflecting()) {
                throw error("Need a reflector as the first rotor.");

            } else if (_installed.size() > 0) {

                if (rtr.reflecting()) {
                    throw error("Reflectors "
                            + "may only be assigned to the first slot.");
                } else if (_installed.get(_installed.size() - 1).rotates()
                        && !rtr.rotates()) {
                    throw error("Reflectors and fixed rotors may not be "
                            + "positioned to the right of a moving rotor.");
                }

            }

            if (_installed.contains(rtr)) {
                throw error("Cannot install a rotor twice.");
            }

            _installed.add(rtr);

            if (rtr.rotates()) {
                pawlCount++;
            }

        }

        if (_installed.size() != _numRotors) {
            throw error(String.format("%d rotors installed in %d slots",
                    _installed.size(), _numRotors));
        }
        if (pawlCount != numPawls()) {
            throw error("Inserted pawl count does not match.");
        }

    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).
     *  Set my rotor's ringstellungs to RING, which takes on the same
     *  format as SETTING and is applied to the corresponding rotor. */
    void setRotors(String setting, String ring) {
        if (setting.length() != _installed.size() - 1) {
            throw error("Wrong number of initial rotor positions.");
        }

        if (ring.length() == 0) {
            for (int i = 0; i < _installed.size(); i++) {
                _installed.get(i).setRingstellung(0);
            }
        } else if (ring.length() == _installed.size() - 1) {
            for (int i = 0; i < ring.length(); i++) {
                if (_alphabet.contains(ring.charAt(i))) {
                    _installed.get(i + 1).setRingstellung(
                            _alphabet.toInt(ring.charAt(i)));
                } else {
                    throw error("Ring configuration"
                            + " contains invalid character");
                }
            }
        } else {
            throw error("Wrong number of initial ring positions");
        }

        for (int i = 0; i < setting.length(); i++) {
            if (_alphabet.contains(setting.charAt(i))) {
                _installed.get(i + 1).set(_alphabet.toInt(setting.charAt(i))
                        - _installed.get(i + 1)._ringstellung);
            } else {
                throw error(String.format("Provided rotor setting %s "
                        + "does not appear in alphabet", setting.charAt(i)));
            }
        }
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        BitSet toRotate = new BitSet(_installed.size());

        toRotate.set(_installed.size() - 1);
        for (int i = 1; i < _installed.size(); i++) {
            Rotor rtrLeft = _installed.get(i - 1);
            Rotor rtr = _installed.get(i);

            if (rtrLeft.rotates() && rtr.atNotch()) {
                toRotate.set(i - 1);
                toRotate.set(i);
            }
        }

        for (int i = 1; i < _installed.size(); i++) {
            if (toRotate.get(i)) {
                _installed.get(i).advance();
            }
        }

        toRotate.clear();

        int conversion = c;

        conversion = _plugboard.permute(conversion);

        for (int i = _installed.size() - 1; i >= 0; i--) {
            conversion = _installed.get(i).convertForward(conversion);
        }

        for (int i = 1; i < _installed.size(); i++) {
            conversion = _installed.get(i).convertBackward(conversion);
        }

        conversion = _plugboard.invert(conversion);

        return conversion;

    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {

        StringBuilder output = new StringBuilder();

        for (int m = 0; m < msg.length(); m++) {
            output.append(_alphabet.toChar(
                    convert(_alphabet.toInt(msg.charAt(m)))));
        }

        return output.toString();
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;

    /** Number of rotor slots. */
    private final int _numRotors;

    /** Number of moving rotor slots. */
    private final int _pawls;

    /** Map between a rotor's name and the actual rotor. */
    private final HashMap<String, Rotor> _allRotors;

    /** The ordered list of installed rotors from left to right.
     *  The leftmost rotor is always a reflector. */
    private ArrayList<Rotor> _installed;

    /** The permutation described by the plugboard connections.
     *  All cycles in this permutation have length 2. */
    private Permutation _plugboard;

}
