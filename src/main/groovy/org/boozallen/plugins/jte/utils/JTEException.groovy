package org.boozallen.plugins.jte.utils

class JTEException extends Exception {
  JTEException(String message){
    super(message)
  }

  JTEException(String message, Throwable t){
    super(message, t)
  }

  JTEException(Throwable t){
    super(t)
  }
}
