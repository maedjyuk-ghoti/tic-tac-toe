# Tic-Tac-Toe
A simple Tic-Tac-Toe game usable from the command line

## Command Line Args
### Board Size
`--board-size 3`

The board may be any size less than 100. The game behaves correctly when board size is an odd number.

### Number of Players
`--players 1`

Currently, only 1 or 2 players are supported. When number of players is 1, a level 0 bot will play.

### Bot Level
`--bot-level 0`

If a bot is used, it will use the bot level set here. Current bot levels are 0, 1, and 2.

## Motivation
An interviewer had me make a tic-tac-toe game in 40 minutes. I struggled through it and made many bad decisions under the pressure. Here is a version that, while it isn't perfect, is much more in line with how I program and think about problems. The initial commit took 2.5 hours with the drawing taking the largest chunk of time. More time has since been spent improving that work.

## Future work
### Referential Transparency
Much output is lifted relatively high in the code, it feels like it'd be a simple matter to lift it further up and supply an IO monad. The input is relatively deep, but it stills feels like it'd be simple to do here due to it being a small program.

### Board Display
Next steps: Make draw work with an NxN size board for any N. The challenge would be in making the initial space for a line variable with magnitude of N. Right now it's hardcoded for N < 100, or magnitude(n) < 3. The spacing at the start of a line should be magnitude(n) + 1, this would leave a single space before the row dividers and 2 spaces before the player mark.