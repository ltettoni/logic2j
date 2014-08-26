/*
    Test the parsing and execution of directives.

*/

directiveFileLoaded :- true.

:- write(thisShouldNotBeExecuted).

:- consult('src/test/resources/test-data.pro').

:- initialization(myInit).

myInit :- write(myInitWasCalled).
