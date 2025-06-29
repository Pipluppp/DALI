package com.dali.ecommerce.payment.maya;

public class RedirectUrl {
    private String success;
    private String failure;
    private String cancel;

    // Getters and Setters
    public String getSuccess() { return success; }
    public void setSuccess(String success) { this.success = success; }
    public String getFailure() { return failure; }
    public void setFailure(String failure) { this.failure = failure; }
    public String getCancel() { return cancel; }
    public void setCancel(String cancel) { this.cancel = cancel; }
}