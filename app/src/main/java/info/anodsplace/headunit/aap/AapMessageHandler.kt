package info.anodsplace.headunit.aap

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */
internal interface AapMessageHandler {
    @Throws(HandleException::class)
    fun handle(message: AapMessage)

    class HandleException internal constructor(cause: Throwable) : Exception(cause)
}
