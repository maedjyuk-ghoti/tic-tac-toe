# Tic-Tac-Toe
A simple Tic-Tac-Toe game usable from the command line

## Motivation
Some interviewer had me make a tic-tac-toe game in 40 minutes. I struggled through it and made many bad decisions under the pressure. Here is a version that, while it isn't perfect, is much more in line with how I program and think about problems. This took 2.5 hours with the drawing taking the largest chunk of time.

## Future work
### Adjustable board size
The ground work for this is already there, it's just a matter of hooking it up so a size can be passed in from the command line instead of recompiling. This also calls for a change to the drawing so that the spacing works out with the rules on the border.

### Referential Transparency
Much output is lifted relatively high in the code, it feels like it'd be a simple matter to lift it further up and supply an IO monad. The input is relatively deep, but it stills feels like it'd be simple to do here due to it being a small program.
