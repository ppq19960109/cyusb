package com.usb.model;

public class CySystemConfig {
    int m_nInt;//探测器积分时间
    int m_nAreaNo;//当前分区
    int m_nSteerStart;//舵机起始位置
    int m_nSteerEnd;//舵机终止位置
    int m_nVideoWidth;//图像宽度
    int m_nVideoHeight;//图像高度
    //24
    public float m_fZeroTemp;//挡片温度
    public float m_TempSlop;//温度校正的一次系数
    public float m_TempOffset;//温度校正的常系数
    public float m_swZeroGray;//挡片灰度
    float m_fFid;    //探测器偏置
    float m_fVskm;    //探测器偏置
    // 24+24=48
    int m_nOutWidth;//探测器输出像素长
    int m_nOutHigh;//探测器输出像素宽
    int m_nPreRecordTime;//前一次pc端发送的系统时间 单位：秒
    int m_nTimeCount;//按秒为单位
    int m_nInitRecordTime;//初始记录时间
    //24+24+20=68
    int m_nVerifyCode;//验证码
    float[] m_pfFID = new float[10];//每个分区fid值
    float[] m_pTempSlop = new float[10];//每个分区的一次系数
    float[] m_pTempOffset = new float[10];//每个分区的常系数
    byte[] m_strIR_No = new byte[16];//摄像头编号
    //24+24+20+140=208
    public float m_VtempSlop;//焦平面温度校正一次系数                                       //***
    public float m_VtempOffset;//焦平面温度校正常系数                                       //***根据试验标定取得的参数，计算出模拟外置标定36°C对应黑体灰度
    public float m_fVtempToGray;//焦平面温度矫正二次系数  //焦平面温度与灰度的转换系数      //***
    float m_fEspValue;//比辐射率
    float m_fDistance;//距离
    public float m_fHum;//作为温度修正系数  //湿度
    int[] reserve = new int[88];
    int m_nCheckSum;//校验位，防止数据出错  为前面数据的代数和
    //24+24+20+140+380=588

    public static final int SIZE = 0x24c;
    private int byteIndex;

    private void byteTobyte(byte[] buf, byte[] bbuf, int num) {
        for (int i = 0; i < num; ++i) {
            bbuf[i] = buf[byteIndex];
            ++byteIndex;
        }
    }

    private int byteToint(byte[] buf) {
        int data =(buf[byteIndex]&0xff) |  ((buf[byteIndex + 1]&0xff) << 8) |  ((buf[byteIndex + 2]&0xff) << 16) |  ((buf[byteIndex + 3]&0xff) << 24);
        byteIndex += 4;
        return data;
    }

    private void byteToint(byte[] buf, int[] ibuf, int num) {
        for (int i = 0; i < num; ++i) {
            ibuf[i] = (buf[byteIndex]&0xff) |  ((buf[byteIndex + 1]&0xff) << 8) |  ((buf[byteIndex + 2]&0xff) << 16) |  ((buf[byteIndex + 3]&0xff) << 24);
            byteIndex += 4;
        }
    }

    private float byteTofloat(byte[] buf) {
        int val = (buf[byteIndex]&0xff) |  ((buf[byteIndex + 1]&0xff) << 8) |  ((buf[byteIndex + 2]&0xff) << 16) |  ((buf[byteIndex + 3]&0xff) << 24);
        float data =Float.intBitsToFloat(val);
        byteIndex += 4;
        return data;
    }

    private void byteTofloat(byte[] buf, float[] fbuf, int num) {
        for (int i = 0; i < num; ++i) {
            int val = (buf[byteIndex]&0xff) |  ((buf[byteIndex + 1]&0xff) << 8) |  ((buf[byteIndex + 2]&0xff) << 16) |  ((buf[byteIndex + 3]&0xff) << 24);
            fbuf[i] =Float.intBitsToFloat(val);
            byteIndex += 4;
        }
    }


    public boolean byteToConfig(byte[] configData) {

        byteIndex = 0;
        m_nInt = byteToint(configData);
        m_nAreaNo = byteToint(configData);//当前分区
        m_nSteerStart = byteToint(configData);//舵机起始位置
        m_nSteerEnd = byteToint(configData);//舵机终止位置
        m_nVideoWidth = byteToint(configData);//图像宽度
        m_nVideoHeight = byteToint(configData);//图像高度
        //24
        m_fZeroTemp = byteTofloat(configData);//挡片温度
        m_TempSlop = byteTofloat(configData);//温度校正的一次系数
        m_TempOffset = byteTofloat(configData);//温度校正的常系数
        m_swZeroGray = byteTofloat(configData);//挡片灰度
        m_fFid = byteTofloat(configData);    //探测器偏置
        m_fVskm = byteTofloat(configData);    //探测器偏置
        // 48
        m_nOutWidth = byteToint(configData);//探测器输出像素长
        m_nOutHigh = byteToint(configData);//探测器输出像素宽
        m_nPreRecordTime = byteToint(configData);//前一次pc端发送的系统时间 单位：秒
        m_nTimeCount = byteToint(configData);//按秒为单位
        m_nInitRecordTime = byteToint(configData);//初始记录时间
        //68
        m_nVerifyCode = byteToint(configData);//验证码
        byteTofloat(configData, m_pfFID, 10);//每个分区fid值
        byteTofloat(configData, m_pTempSlop, 10);
        ;//每个分区的一次系数
        byteTofloat(configData, m_pTempOffset, 10);//每个分区的常系数
        byteTobyte(configData, m_strIR_No, 16);//摄像头编号
        //208
        m_VtempSlop = byteTofloat(configData);//焦平面温度校正一次系数
        m_VtempOffset = byteTofloat(configData);//焦平面温度校正常系数
        m_fVtempToGray = byteTofloat(configData);//焦平面温度矫正二次系数  //焦平面温度与灰度的转换系数
        m_fEspValue = byteTofloat(configData);//比辐射率
        m_fDistance = byteTofloat(configData);//距离
        m_fHum = byteTofloat(configData);//作为温度修正系数  //湿度
        byteToint(configData, reserve, 88);
        m_nCheckSum = byteToint(configData);//校验位，防止数据出错  为前面数据的代数和
        //588
        if (byteIndex != 588) {
            return false;
        }
        return true;
    }

    public void grayToTemp(short[] source,float[] target, boolean bool) {
        if (bool) {
            for (int i = 0; i < source.length; ++i) {
                target[i] = (source[i] - m_swZeroGray) * m_TempSlop + m_fZeroTemp + m_TempOffset;
            }
        } else {
            for (int i = 0; i < source.length; ++i) {
                float temp = 36 - m_fZeroTemp; //外置黑体温度（36°）与挡片温度差
                float outBlackGray = m_VtempSlop * temp + m_fVtempToGray * temp * temp + m_VtempOffset; //计算外置黑体灰度
                outBlackGray += m_swZeroGray;
                temp = m_fZeroTemp * m_TempSlop +m_TempOffset; //根据挡片计算不同环境温度下的温度修正系数
                target[i] = (source[i] - outBlackGray) * temp + m_fHum + 36; //计算目标温度值
            }
        }
    }

}
