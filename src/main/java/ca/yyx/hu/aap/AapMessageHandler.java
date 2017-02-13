package ca.yyx.hu.aap;

/**
 * @author algavris
 * @date 13/02/2017.
 */
interface AapMessageHandler {
    void handle(AapMessage message) throws HandleException;

    class HandleException extends Exception {
        HandleException(Throwable cause) {
            super(cause);
        }
    }
}
