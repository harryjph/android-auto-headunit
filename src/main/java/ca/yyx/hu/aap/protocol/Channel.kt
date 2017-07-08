package ca.yyx.hu.aap.protocol

object Channel {

    const val ID_CTR = 0
    const val ID_SEN = 1
    const val ID_VID = 2
    const val ID_INP = 3
    const val ID_AUD = 6
    const val ID_AU1 = 4
    const val ID_AU2 = 5
    const val ID_MIC = 7
    const val ID_BTH = 8
    const val ID_MPB = 9

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
            ID_MPB -> return "MPB"
        }
        return "UNK"
    }

    fun isAudio(chan: Int): Boolean {
        return chan == Channel.ID_AUD || chan == Channel.ID_AU1 || chan == Channel.ID_AU2
    }
}