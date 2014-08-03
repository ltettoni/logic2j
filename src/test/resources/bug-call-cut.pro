/*
   This code has an issue when calling:

     setof(X,a(X), L), member(E, L), exists(a(E)).

   whereas this was OK (in both tuProlog and logic2j)
     setof(X,a(X), L), member(E, L), existsOk(a(E)).


   L will be a list of [1,2,3,4].
   E will take the four solutions 1,2,3,4 in sequence
   But exists() will invoke call(P) whose CUT is processed at the level of the main goal
     (hence yielding only solution 1)
   whereas due to call/1 it should yield one solution PER invocation of exists/1 !


*/

a(1).
a(2).
a(3).
a(2).
a(4).
a(2).

% with tuProlog, yields 4 solutions
% with logic2j, used to yield only one solution
exists(P) :- call(P), !.


% This was working with both tuProlog and logic2j.
existsOk(P) :- stub(P), !.
stub(Q) :- call(Q).
