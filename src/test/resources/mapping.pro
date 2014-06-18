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

remap(main(ID), eav_any(ID, classifications, 'LEVEL_MAIN_COMMITTEE')).

alias(iso_id, 68).
alias(toto, organization(iso_id)).
alias(commIso(ID), (committee(ID), owner(ID, iso_id))).

dbBinding(committee(ID),  col(committee, id)=ID).
dbBinding(organization(ID),  col(organization, id)=ID).

dbBinding(owner(ID, N),      [col(pred_owner, id)=ID, pred_owner(pred_owner, owner)=N]).
dbBinding(tcNumber(ID, N),   [col(committee, id)=ID, col(committee, tcNum)=N]).



gd3(Predicate, DbBindings) :-
        info(entry, Predicate),
        map(alias, Predicate, Simplified, 'before,iter'),
        info(after_alias, Simplified),
        map(dbBinding, Simplified, DbBindings, 'before,iter'),
        info(after_db_bindings, DbBindings).

gd3(Predicate) :- gd3(Predicate, _).
