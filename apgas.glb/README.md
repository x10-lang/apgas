# GLB

Lifeline-based Global Load Balancing for the Asynchroneous Partitioned Global Address Space API.

This project contains the source files for the APGAS-GLB API.

## To Do
By priority

- [x] Allow return values from the computation, folding operations 2018/4/4
- [ ] Implement a Lifeline strategy component (choice made possible at creation of the factory)

## To keep in mind

- [ ] Adapt the GLB to replicate the stream API of java.util.stream package.
- [ ] Review the apgas.util.Queue management, perhaps avoid massive array copies. Should review the addTask scheme as well.
- [ ] Multithreading the work at one place ?

## Done
- [x] Handle TaskQueue potential Overflow 2018/03/13
- [x] Allow choice of parameters for Work unit, number of random steals before lifeline use 2018/03/14
- [x] Review the beginning of the computation : Places != 0 waste time without work and need to wait for the work distribution of the place 0
