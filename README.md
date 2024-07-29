This project aims to find the best move in a game of Cab.

# Rules of Cab
Each player starts with three cards. On your turn, you may do one of the following:
- Take the top-most card of the discard pile, then discard a card from your hand
- Take a random card from the deck, then discard a card from your hand
- Knock (if the value of your hand is at least 26)

When a player knocks, all other players get one more turn, then the player with the highest hand value wins.

Hand value is the greatest sum of cards of the same suit from your hand. If you have three of one card (e.g. three nines, three aces) your hand value is 30. Aces count high and all face cards are 10.

If at any point your hand value is 31, you win the game immediately.
