# Lab 10 - ATM Client

>Course: CSCI 2020U: Software Systems Development and Integration

[![git](https://badgen.net/badge/icon/git?icon=git&label)](https://git-scm.com)

This is java only lab based off of the [ChatServerDemo](https://github.com/OntarioTech-CS-program/W23-LectureExamples) lecture.

## Overview

In this lab, you'll need to implement the functionality of these functions:

In `ATMThread.java`:

- `processWITH(String)` this function withdraws money from a user's account
  - in reality, this deducts the amount passed by the user from the `balances` hashmap

In `ATMClient.java`:

- `withdrawMoney()` this function reads user input and sends a request to the server to withdraw $x$ amount of money
- `createNewAccount()` this function creates a new account on the server

## Lab Work

See each subdirectory for instructions on what to do.

```dir
src/main/java/org/example
   client/
      readme.md
      ATMClient.java
   server/
      readme.md
      ATMServer.java
      ATMThread.jva
```
