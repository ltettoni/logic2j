%---------------------------------------------------------------------------------------------------------------
%  Business definitions
%---------------------------------------------------------------------------------------------------------------


business_definition(tcsccategories,     ['TC', 'SC']).
business_definition(wgtypes,            ['WG','JWG','SWG']).
business_definition(coco_names,         ['ISO/CASCO','ISO/COPOLCO','ISO/DEVCO', 'ISO/REMCO', 'ISO/STACO', 'ISO/INFCO']).



%---------------------------------------------------------------------------------------------------------------
%  Business predicates ("is-a")
%---------------------------------------------------------------------------------------------------------------

coco(X)                 :- casco(X); copolco(X); devco(X); remco(X); staco(X); infco(X).

isoiec_comm(X)          :- comm_originator(X, isoiec).

comm_active(X)          :- comm_status(X, 'ACTIVE').

wg(X)                   :- comm_category(X, wgtypes).
notwgtypes(X)           :- comm_category(X, '!=', wgtypes).

notwg(X)                :- comm_numbers(X, _, _, 0).

wg_active(X)            :- wg(X), comm_active(X).

isoiec_wg_active(X)     :- isoiec_comm(X), wg_active(X).

definitionCommittee(X)  :- comm_title(X, 'Definitions'). 







/* 

  Validation is based on the predicate "validator":
  
  validator/5
    validator(dataElement, Variable, invalidityConditionsOnVariable, errorLevel, errorMessage)

  validator/4
    validator(predicateWithArgs, invalidityConditionsOnPredicateArgs, errorLevel, errorMessage)
  
*/

% VR1: Validation of a simple property, independent of context
%
%validator('MiniComm.tcNumber', N, N < 2, 'ERROR_DATA', 'TC number must not be below 2').
validator((comm_category(C, 'TC'), comm_tcnumber(C, N)), N < 2, 'ERROR_DATA', 'TC number must not be below 2').
validator('MiniComm.scNumber', N, N < 3, 'ERROR_DATA', 'SC number must not be below 3').




% VR2: Validation of a property using the semantics of the data element represented by the property
%
validator('MiniComm.scope', S, not(committeeScope(S)), 'ERROR', 'Committee scope must be a sentence of reasonable length').




% VR3: Validation of the value for a given Class (here: Number); irrelevant of which property or data element holds that value
%
validator(_, N, (number(N), N < -1000), 'WARN', 'No integer number allowed to be below -1000').


% VR3: Same as above, just making sure every date (for every data element, and every object property), is between 1900 and 2100
validator(_, Date, (date(Date), date_seq(Date, '1900-01-01')), 'ERROR', 'Any date must be after 1900').
validator(_, Date, (date(Date), date_seq('2100-01-01', Date)), 'ERROR', 'Any date must be before 2100').

validator(_, Date, date_is_sunday(Date), 'WARN', 'Sundays are not a recommended day').



% VR4: Context-dependent validation: validate a property depending on some other (predicate condition) within the same entity
%   If the sector is "Nuclear energy" the state may only be "STANDBY".
validator('MiniComm.state', State, (nuclear_energy(commandTarget), not(State='STANDBY')), 'ERROR', 'State of committees in sector of nuclear energy may only be STANDBY').

% If the type is a PDC then the reference is "${owner}/.+", otherwise generate ref using a Java class

% Difficult: if the parent is in state disbanded, any child must be in state disbanded too
% hierarchy('MiniComm.state', 


% VR5: Universe-dependent validation: validate a property depending on others such as currently in database: 
%   BRI2-607: The committee reference is unique
validator(comm_ref(C, Ref), gd3((comm_category(C2, 'SC'), comm_ref(C2, Ref2), Ref2=Ref)), 'ERROR_DATA', 'Committee reference is already used').


% VR5: Universe-dependent validation: validate a property depending on others such as currently in database
%      Same as above but include further restrictions: the title of a structure must be unique within all siblings, i.e. children of its parent,
%      with the exclusion of oneself
validator('MiniComm.title', T, (
      state(commandTarget, 'MiniComm.id', Id), 
      db((rel(Par, comm_childself, Id), rel(Par, comm_childself, Sibling), cat(Sibling, comm_title, T), rel(Id, artefact_notself, Sibling)), Matches), 
      length(Matches, N), 
      N > 0), 'ERROR', 'There is/are already a sibling committee(s) or working group(s) with the same title').


% VR6: List of values (eg. committee scopes) may depend on some other conditions (e.g. JTC 1)
% TBD



% BRI2-147: The Chairperson mandate end date cannot be in the past
% Start defining a predicate to hold the validity check
%  rel_chp_mandate_end(RelId, Date).
% Create the validator as below
validator(rel_chp_mandate_end(Relation, Date), date_seq(Date, sysdate), 'WARN_NOK', 'Relation end date must not be in the past').
% Then in "rules-bindings-state.pl", bind the predicate to a "state" assmilated fact
% Similarly in "rules-bindings-gd3.pl", bind the predicate to a database tables or views
% Then see how to invoke validation from the test cases








% Definition of Data Elements
committeeScope(S) :- reasonableSentence(S).


% Higher-level definitions that may be used to define validators
reasonableSentence(X) :- atom_length(X, L), L>20.




% -------------------------------------------------------------------------------------------
% Projections of technical sector
% -------------------------------------------------------------------------------------------

% committeeTechnicalSector(X) :- cen, !.
committeeTechnicalSector(X) :- isoiecOwned(commandTarget), X='TECHSEC_IT', !.
committeeTechnicalSector(X) :- state(commandTarget, 'CommitteeDetails.tcNum', 1), X='TECHSEC_IT', !.
committeeTechnicalSector(X) :- technicalSector(X).

technicalSector('Agriculture').
technicalSector('Ores and metals').
technicalSector('Metallurgy').


isoiecOwned(X) :- state(X, 'MiniComm.owner', Y), isoiec(Y).
isoiecOwned(X) :- state(X, 'MiniOrg.owner', Y),  isoiec(Y).
isoiecOwned(X) :- state(X, 'CommitteeDetails.owner', Y), isoiec(Y).




% -------------------------------------------------------------------------------------------
% Predicates on entities
% -------------------------------------------------------------------------------------------





%---------------------------------------------------------------------------------------------------------------
% NOT USED YET - the data type of a data element may be later needed
%---------------------------------------------------------------------------------------------------------------
data_element(org_id, number).
data_element(comm_id, number).
data_element(comm_category, varchar2).
data_element(comm_tc_number, number).
data_element(comm_sc_number, number).
data_element(comm_wg_number, number).

