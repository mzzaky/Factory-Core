package com.aithor.factorycore.models;
import java.util.*;

public class Invoice {
    private String id;
    private UUID factoryId;
    private InvoiceType type;
    private double amount;
    private long dueDate;
    private boolean paid;
    
    public Invoice(String id, UUID factoryId, InvoiceType type, double amount, long dueDate) {
        this.id = id;
        this.factoryId = factoryId;
        this.type = type;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = false;
    }
    
    public String getId() { return id; }
    public UUID getFactoryId() { return factoryId; }
    public InvoiceType getType() { return type; }
    public double getAmount() { return amount; }
    public long getDueDate() { return dueDate; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    
    public boolean isOverdue() {
        return System.currentTimeMillis() > dueDate && !paid;
    }
}