# GLB

Lifeline-based Global Load Balancing for the Asynchroneous Partitioned Global Address Space API.

This project contains the source files for the APGAS-GLB API.

## To Do
By priority

- [ ] Implement a Lifeline strategy component (choice made possible at creation of the factory)
- [ ] Create a balance mechanism to account for tasks of different nature such that the places can perform their steals regularly
- [/] Allow return values from the computation / folding operations

## To keep in mind

- [ ] Adapt the GLB to replicate the stream API of java.util.stream package.
- [ ] Review the TaskQueue management, perhaps avoid massive arrays copies ?
- [ ] Multithreading the work at one place ?

## Done
- [x] Handle TaskQueue potential Overflow 2018/03/13
- [x] Allow choice of parameters for Work unit, number of random steals before lifeline use 2018/03/14
- [x] Review the beginning of the computation : Places != 0 waste time without work and need to wait for the work distribution of the place 0
