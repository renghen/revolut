# revolut

## work done 

1. kotlin was used as langueage of choice
2. stm used to implement account balance
   the lib used is called scala-stm and is lock free
3. Bank has accounts
   There is a tight coupling between the bank and account, it is built such that there may be multiple banks
4. http4k is used for http interface
    a light weight http framework
5. concurrent test using completabl futures but not for http side

## todo
1. ~concurrent test for http~
2. ~implement multiple banks~
3. ~transfer across bank account
