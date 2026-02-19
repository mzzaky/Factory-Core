package com.aithor.factorycore.models;

import java.util.*;

public enum FactoryStatus {
    RUNNING("§aRUNNING"),
    STOPPED("§cSTOPPED"),
    NO_PARTS("§eOUT OF MACHINE PARTS");

    private final String display;

    FactoryStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
