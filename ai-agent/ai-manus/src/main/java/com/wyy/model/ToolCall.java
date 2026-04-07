package com.wyy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolCall {
    private String id;
    private String type = "function";
    private Function function;
}