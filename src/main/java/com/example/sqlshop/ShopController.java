package com.example.sqlshop;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ShopController {

    @Autowired private JdbcTemplate jdbc; // Direct JDBC = perfect for raw SQLi

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("username", session.getAttribute("user"));
        model.addAttribute("products", jdbc.queryForList(
                "SELECT id, name, price FROM products ORDER BY id LIMIT 9"));
        return "home";
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    // CLASSIC STRING SQL INJECTION
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        String sql = "SELECT id, username, fullname, role FROM customers WHERE username = '" + username +
                "' AND password = '" + password + "'";

        List<Map<String, Object>> result = jdbc.queryForList(sql);

        if (!result.isEmpty()) {
            var user = result.get(0);
            session.setAttribute("user", user.get("username"));
            session.setAttribute("role", user.get("role"));
            session.setAttribute("fullName", user.get("fullname"));
            return "redirect:/";
        }
        model.addAttribute("error", "Invalid credentials");
        return "login";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        if (q == null || q.trim().isEmpty()) {
            return "search";
        }

//        String sql = "SELECT id, name, price FROM products WHERE name ILIKE '%" + q.replace("'", "''") + "%'";
        String sql = "SELECT id, name, price FROM products WHERE name ILIKE '%" + q + "%'";


        model.addAttribute("results", jdbc.queryForList(sql));
        model.addAttribute("query", q);
        return "search";
    }

    @GetMapping("/product")
    public String product(@RequestParam Long id, Model model) {
        // NUMERIC SQL INJECTION
        String sql = "SELECT * FROM products WHERE id = " + id;
        model.addAttribute("p", jdbc.queryForMap(sql));
        return "product";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/";
        }
        model.addAttribute("users", jdbc.queryForList("SELECT id, username, email FROM customers"));
        return "admin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}