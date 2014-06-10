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

remap(11, [10,1]).

remap(original(X), transformed(X)).

remap(main(X), eav_any(X, classifications, 'LEVEL_MAIN_COMMITTEE')).



dbBinding(tcNumber(X, N),   [col(committee, id)=X, col(committee, tcNum)=N]).

gd3(Predicate) :- 
        info(entry, Predicate),
        map(alias, Predicate, Simplified),
        info(after_alias, Simplified),
        map(dbBinding, Simplified, DbBindings),
        info(after_db_bindings, DbBindings).
