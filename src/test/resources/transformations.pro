
% TODO: only used by mappingTransformer(): remove

% Test mappings and transformations

contextBinding(1, one).
contextBinding(10, ten).
contextBinding(committee(X), eav(X, class, 'Committee')).
contextBinding(main(X), eav(X, classification, 'LEVEL_MAIN')).

definition(11, (1,10)).
definition(tc(X), (committee(X), main(X))).

transformForContext(X, Y) :- contextBinding(X, Y), info(simple_binding, X, Y), !.
transformForContext(X, (Y1,Y2)) :- definition(X, (X1, X2)), info(matched_definition, X, X1, X2), transformForContext(X1, Y1), transformForContext(X2, Y2), !.
transformForContext(X, X).
