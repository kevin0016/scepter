package com.kevin.communication.core.session;

import com.alibaba.fastjson.JSON;
import com.kevin.communication.core.config.ServerConstant;
import com.kevin.communication.utils.CreateSessionKey;
import com.kevin.message.protocol.message.HeartBeatMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: kevin
 * @description: session连接管理器
 * @updateRemark: 修改内容(每次大改都要写修改内容)
 * @date: 2019-07-29 19:04
 */
public class SessionManager {

    private final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);

    /**
     * 设备ID
     */
    public static final AttributeKey<String> ATTR_KEY_DEVICEID = AttributeKey.valueOf("DEVICEID");

    private static final SessionManager INSTANCE = new SessionManager();

    private Map<String, Session> sessionMap = new ConcurrentHashMap<>(ServerConstant.DEFAULT_SESSION_INIT_SIZE);

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取所有的sessionMap
     * @return
     */
    public Map<String, Session> getSessionMap() {
        return sessionMap;
    }


    /**
     * 创建session
     *
     * @param ctx
     * @param msg
     * @return
     */
    public Session createSession(ChannelHandlerContext ctx, HeartBeatMessage msg) {
        String deviceId = msg.getDeviceId();
        LOGGER.info("CONNECT deviceId <{}> for gateway", deviceId);
        String key = CreateSessionKey.inetSocketAddressByChannelHandlerContexts(ctx);
        if (key == null) {
            throw new RuntimeException("找不到key");
        }
        // 如果该设备已经存在，则关闭掉，重新建立一个新通道
        Session session = sessionMap.get(key);
        if (session != null) {
            //如果通道是一个，则不管
            if (session.getCtx() != ctx) {
                LOGGER.info("Found an existing connection with same device ID <{}>, forcing to close", deviceId);
                session.close();
            }
        }

        session = new DefaultSession(ctx, deviceId);
        LOGGER.info("这是地址" + key);
        sessionMap.put(key, session);
        //将设备ID绑定到channel上
        ctx.channel().attr(SessionManager.ATTR_KEY_DEVICEID).set(deviceId);

        return session;
    }

    /**
     * 通过心跳维持一个session
     *
     * @param ctx
     */
    public boolean keepSession(ChannelHandlerContext ctx) {
        try {
            return true;
        } catch (Exception e) {
            LOGGER.warn("PING fail");
            return false;
        }
    }

    /**
     * 根据ChannelHandlerContext获取Session
     *
     * @param ctx - ChannelHandlerContext
     * @return Session
     */
    public Session getSession(ChannelHandlerContext ctx) {
        String key = CreateSessionKey.inetSocketAddressByChannelHandlerContexts(ctx);
        if (key != null) {
            return getSession(key);
        }
        return null;
    }

    /**
     * 获取session
     *
     * @param ipPort - 设备地址
     * @return Session
     */
    public Session getSession(String ipPort) {
        return sessionMap.get(ipPort);
    }

    /**
     * 批量获取session
     *
     * @param deviceIds - 设备ID
     * @return Set<Session>
     */
//    public Set<Session> getSessions(String... deviceIds) {
//        if (deviceIds == null || deviceIds.length == 0) {
//            return Collections.emptySet();
//        }
//
//        Set<Session> sessions = new HashSet<>();
//        for (String deviceId : deviceIds) {
//            sessions.add(sessionMap.get(deviceId));
//        }
//        return sessions;
//    }

    /**
     * 从session管理器中移除
     *
     * @param session - Session
     */
    public void removeSession(Session session) {
        String key = CreateSessionKey.inetSocketAddressByChannelHandlerContexts(session.getCtx());
        //不管怎样，先移除走
        if (key != null) {
            sessionMap.remove(key);
        }
    }

    /**
     * 从session管理器中移除
     *
     * @param deviceId - deviceId
     */
//    public void removeSession(String deviceId) {
//        //不管怎样，先移除走
//        if (deviceId != null) {
//            sessionMap.remove(deviceId);
//        }
//    }

    /**
     * 获得ChannelHandlerContext绑定的设备ID
     *
     * @param ctx - ChannelHandlerContext
     * @return String
     */
    public String getDeviceId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(SessionManager.ATTR_KEY_DEVICEID).get();
    }

    @Override
    public String toString() {
        return "SessionManager : [" + "session size = " + sessionMap.size() + "]";
    }
}
