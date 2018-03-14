# GLB

Lifeline-based Global Load Balancing for the Asynchroneous Partitioned Global Address Space API.

This project contains the source files for the APGAS-GLB API.

## To Do
By priority

- [ ] Allow return values from the computation / folding operations
- [ ] Implement a Lifeline strategy component (choice made possible at creation of the factory)
- [ ] Review the beginning of the computation : Places != 0 waste time without work and need to wait - for the work distribution of the place 0

## To keep in mind for later

- [ ] Adapt the GLB toreplicate the stream API of java.util.stream package.
- [ ] Review the TaskQueue management, perhaps avoid massive arrays copies ?
- [ ] Multithreading the work at one place ?

## Done
- [x] Handle TaskQueue potential Overflow 2018/03/13
- [x] Allow choice of parameters for Work unit, number of random steals before lifeline use 2018/03/14