package com.wyy.tools;

/**
 * 作者：IT周瑜
 * 公众号：IT周瑜
 * 微信号：it_zhouyu
 */
public class ToolResult {
    private final Object output;
    private final String error;
    private final String base64Image;
    
    public ToolResult(Object output) {
        this(output, null, null);
    }
    
    public ToolResult(Object output, String error) {
        this(output, error, null);
    }
    
    public ToolResult(Object output, String error, String base64Image) {
        this.output = output;
        this.error = error;
        this.base64Image = base64Image;
    }
    
    public static ToolResult success(Object output) {
        return new ToolResult(output);
    }
    
    public static ToolResult success(Object output, String base64Image) {
        return new ToolResult(output, null, base64Image);
    }
    
    public static ToolResult error(String error) {
        return new ToolResult(null, error);
    }
    
    public Object getOutput() { return output; }
    public String getError() { return error; }
    public String getBase64Image() { return base64Image; }
    
    public boolean isSuccess() { return error == null; }
    public boolean hasError() { return error != null; }
    public boolean hasImage() { return base64Image != null; }
}