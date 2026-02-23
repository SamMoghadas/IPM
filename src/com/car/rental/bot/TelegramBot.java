//package com.car.rental;
//
//import org.telegram.telegrambots.bots.TelegramLongPollingBot;
//import org.telegram.telegrambots.meta.TelegramBotsApi;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
//
//import java.sql.*;
//import java.util.Random;
//
//public class TelegramBot extends TelegramLongPollingBot {
//    private final String botToken = "YOUR_BOT_TOKEN_HERE";
//    private final String botUsername = "YOUR_BOT_USERNAME_HERE";
//    private Connection conn;
//
//    public TelegramBot() {
//        try {
//            conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
//            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
//            botsApi.registerBot(new TelegramBot());
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public String getBotToken() {
//        return botToken;
//    }
//
//    @Override
//    public String getBotUsername() {
//        return botUsername;
//    }
//
//    @Override
//    public void onUpdateReceived(Update update) {
//        if (update.hasMessage() && update.getMessage().hasText()) {
//            String chatId = update.getMessage().getChatId().toString();
//            String text = update.getMessage().getText().toLowerCase();
//            SendMessage message = new SendMessage();
//            message.setChatId(chatId);
//
//            if (text.startsWith("/start")) {
//                message.setText("به بات مدیریت ماشین IPM خوش آمدید!\n" +
//                        "دستورات:\n/start - شروع\n/pickup - ثبت تحویل\n/return - ثبت بازگشت");
//            } else if (text.startsWith("/pickup")) {
//                String response = handlePickup(chatId);
//                message.setText("ثبت تحویل:\n```" + response + "```");
//            } else if (text.startsWith("/return")) {
//                String response = handleReturn(chatId);
//                message.setText("ثبت بازگشت:\n```" + response + "```");
//            } else {
//                message.setText("دستور نامعتبر! از /start استفاده کنید.");
//            }
//
//            try {
//                execute(message);
//            } catch (TelegramApiException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private String handlePickup(String chatId) {
//        try {
//            String sql = "SELECT name FROM Employee LIMIT 1"; // نمونه کارمند
//            String empName = "";
//            try (Statement stmt = conn.createStatement()) {
//                ResultSet rs = stmt.executeQuery(sql);
//                if (rs.next()) empName = rs.getString("name");
//            }
//
//            String rentalCode = generateRentalCode();
//            String pickupTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
//            String sqlInsert = "INSERT INTO CarRental(rental_code, employee_phone, car_plate, delivery_date, destination) VALUES (?, ?, ?, ?, ?)";
//            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
//                pstmt.setString(1, rentalCode);
//                pstmt.setString(2, "09123456789"); // نمونه شماره تلفن (باید داینامیک بشه)
//                pstmt.setString(3, "12ه345م"); // نمونه پلاک (باید داینامیک بشه)
//                pstmt.setString(4, pickupTime);
//                pstmt.setString(5, "تهران");
//                pstmt.executeUpdate();
//            }
//            return "کد تحویل: " + rentalCode + "\nزمان: " + pickupTime;
//        } catch (SQLException e) {
//            return "خطا: " + e.getMessage();
//        }
//    }
//
//    private String handleReturn(String chatId) {
//        try {
//            String rentalCode = "12345"; // نمونه (باید از کاربر بگیره)
//            String returnTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
//            String sqlUpdate = "UPDATE CarRental SET return_date = ? WHERE rental_code = ? AND return_date IS NULL";
//            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
//                pstmt.setString(1, returnTime);
//                pstmt.setString(2, rentalCode);
//                int rows = pstmt.executeUpdate();
//                if (rows > 0) {
//                    return "بازگشت ثبت شد\nزمان: " + returnTime;
//                } else {
//                    return "کد تحویل نامعتبر!";
//                }
//            }
//        } catch (SQLException e) {
//            return "خطا: " + e.getMessage();
//        }
//    }
//
//    private String generateRentalCode() {
//        Random rand = new Random();
//        int num = 10000 + rand.nextInt(90000);
//        return String.valueOf(num);
//    }
//}