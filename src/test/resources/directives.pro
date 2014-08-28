/*
    Test the parsing and execution of directives.
*/

directiveFileLoaded :- true.

substituted(${env.Path}).
substituted(${jvm.user.dir}).

:- write(thisShouldNotBeExecutedButWill), nl.


:- consult('src/test/resources/test-data.pro').

:- initialization(myInit).

myInit :- write(myInitWasCalled), nl, substituted(X), write(X), nl.
