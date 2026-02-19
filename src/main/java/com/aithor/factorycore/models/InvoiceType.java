package com.aithor.factorycore.models;
import java.util.*;

public enum InvoiceType {
    TAX("§eTax"),
    SALARY("§6Employee Salary");
    
    private final String display;
    
    InvoiceType(String display) {
        this.display = display;
    }
    
    public String getDisplay() { return display; }
}