/*

        Mapping of tree structures.
        
        See FunctionLibraryTest
*/
remap(10, ten).
remap(1, one).
remap(9, notNine) :- fail.

remap(t1, t2).
remap(t2, t3).
remap(t3, t4).

remap(f1, f(1)).

remap(11, [10,1]).

remap(original(X), transformed(X)).

remap(main(X), eav_any(X, classifications, 'LEVEL_MAIN_COMMITTEE')).


dbBinding(committee(ID),  col(committee, id)=ID).

dbBinding(tcNumber(X, N),   [col(committee, id)=X, col(committee, tcNum)=N]).


gd3(Predicate, DbBindings) :-
        info(entry, Predicate),
        map(alias, Predicate, Simplified, 'before'),
        info(after_alias, Simplified),
        map(dbBinding, Simplified, DbBindings, 'before'),
        info(after_db_bindings, DbBindings).

gd3(Predicate) :- gd3(Predicate, _).
