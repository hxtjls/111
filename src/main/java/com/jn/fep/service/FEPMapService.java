package com.jn.fep.service;

import io.netty.channel.socket.SocketChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hxt
 * @create 2019-08-28 11:19
 * @description Map维护
 * @version 1.0
 */
public class FEPMapService {

    private static Map<String, SocketChannel> ZDMap = new HashMap<>();
    private static Map<String, SocketChannel> MSMap = new HashMap<>();

    public static void addZDMapChannel(String logicAddr, SocketChannel gateway_channel){
        ZDMap.put(logicAddr, gateway_channel);
    }

    public static Map<String, SocketChannel> getZDMap(){
        return ZDMap;
    }

    public static SocketChannel getZDChannel(String logicAddr){
        return ZDMap.get(logicAddr);
    }

    public static void removeZDMapChannel(String logicAddr){
        ZDMap.remove(logicAddr);
    }

    public static void addMSMapChannel(String clientMSAddress, SocketChannel gateway_channel){
        MSMap.put(clientMSAddress, gateway_channel);
    }

    public static Map<String, SocketChannel> getMSMap(){
        return MSMap;
    }

    public static SocketChannel getMSChannel(String clientMSAddress){
        return MSMap.get(clientMSAddress);
    }

    public static void removeMSMapChannel(String clientMSAddress){
        MSMap.remove(clientMSAddress);
    }
}
