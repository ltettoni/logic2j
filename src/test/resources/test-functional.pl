/*

  Functional testing.

*/
f(a).
f(b) :- true.
f(_).
f(c) :- true.
f(d).

a(1).
a(2).
a(3).

b(1).
b(2).
b(3).

c(1).
c(2).
c(3).


d(1).
d(2).
d(3).

% testing cut behaviour
c1(1) :- !.
c1(2).
c1(3).


c2(1).
c2(2) :- !.
c2(3).

c4 :- a(_).
c4 :- b(_), !.
c4.

d4:-c2(_).d4:-c2(_).



p(X):-q(X),X>1.
pc(X):-q(X),!,X>1.

q(0).
q(1).
q(2).
q(3).
q(4).


% The sum equivalent of factorial. To test recursivity.
sumial(0, 0) :- !.
sumial(N, R) :- N1 is N-1, sumial(N1, R1), R is N+R1.

% Special test case using this!
unifyterms(X, X).
unifyterms21(A, B) :- unifyterms(A, Tmp), unifyterms(Tmp, B).
unifyterms22(A, B) :- unifyterms(B, Tmp), unifyterms(Tmp, A).

% Don't remove - used in a test case!
unifyterms3(f(A,B), g(A2,B2)) :- unifyterms(A,A2), unifyterms(B,B2).

% Don't remove - used in a test case!
final(V) :- V=f(FinalVar).

bool_3t_2f(true).
bool_3t_2f(true).
bool_3t_2f(false).
bool_3t_2f(true).
bool_3t_2f(false).


% Testing a bug: call/1 was not dereferencing variables properly
call_over_call(Goal) :- call(Goal).   % call_over_call is a Prolog clause wrapper to Prolog goal.
call_check(1).
call_check(2).
call_check(3).




