/*

        Mapping of tree structures.
        
*/
map(10, ten).
map(1, one).
map(9, notNine) :- fail.

map(11, [10,1]).

map(original(X), transformed(X)).
