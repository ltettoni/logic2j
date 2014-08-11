/*
        Various sorting algorithms.
        Resources from http://ktiml.mff.cuni.cz/~bartak/prolog/sorting.html

        Predicates provided by CoreLibrary:
                takeout/3, perm/2

*/



% Naive sort uses the generate and test approach to solving problems which is usually utilized 
% in case when everything else failed. However, sort is not such case.

naive_sort(List, Sorted) :- perm(List, Sorted), is_sorted(Sorted).
   
is_sorted([]).
is_sorted([_]).
is_sorted([X,Y|T]) :- X=<Y, is_sorted([Y|T]).




% Insert sort is a traditional sort algorithm. Prolog implementation of insert sort is based on idea of accumulator.

insert_sort(List,Sorted):-i_sort(List,[],Sorted).
i_sort([],Acc,Acc).
i_sort([H|T],Acc,Sorted):-insert(H,Acc,NAcc),i_sort(T,NAcc,Sorted).
   
insert(X,[Y|T],[Y|NT]):-X>Y,insert(X,T,NT).
insert(X,[Y|T],[X,Y|T]):-X=<Y.
insert(X,[],[X]).




% Bubble sort is another traditional sort algorithm which is not very effective. Again, we use accumulator to implement bubble sort.

bubble_sort(List,Sorted):-b_sort(List,[],Sorted).
b_sort([],Acc,Acc).
b_sort([H|T],Acc,Sorted):-bubble(H,T,NT,Max),b_sort(NT,[Max|Acc],Sorted).
   
bubble(X,[],[],X).
bubble(X,[Y|T],[Y|NT],Max):-X>Y,bubble(X,T,NT,Max).
bubble(X,[Y|T],[X|NT],Max):-X=<Y,bubble(Y,T,NT,Max).




% Merge sort

merge_sort([],[]).     % empty list is already sorted
merge_sort([X],[X]).   % single element list is already sorted
merge_sort(List,Sorted):-
    List=[_,_|_],divide(List,L1,L2),     % list with at least two elements is divided into two parts
        merge_sort(L1,Sorted1),merge_sort(L2,Sorted2),  % then each part is sorted
        merge(Sorted1,Sorted2,Sorted).                  % and sorted parts are merged
merge([],L,L).
merge(L,[],L):-L\=[].
merge([X|T1],[Y|T2],[X|T]):-X=<Y,merge(T1,[Y|T2],T).
merge([X|T1],[Y|T2],[Y|T]):-X>Y,merge([X|T1],T2,T).

divide(L,L1,L2):-even_odd(L,L1,L2).
even_odd(L,E,O):-odd(L,E,O).
   
odd([],[],[]).
odd([H|T],E,[H|O]):-even(T,E,O).
   
even([],[],[]).
even([H|T],[H|E],O):-odd(T,E,O).





% Quick sort is one of the fastest sort algorithms. However, its power is often overvalued.
% The efficiency of quick sort is sensitive to choice of pivot which is used to distribute list into two "halfs".

quick_sort(List,Sorted):-q_sort(List,[],Sorted).
q_sort([],Acc,Acc).
q_sort([H|T],Acc,Sorted):-
        pivoting(H,T,L1,L2),
        q_sort(L1,Acc,Sorted1),q_sort(L2,[H|Sorted1],Sorted).
pivoting(H,[],[],[]).
pivoting(H,[X|T],[X|L],G):-X>H,pivoting(H,T,L,G).
pivoting(H,[X|T],L,[X|G]):-X=<H,pivoting(H,T,L,G).


