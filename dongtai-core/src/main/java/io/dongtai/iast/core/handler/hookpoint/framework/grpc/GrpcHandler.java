package io.dongtai.iast.core.handler.hookpoint.framework.grpc;

import io.dongtai.iast.core.EngineManager;
import io.dongtai.iast.core.handler.context.ContextManager;
import io.dongtai.iast.core.handler.hookpoint.IastClassLoader;
import io.dongtai.iast.core.handler.hookpoint.SpyDispatcherImpl;
import io.dongtai.iast.core.handler.hookpoint.controller.impl.SourceImpl;
import io.dongtai.iast.core.handler.hookpoint.graphy.GraphBuilder;
import io.dongtai.iast.core.handler.hookpoint.models.MethodEvent;
import io.dongtai.iast.core.service.ErrorLogReport;
import io.dongtai.iast.core.utils.*;
import io.dongtai.log.DongTaiLog;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class GrpcHandler {
    private static IastClassLoader gRpcClassLoader;
    public static File grpcPluginPath;
    private static Method methodOfInterceptChannel;
    private static Method methodOfInterceptService;
    private static Method methodOfGetRequestMetadata;
    private static ThreadLocal<String> sharedTraceId = new ThreadLocal<String>();
    public static ThreadLocal<Boolean> addCustomResp = new ThreadLocal<Boolean>();
    private static ThreadLocal<Set<Object>> sharedRespData = new ThreadLocal<Set<Object>>();

    static {
        grpcPluginPath = new File(System.getProperty("java.io.tmpdir.dongtai") + "iast" + File.separator + "dongtai-grpc.jar");
        if (!grpcPluginPath.exists()) {
            HttpClientUtils.downloadRemoteJar("/api/v1/engine/download?engineName=dongtai-grpc", grpcPluginPath.getAbsolutePath());
        }
    }

    /**
     * 根据 channel 或 service 创建继承自 grpc classloader 的自定义类加载器，后续创建拦截器用于获取相关数据
     *
     * @param channel
     */
    private static void createClassLoader(Object channel) {
        try {
            if (gRpcClassLoader != null) {
                return;
            }
            gRpcClassLoader = new IastClassLoader(
                    channel.getClass().getClassLoader(),
                    new URL[]{grpcPluginPath.toURI().toURL()}
            );

            Class<?> classOfGrpcProxy = gRpcClassLoader.loadClass("io.dongtai.plugin.GrpcProxy");
            methodOfInterceptChannel = classOfGrpcProxy
                    .getDeclaredMethod("interceptChannel", Object.class, Object.class);
            methodOfInterceptService = classOfGrpcProxy
                    .getDeclaredMethod("interceptService", Object.class);
            methodOfGetRequestMetadata = classOfGrpcProxy.getDeclaredMethod("getServerMeta");
            grpcPluginPath.delete();
        } catch (MalformedURLException | NoSuchMethodException e) {
            DongTaiLog.error(e);
        }
    }

    public static void setSharedTraceId(String traceId) {
        sharedTraceId.set(traceId);
    }

    /**
     * 拦截 Grpc client 的 channel，后续client调用Server端服务会经过该拦截器
     *
     * @param channel
     * @return
     */
    public static Object interceptChannel(Object channel) {
        if (methodOfInterceptChannel == null) {
            createClassLoader(channel);
        }
        try {
            return methodOfInterceptChannel.invoke(null, channel, new GrpcTraceManager());
        } catch (Exception e) {
            DongTaiLog.error(e);
        }

        return channel;
    }

    /**
     * 拦截 Grpc 的 Service，后续服务的调用会经过该拦截器
     *
     * @param service
     * @return
     */
    public static Object interceptService(Object service) {
        try {
            if (methodOfInterceptService == null) {
                createClassLoader(service);
            }
            return methodOfInterceptService.invoke(null, service);
        } catch (Exception e) {
            DongTaiLog.error(e);
        }
        return service;
    }

    /**
     * 进入 Grpc 处理逻辑，获取请求元数据，初始化 agent 状态
     * todo: 细化元数据，保证采集到的数据可人工查看
     */
    public static void startTrace() {
        try {
            Map<String, Object> metadata = (Map<String, Object>) methodOfGetRequestMetadata.invoke(null);
            if (metadata.containsKey("dt-traceid")) {
                ContextManager.getOrCreateGlobalTraceId(String.valueOf(metadata.get("dt-traceid")), EngineManager.getAgentId());
            } else {
                String newTraceId = ContextManager.getOrCreateGlobalTraceId(null, EngineManager.getAgentId());
                metadata.put("dt-traceid", newTraceId);
            }
            Map<String, Object> requestMeta = new HashMap<String, Object>();
            requestMeta.put("method", "RPC");
            requestMeta.put("protocol", "ProtoBuf");
            requestMeta.put("scheme", "grpc");
            requestMeta.put("requestURL", metadata.get("serverAddr") + "/" + metadata.get("requestURI"));
            requestMeta.put("requestURI", metadata.get("requestURI"));
            requestMeta.put("headers", metadata);
            EngineManager.REQUEST_CONTEXT.set(requestMeta);
            EngineManager.TRACK_MAP.set(new HashMap<Integer, MethodEvent>(1024));
            EngineManager.TAINT_POOL.set(new HashSet<Object>());
            EngineManager.TAINT_HASH_CODES.set(new HashSet<Integer>());
            EngineManager.SCOPE_TRACKER.get().enterGrpc();
            EngineManager.turnOnDongTai();
        } catch (Exception e) {
            DongTaiLog.error(e);
        }
    }

    /**
     * 关闭Grpc的Call调用，上报数据到Server端并清空agent状态
     */
    public static void closeGrpcCall() {
        try {
            if (EngineManager.isEnterEntry(null)) {
                EngineManager.turnOffDongTai();
                EngineManager.SCOPE_TRACKER.get().leaveGrpc();
                EngineManager.maintainRequestCount();
                GraphBuilder.buildAndReport(null, null);
                EngineManager.cleanThreadState();
            }
        } catch (Exception e) {
            ErrorLogReport.sendErrorLog(e);
            EngineManager.cleanThreadState();
        } finally {
            EngineManager.turnOnDongTai();
        }
    }

    public static void blockingUnaryCall(Object req, Object res) {
        // todo: 判断 req 的相关自定义对象是否与污点有关
        if (EngineManager.TAINT_POOL.isEmpty()) {
            return;
        }

        MethodEvent event = new MethodEvent(
                0,
                0,
                "io.grpc.stub.ClientCalls",
                "io.grpc.stub.ClientCalls",
                "blockingUnaryCall",
                "io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel, io.grpc.MethodDescriptor<ReqT,RespT>, io.grpc.CallOptions, ReqT)",
                "io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel, io.grpc.MethodDescriptor<ReqT,RespT>, io.grpc.CallOptions, ReqT)",
                null,
                new Object[]{req},
                res,
                "GRPC",
                false,
                null
        );
        Set<Object> modelItems = SourceImpl.parseCustomModel(req);
        boolean isHitTaints = false;
        for (Object item : modelItems) {
            isHitTaints = isHitTaints || TaintPoolUtils.poolContains(item, event, false);
        }
        if (isHitTaints) {
            int invokeId = SpyDispatcherImpl.INVOKE_ID_SEQUENCER.getAndIncrement();
            event.setInvokeId(invokeId);
            event.setPlugin("GRPC");
            // todo: 获取 service name
            event.setServiceName("");
            // todo: 获取 traceId
            event.setTraceId(sharedTraceId.get());
            event.setCallStack(StackUtils.getLatestStack(5));
            EngineManager.TRACK_MAP.addTrackMethod(invokeId, event);
            Set<Object> resModelItems = SourceImpl.parseCustomModel(res);
            sharedRespData.remove();
            Set<Object> taintPool = EngineManager.TAINT_POOL.get();
            Set<Object> resModelSet = new HashSet<Object>();
            for (Object obj : resModelItems) {
                // fixme: 暂时只跟踪字符串相关内容
                if (obj instanceof String) {
                    resModelSet.add(obj);
                    addCustomResp.set(true);
                    taintPool.add(obj);
                    int identityHashCode = System.identityHashCode(obj);
                    event.addTargetHash(identityHashCode);
                    event.addTargetHashForRpc(obj.hashCode());
                    EngineManager.TAINT_HASH_CODES.get().add(identityHashCode);
                }
            }
            sharedRespData.set(resModelSet);
        }
    }

    public static void sendMessage(Object message) {
        if (EngineManager.TAINT_POOL.isEmpty()) {
            return;
        }

        MethodEvent event = new MethodEvent(
                0,
                0,
                "io.grpc.internal.ServerCallImpl",
                "io.grpc.internal.ServerCallImpl",
                "sendMessage",
                "io.grpc.internal.ServerCallImpl.sendMessage(RespT)",
                "io.grpc.internal.ServerCallImpl.sendMessage(RespT)",
                null,
                new Object[]{message},
                null,
                "GRPC",
                false,
                null
        );
        Set<Object> modelItems = SourceImpl.parseCustomModel(message);
        boolean isHitTaints = false;
        for (Object item : modelItems) {
            isHitTaints = isHitTaints || TaintPoolUtils.poolContains(item, event, false);
        }
        if (isHitTaints) {
            int invokeId = SpyDispatcherImpl.INVOKE_ID_SEQUENCER.getAndIncrement();
            event.setInvokeId(invokeId);
            event.setPlugin("GRPC");
            event.setServiceName("");
            event.setProjectPropagatorClose(true);
            event.setCallStack(StackUtils.getLatestStack(5));
            EngineManager.TRACK_MAP.addTrackMethod(invokeId, event);
        }
    }

    public static void toStringUtf8(Object value) {
        Boolean added = addCustomResp.get();
        if (added != null && added) {
            if (sharedRespData.get().contains(value)) {
                EngineManager.TAINT_POOL.addToPool(value);
            }
        }
    }
}
