package com.aerospikeworkshop.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aerospikeworkshop.model.Cart;
import com.aerospikeworkshop.service.KeyValueServiceInterface;

@RestController
@RequestMapping("/rest/v1/cart")
public class CartController {
    
    private final KeyValueServiceInterface keyValueService;

    public CartController(KeyValueServiceInterface keyValueService) {
        this.keyValueService = keyValueService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable String userId) {
        try {
            Cart cart = keyValueService.getCart(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cart.getItems());
            response.put("total", cart.getTotal());
            response.put("itemCount", cart.getItemCount());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<Map<String, Object>> addToCart(
            @PathVariable String userId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        try {
            Cart cart = keyValueService.addToCart(userId, productId, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cart.getItems());
            response.put("total", cart.getTotal());
            response.put("itemCount", cart.getItemCount());
            response.put("success", true);
            response.put("message", "Item added to cart successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{userId}/update")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable String userId,
            @RequestParam String productId,
            @RequestParam int quantity) {
        try {
            Cart cart = keyValueService.updateCartItem(userId, productId, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cart.getItems());
            response.put("total", cart.getTotal());
            response.put("itemCount", cart.getItemCount());
            response.put("success", true);
            response.put("message", "Cart updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{userId}/remove")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable String userId,
            @RequestParam String productId) {
        try {
            Cart cart = keyValueService.removeFromCart(userId, productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cart.getItems());
            response.put("total", cart.getTotal());
            response.put("itemCount", cart.getItemCount());
            response.put("success", true);
            response.put("message", "Item removed from cart");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable String userId) {
        try {
            Cart cart = keyValueService.clearCart(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", cart.getItems());
            response.put("total", cart.getTotal());
            response.put("itemCount", cart.getItemCount());
            response.put("success", true);
            response.put("message", "Cart cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "cart");
        return ResponseEntity.ok(response);
    }
}
