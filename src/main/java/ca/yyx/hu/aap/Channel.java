package ca.yyx.hu.aap;

class Channel {

    static final int AA_CH_CTR = 0;                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    static final int AA_CH_SEN = 1;
    static final int AA_CH_VID = 2;
    static final int AA_CH_TOU = 3;
    static final int AA_CH_AUD = 4;
    static final int AA_CH_AU1 = 5;
    static final int AA_CH_AU2 = 6;
    static final int AA_CH_MIC = 7;

    static final int MAX = 7;

    static String name(int channel)
    {
        switch (channel) {
            case AA_CH_CTR: return "CTR";
            case AA_CH_VID: return "VID";
            case AA_CH_TOU: return "TOU";
            case AA_CH_SEN: return "SEN";
            case AA_CH_MIC: return "MIC";
            case AA_CH_AUD: return "AUD";
            case AA_CH_AU1: return "AU1";
            case AA_CH_AU2: return "AU2";
        }
        return "UNK";
    }

    static boolean isAudio(int chan) {
        return chan == Channel.AA_CH_AUD || chan == Channel.AA_CH_AU1 || chan == Channel.AA_CH_AU2;
    }
}