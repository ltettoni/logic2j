 /*
  * This is the Prolog part of the PojoLibrary.
  */

/*
  Obviously will not be backtrackable since we mutate Java objects...
*/
javaUnify(Pojo, ListOfAssignments) :-
    member(Property=Value, ListOfAssignments),
    property(Pojo, Property, Value, update),
    fail.
javaUnify(_, _).

