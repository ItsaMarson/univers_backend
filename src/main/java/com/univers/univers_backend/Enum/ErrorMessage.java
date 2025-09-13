package com.univers.univers_backend.Enum;

public enum ErrorMessage {

    EMAIL_IN_USE("Email already in use."),
    ID_NUMBER_IN_USE("Id number already in use."),
    UNKNOWN_ERROR("An unknown error occurred."),

    INVALID_EMAIL_DOMAIN("Invalid email! Use your institutional email.");

    private final String message;

    ErrorMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

    public static ErrorMessage fromString(String text){
        for(ErrorMessage b: ErrorMessage.values()){
            if(b.message.equalsIgnoreCase(text)){
                return  b;
            }
        }
        return UNKNOWN_ERROR;
    }
}

