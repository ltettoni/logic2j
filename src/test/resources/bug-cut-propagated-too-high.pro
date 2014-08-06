/*
   This code has an issue when calling:

     (E=1;E=2), existsKo1(a(E)).
     (E=1;E=2), existsKo2(a(E)).

   whereas this was OK (in both tuProlog and logic2j)
     (E=1;E=2), existsOk(a(E)).

   E will take the two solutions 1,2 sequentially
   But exists() will invoke call(P) whose CUT is processed at the level of the main goal
     (hence yielding only solution 1)
   whereas due to call/1 it should yield one solution PER invocation of exists/1 !

   Actually this was not an issue with call/1 (test case existsKo1/1)
   but an intrinsic bug with cut (test case existsKo2/1).

   See associated test case in BugRegressionTest
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
