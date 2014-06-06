/*

        Mapping of tree structures.
        
        See FunctionLibraryTest
*/
map(10, ten).
map(1, one).
map(9, notNine) :- fail.

map(t1, t2).
map(t2, t3).
map(t3, t4).

map(11, [10,1]).

map(original(X), transformed(X)).

map(main(X), eav_any(X, classifications, 'LEVEL_MAIN_COMMITTEE')).



dbBinding(tcNumber(X, N),   [col(committee, id)=X, col(committee, tcNum)=N]).

gd3(Predicate) :- 
        info(entry, Predicate),
        map(alias, Predicate, Simplified), 
        info(after_alias, Simplified),
        map(dbBinding, Simplified, DbBindings), 
        info(after_db_bindings, DbBindings).
