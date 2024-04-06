# Implementing Functionality of the Client

Here, you'll implement:

- `withdrawMoney()`
- `createNewAccount()`

There are also comments in the `ATMClient.java` file as well.
This lab also revolves around sending [HTTP status codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status) (although not using HTTP but websockets).

## Functions

These are boolean functions that should always return true, this is because of the main loop which continuously reads
user input/commands and processes them. If any of the functions return false, this means that the user is trying
to exit the program.

Some useful functions that you should use to implement the functions are:

- `getStatusCode(string)`
- `getStatusMessage(string)`
- `Integer.valueOf()`
- `tryReadInput()`

Some exceptions you'll need to handle:

- `NumberFormatException`
- `IOException`

### `createNewAccount()`

In this function, the program should prompt the user for a username and password. The function should reject if either
the username or password is blank, a single space or null and keep looping.

If either the username or password is `'q'`, the break from the function, returning `true`.

If the input is valid, then send a request of the following format to the server:

```sh
NEW <username> <password>
```

Possible responses from the server:

- on success, the server will return `201 Created`
  - you should print a small message saying that the account was successfully created
- on failure, the server will return `400 Username or password is invalid`
  - you should print a small message saying there was an error and the reason why
- on the server side, if there is an error during this process, the server will return `500 Internal server error`
  - you should print a small message saying there was an error and the reason why
- if there was an error reading a message from the server
  - you should print a small message saying there was an error and a stack trace

You'll need to handle all of above cases.

### `withdrawMoney()`

This function should have the following flow:

1. send an initial request to the server of form `WITH`
2. the server should then respond with `100 <the balance>`
   1. if the status code of this response isn't `100`
      1. print out a small error message to `STDOUT` with the reason given
   2. if the status code of this response **is** `100`
      1. print out a small message displaying `<the balance>`
3. if the balance itself is `0`
   1. print out a small message displaying that the user cannot withdraw any money and return true
4. after this, you'll then prompt the user for how much money they would like to withdraw
   1. if the user inputs `'q'`, send `WITH BREAK` to the server and break from the function, returning true
5. if the input is a number that is less or equal to the balance given by the server
   1. send a request of form `WITH <amount>` to the server

Possible responses from the server:

- on success, the server will return `200 <new balance>`
  - you should print a small message displaying the new balance sent by the server
- on failure, the server will return `400 Bad request`
  - you should print a small message saying there was an error and the reason why
- on the server side, if there is an error during this process, the server will return `500 Internal server error`
  - you should print a small message saying there was an error and the reason why
- if there was an error reading a message from the server
  - you should print a small message saying there was an error and a stack trace

You'll need to handle all of above cases.
