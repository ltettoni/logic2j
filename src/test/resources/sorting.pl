delete(X,[X|T],T).
delete(X,[H|T],[H|NT]):-delete(X,T,NT).

perm(List,[H|Perm]):-delete(H,List,Rest),perm(Rest,Perm).
perm([],[]).
