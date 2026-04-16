package com.cinebook.cli;

import com.cinebook.domain.User;
import com.cinebook.exception.AuthenticationException;
import com.cinebook.exception.InvalidInputException;
import com.cinebook.service.AuthService;

/** CLI controller for login, registration, and account management. */
public class AuthController {

    private final AuthService authService;
    private final InputReader input;

    public AuthController(AuthService authService, InputReader input) {
        this.authService = authService;
        this.input = input;
    }

    /** Show the login/register prompt. Returns true if user logged in. */
    public boolean loginOrRegister() {
        System.out.println("================================");
        System.out.println("       Account Access");
        System.out.println("================================");
        System.out.println(" [1] Login");
        System.out.println(" [2] Register");
        System.out.println(" [B] Back");
        System.out.println("--------------------------------");

        String choice = input.readLine(" > ");
        switch (choice) {
            case "1" -> { return doLogin(); }
            case "2" -> { return doRegister(); }
            default -> { return false; }
        }
    }

    /** Show account menu (logout). */
    public void accountMenu() {
        User user = authService.getCurrentUser();
        if (user == null) {
            System.out.println(" Not logged in.");
            return;
        }
        System.out.println("================================");
        System.out.println("       Account");
        System.out.println("================================");
        System.out.println(" Username : " + user.getUsername());
        System.out.println(" Role     : " + user.getRole());
        System.out.println(" User ID  : " + user.getUserId());
        System.out.println("--------------------------------");
        System.out.println(" [1] Logout");
        System.out.println(" [B] Back");
        System.out.println("--------------------------------");

        String choice = input.readLine(" > ");
        if ("1".equals(choice)) {
            authService.logout();
            System.out.println(" Logged out.");
        }
    }

    private boolean doLogin() {
        String username = input.readNonBlank(" Username: ");
        String password = input.readPassword(" Password: ");
        try {
            authService.login(username, password);
            System.out.println(" Welcome back, " + username + "!");
            return true;
        } catch (AuthenticationException e) {
            System.out.println(" " + e.getMessage());
            return false;
        }
    }

    private boolean doRegister() {
        String username = input.readNonBlank(" Choose username: ");
        String password = input.readPassword(" Choose password (min 4 chars): ");
        try {
            authService.register(username, password);
            System.out.println(" Account created! Please login.");
            return false;
        } catch (InvalidInputException e) {
            System.out.println(" " + e.getMessage());
            return false;
        }
    }
}
