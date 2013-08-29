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

member(E,[E|_]).
member(E,[_|L]):- member(E,L).

append([],L2,L2).
append([E|T1],L2,[E|T2]):- append(T1,L2,T2).

% Takeout element X from a list
takeout(X, [X|R], R).
takeout(X, [F|R], [F|S]) :- takeout(X,R,S).

reverse([X|Y],Z,W) :- reverse(Y,[X|Z],W).
reverse([],X,X).

reverse(A,R) :- reverse(A,[],R).

% All permutations of a list
perm(List, [H|Perm]) :- takeout(H, List, Rest), perm(Rest, Perm).
perm([], []).
  

%- not/1 is now implemented in the CoreLibrary in Java
%not(P) :- call(P), !, fail.
%not(P).

% Is this correct?
C -> T ; B  :- !, ';'((call(C), !, call(T)), call(B)).
C -> T      :- call(C), !, call(T).


/*
  This is a working version of OR implemented in prolog. 
  However we prefer the N-arity implmentation of OR in Java, see DefaultSolver.

A ; B :- call(A).
A ; B :- call(B).
*/
