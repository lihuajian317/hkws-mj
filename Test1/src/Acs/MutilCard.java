package Acs;


import NetSDKDemo.HCNetSDK;

/**
 * 多重卡认证功能，下发人员的输入json中参数belongGroup绑定人员群组
 */
public class MutilCard {


    //设置群组参数
    public static void setGroupCfg(int lUserID)
    {
        HCNetSDK.NET_DVR_GROUP_CFG struGroupCfg = new HCNetSDK.NET_DVR_GROUP_CFG();
        struGroupCfg.read();

        struGroupCfg.dwSize = struGroupCfg.size();
        struGroupCfg.byEnable = 1;
        struGroupCfg.byGroupName = "test".getBytes();  //
        struGroupCfg.struValidPeriodCfg.byEnable = 1;
        struGroupCfg.struValidPeriodCfg.struBeginTime.wYear = 2021;
        struGroupCfg.struValidPeriodCfg.struBeginTime.byMonth = 1;
        struGroupCfg.struValidPeriodCfg.struBeginTime.byDay = 1;
        struGroupCfg.struValidPeriodCfg.struBeginTime.byHour = 0;
        struGroupCfg.struValidPeriodCfg.struBeginTime.byMinute = 0;
        struGroupCfg.struValidPeriodCfg.struBeginTime.bySecond = 0;
        struGroupCfg.struValidPeriodCfg.struEndTime.wYear = 2037;
        struGroupCfg.struValidPeriodCfg.struEndTime.byMonth = 12;
        struGroupCfg.struValidPeriodCfg.struEndTime.byDay = 31;
        struGroupCfg.struValidPeriodCfg.struEndTime.byHour = 23;
        struGroupCfg.struValidPeriodCfg.struEndTime.byMinute = 59;
        struGroupCfg.struValidPeriodCfg.struEndTime.bySecond = 59;
        struGroupCfg.write();
        if(!AcsMain.hCNetSDK.NET_DVR_SetDVRConfig(lUserID,2113,1,struGroupCfg.getPointer(),struGroupCfg.size()))
        {
            System.out.println("NET_DVR_SetDVRConfig NET_DVR_SET_GROUP_CFG失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
        } else {
            System.out.println("NET_DVR_SetDVRConfig NET_DVR_SET_GROUP_CFG成功");
        }
    }



    //设置多重认证参数

    /**
     * 多重卡刷卡开门功能：
     * 有权限的任意n张卡刷卡之后才能开门，不限制先后次序，则只需要设置一个群组组合（卡号都配置关联该群组），byMemberNum设为n，bySequenceNo设为1。
     * 有权限的n张A类卡和m张B类卡刷卡之后才能开门，而且先刷A类卡再刷B类卡，则需要设置2个群组组合，2个群组组合的byMemberNum分别为n和m，
     * bySequenceNo分别为1、2；如果不需要限制刷卡先后次序，则bySequenceNo都设为0，0表示无序。
     * @param lUserID
     */
    public static void setMultiCardCfg(int lUserID)
    {
        HCNetSDK.NET_DVR_MULTI_CARD_CFG_V50 struMultiCardCfg = new HCNetSDK.NET_DVR_MULTI_CARD_CFG_V50();
        struMultiCardCfg.read();
        struMultiCardCfg.dwSize = struMultiCardCfg.size();
        struMultiCardCfg.byEnable = 1;
        struMultiCardCfg.bySwipeIntervalTimeout = 30;   //刷卡认证超时时间
        struMultiCardCfg.struGroupCfg[0].byEnable = 1;
        struMultiCardCfg.struGroupCfg[0].dwTemplateNo = 1;
        struMultiCardCfg.struGroupCfg[0].struGroupCombination[0].byEnable = 1;
        struMultiCardCfg.struGroupCfg[0].struGroupCombination[0].byMemberNum = 2; //刷卡成员数量，群组里面需要刷卡的卡个数
        struMultiCardCfg.struGroupCfg[0].struGroupCombination[0].bySequenceNo = 1; //群组刷卡次序号
        struMultiCardCfg.struGroupCfg[0].struGroupCombination[0].dwGroupNo = 1;    //群组编号         //刷卡认证组
        struMultiCardCfg.write();

        if(!AcsMain.hCNetSDK.NET_DVR_SetDVRConfig(lUserID, HCNetSDK.NET_DVR_SET_MULTI_CARD_CFG_V50,1,struMultiCardCfg.getPointer(),struMultiCardCfg.size()))
        {
            System.out.println("NET_DVR_SetDVRConfig NET_DVR_SET_MULTI_CARD_CFG_V50 失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
        } else {
            System.out.println("NET_DVR_SetDVRConfig NET_DVR_SET_MULTI_CARD_CFG_V50 成功");
        }
    }
}
