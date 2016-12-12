package ca.yyx.hu.aap.protocol;

public class Channel {

    public static final int ID_CTR = 0;                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    public static final int ID_SEN = 1;
    public static final int ID_VID = 2;
    public static final int ID_INP = 3;
    public static final int ID_AUD = 6;
    public static final int ID_AU1 = 4;
    public static final int ID_AU2 = 5;
    public static final int ID_MIC = 7;
    public static final int ID_BTH = 8;

    static final int MAX = 8;

    public static String name(int channel)
    {
        switch (channel) {
            case ID_CTR: return "CTR";
            case ID_VID: return "VID";
            case ID_INP: return "INP";
            case ID_SEN: return "SEN";
            case ID_MIC: return "MIC";
            case ID_AUD: return "AUD";
            case ID_AU1: return "AU1";
            case ID_AU2: return "AU2";
            case ID_BTH: return "BTH";
        }
        return "UNK";
    }

    public static boolean isAudio(int chan) {
        return chan == Channel.ID_AUD || chan == Channel.ID_AU1 || chan == Channel.ID_AU2;
    }
}