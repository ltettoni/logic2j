/*
  2021-07-26 Found a serious bug with querying with the anonymous variable.
  Querying with c(_Z) will find 2 solutions.
  Querying with c(_) will find 2 x 2 solutions :-(
*/

a(1).
a(2).

b(1).
b(2).

% The "Local" variable is local to the rule, whichever it binds with a real or anonymous variable
% in the invoking context, there will be 2 solutions. Not 4.
c(Local) :- a(Local), a(Local).
