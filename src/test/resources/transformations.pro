
% TODO: only used by test case : mappingTransformer() in HigherLevelTest : remove

% Test mappings and transformations

contextBinding(1, one).
contextBinding(10, ten).
contextBinding(committee(X), eav(X, class, 'Committee')).
contextBinding(main(X),      eav(X, classification, 'LEVEL_MAIN')).

definition(11, (1,10)).
definition(tc(X), (committee(X), main(X))).

transformForContext(X, Y) :- contextBinding(X, Y), info(transformForContext, simple_binding, X, into, Y), !.
transformForContext(X, (Y1,Y2)) :- definition(X, (X1, X2)), info(transformForContext, matched_definition, X, into, X1, X2), transformForContext(X1, Y1), transformForContext(X2, Y2), info(gives, Y1, Y2), !.
% catch all: no transformation
transformForContext(X, X) :- info(catch_all, X).
