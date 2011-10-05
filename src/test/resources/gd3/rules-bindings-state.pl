/*
   Bindings between rules definitions (Prolog predicates) to the Prolog working memory,
   where facts are assimilated to represent the state of Environment, Application, 
   Session, Conversation and Request scopes.
*/


%---------------------------------------------------------------------------------------------------------------
% Equivalences (or aliases).
%---------------------------------------------------------------------------------------------------------------

dev   :- state(system, platform, dev).
test  :- state(system, platform, test).
train :- state(system, platform, train).
prod  :- state(system, platform, prod).

iso   :- gd3_definition(iso, X), state(system,customer,X).
cen   :- gd3_definition(cen, X), state(system,customer,X).

administrator :- state(session,role,administrator).




%---------------------------------------------------------------------------------------------------------------
% Mapping of predicates to state-assimilated facts
%---------------------------------------------------------------------------------------------------------------

state_mapping(comm_status(Com, Value),            state(Com, 'MiniComm.state', Value)).
state_mapping(comm_category(Com, Value),          state(Com, 'MiniComm.category', Value)).
state_mapping(comm_originator(Com, Value),        state(Com, 'MiniComm.owner', Value)).
state_mapping(comm_ref(Com, Value),               state(Com, 'MiniComm.display', Value)).
state_mapping(comm_title(Com, Value),             state(Com, 'MiniComm.title', Value)).
state_mapping(comm_id(Com, Value),                state(Com, 'MiniComm.id', Value)).

% BRI2-147: The Chairperson mandate end date cannot be in the past
state_mapping(rel_chp_mandate_end(Rel, Date),     (state(Rel, 'MiniRelation.endDate', Date), state(Rel, 'MiniRelation.type', 'CHP'))).




%---------------------------------------------------------------------------------------------------------------
%   Solver of rule engine predicates to state-assimilated facts (handle with care - not that trivial!)
%   Note: This is still work in progress!
%---------------------------------------------------------------------------------------------------------------

state_solve(true) :- !.
%state_solve(G)         :- log([enter_state_solve, G]), fail.
state_solve(G)          :- call(G), nolog([state_solve_call, G]).                 % Are these two redundant???? If call() succeeds, the body is proven?
state_solve(G)          :- clause(G, Body), nolog([state_solve_clause, G, Body]), state_solve(Body).
state_solve(G)          :- prolog_definition(G, Tmp), nolog([state_solve_prolog_definition, G, Tmp]), state_solve(Tmp).
state_solve(G)          :- state_mapping(G, G2), nolog([state_solve_mapping, G, G2]), state_match(G2).
state_solve((G1,G2))    :- state_solve(G1), state_solve(G2).
state_solve((G1;G2))    :- state_solve(G1); state_solve(G2).

state_match(state(Entity, Property, Def))   :- business_definition(Def, Value), state_match(state(Entity, Property, Value)), !.
state_match(state(Entity, Property, Def))   :- gd3_definition(Def, Value), state_match(state(Entity, Property, Value)), !.
state_match(state(Entity, Property, Value)) :- not(list(Value)), state(Entity, Property, Value).
state_match(state(Entity, Property, List))  :- list(List), state(Entity, Property, Value), log([matching_list_of_values, List, with, Value]), member(Value, List).



% Binding expressed directly in the Prolog Theory, mainly used to solve equivalences such as naming a constant for a fixed list, etc.
prolog_definition(From, To) :- clause(From, To), To\=(_;_).



%---------------------------------------------------------------------------------------------------------------
%   Evaluate validators (all against the state-assimilated facts).
%---------------------------------------------------------------------------------------------------------------

evalValidator(DataElement, Severity, Message) :- 
  state(commandTarget,DataElement, Value), 
  validator(DataElement, Value, Condition, Severity, Message), 
  call(Condition).

evalValidator(DataElement, Severity, Message) :- 
  state(Predicate), 
  validator(Predicate, Condition, Severity, Message), 
  call(Condition),
  Predicate =.. [DataElement|_].

% Should be refactored
evalValidator(DataElement, Severity, Message) :- 
  state_mapping(Predicate, StateCondition),
  call(StateCondition),
  validator(Predicate, ValidationCondition, Severity, Message), 
  call(ValidationCondition),
  Predicate =.. [DataElement|_].

% Avoid the findall - list solutions from Java
executeAllValidators(Results) :- findall(
   validationResult(DataElement, Severity, Message),     % We will return structures of the shape "validationResult(...)"
   evalValidator(DataElement, Severity, Message),        % The expression we want to find all matches
   Results).


