package com.jn.fep.parse;

import com.jn.fep.fepwork.FEPMSServerHandler;
import com.jn.fep.fepwork.FEPZDServerHandler;
import com.jn.fep.service.FEPMapService;
import com.jn.fep.util.CheckUtil;
import com.jn.fep.util.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.log4j.Logger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hxt
 * @create 2019-08-27 16:22
 * @description 报文解析
 * @version 1.0
 */
public class MessageParse {
    private static Logger logger = Logger.getLogger(MessageParse.class);

    private static final String AFN_QRFR = "00";
    private static final String AFN_FWML = "01";
    private static final String AFN_LLJKJC = "02";
    private static final String AFN_ZJZML ="03";
    private static final String AFN_SZCS ="04";
    private static final String AFN_KZML ="05";
    private static final String AFN_SFRZMYXS ="06";
    private static final String AFN_BJLZDZDSB ="08";
    private static final String AFN_ZDPZXX ="09";
    private static final String AFN_PARAMS ="0A";
    private static final String AFN_REQUESTDASK ="0B";
    private static final String AFN_REQUEST1 = "0C";
    private static final String AFN_REQUEST2 = "0D";
    private static final String AFN_REQUEST3 = "0E";
    private static final String AFN_FILECS = "0F";
    private static final String AFN_DATAFORWARD = "10";

    public static int ZDofflineFlag; //终端离线标志
    public static String logicAddrtips = null; //终端逻辑地址(解析后)
    public static int forwardFlag; //转发标志 0主站主动 1终端被动 2终端主动
    public static StringBuffer denybf; //否认帧
    public static Map<String,String> ZDlaipMap = new HashMap<>(); //终端网络地址和逻辑地址map
    public static String MSnetworkAddr; //主站网络地址
    private static String clientNetwork; //终端网络地址
    private static List<String> frameList =new ArrayList<>(); //拆包粘包帧list
    private static int dismantlingFlag; //拆包粘包标记
    private static String afterFrame; //粘包后续报文
    private static int totalLength = 0; //实际总长度
    private static int caclframeLength = 0; //报文计算的总长度

    private static String replyMessage; //返回给主站或者前置机的报文

    synchronized public static String Parse(byte[] buf , ChannelHandlerContext ctx) {
        try {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
            clientNetwork = insocket.getAddress().getHostAddress() + ":" + insocket.getPort();  //主站和终端网络地址(IP+端口)
            String receiveMessage = ParseUtil.bytesToHexString(buf);
            //System.out.println("前置机收到到完整TCP单次报文：" + receiveMessage);
            String receiveMessageCS =null;
            String frameCS =null;
            if(CheckUtil.judgeBegin(receiveMessage)) {
                //System.out.println("68开头的报文："+receiveMessage);
                totalLength = receiveMessage.length() / 2;
                //System.out.println("报文字节总长度："+totalLength);
                caclframeLength = CheckUtil.calcLength(receiveMessage);
                //System.out.println("计算字节总长度："+caclframeLength);
                if(receiveMessage.startsWith("68") && receiveMessage.endsWith("16") && totalLength == caclframeLength){
                    receiveMessageCS = receiveMessage.substring(12,receiveMessage.lastIndexOf("16") -2);
                    //System.out.println("报文的数据区域"+receiveMessageCS);
                    //System.out.println("计算出来的校验和"+CheckUtil.makeChecksum(receiveMessageCS));
                    frameCS = receiveMessage.substring(receiveMessage.lastIndexOf("16") -2, receiveMessage.lastIndexOf("16"));
                    //System.out.println("报文实际校验和"+frameCS);
                }
            }

            if(receiveMessage.startsWith("68") && receiveMessage.length() > 20 && CheckUtil.judgeBegin(receiveMessage) && totalLength == caclframeLength && CheckUtil.makeChecksum(receiveMessageCS).equals(frameCS)){
                //System.out.println("正确的报文");
                insideParse(receiveMessage, ctx);

            }else if(!receiveMessage.startsWith("68") || !receiveMessage.endsWith("16") || (receiveMessage.length() < 20)){

                logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger + "][(" + clientNetwork + ")前置机接收到TCP粘包拆包报文(" + logicAddrtips + ")] : [" + ParseUtil.regexString(receiveMessage) + "]" + "\n");
                if (totalLength < caclframeLength || dismantlingFlag == 1) {
                    System.out.println("拆包");
                    StringBuilder appendFrame = new StringBuilder();
                    frameList.add(receiveMessage);
                    dismantlingFlag = 1;
                    for (String frame : frameList) {
                        appendFrame.append(frame);
                    }
                    String finalFrame = appendFrame.toString();
                    //System.out.println("拆包最后拼接的帧" + finalFrame);
                    String demolitionBegin = finalFrame.substring(0, 12);
                    totalLength = finalFrame.length() / 2;//实际总长度
                    caclframeLength = CheckUtil.calcLength(finalFrame);//报文计算的总长度
                    if (finalFrame.startsWith("68") && finalFrame.endsWith("16") && finalFrame.length() > 20 && totalLength == caclframeLength) {
                        insideParse(finalFrame,ctx);
                        dismantlingFlag = 0;
                        if (!frameList.isEmpty()) {
                            frameList.clear();
                        }
                    }
                } else if (totalLength > caclframeLength || dismantlingFlag == 2) {
                    System.out.println("粘包");
                    if (afterFrame == null) {
                        dismantlingFlag = 2;
                        String stickBegin = receiveMessage.substring(0, 12);
                        int stickcaclframeLength = CheckUtil.calcLength(stickBegin) * 2; //粘包前帧长度
                        //System.out.println(stickcaclframeLength);
                        String firstFrame = receiveMessage.substring(0, stickcaclframeLength); //粘包首帧
                        System.out.println("开头"+firstFrame);
                        insideParse(firstFrame,ctx);
                        afterFrame = receiveMessage.substring(stickcaclframeLength); //粘包后续帧
                        System.out.println("后续帧："+afterFrame);
                    } else if (!afterFrame.equals("") && dismantlingFlag == 2) {
                        String finalFrame = afterFrame + receiveMessage;
                        insideParse(finalFrame,ctx);
                        dismantlingFlag = 0;
                    }
                }
            } else {
                System.out.println("开头68多余的情况" + receiveMessage);
                receiveMessage = receiveMessage.substring(receiveMessage.indexOf("68", receiveMessage.indexOf("68") +1));
                System.out.println("截取掉68前部废弃后的报文"+receiveMessage);
                Parse(ParseUtil.hex2byte(receiveMessage.getBytes()), ctx);
            }

        } catch (Exception e) {
            e.printStackTrace();
            //logger.error("异常信息", e.fillInStackTrace());
        }
        //返回解析后回复或者转发的报文
        return replyMessage;
    }
    synchronized private static void insideParse(String receiveMessage ,ChannelHandlerContext ctx ){
        if(receiveMessage.length()> 0) {
            //receiveMessage = receiveMessage.substring(0, receiveMessage.lastIndexOf("16") + 2); //截取到16结尾符
            String formatMessage = ParseUtil.regexString(receiveMessage); //输出日志美化格式
            totalLength = receiveMessage.length() / 2;//实际总长度
            caclframeLength = CheckUtil.calcLength(receiveMessage);//报文计算的总长度
            //解析收到的报文
            if (receiveMessage.length() >= 20 && receiveMessage.endsWith("16") && receiveMessage.startsWith("68") && totalLength == caclframeLength) {
                String C = receiveMessage.substring(12, 14);
                String CParse = C.substring(0, 1);
                String logicAddr = receiveMessage.substring(14, 22);
                logicAddrtips = logicAddr.substring(2, 4) + logicAddr.substring(0, 2) + logicAddr.substring(6, 8) + logicAddr.substring(4, 6);
                String logicAddrParse = logicAddr.substring(2, 4) + logicAddr.substring(0, 2) + " - " + logicAddr.substring(6, 8) + logicAddr.substring(4, 6);
                String AFN = receiveMessage.substring(24, 26);
                String SEQ = receiveMessage.substring(26, 28);
                String pn = receiveMessage.substring(28, 32);
                String fn = receiveMessage.substring(32, 36);

                String before = fn.substring(0, 2);
                String after = fn.substring(2, 4);
                int i = Integer.parseInt(before, 16); //16进制转10进制

                //fn前两位算法
                int beforeRes = 32 - ParseUtil.toFullBinaryString(i).indexOf("1");
                //System.out.println("beforeRes " + beforeRes);
                //fn后两位算法
                int j = Integer.parseInt(after, 16);
                int afterRes = j * 8;
                //System.out.println("afterRes " + afterRes);
                //判断fn fn等于前两位算法跟后两位算法相加
                int result = beforeRes + afterRes;
                String Fnflag = Integer.toString(result);

                //终端未登陆，回复否认帧
                denybf = new StringBuffer();
                String SEQR = "6" + SEQ.substring(1, 2);
                String CS = CheckUtil.makeChecksum(("00" + logicAddr + "0000" + SEQR + "00000200").replace(" ", ""));
                denybf.append("683200320068").append("00").append(logicAddr).append("0000").append(SEQR).append("00000200").append(CS).append("16"); //FN 0200 = F2 否认帧

                //确认/否认
                if (AFN.equals(AFN_QRFR)) {

                    if (CParse.equals("0")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (Fnflag.equals("1")) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")主站回复确认帧成功(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        } else if (Fnflag.equals("2")) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")主站回复否认帧成功(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        if (Fnflag.equals("1")) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")终端回复确认帧成功(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        } else if (Fnflag.equals("2")) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")终端回复否认帧成功(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        }
                    }

                    //复位命令
                } else if (AFN.equals(AFN_FWML)) {
                    //收到AFN=01复位命令，直接转发给终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发复位命令，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发复位命令，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    }

                    //链路接口检测
                } else if (AFN.equals(AFN_LLJKJC)) {

                    StringBuilder confirmbf = new StringBuilder();
                    SEQR = "6" + SEQ.substring(1, 2);
                    CS = CheckUtil.makeChecksum(("00" + logicAddr + "0000" + SEQR + "00000100").replace(" ", ""));
                    confirmbf.append("683200320068").append("00").append(logicAddr).append("0000").append(SEQR).append("00000100").append(CS).append("16"); //FN 0100=F1 确认帧

                    if (Fnflag.equals("1")) {
                        //收到AFN=02H链路接口检测登录帧 返回确认帧
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")终端连接前置机成功，等待终端登录...]" + "\n");
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机收到终端登录帧(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        for (String ZDlogicAddr : FEPMapService.getZDMap().keySet()) {
                            if (ZDlogicAddr.equals(logicAddrtips)) {
                                InetSocketAddress inetSocketAddress = FEPMapService.getZDChannel(logicAddrtips).remoteAddress();
                                String lastZDIP = inetSocketAddress.getAddress().getHostAddress();
                                int lastZDPort = inetSocketAddress.getPort();
                                String lastZDAddr = lastZDIP + ":" + lastZDPort;
                                logger.warn("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + lastZDAddr + ")该终端已经登录，程序将断开此终端上一次连接(" + logicAddrtips + ")]" + "\n");
                                FEPMapService.getZDChannel(logicAddrtips).close();
                                FEPMapService.removeZDMapChannel(logicAddrtips);
                                ZDofflineFlag = -1;
                                //System.out.println("删除了一个终端map" + lastZDAddr);
                            }
                        }
                        //map key:逻辑地址 value:SocketChannel
                        if(FEPMapService.getZDChannel(logicAddrtips) == null) {
                            FEPMapService.addZDMapChannel(logicAddrtips , (SocketChannel) ctx.channel());
                            //System.out.println("添加了一个终端map" + MessageParse.logicAddrtips);
                            FEPZDServerHandler.ZDatomicInteger.incrementAndGet();
                        }
                            /*if(ZDlaipMap.get(ZDclientNetwork) == null){
                                ZDlaipMap.put(ZDclientNetwork, MessageParse.logicAddrtips);
                                System.out.println("添加了一个终端地址：" + ZDclientNetwork + "逻辑地址："+MessageParse.logicAddrtips);
                            }*/
                        //map key:网络地址 value:逻辑地址
                        ZDlaipMap.computeIfAbsent(clientNetwork, k -> logicAddrtips);

                        forwardFlag = 0;
                        replyMessage = confirmbf.toString();

                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机回复终端登录帧成功(" + logicAddrtips + ")] : [" + ParseUtil.regexString(replyMessage) + "]" + "\n");
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + logicAddrParse + ")终端登录成功!]" + "\n");

                    } else if (Fnflag.equals("2")) {
                        //收到AFN=02H链路接口检测登出帧 返回确认帧
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机收到终端登出帧(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        forwardFlag = 0;
                        replyMessage = confirmbf.toString();
                        FEPMapService.getZDChannel(MessageParse.logicAddrtips).close();
                        FEPMapService.removeZDMapChannel(MessageParse.logicAddrtips);
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机回复终端登出帧成功(" + logicAddrtips + ")] : [" + ParseUtil.regexString(replyMessage) + "]" + "\n");

                    } else if (Fnflag.equals("3")) {
                        //收到AFN=02H链路接口检测心跳帧 返回确认帧
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机收到终端心跳帧(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");

                        replyMessage = confirmbf.toString();
                        forwardFlag = 0;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")前置机回复终端心跳帧成功(" + logicAddrtips + ")] : [" + ParseUtil.regexString(replyMessage) + "]" + "\n");

                    }

                    //中继站命令
                } else if (AFN.equals(AFN_ZJZML)) {
                    //收到AFN=03H中继站命令，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发中继站命令，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发中继站命令，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报中继站命令，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //设置参数
                } else if (AFN.equals(AFN_SZCS)) {
                    //收到AFN=04H设置参数，直接转发给终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发设置参数，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发设置参数，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    }
                    //控制命令
                } else if (AFN.equals(AFN_KZML)) {
                    //收到AFN=05H控制命令，直接转发给终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发控制命令，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发控制命令，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    }

                    //身份认证及密钥协商
                } else if (AFN.equals(AFN_SFRZMYXS)) {
                    //收到AFN=06H身份认证及密钥协商，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发身份认证及密钥协商，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发身份认证及密钥协商，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        System.out.println("该报文是终端被动上报，直接转发给主站");
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报身份认证及密钥协商，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求被级联终端主动上报
                } else if (AFN.equals(AFN_BJLZDZDSB)) {
                    //收到AFN=08H请求被级联终端主动上报，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求被级联终端主动上报，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求被级联终端主动上报，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("C")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 2;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端主动上报请求被级联终端主动上报，转发给主站(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求终端配置及信息
                } else if (AFN.equals(AFN_ZDPZXX)) {
                    //收到AFN=09H请求终端配置及信息，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求终端配置及信息，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求终端配置及信息，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报请求终端配置及信息，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //查询参数
                } else if (AFN.equals(AFN_PARAMS)) {
                    //收到AFN=0AH查询参数，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动查询参数，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动查询参数，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报查询参数，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求任务数据
                } else if (AFN.equals(AFN_REQUESTDASK)) {
                    //收到AFN=0AH查询参数，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求任务数据，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求任务数据，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报任务数据，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求1类数据
                } else if (AFN.equals(AFN_REQUEST1)) {
                    //收到AFN=0CH请求1类数据(实时数据)，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求1类数据(实时数据)，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求1类数据(实时数据)，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报1类数据(实时数据)，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    } else if (CParse.equals("C")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 2;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端主动上报1类数据(实时数据)，转发给主站(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求2类数据
                } else if (AFN.equals(AFN_REQUEST2)) {
                    //收到AFN=0DH请求2类数据(历史数据)，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求2类数据(历史数据)，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求2类数据(历史数据)，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报2类数据(历史数据)，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    } else if (CParse.equals("C")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 2;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端主动上报2类数据(历史数据)，转发给主站(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //请求3类数据
                } else if (AFN.equals(AFN_REQUEST3)) {
                    //收到AFN=0EH请求3类数据(事件数据)，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求3类数据(事件数据)，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动请求3类数据(事件数据)，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报3类数据(事件数据)，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    } else if (CParse.equals("C")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 2;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端主动上报3类数据(事件数据)，转发给主站(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //传输文件
                } else if (AFN.equals(AFN_FILECS)) {
                    //收到AFN=0FH传输文件，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发传输文件，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发传输文件，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报传输文件，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }

                    //数据转发
                } else if (AFN.equals(AFN_DATAFORWARD)) {
                    //收到AFN=10H数据转发，直接转发给主站或者终端
                    if (CParse.equals("4")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 0;
                        MSnetworkAddr = clientNetwork;
                        if (FEPMapService.getZDChannel(logicAddrtips) != null) {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发数据转发，转发给终端(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        } else {
                            logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是主站主动下发数据转发，终端尚未登录(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                        }
                    } else if (CParse.equals("8")) {
                        replyMessage = receiveMessage;
                        forwardFlag = 1;
                        logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger +"][(" + clientNetwork + ")报文是终端被动上报数据转发，转发给主站(" + FEPMSServerHandler.MSAddr + ")] : [" + formatMessage + "]" + "\n");
                    }
                }
            } else if (!receiveMessage.startsWith("68") || !receiveMessage.endsWith("16") || (receiveMessage.length() < 20) || totalLength != caclframeLength) {
                System.out.println("第二遍");
                logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger + "][(" + clientNetwork + ")前置机接收到TCP粘包拆包报文(" + logicAddrtips + ")] : [" + formatMessage + "]" + "\n");
                if (totalLength < caclframeLength || dismantlingFlag == 1) {
                    System.out.println("拆包");
                    StringBuilder appendFrame = new StringBuilder();
                    frameList.add(receiveMessage);
                    dismantlingFlag = 1;
                    for (String frame : frameList) {
                        appendFrame.append(frame);
                    }
                    String finalFrame = appendFrame.toString();
                    System.out.println("拆包最后拼接的帧" + finalFrame);
                    String demolitionBegin = finalFrame.substring(0, 12);
                    totalLength = finalFrame.length() / 2;//实际总长度
                    caclframeLength = CheckUtil.calcLength(demolitionBegin);//报文计算的总长度
                    if (finalFrame.startsWith("68") && finalFrame.endsWith("16") && finalFrame.length() > 20 && totalLength == caclframeLength) {
                        Parse(ParseUtil.hex2byte(finalFrame.getBytes()), ctx);
                        dismantlingFlag = 0;
                        if (!frameList.isEmpty()) {
                            frameList.clear();
                        }
                    }
                } else if (totalLength > caclframeLength || dismantlingFlag == 2) {
                    System.out.println("粘包");
                    if (afterFrame == null) {
                        dismantlingFlag = 2;
                        String stickBegin = receiveMessage.substring(0, 12);
                        int stickcaclframeLength = CheckUtil.calcLength(stickBegin) * 2; //粘包前帧长度
                        System.out.println(stickcaclframeLength);
                        String firstFrame = receiveMessage.substring(0, stickcaclframeLength); //粘包首帧
                        Parse(ParseUtil.hex2byte(firstFrame.getBytes()), ctx);
                        afterFrame = receiveMessage.substring(stickcaclframeLength); //粘包后续帧
                    } else if (!afterFrame.equals("") && dismantlingFlag == 2) {
                        String finalFrame = afterFrame + receiveMessage;
                        Parse(ParseUtil.hex2byte(finalFrame.getBytes()), ctx);
                        dismantlingFlag = 0;
                    }
                } else {
                    logger.info("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger + "][接收的报文格式或长度不正确!] : [" + formatMessage + "]" + "\n");
                }
            }
        }else {
            logger.warn("[" + FEPMSServerHandler.MSatomicInteger + "/" + FEPZDServerHandler.ZDatomicInteger + "][接收到空报文，无法处理...]" + "\n");
        }
    }
}
