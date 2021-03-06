package com.xcxcxcxcx.client.storage.cache;

import com.xcxcxcxcx.client.storage.abs.MessageInfo;
import com.xcxcxcxcx.client.storage.abs.MessageStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public final class CacheCenter implements MessageStorage {

    /**
     * 用map，类似建立了主键索引
     *
     * @param messageInfos
     */
    private final Map<Long, MessageInfo> messageInfoMap1 = new ConcurrentHashMap<>();

    private final Map<Long, MessageInfo> messageInfoMap2 = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        MessageStorage messageStorage = new CacheCenter();
        List<MessageInfo> messageInfos = new ArrayList<>();
        for(int i = 0; i < 9999999; i++){
            messageInfos.add(new MessageInfo().setId((long)i));
        }
        List<Long> messageIds = messageInfos.stream().map(messageInfo -> messageInfo.getId()).collect(Collectors.toList());
        messageStorage.remotePush(messageInfos);
        messageStorage.localPushAck(messageIds);
        messageStorage.remotePushAck(messageIds);
        messageStorage.remotePull(messageInfos);
        messageStorage.localPullAck(messageIds);
        messageStorage.remotePullAck(messageIds);
    }

    /**
     * 线程不安全的，这里的messageInfos在遍历过程中不允许改变，否则需要copy处理而耗费空间
     * 从程序上考虑，这里的messageInfos是不可变对象。
     * <p>
     * status = 0
     *
     * @param messageInfos
     */
    @Override
    public void remotePush(List<MessageInfo> messageInfos) {
        if(messageInfos == null || messageInfos.isEmpty()){
            return;
        }
        for (MessageInfo info : messageInfos) {
            messageInfoMap1.putIfAbsent(info.getId().longValue(), info);
        }
    }

    /**
     * status = 1
     *
     * @param ids
     */
    @Override
    public void localPushAck(List<Long> ids) {
        if(ids == null || ids.isEmpty()){
            return;
        }
        for (Long id : ids) {
            messageInfoMap1.get(id).setStatus(1);
        }
    }

    /**
     * remove status = 1 的消息
     *
     * @param ids
     */
    @Override
    public void remotePushAck(List<Long> ids) {
        if(ids == null || ids.isEmpty()){
            return;
        }
        synchronized (messageInfoMap1) {
            for (Long id : ids) {
                MessageInfo messageInfo = messageInfoMap1.get(id);
                if(messageInfo == null){
                    continue;
                }
                if (messageInfo.getStatus() == 1) {
                    messageInfoMap1.remove(id);
                }
            }
        }
    }

    /**
     * get status = 1 and has expired
     *
     * @return
     */
    @Override
    public List<Long> getPushAckFailedMessageId() {
        List<Long> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, MessageInfo> entry : messageInfoMap1.entrySet()) {
            MessageInfo info = entry.getValue();
            if (info.getStatus() == 1 && info.getExpired() < now) {
                result.add(info.getId());
            }
        }
        return result.size() == 0 ? null : result;
    }

    /**
     * status = 2
     *
     * @param messageInfos
     */
    @Override
    public void remotePull(List<MessageInfo> messageInfos) {
        if(messageInfos == null || messageInfos.isEmpty()){
            return;
        }
        for (MessageInfo info : messageInfos) {
            messageInfoMap2.putIfAbsent(info.getId().longValue(), info);
        }
    }

    /**
     * set status = 3
     *
     * @param ids
     */
    @Override
    public void localPullAck(List<Long> ids) {
        if(ids == null){
            return;
        }
        for (Long id : ids) {
            messageInfoMap2.get(id).setStatus(3);
        }
    }

    @Override
    public void localPullReject(List<Long> ids) {
        if(ids == null){
            return;
        }
        for (Long id : ids) {
            messageInfoMap2.get(id).setStatus(4);
        }
    }


    /**
     * remove status = 3
     * @param ids
     */
    @Override
    public void remotePullAck(List<Long> ids) {
        if(ids == null){
            return;
        }
        synchronized (messageInfoMap2) {
            for (Long id : ids) {
                MessageInfo messageInfo = messageInfoMap2.get(id);
                if(messageInfo == null){
                    continue;
                }
                if (messageInfo.getStatus() == 3) {
                    messageInfoMap2.remove(id);
                }
            }
        }
    }

    @Override
    public void remotePullReject(List<Long> ids) {
        if(ids == null){
            return;
        }
        synchronized (messageInfoMap2) {
            for (Long id : ids) {
                MessageInfo messageInfo = messageInfoMap2.get(id);
                if(messageInfo == null){
                    continue;
                }
                if (messageInfo.getStatus() == 4) {
                    messageInfoMap2.remove(id);
                }
            }
        }
    }

    /**
     * get status = 3 and has expired
     * @return
     */
    @Override
    public List<Long> getPullAckFailedMessageId() {
        List<Long> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, MessageInfo> entry : messageInfoMap2.entrySet()) {
            MessageInfo info = entry.getValue();
            if (info.getStatus() == 3 && info.getExpired() < now) {
                result.add(info.getId());
            }
        }
        return result.size() == 0 ? null : result;
    }

    @Override
    public List<Long> getPullRejectFailedMessageId() {
        List<Long> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, MessageInfo> entry : messageInfoMap2.entrySet()) {
            MessageInfo info = entry.getValue();
            if (info.getStatus() == 4 && info.getExpired() < now) {
                result.add(info.getId());
            }
        }
        return result.size() == 0 ? null : result;
    }
}
