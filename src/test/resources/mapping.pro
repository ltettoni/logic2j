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

% Checking multi-solution remappings
remap(2, two).
remap(2, deux).
remap(2, zwei).


%
% Convert associative structures of AND and OR into flattened lists
%
dessoc( (A, op(and, List)), op(and, [A|List])) :- !.
dessoc( (A, B) , op(and, [A,B])).

dessoc( (A; op(or, List)), op(or, [A|List])) :- !.
dessoc( (A; B) , op(or, [A,B])).

