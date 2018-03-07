// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package gtoken.com.nep2.Scrypt.jni;

/**
 * Exception thrown when the current platform cannot be detected.
 *
 * @author Will Glozer
 */
public class UnsupportedPlatformException extends RuntimeException {
    public UnsupportedPlatformException(String s) {
        super(s);
    }
}
