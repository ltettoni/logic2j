/*
   Bindings of Rule Engine definitions and predicates to the GD3 repository.
*/



%---------------------------------------------------------------------------------------------------------------
% Equivalences (or aliases). Note that all IDs will become entity URIs, i.e. RefId
%---------------------------------------------------------------------------------------------------------------

gd3_definition(iso,        68).
gd3_definition(isoiec,     69).
gd3_definition(iec,        70).
gd3_definition(cen,    250321).

gd3_definition(casco,   54998).
gd3_definition(copolco, 55000).
gd3_definition(devco,   55004).
gd3_definition(remco,   55002).
gd3_definition(staco,   55008).
gd3_definition(infco,   55006).


% THE FOLLOWING DEFINITIONS TO BE REWORKED !!!!
% still needed but not really good?

casco(54998).
copolco(55000).
devco(55004).
remco(55002).
staco(55008).
infco(55006).
% Also still needed for assimilated facts inference - to be removed later on
casco(X)                :- comm_id(X, 54998).




%---------------------------------------------------------------------------------------------------------------
% Bindings of rule engine predicates to database tables
% Note: Can we remove the call to gd3_solve??? Issue is that probably no: number of args varies...
%       However it's a big pain, I've lost 2 hours finding why checking the existence of a mapping with
%       goal "gd3_mapping(Pred, _)" induced side effect: because of futher calles to gd3_solve.
%---------------------------------------------------------------------------------------------------------------

gd3_mapping(comm_originator(Com, V2),      
        [[tbl(re_comm_orig, comm_id, comm_id, Com2), tbl(re_comm_orig, org_id, org_id, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_category(Com, V2),  
        [[tbl(committee, id, comm_id, Com2), tbl(committee, classification, comm_category, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).
gd3_mapping(comm_category(Com, Oper, V2),  
        [[tbl(committee, id, comm_id, Com2), tbl(committee, classification, comm_category, V2R, Oper)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).
gd3_mapping(comm_numbers(Com, V2, V3, V4),         [[tbl(committee, id, comm_id, Com2), tbl(committee, tc_number, comm_tc_number, V2R), tbl(committee, sc_number, comm_sc_number, V3R), tbl(committee, wg_number, comm_wg_number, V4R)]]) :-        gd3_solve(Com, Com2), gd3_solve(V2, V2R), gd3_solve(V3, V3R), gd3_solve(V4, V4R).

gd3_mapping(comm_tcnumber(Com, Val), 
        [[tbl(committee, id, comm_id, Com2), tbl(committee, tc_number, comm_tc_number, Val2)]]) :-
        gd3_solve(Com, Com2), gd3_solve(Val, Val2).


gd3_mapping(comm_ref(Com, V2),       
        [[tbl(committee, id, comm_id, Com2), tbl(committee, reference, comm_ref, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_title(Com, V2),     
        [[tbl(committee, id, comm_id, Com2), tbl(committee, title, comm_title, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_parent(Com, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, id_parent, comm_id, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_status(Com, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, status, comm_status, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_status(Com, Oper, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, status, comm_status, V2R, Oper)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(comm_nbchildren(Com, V2), 
        [[tbl(re_comm_nbchildren, comm_id, comm_id, Com2), tbl(re_comm_nbchildren, nbr, number, V2R)]]) :- gd3_solve(Com, Com2), gd3_solve(V2, V2R).

gd3_mapping(date_seq(D1, D2), [[  D1 =< D2 ]]).


% BRI2-147: The Chairperson mandate end date cannot be in the past
gd3_mapping(rel_chp_mandate_end(Rel, Date),    
        [[tbl(relation, id, rel_id, Rel2), tbl(relation, type, rel_type, 'CHP'), tbl(relation, enddate, rel_date_end, Date2)]]) :- gd3_solve(Rel, Rel2), gd3_solve(Date, Date2).

% Check presence of a mapping for a predicate but won't execute it in case consequent part of mapping would do something.
has_gd3_mapping(Predicate) :- clause(gd3_mapping(Predicate, _), _), !.
has_gd3_mapping((A,B)) :- has_gd3_mapping(A), has_gd3_mapping(B), !.
has_gd3_mapping(_ = _).
has_gd3_mapping(_ \= _).
has_gd3_mapping(_ < _).
has_gd3_mapping(_ =< _).
has_gd3_mapping(_ >= _).
has_gd3_mapping(_ > _).


%
% Syntactic helpers to invoke the GD3 "select" predicate.
%
gd3(Goal)         :- select(gd30, Goal, normal).
gd3distinct(Goal) :- select(gd30, Goal, distinct).



%---------------------------------------------------------------------------------------------------------------
%   Validation against database
%---------------------------------------------------------------------------------------------------------------

% validateAgainstDb(DataElement) :- validator(DataElement, _, _, _, _).

%validateAgainstDb(Result) :- 
%  validator(Predicate, gd3(Condition), Severity, Message), 
%  has_gd3_mapping(Predicate),
%  log('Special GD3 condition'),
%  Result = validationResult(Predicate, 'OK', 'Ignored validation where condition is specified against database').

validateAgainstDb(Result) :- 
  validator(Predicate, Condition, Severity, Message), 
  has_gd3_mapping(Predicate),
  has_gd3_mapping(Condition),
  gd3((Predicate,Condition)), 
  Result = validationResult(Predicate, Severity, Message).

validateAgainstDb(Result) :- 
  validator(Predicate, Condition, Severity, Message), 
  has_gd3_mapping(Predicate), 
  not(has_gd3_mapping(Condition)),
  gd3(Predicate),
  Condition \= gd3(_), % Avoid going back to DB - although it works it's painful to validate refs
  call(Condition), 
  Result = validationResult(Predicate, Severity, Message).


%---------------------------------------------------------------------------------------------------------------
%   Solver of rule engine predicates to database table conditions (handle with care - not that trivial!)
%---------------------------------------------------------------------------------------------------------------

gd3_solve(Var, Var) :- var(Var), !.
gd3_solve(Expr, Res)  :- gd3_mapping(Expr, Res), !.
gd3_solve(X=Value, [[X=Value]]).
gd3_solve(X\=Value, [[X\=Value]]). % Conversion of Prolog to SQL operator done in the select/3 predicate
gd3_solve(X<Value, [[X<Value]]).
gd3_solve(X=<Value, [[X=<Value]]). % Conversion of Prolog to SQL operator done in the select/3 predicate
gd3_solve(X>=Value, [[X>=Value]]).
gd3_solve(X>Value, [[X>Value]]).
gd3_solve(Expr, Res)  :- business_definition(Expr, Tmp), gd3_solve(Tmp, Res), !.
gd3_solve(Expr, Res)  :- gd3_definition(Expr, Tmp), gd3_solve(Tmp, Res), !.
gd3_solve(Expr, Res)  :- prolog_definition(Expr, Tmp), nolog([bind_to, Tmp]), gd3_solve(Tmp, Res), !.
gd3_solve(Expr, Res)  :- Expr =.. [Pred, Arg], findall(Arg, Expr, Matches), Res=[member(Arg, Matches)], !.  % Used by coco(Com)
gd3_solve((A,B), Res) :- gd3_solve(A, RA), gd3_solve(B, RB), append(RA, RB, Res), !.
gd3_solve(Expr, Expr) :- nolog([catch_all, Expr]).







/*---------------------------------------------------------------------------------------------------------------
% Testing the gd3_solve/2 internal predicate, and gd3/1 facade predicate.

gd3_solve(68, X).
gd3_solve(Var, X).
gd3_solve(isoiec, X).

gd3_solve(comm_originator(Com, 69), X).
gd3_solve(comm_originator(Com, isoiec), X).
gd3_solve(comm_category(Com, 'WG'), X).
gd3_solve((comm_originator(Com, isoiec), comm_category(Com, 'COMACRO_WG')), X).
gd3_solve((comm_originator(Com, isoiec), comm_category(Com, 'COMACRO_WG'), comm_numbers(Com, _, 27, _)), X).

gd3(comm_originator(Com, 69)).

ISO/TC 22/SC 4/WG 2  : 46706  46778   46780 / 46782
*/
