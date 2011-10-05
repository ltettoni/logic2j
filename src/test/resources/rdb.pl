/*
solve(68, X).
solve(Var, X).
solve(isoiec, X).

solve(comm_originator(Com, 68), X).
solve(comm_originator(Com, isoiec), X).
solve(comm_category(Com, 'WG'), X).
solve((comm_originator(Com, isoiec), comm_category(Com, 'WG')), X).
solve((comm_originator(Com, isoiec), comm_category(Com, 'WG'), comm_numbers(Com, _, 27, _)), X).
solve(isoiec_comm(Com), X).

ISO/TC 22/SC 4/WG 2  : 46706  46778   46780 / 46782


*/
a(1).
a(2).
a(2,3).
a(X,Y,Z) :-  b(Z,Y,X).

preds(Functor,P) :- P =.. [Functor].
preds(Functor,P) :- P =.. [Functor,P1].
preds(Functor,P) :- P =.. [Functor,P1,P2].
preds(Functor,P) :- P =.. [Functor,P1,P2,P3].

% preds(a,X), clause(X, Z), Z\=true, assertz(eq(X,Z)).






% IDs of some major entities
data_binding(iso, 68).
data_binding(isoiec, 69).

data_binding(coco_names, ['ISO/CASCO','ISO/COPOLCO','ISO/DEVCO', 'ISO/REMCO', 'ISO/STACO', 'ISO/INFCO']).
data_binding(wgtypes, ['WG','JWG','SWG']).     % Some types of WGs
data_binding(isoiec_comm(V1), comm_originator(V1, isoiec)).
data_binding(wg(X), comm_category(X, wgtypes)).
data_binding(comm_active(X), comm_status(X, 'ACTIVE')).
data_binding(wg_active(X), (wg(X), comm_active(X))).
data_binding(isoiec_wg_active(X), (isoiec_comm(X), wg_active(X))).
data_binding(coco(X), comm_ref(X, coco_names)).


data_binding(notwg(V1), comm_numbers(V1, _, _, 0)).
data_binding(notwgtypes(X), comm_category(X, '!=', wgtypes)).

predicate2table(comm_originator(Com, V2),      
        [[tbl(re_comm_orig, comm_id, comm_id, Com2), tbl(re_comm_orig, org_id, org_id, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_category(Com, V2),  
        [[tbl(committee, id, comm_id, Com2), tbl(committee, classification, comm_category, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).
predicate2table(comm_category(Com, Oper, V2),  
        [[tbl(committee, id, comm_id, Com2), tbl(committee, classification, comm_category, V2R, Oper)]]) :- solve(Com, Com2), solve(V2, V2R).
predicate2table(comm_numbers(Com, V2, V3, V4), 
        [[tbl(committee, id, comm_id, Com2), tbl(committee, tc_number, comm_tc_number, V2R), tbl(committee, sc_number, comm_sc_number, V3R), tbl(committee, wg_number, comm_wg_number, V4R)]]) :-
        solve(Com, Com2), solve(V2, V2R), solve(V3, V3R), solve(V4, V4R).

predicate2table(comm_tcnumber(Com, Val), 
        [[tbl(committee, id, comm_id, Com2), tbl(committee, tc_number, comm_tc_number, Val2)]]) :-
        solve(Com, Com2), solve(Val, Val2).


predicate2table(comm_ref(Com, V2),       
        [[tbl(committee, id, comm_id, Com2), tbl(committee, reference, comm_ref, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_title(Com, V2),     
        [[tbl(committee, id, comm_id, Com2), tbl(committee, title, comm_title, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_parent(Com, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, id_parent, comm_id, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_status(Com, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, status, comm_status, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_status(Com, Oper, V2),    
        [[tbl(committee, id, comm_id, Com2), tbl(committee, status, comm_status, V2R, Oper)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(comm_nbchildren(Com, V2), 
        [[tbl(re_comm_nbchildren, comm_id, comm_id, Com2), tbl(re_comm_nbchildren, nbr, number, V2R)]]) :- solve(Com, Com2), solve(V2, V2R).

predicate2table(date_seq(D1, D2), [[  D1 =< D2 ]]).


% BRI2-147: The Chairperson mandate end date cannot be in the past
predicate2table(rel_chp_mandate_end(Rel, Date),    
        [[tbl(relation, id, rel_id, Rel2), tbl(relation, type, rel_type, 'CHP'), tbl(relation, enddate, rel_date_end, Date2)]]) :- solve(Rel, Rel2), solve(Date, Date2).






data_element(org_id, number).
data_element(comm_id, number).
data_element(comm_category, varchar2).
data_element(comm_tc_number, number).
data_element(comm_sc_number, number).
data_element(comm_wg_number, number).


%
% Solver
%
solve(Var, Var) :- var(Var), !.

solve(Expr, Res)  :- predicate2table(Expr, Res), !.
solve(Expr, Res)  :- data_binding(Expr, Tmp), solve(Tmp, Res), !.
solve((A,B), Res) :- solve(A, RA), solve(B, RB), append(RA, RB, Res), !.
% solve((A,B), Res) :- solve(A, RA), solve(B, RB), Res = [RA|RB], !.

solve(Expr, Expr).


gd3(Goal) :- select(gd30, Goal).
gd3distinct(Goal) :- select(gd30, Goal, distinct).

