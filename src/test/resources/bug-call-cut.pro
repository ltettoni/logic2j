/*
   This code has an issue when calling:

     member(E, [1,2]), existsKo1(a(E)).

   whereas this was OK (in both tuProlog and logic2j)
     member(E, [1,2]), existsOk(a(E)).

   L will be a list of [1,2,3,4].
   E will take the four solutions 1,2,3,4 in sequence
   But exists() will invoke call(P) whose CUT is processed at the level of the main goal
     (hence yielding only solution 1)
   whereas due to call/1 it should yield one solution PER invocation of exists/1 !

*/

a(1).
a(2).
a(1).
a(2).
a(2).

% with tuProlog, yields 4 solutions
% with logic2j, used to yield only one solution
existsKo1(Pred1) :- call(Pred1), !.

% This also did not work with logic2j
existsKo2(a(Elem)) :- a(Elem), !.

% This was working OK with both tuProlog and logic2j.
existsOk(Pred2) :- stub(Pred2), !.
stub(Q) :- call(Q).
