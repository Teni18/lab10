package org.example.server;

import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ATMThread extends Thread
{
    // server vars
    protected String id;
    protected Socket socket;
    protected PrintWriter out = null;
    protected BufferedReader in = null;

    // vars to track the user trying to log in
    protected String attempted_user = null;
    protected String attempted_pass = null;

    // if the user successfully logs in, set to true to enable other commands
    protected boolean auth = false;
    protected String user = null;

    // collection of users, storing their <username, password> but their password is
    // encrypted
    protected HashMap<String, byte[]> users;
    protected HashMap<String, Integer> balances;

    /// list of possible commands

    protected final static String PWD = "PWD"; // password command
    protected final static String UID = "UID"; // username command
    protected final static String NEW = "NEW"; // new user command
    protected final static String DEP = "DEP"; // deposit command
    protected final static String WITH = "WITH"; // withdraw command
    protected final static String VIEW = "VIEW"; // view balance command
    protected final static String LOGOUT = "LOGOUT"; // logout command

    // storing the list of recognized commands
    protected final static String[] COMMANDS =
            {
            PWD,
            UID,
            NEW,
            DEP,
            WITH,
            VIEW,
            LOGOUT
    };

    // constructor
    public ATMThread(String _id, Socket _socket, HashMap<String, byte[]> _users,
                     HashMap<String, Integer> _balances)
    {
        // calling the Thread super class
        super();

        // copying over the arguments into the class
        this.id = _id;
        this.socket = _socket;
        this.users = _users;
        this.balances = _balances;

        // establishing a connection to the server
        try
        {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e)
        {
            this.err("IOException while opening a read/write connection");
        }
    }

    public void run()
    {
        // initialize interaction
        out.println("Welcome to the ATM Machine");
        out.println("100 Ready");

        // main loop
        while (processCommand())
            ;

        /// closing the thread

        // printing a closing message
        if (user != null)
        {
            this.log(user + " disconnected");
        }
        else
        {
            this.log("Client disconnected");
        }

        // closing the socket
        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * this function parses a command from the client
     *
     * @return true if the command was valid, false otherwise
     */
    protected boolean processCommand()
    {
        // the incoming command from the client
        String message;

        // trying to read a message from the client
        try
        {
            message = in.readLine();
        }
        catch (IOException e)
        {
            // there was an error, abort
            this.err("Error reading command from socket.");
            return false;
        }

        // if no command was passed to the server, ignore and continue
        if (message == null)
        {
            return true;
        }

        // parsing the message received from client, format: "CMD argument(s)"
        StringTokenizer st = new StringTokenizer(message);
        String command = st.nextToken();
        String args = null;
        if (st.hasMoreTokens())
        {
            args = message.substring(command.length() + 1);
        }

        // process command using the cmd and the arguments parsed
        return processCommand(command, args);
    }

    /**
     * Method processes the known commands:
     * - UID: receives the username
     * - PWD: checks if sent password matches the user
     * - LOGOUT: stops the thread
     *
     * @return true if the command was valid, false otherwise
     */
    protected boolean processCommand(String command, String arguments)
    {
        // uppercasing the command to feed it into a switch statement
        command = command.toUpperCase();

        // logging the command that was passed to the server
        this.log("Caught command " + command + " from the user");

        // rejecting any command the server doesn't recognize
        if (!isValidCommand(command))
        {
            out.println("404 Unrecognized Command: " + command);
            return true;
        }

        // droppping into a switch statement
        switch (command.toUpperCase())
        {
            case LOGOUT:
                // grabbing the username from the login exchange
                return logout();
            case UID:
                // grabbing the username from the login exchange
                return processUID(arguments);
            case PWD:
                // grabbing the password from the login exchange
                return processPWD(arguments);
            case NEW:
                // creating a new user
                return processNEW(arguments);
            case WITH:
                // withdrawing money from the user's account
                return processWITH(arguments);
            default:
                // if the user gets here, break and go to the auth section
                break;
        }

        // if the user is authenticated, allow for more commands
        if (auth)
        {
            this.log("Allowing authorized commands for user: " + user);

            // droppping into a switch statement
            switch (command.toUpperCase())
            {
                case VIEW:
                    // grabbing the password from the login exchange
                    return processVIEW();
                case DEP:
                    // creating a new user
                    return processDEP(arguments);
                default:
                    // the user should never get here
                    break;
            }
        }
        else
        {
            out.println("401 Unauthenticated user");
            this.log("User tried to send a command without authorization");
        }

        // base case
        return true;
    }

    /**
     * this function processes the UID command which is the username
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processUID(String argument)
    {
        // saving the username into the class variable
        attempted_user = argument;
        return true;
    }

    /**
     * this function processes the PWD command which is the password
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processPWD(String argument)
    {
        // saving the password into the class variable
        attempted_pass = argument;

        // if the user has already entered a username, attempt to log them in
        if (attempted_user != null)
        {
            return login();
        }
        else
        {
            // no username, break
            out.println("Error: No username entered before password.");
            return true;
        }
    }

    /**
     * this function processes the NEW command which is to create a new account
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processNEW(String argument)
    {
        // splitting up the argument
        StringTokenizer st = new StringTokenizer(argument);
        String username = st.nextToken();
        String password = st.nextToken();

        // check if the user already exists
        if (users.containsKey(username))
        {
            out.println("400 Username already exists");
            return true;
        }

        // encrypting the password
        byte[] encryptedPassword = encryptPassword(password);

        // saving the user's credentials
        users.put(username, encryptedPassword);
        balances.put(username, 0);

        // confirming success
        out.println("201 Created");
        return true;
    }

    /**
     * this function processes the WITH command which is to withdraw money from the user's account
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processWITH(String argument)
    {
        // Scenario 1: If the argument is null or empty, respond with the user's balance
        if (argument == null || argument.isEmpty())
        {
            return processVIEW("100");
        }

        // Scenario 2: If the argument is "BREAK", log and return true from the function
        if (argument.equalsIgnoreCase("BREAK"))
        {
            this.log(WITH + " No further requests from the user.");
            return true;
        }

        // Scenario 3: If the argument is a valid withdrawal amount
        try
        {
            int withdrawalAmount = Integer.parseInt(argument);

            // Check if the user has a balance
            Integer balance = balances.get(user);
            if (balance == null)
            {
                // Respond with 500 Internal server error if the user doesn't have a balance
                this.err("User " + user + " has no balance.");
                out.println("500 Internal server error");
                return false;
            }

            // Check if the amount is greater than the balance
            if (withdrawalAmount > balance)
            {
                // Respond with 400 Bad request if the amount is greater than the balance
                this.err("User " + user + " tried to withdraw more than their balance.");
                out.println("400 Bad request");
                return false;
            }

            // Deduct the withdrawal amount from the user's balance
            balance -= withdrawalAmount;
            balances.put(user, balance);

            // Respond with "200 <user's new balance>"
            out.println("200 " + balance);

            // Log the transaction
            this.log("Withdrawn " + withdrawalAmount + " from the balance of " + user);

            // Return true to indicate successful processing
            return true;
        }
        catch (NumberFormatException e)
        {
            // Respond with 400 Bad request if the argument is not a number
            this.err("Invalid withdrawal amount provided by user " + user);
            out.println("400 Bad request");
            return false;
        }
    }

    /**
     * this function processes the VIEW command which is to view the user's account balance
     *
     * @return true if the command was valid, false otherwise
     */
    protected boolean processVIEW()
    {
        // Get the user's balance
        Integer balance = balances.get(user);

        // Check if the user has a balance
        if (balance == null)
        {
            // Respond with 500 Internal server error if the user doesn't have a balance
            this.err("User " + user + " has no balance.");
            out.println("500 Internal server error");
            return false;
        }

        // Respond with "200 <user's balance>"
        out.println("200 " + balance);

        // Log the balance request
        this.log("Viewed balance: " + balance + " for user " + user);

        return true;
    }

    /**
     * this function processes the VIEW command which is to view the user's account balance
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processVIEW(String argument)
    {
        // Get the user's balance
        Integer balance = balances.get(user);

        // Check if the user has a balance
        if (balance == null)
        {
            // Respond with 500 Internal server error if the user doesn't have a balance
            this.err("User " + user + " has no balance.");
            out.println("500 Internal server error");
            return false;
        }

        // Respond with "200 <user's balance>"
        out.println("200 " + balance);

        // Log the balance request
        this.log("Viewed balance: " + balance + " for user " + user);

        return true;
    }

    /**
     * this function processes the DEP command which is to deposit money into the user's account
     *
     * @param argument the argument to process
     * @return true if the command was valid, false otherwise
     */
    protected boolean processDEP(String argument)
    {
        // Scenario 1: If the argument is null or empty, respond with the user's balance
        if (argument == null || argument.isEmpty())
        {
            return processVIEW("100");
        }

        // Scenario 2: If the argument is "BREAK", log and return true from the function
        if (argument.equalsIgnoreCase("BREAK"))
        {
            this.log(DEP + " No further requests from the user.");
            return true;
        }

        // Scenario 3: If the argument is a valid deposit amount
        try
        {
            int depositAmount = Integer.parseInt(argument);

            // Check if the user has a balance
            Integer balance = balances.get(user);
            if (balance == null)
            {
                // Respond with 500 Internal server error if the user doesn't have a balance
                this.err("User " + user + " has no balance.");
                out.println("500 Internal server error");
                return false;
            }

            // Add the deposit amount to the user's balance
            balance += depositAmount;
            balances.put(user, balance);

            // Respond with "200 <user's new balance>"
            out.println("200 " + balance);

            // Log the transaction
            this.log("Deposited " + depositAmount + " into the balance of " + user);

            // Return true to indicate successful processing
            return true;
        }
        catch (NumberFormatException e)
        {
            // Respond with 400 Bad request if the argument is not a number
            this.err("Invalid deposit amount provided by user " + user);
            out.println("400 Bad request");
            return false;
        }
    }

    /**
     * this function handles the user login attempt
     *
     * @return true if the login was successful, false otherwise
     */
    protected boolean login()
    {
        // if the user is already authenticated, return true
        if (auth)
        {
            return true;
        }

        // check if the user exists
        byte[] expected = users.get(attempted_user);
        if (expected == null)
        {
            out.println("403 Invalid Username/Password");
            return false;
        }

        // compare the hashes
        byte[] actual = encryptPassword(attempted_pass);
        if (!Arrays.equals(actual, expected))
        {
            out.println("403 Invalid Username/Password");
            return false;
        }

        // login success, update auth status and user
        auth = true;
        user = attempted_user;
        out.println("200 OK");
        return true;
    }

    /**
     * this function handles the user logout
     *
     * @return true if the logout was successful, false otherwise
     */
    protected boolean logout()
    {
        // if the user is not authenticated, return true
        if (!auth)
        {
            return true;
        }

        // logout success, update auth status and user
        auth = false;
        user = null;
        out.println("200 OK");
        return true;
    }

    /**
     * this function encrypts the password provided using SHA-256
     *
     * @param password the password to encrypt
     * @return the encrypted password
     */
    protected byte[] encryptPassword(String password)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException e)
        {
            // this should never happen
            e.printStackTrace();
            return null;
        }
    }

    /**
     * this function is a wrapper around System.out.println
     * it prints the thread id + the message
     *
     * @param message the message to print
     */
    protected void log(String message)
    {
        System.out.println(this.id + ": " + message);
    }

    /**
     * this function is a wrapper around System.err.println
     * it prints the thread id + the message
     *
     * @param message the message to print
     */
    protected void err(String message)
    {
        System.err.println(this.id + ": " + message);
    }

    /**
     * this function tests if the list contains target
     *
     * @param target the target to look for
     * @return true if found, false otherwise
     */
    protected boolean isValidCommand(String target)
    {
        for (String e : ATMThread.COMMANDS)
        {
            if (e.equalsIgnoreCase(target))
            {
                return true;
            }
        }
        return false;
    }
}
