/*
        The towers of Hanoi
*/


inform(X,Y) :- 
    write('Move top disk from '), 
    write(X), 
    write(' to '), 
    write(Y), 
    nl. 

% Choose one the following: with or without writing operations.

% move(1,X,Y,_) :- inform(X,Y).
move(1,X,Y,_).

/* 
move(N,X,Y,Z) :- 
    N>1, 
    M is N-1, 
    move(M,X,Z,Y), 
    move(1,X,Y,_), 
    move(M,Z,Y,X).  
*/

move(N,X,Y,Z) :- ','(N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X)).  


% Goal:  move(5,left,right,center).
