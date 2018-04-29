/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

 /*
  * This is the Prolog part of the CoreLibrary. It defines commonly-used predicates.
  */

once(Predicate) :- call((Predicate, !)).

% Non-efficient implementation - could be done in Java we have the method in TermApi
list([]).
list([_|L]) :- list(L).

member(E,[E|_]).
member(E,[_|L]):- member(E,L).

append([],L2,L2).
append([E|T1],L2,[E|T2]):- append(T1,L2,T2).

% Takeout element X from a list
takeout(X, [X|R], R).
takeout(X, [F|R], [F|S]) :- takeout(X,R,S).

% Delete list elements form a list
deletelist(AllElements,ToRemove,RemainingElements) :- findall(A, ( member(A,AllElements), \+(member(A,ToRemove)) ), RemainingElements).


reverse([X|Y],Z,W) :- reverse(Y,[X|Z],W).
reverse([],X,X).

reverse(A,R) :- reverse(A,[],R).

% All permutations of a list
perm(List, [H|Perm]) :- takeout(H, List, Rest), perm(Rest, Perm).
perm([], []).


% Implication (see note below regarding the implementation of OR (;))
% if-then-else   -   Watch out: due to precedence the rule head is actually ((C->T) ; B), and it clearly conflicts with the definition of A ; B
Cond -> Then ; Else  :- ! /* The CUT will prevent backtracking to hit the rules for (A ; B) */ ,  ( (call(Cond), !, call(Then)) ; call(Else) ).

% if-then
Cond -> Then   :- call(Cond), !, call(Then).





/*
  This is a working version of OR implemented in prolog.
  WATCH OUT: Define the OR predicate AFTER other more complex clauses that may pattern match on it,
  such as the definition of '->' see above. Also a "!" is needed in the above definition to not backtrack
  to this definition. Mhh, not so good.

  Generally I would prefer the more efficient and more general N-arity implementation of OR in Java, in DefaultSolver,
  however there is ONE case in which this can't work: the definition of the implication predicate '->' is such that
  the head is (C->T);B and if we don't rely on head matching but rather implement ";" directly in the Solver, this rule
  won't be picked-up and '->' will not work.
*/
A ; B :- call(A).
A ; B :- call(B).


% not/1 is now implemented as a Java Primitive in the CoreLibrary
% (but the implementation below works as well)
%not(P) :- call(P), !, fail.
%not(P).
