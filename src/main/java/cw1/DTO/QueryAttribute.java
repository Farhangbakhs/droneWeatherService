// src/main/java/cw1/DTO/QueryAttribute.java
package cw1.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class QueryAttribute {
    private String attribute;  // e.g., "capacity", "cooling", "costPerMove", "name"
    private String operator;   // one of: "=", "!=", "<", ">"
    private String value;      // value as string; will be parsed based on attribute type
}



