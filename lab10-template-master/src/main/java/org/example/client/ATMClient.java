package org.example.client;

import java.io.*;
import java.net.*;
import java.util.*;

public class ATMClient
{
    private Socket socket;
    private PrintWriter networkOut;
    private BufferedReader networkIn;
    private BufferedReader inputKeyboard = null;

    // we can read this from the user too
    public static String SERVER_ADDRESS = "localhost";
    public static int SERVER_PORT = 16789;

    // authentication status
    // it's fine if the user manipulates this variable since the server also handles
    // authentication
    // this is just used to limit the commands available to the user
    boolean auth = false;

    public ATMClient()
    {
        /// connecting to the Server
        try
        {
            // trying to connect to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        }
        catch (UnknownHostException e)
        {
            // catching connection errors
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        }
        catch (IOException e)
        {
            // catching connection errors
            System.err.println("IOException while connecting to server: " + SERVER_ADDRESS);
        }

        // aborting if we couldn't establish a connection
        if (socket == null)
        {
            System.err.println("socket is null");
            System.exit(1);
        }

        // get in and outputstream from the socket/connection
        try
        {
            networkOut = new PrintWriter(socket.getOutputStream(), true);
            networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e)
        {
            System.err.println("IOException while opening a read/write connection");
            System.exit(1);
        }

        // user will be typing the command
        inputKeyboard = new BufferedReader(new InputStreamReader(System.in));

        /// reading initial response from the server
        try
        {
            // reading the first two messages
            System.out.println(networkIn.readLine()); // Welcome to chat
            if (getStatusCode(networkIn.readLine()) != 100)
            {
                System.out.println("Incorrect greeting from server, aborting");
                System.exit(1);
            }
        }
        catch (IOException e)
        {
            // should break since there is an error
            System.err.println("Error reading initial greeting from socket.");
            System.exit(1);
        }

        // processing commands
        while (processUserInput())
            ;

        // aborting program, close the socket
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
     * this function processes user input, if a command returns false, break out of
     * the parent loop
     *
     * @see #login()
     * @see #createNewAccount()
     * @see #logout()
     * @see #viewBalance()
     * @see #depositMoney()
     * @see #withdrawMoney()
     * @return true on command success, false otherwise
     */
    protected boolean processUserInput()
    {
        // print the menu
        System.out.println("Commands: ");
        System.out.println("1 - Login");
        System.out.println("2 - Create new account");
        System.out.println("3 - (Q)uit");

        // printing more commands if authenticated
        if (auth)
        {
            System.out.println("4 - View balance");
            System.out.println("5 - Deposit money");
            System.out.println("6 - Withdraw money");
        }

        System.out.print("Command> ");

        // parsing user input
        String input = tryReadInput();

        /// seeing if the user inputted a proper command

        // shortcuts
        if (input.equalsIgnoreCase("q"))
        {
            return logout();
        }

        // try/catch to if the user doesn't input a number
        try
        {
            // dropping into a switch statement
            switch (Integer.valueOf(input))
            {
                case 1:
                    try
                    {
                        // returning the result of the login function, will only return false
                        // if the user quits out
                        return login();
                    }
                    catch (IOException e)
                    {
                        System.out.println("Something happened during the login function:\n" + e);
                        e.printStackTrace();
                        return false;
                    }
                case 2:
                    return createNewAccount();
                case 3:
                    return logout();
                case 4:
                    return viewBalance();
                case 5:
                    return depositMoney();
                case 6:
                    return withdrawMoney();
                default:
                    break;
            }
        }
        catch (NumberFormatException e)
        {
            System.out.println("Please enter a valid command.");
        }

        // base case
        return true;
    }

    /**
     * login function
     *
     * @throws IOException if there's an error reading data from the server
     * @return false if the user quits, true otherwise
     */
    protected boolean login() throws IOException, RuntimeException
    {
        String input;
        String message;

        // clearing the auth state if the user decides to log into another account
        auth = false;

        // get username
        System.out.print("Type your username (quit to exit): ");
        // reading user input
        input = tryReadInput();

        // attempting to log in unless the user quits out of the program
        if (input.equalsIgnoreCase("quit"))
        {
            // return false if quit, else proceeds
            return false;
        }

        // sending the user id to the server
        networkOut.println("UID " + input);
        try
        {
            message = networkIn.readLine();

            if (getStatusCode(message) != 100)
            {
                System.out.println(
                        "Something went wrong when trying to send the username.\nReason: " + getStatusMessage(message));
                return true;
            }
        }
        catch (IOException e)
        {
            System.err.println("Error reading response to UID.");
            return true;
        }

        /// get password

        System.out.print("Passcode: ");
        // reading user input
        input = tryReadInput();

        // sending the password to the server
        networkOut.println("PWD " + input);
        try
        {
            message = networkIn.readLine();

            // parsing the status code
            if (getStatusCode(message) != 200)
            {
                System.out.println("Login unsuccessful: " + getStatusMessage(message));
                return true;
            }
        }
        catch (IOException e)
        {
            System.err.println("Error reading response to PWD.");
            return true;
        }

        // login success
        auth = true;
        return true;
    }

    /**
     * this function should prompt the user for a new username and password.
     * the function should reject the input if the user inputted a space in either
     * the username or password
     * the function should reject the input if the user didn't input anything (e.g.,
     * ' ', '', etc.)
     *
     * if the input is valid send a request of "NEW <username> <password>"
     *
     * on success, the server will return "201 Created"
     * on failure, the server will return "400 Username or password is invalid"
     * if there is a server error, the server will return "500 Internal server
     * error"
     *
     * @return true, always
     */
    protected boolean createNewAccount()
    {
        String username;
        String password;

        while (true)
        {
            // Prompt user for username
            System.out.print("Enter username (or 'q' to cancel): ");
            username = tryReadInput();

            if (username.equalsIgnoreCase("q"))
            {
                // User cancels operation
                return true;
            }
            else if (username.trim().isEmpty())
            {
                System.out.println("Username cannot be blank.");
                continue;
            }

            // Prompt user for password
            System.out.print("Enter password (or 'q' to cancel): ");
            password = tryReadInput();

            if (password.equalsIgnoreCase("q"))
            {
                // User cancels operation
                return true;
            }
            else if (password.trim().isEmpty())
            {
                System.out.println("Password cannot be blank.");
                continue;
            }

            // Send request to server
            networkOut.println("NEW " + username + " " + password);

            // Receive response from server
            try
            {
                String response = networkIn.readLine();

                // Parse status code
                int statusCode = getStatusCode(response);

                // Handle response
                switch (statusCode)
                {
                    case 201:
                        System.out.println("Account created successfully.");
                        return true;
                    case 400:
                        System.out.println("Username or password is invalid.");
                        break;
                    case 500:
                        System.out.println("Internal server error.");
                        break;
                    default:
                        System.out.println("Unexpected response from server: " + response);
                }
            }
            catch (IOException e)
            {
                System.out.println("Error reading server response: " + e.getMessage());
                e.printStackTrace();
                return true;
            }
        }
    }

    /**
     * this function sends the logout command to the server and then returns false
     * to
     * exit the main loop
     *
     * @return false, always
     */
    protected boolean logout()
    {
        networkOut.println("LOGOUT");
        return false;
    }

    protected boolean viewBalance()
    {
        // sending prompt to the server
        networkOut.println("VIEW");

        // getting reply from the server
        try
        {
            // reading the message and status code
            String message = networkIn.readLine();
            int statusCode = getStatusCode(message);

            // reading status code
            if (statusCode == 200)
            {
                System.out.println("Account balance: " + getStatusMessage(message));
            }
            else
            {
                System.out.println("Error retrieving balance from the server.\nReason: " + getStatusMessage(message));
            }

        }
        catch (IOException e)
        {
            System.out.println("Error reading information from the server: " + e);
            e.printStackTrace();
        }

        // returning
        return true;
    }

    protected boolean depositMoney()
    {
        // sending prompt to the server
        networkOut.println("DEP");

        // responses from the server
        String message;
        Integer statusCode;

        // getting reply from the server
        try
        {
            // reading the message and status code
            message = networkIn.readLine();
            statusCode = getStatusCode(message);

            // reading status code
            if (statusCode == 100)
            {
                System.out.println("Account balance: " + getStatusMessage(message));
            }
            else
            {
                System.out.println("Error retrieving balance from the server.\nReason: " + getStatusMessage(message));
                return true;
            }
        }
        catch (IOException e)
        {
            System.out.println("Error reading information from the server: " + e);
            e.printStackTrace();
        }

        // getting the amount the user wants to deposit
        System.out.println("Enter the amount you would like to deposit. ('q' to break)");
        Integer amount = null;
        while (amount == null)
        {
            // prompting the user for input
            System.out.print("$ ");
            String input = tryReadInput();

            // testing if the user wants to break
            if (input.equalsIgnoreCase("q"))
            {
                networkOut.println("DEP BREAK");
                return true;
            }

            // parsing the integer from the input
            try
            {
                amount = Integer.valueOf(input);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Enter a valid number.");
            }
        }

        // making another request to the server
        networkOut.println("DEP " + amount);

        // getting response from the server
        try
        {
            // reading the message and status code
            message = networkIn.readLine();
            statusCode = getStatusCode(message);

            // reading status code
            if (statusCode == 202)
            {
                System.out.println("Account balance: " + getStatusMessage(message));
            }
            else
            {
                System.out.println("Error retrieving balance from the server.\nReason: " + getStatusMessage(message));
            }
        }
        catch (IOException e)
        {
            System.out.println("Error reading information from the server: " + e);
            e.printStackTrace();
        }

        // breaking from function
        return true;
    }

    /**
     * this function reads input from the user of how much money they would like to
     * withdraw.
     * they shouldn't be able to withdraw more than what they have, the client &
     * server should check this.
     *
     * after sending the withdraw request: "WITH", the server should respond with
     * "100 <user-balance>"
     * you'll need to save the <user-balance> into an Integer variable, if there are
     * any server errors or errors parsing the
     * response from the server you'll need to break out of the function and return
     * true.
     *
     * after reading the response from the server, you'll then prompt the client for
     * an amount (that cannot be more than what the server sent).
     * during this loop, if the client inputs 'q', send "WITH BREAK" to the server
     * to let it know that there won't be any other requests and return true.
     *
     * if the user inputs a valid number, send another response to the server of
     * form: "WITH <amount>", the server should
     * check that this amount is less or equal to the balance that is saved.
     *
     * on success, the server should send "200 <new-balance>"
     * if there are any errors (if the amount to withdraw is greater than the
     * balance on file) the server should reply
     * with "400 Bad request"
     *
     * some functions that will be of use to you.
     *
     * @see #tryReadInput()
     * @see #depositMoney()
     * @see #getStatusCode(String)
     * @see #getStatusMessage(String)
     * @return true, always
     */
    protected boolean withdrawMoney()
    {
        // Send request to server to get current balance
        networkOut.println("WITH");

        // Receive response from server
        String response;
        int userBalance;
        try
        {
            response = networkIn.readLine();
            int statusCode = getStatusCode(response);

            // Parse user balance from response
            if (statusCode == 100)
            {
                userBalance = Integer.parseInt(getStatusMessage(response));
            }
            else
            {
                System.out.println("Error: " + getStatusMessage(response));
                return true;
            }
        }
        catch (IOException e)
        {
            System.out.println("Error reading server response: " + e.getMessage());
            e.printStackTrace();
            return true;
        }

        // Prompt user for withdrawal amount
        System.out.print("Enter amount to withdraw (or 'q' to cancel): ");
        String input = tryReadInput();

        // Check if user wants to cancel
        if (input.equalsIgnoreCase("q"))
        {
            networkOut.println("WITH BREAK");
            return true;
        }

        // Parse withdrawal amount
        int withdrawalAmount;
        try
        {
            withdrawalAmount = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Invalid input. Please enter a valid number.");
            return true;
        }

        // Check if withdrawal amount exceeds balance
        if (withdrawalAmount > userBalance)
        {
            System.out.println("Error: Withdrawal amount exceeds account balance.");
            return true;
        }

        // Send withdrawal request to server
        networkOut.println("WITH " + withdrawalAmount);

        // Receive response from server
        try
        {
            response = networkIn.readLine();
            int statusCode = getStatusCode(response);

            // Parse new balance from response
            if (statusCode == 200)
            {
                int newBalance = Integer.parseInt(getStatusMessage(response));
                System.out.println("Withdrawal successful. New balance: " + newBalance);
            }
            else if (statusCode == 400)
            {
                System.out.println("Error: " + getStatusMessage(response));
            }
            else
            {
                System.out.println("Unexpected response from server: " + response);
            }
        }
        catch (IOException e)
        {
            System.out.println("Error reading server response: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /// ------------------------- helper functions -------------------------

    // Helper function
    protected int getStatusCode(String message)
    {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        return Integer.valueOf(code).intValue();
    }

    // Helper function
    protected String getStatusMessage(String message)
    {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        String errorMessage = null;
        if (st.hasMoreTokens())
        {
            errorMessage = message.substring(code.length() + 1, message.length());
        }
        return errorMessage;
    }

    /**
     * this function drops the user into an infinite loop that will always refuse a
     * non-answer
     *
     * @return the user's input
     */
    protected String tryReadInput()
    {
        String input = null;
        while (true)
        {
            try
            {
                input = inputKeyboard.readLine();
                if (!input.isEmpty())
                    return input;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        // main method
        new ATMClient();
    }
}
