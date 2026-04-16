package com.cinebook.cli;

import com.cinebook.service.AuthService;

/** Main menu loop for the CineBook CLI. */
public class MainMenu {

    private final AuthService authService;
    private final AuthController authController;
    private final BookingController bookingController;
    private final AdminController adminController;
    private final InputReader input;

    public MainMenu(AuthService authService,
                    AuthController authController,
                    BookingController bookingController,
                    AdminController adminController,
                    InputReader input) {
        this.authService = authService;
        this.authController = authController;
        this.bookingController = bookingController;
        this.adminController = adminController;
        this.input = input;
    }

    /** Run the main menu loop until user quits. */
    public void run() {
        while (true) {
            printMenu();
            String choice = input.readLine(" > ");
            try {
                switch (choice.toUpperCase()) {
                    case "1" -> bookingController.browseShowtimes();
                    case "2" -> bookingController.myBookings();
                    case "3" -> handleAccount();
                    case "4" -> handleAdmin();
                    case "Q" -> {
                        System.out.println(" Goodbye!");
                        return;
                    }
                    default -> System.out.println(" Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println(" Something went wrong. Your data is safe. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("================================");
        System.out.println("       CineBook  v1.0");
        System.out.println("================================");
        System.out.println(" [1] Browse Showtimes");
        System.out.println(" [2] My Bookings");
        System.out.println(" [3] Account");
        if (authService.isAdmin()) {
            System.out.println(" [4] Admin");
        }
        System.out.println(" [Q] Quit");
        System.out.println("--------------------------------");
        if (authService.isLoggedIn()) {
            System.out.println(" Logged in as: " + authService.getCurrentUser().getUsername());
        } else {
            System.out.println(" Not logged in");
        }
    }

    private void handleAccount() {
        if (authService.isLoggedIn()) {
            authController.accountMenu();
        } else {
            authController.loginOrRegister();
        }
    }

    private void handleAdmin() {
        if (!authService.isAdmin()) {
            System.out.println(" Admin access required. Please login as admin.");
            return;
        }
        adminController.adminMenu();
    }
}
