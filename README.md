# Free samples
Snippets of project work. Many of my projects come from Berkeley coursework and cannot be fully publicly shared: the full programs are available upon request.

## Signpost
_a sudoku-like game involving connecting all successive squares on a board using only the direction of each square's successor._
* Model: describes the state of the game and determines the outcomes of interacting with the board.

## Enigma
_A recreation of the World War II era cipher machine._
* Alphabet: initialization of custom alphabets.
* Permutation: describes the cipher logic for a rotor.
* Machine: sets up an Enigma machine with the provided alphabet and rotors.

## Tablut 
_a Norse attack-and-defense board game in which all pieces move like chess rooks; white must move its unique King piece to the edge of the board to win._
* Board: models the state of the board at any given time
* AI: the AI and its move selection logic; implements the minimax algorithm and alpha-beta pruning to find the optimal move for a specified depth.

## Gitlet
_a lite version control system modeled after Git._
* Blob: a serializable object containing one file's name, contents, and additional metadata.
* Commit: a serializable object which tracks a set of files represented by their SHA-1 hash.
