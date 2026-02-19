package com.aithor.factorycore.models;
import java.util.*;

public class Recipe {
    private String id;
    private String name;
    private String factoryType;
    private int productionTime;
    private Map<String, Integer> inputs;
    private Map<String, Integer> outputs;
    private List<String> consoleCommands;
    private String icon;
    
    public Recipe(String id, String name, String factoryType, int productionTime) {
        this.id = id;
        this.name = name;
        this.factoryType = factoryType;
        this.productionTime = productionTime;
        this.inputs = new HashMap<>();
        this.outputs = new HashMap<>();
        this.consoleCommands = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getFactoryType() { return factoryType; }
    public int getProductionTime() { return productionTime; }
    public Map<String, Integer> getInputs() { return inputs; }
    public Map<String, Integer> getOutputs() { return outputs; }
    public List<String> getConsoleCommands() { return consoleCommands; }
    public String getIcon() { return icon; }
    
    public void setIcon(String icon) { this.icon = icon; }
    public void addInput(String resource, int amount) { inputs.put(resource, amount); }
    public void addOutput(String resource, int amount) { outputs.put(resource, amount); }
    public void addConsoleCommand(String command) { consoleCommands.add(command); }
}