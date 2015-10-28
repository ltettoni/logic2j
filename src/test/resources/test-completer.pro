/*

        Test the autocompleter.
        
*/

% Predicates a, b, c, d will each have 3 numeric solutions. This is to test the Solver and cut behaviour.
a(1).
a(2).
a(3).


b('These').
b(are).
b('special''s').


ab(1,11).
ab(2,12).
ab(3,13).
ab(4,14).
ab(5,15).
ab(6,16).

ac(1,11).
ac(2,twelve).
ac(3,13).
ac(4,fourteen).
ac(5,15).
ac(6,sixteen).

txt('One').
txt('Once').
txt('Two').
