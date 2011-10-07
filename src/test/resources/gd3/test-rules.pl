/*
   Rules used in test cases.

*/

% Testing projections: unconditional, constant, state-independent list of values
rgbColor(red).
rgbColor(green).
rgbColor(blue).

% Testing projections: values depend on application scope (ISO or CEN, prod, test or dev).
searchableState('Being created') :- cen ; administrator.
searchableState('Active').
searchableState('Standby').
searchableState('Withdrawn').

% Testing projections: roles available depend on many things...
availableComRole(chp) :- comm_active(commandTarget).
availableComRole(sec) :- true.
availableComRole(sst) :- isoiec_comm(commandTarget).
availableComRole(mbr) :- comm_category(commandTarget, 'TC').
availableComRole(obs) :- iso.
availableComRole(cib) :- test.


