/*

        Functional testing. These predicates are referenced by TestCases: do not alter.
        
*/
f(a).
f(b) :- true.
f(_).
f(c) :- true.
f(d).

% Predicates a, b, c, d will each have 3 numeric solutions. This is to test the Solver and cut behaviour.
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

% -----------------------------
% More testing of cut behaviour
% -----------------------------

% Cut after 1
cut1(1) :- !.
cut1(2).
cut1(3).


% Cut after 2
cut2(1).
cut2(2) :- !.
cut2(3).

% Cut after 4
cut4 :- a(_).       % 3 solutions
cut4 :- b(_), !.    % Plus one
cut4.

cut4b:-cut2(_).cut4b:-cut2(_).


p(X)  :- int5(X), X>1.
pc(X) :- int5(X), !, X>1.

int5(0).
int5(1).
int5(2).
int5(3).
int5(4).

int5_rule(X) :- int5(X).


% The sum equivalent of factorial. To test recursivity without oveflowing numeric values.
sumial(0, 0) :- !.
sumial(N, R) :- N1 is N-1, sumial(N1, R1), R is N+R1.

% Special test case using this
unifyterms(X, X).
unifyterms21(A, B) :- unifyterms(A, Tmp), unifyterms(Tmp, B).
unifyterms22(A, B) :- unifyterms(B, Tmp), unifyterms(Tmp, A).

% Don't remove - used in a test case
unifyterms3(f(A,B), g(A2,B2)) :- unifyterms(A,A2), unifyterms(B,B2).

% Don't remove - used in a test case!
final(V) :- V=f(FinalVar).

% Boolean perdicates with sequential solutions: true, true, false, true, false
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
