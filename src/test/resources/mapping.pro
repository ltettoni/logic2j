/*
        Mapping of tree structures.
        
        See FunctionLibraryTest
*/
% Basic transformation, and non-transformation if rule fails
remap(10, ten).
remap(1, one).
remap(9, notNine) :- fail.

% Used to test iterative transformations (t1->t2->t3->t4)
remap(t1, t2).
remap(t2, t3).
remap(t3, t4).

% Test transformations "before"
remap(f1, f(1)).
% Test transformations "after"
remap(11, [10,1]).

remap(original(X), transformed(X)).


%
% More real-life transformations
%
alias(iso_id, 68).

alias(commIso(ID), (committee(ID), owner(ID, iso_id))).

dbBinding(owner(ID, N),      [col(pred_owner, id)=ID, pred_owner(pred_owner, owner)=N]).
dbBinding(committee(ID),  col(committee, id)=ID).
dbBinding(organization(ID),  col(organization, id)=ID).
dbBinding(tcNumber(ID, N),   [col(committee, id)=ID, col(committee, tcNum)=N]).



gd3(Predicate, DbBindings) :-
        info(entry, Predicate),
        map(alias, Predicate, Simplified, 'before,iter'),
        info(after_alias, Simplified),
        map(dbBinding, Simplified, DbBindings, 'before,iter'),
        info(after_db_bindings, DbBindings).

gd3(Predicate) :- gd3(Predicate, _).
