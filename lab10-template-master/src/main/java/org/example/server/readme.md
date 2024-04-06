# Implementing Functionality of the Server

Here, you'll implement:

- `processWITH()`

There are also comments in the `ATMThread.java` file as well.
This lab also revolves around sending [HTTP status codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status) (although not using HTTP but websockets).

>This is not an example of a RESTful API since the thread keeps state (authentication state), in practice, you'd use cookies or a
>JIT token.

## Functions

These are boolean functions that should always return true, this is because of the main loop which continuously reads
user input/commands and processes them. If any of the functions return false, this means that there was an internal server error namely, something that should exist, but doesn't.

Some useful functions that you should use to implement the `processWITH(String)` are:

- `Integer.valueOf(String)`
- `HashMap.get(Object)`
- `HashMap.put(Object, Object)`
- `this.err(String)` - this prefixes the error with the name of the thread
- `this.log(String)` - this prefixes the output with the name of the thread

Some exceptions you'll need to handle:

- `NumberFormatException`
- `IOException`

### `processWITH(String)`

You'll need to handle three possible requests each with their own form, if the server encounters...

- `WITH` then the function should return the result of `processView("100")`
- `WITH BREAK` then the function should log that there aren't any more requests and return true
- `WITH <amount>` then try to deduct `<amount>` from the user's balance (a hashmap) and return true

If...

- the user doesn't have a balance, respond with `500 Internal server error` and return false
- `<amount>` isn't a number, respond with `400 Bad request` and return true
- `<amount>` is greater than the balance, respond with `400 Bad request` and return true

Otherwise, respond to the client with `200 <user's new balance>` and return true.

## Extra Reading

In the `processNEW()` function and even in `ATMServer.java`, you'll see that there is an encryption algorithm involved, MD5.

You should never, under any circumstance, store passwords in plaintext (I'm guilty of having a `passwords.txt` file).
In the industry, you should never store passwords as plaintext as if there's a data breach, there's no work for the
hackers to do, you've done the work for them.
Users are also guilty of reusing passwords for other websites (I've personally done this too, don't do this either).

>Use a password manager like [KeePassXC](https://keepassxc.org/), this is the one that I personally use.

In this lab, you'll see that the passwords are stored as MD5 hashes (in java, `byte[]` objects).
In practice, you should use something like SHA256 or SHA512 or another secure cryptographic algorithm.
MD5 has been ruthlessly broken and beaten over time.

>You cannot directly compare `byte[]` objects in java, hence why the `isSameHash(byte[], byte[])` exists.

It's an asset to do this for your employer since if there are hacked and the passwords are stored in plaintext, they
will be held responsible. Wouldn't it be nice if they had someone to cover this or someone that's knowledgeable?
