
% This simple Prolog program checks or generates change adding up to a dollar consisting of half-dollars, quarters, dimes, nickels, and pennies. 

change([H,Q,D,N,P]) :- 
    member(H,[0,1,2]),                      /* Half-dollars */ 
    member(Q,[0,1,2,3,4]),                  /* quarters     */ 
    member(D,[0,1,2,3,4,5,6,7,8,9,10]) ,    /* dimes        */ 
    member(N,[0,1,2,3,4,5,6,7,8,9,10,       /* nickels      */ 
               11,12,13,14,15,16,17,18,19,20]),  
    S is 50*H + 25*Q +10*D + 5*N, 
    S < 101, 
    P is 100-S. 
