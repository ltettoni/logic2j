member(E,[E|_]).
member(E,[_|L]):- member(E,L).

append([],L2,L2).
append([E|T1],L2,[E|T2]):- append(T1,L2,T2).

takeout(X,[X|R],R).
takeout(X,[F|R],[F|S]) :- takeout(X,R,S).

reverse([X|Y],Z,W) :- reverse(Y,[X|Z],W).
reverse([],X,X).

reverse(A,R) :- reverse(A,[],R).

perm([X|Y],Z) :- perm(Y,W), takeout(X,Z,W).   
perm([],[]).

%- not/1 is implemented in Java
%not(P) :- call(P), !, fail.
%not(P).

