package com.paymentwallet.user_service.exception;

public class WalletPayIdAlreadyExistsException
        extends RuntimeException {

    public WalletPayIdAlreadyExistsException() {
        super("WalletPay ID is already taken");
    }
}