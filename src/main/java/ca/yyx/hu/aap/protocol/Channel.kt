package ca.yyx.hu.aap.protocol

object Channel {

    val ID_CTR = 0                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    val ID_SEN = 1
    val ID_VID = 2
    val ID_INP = 3
    val ID_AUD = 6
    val ID_AU1 = 4
    val ID_AU2 = 5
    val ID_MIC = 7
    val ID_BTH = 8

    internal val MAX = 8

    fun name(channel: Int): String {
        when (channel) {
            ID_CTR -> return "CTR"
            ID_VID -> return "VID"
            ID_INP -> return "INP"
            ID_SEN -> return "SEN"
            ID_MIC -> return "MIC"
            ID_AUD -> return "AUD"
            ID_AU1 -> return "AU1"
            ID_AU2 -> return "AU2"
            ID_BTH -> return "BTH"
        }
        return "UNK"
    }

    fun isAudio(chan: Int): Boolean {
        return chan == Channel.ID_AUD || chan == Channel.ID_AU1 || chan == Channel.ID_AU2
    }
}