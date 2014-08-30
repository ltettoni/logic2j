/*
    Test the parsing and execution of directives.
*/

% A simple fact to check that the file was properly loaded in a unit test assertion
directiveFileLoaded :- true.

% These two facts will actually have their argument replaced by effective JVM or system (process) properties
substituted('env.path',      ${env.Path}).
substituted('jvm.user.dir',  ${jvm.user.dir}).

:- write('A directive was executed (although in classic Prolog it would not)'), nl.

% Thread-local variables
:- bind('thread.toto', Z),   write('Thread-bound value of toto before: ', Z), nl.
:- bind('thread.toto', 123), write('Z has been bound to 123'), nl.
:- bind('thread.toto', Z),   write('Thread-bound value of toto after : ', Z), nl.
:- bind('thread.toto', 123), write('Z is indeed 123'), nl.
:- bind('thread.toto', 124), write('!!!!!! Should not be here because Z is not 124'), nl.

% Regular variables can be bound and read
:- bind(regularVar, valueOfRegularVar).
substituted(regularVar,  ${regularVar}).


% Initialization goal will be executed after the whole theory is loaded

:- initialization(myInit).

myInit :-
    write('Execution of myInit custom predicate'), nl,
    reportSubstitutedPredicate.

reportSubstitutedPredicate :-
    write('Substituted variables:'), nl,
    substituted(Name, Value), write('  ', Name, '  =  ', Value), nl.
