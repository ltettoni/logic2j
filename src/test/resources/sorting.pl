/*
        Various sorting algorithms

        Predicates provided by CoreLibrary:
                takeout/3, perm/2

*/




naive_sort(List, Sorted) :- perm(List, Sorted), is_sorted(Sorted).
   
is_sorted([]).
is_sorted([_]).
is_sorted([X,Y|T]) :- X=<Y, is_sorted([Y|T]).
