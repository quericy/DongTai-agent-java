package io.dongtai.iast.core.bytecode.enhance.asm;

import org.objectweb.asm.commons.Method;

import java.lang.dongtai.SpyDispatcher;
import java.lang.dongtai.SpyDispatcherHandler;

/**
 * 常用的ASM method 集合 省得我到处声明
 *
 * @author luanjia@taobao.com
 * @date 16/5/21 Modified by dongzhiyong@huoxian.cn
 */
public interface AsmMethods {

    class InnerHelper {

        private InnerHelper() {
        }

        static Method getAsmMethod(final Class<?> clazz,
                                   final String methodName,
                                   final Class<?>... parameterClassArray) {
            return Method.getMethod(
                    SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod(clazz, methodName, parameterClassArray));
        }
    }

    Method SPY_HANDLER$getDispatcher = InnerHelper.getAsmMethod(
            SpyDispatcherHandler.class,
            "getDispatcher"
    );
    Method SPY$enterHttp = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterHttp"
    );
    Method SPY$leaveHttp = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leaveHttp",
            Object.class,
            Object.class
    );
    Method SPY$isFirstLevelHttp = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isFirstLevelHttp"
    );
    Method SPY$cloneRequest = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "cloneRequest",
            Object.class,
            boolean.class
    );
    Method SPY$cloneResponse = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "cloneResponse",
            Object.class,
            boolean.class
    );
    Method SPY$enterDubbo = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterDubbo"
    );
    Method SPY$leaveDubbo = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leaveDubbo",
            Object.class,
            Object.class
    );
    Method SPY$isFirstLevelDubbo = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isFirstLevelDubbo"
    );
    Method SPY$enterKafka = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterKafka",
            Object.class
    );
    Method SPY$kafkaBeforeSend = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "kafkaBeforeSend",
            Object.class
    );
    Method SPY$kafkaAfterPoll = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "kafkaAfterPoll",
            Object.class
    );
    Method SPY$leaveKafka = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leaveKafka"
    );
    Method SPY$enterSource = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterSource"
    );
    Method SPY$leaveSource = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leaveSource"
    );
    Method SPY$isFirstLevelSource = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isFirstLevelSource"
    );
    Method SPY$enterPropagator = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterPropagator"
    );
    Method SPY$leavePropagator = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leavePropagator"
    );
    Method SPY$isFirstLevelPropagator = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isFirstLevelPropagator"
    );
    Method SPY$enterSink = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "enterSink"
    );
    Method SPY$leaveSink = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "leaveSink"
    );
    Method SPY$isFirstLevelSink = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isFirstLevelSink"
    );
    Method SPY$collectMethodPool = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "collectMethodPool",
            Object.class,
            Object[].class,
            Object.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            boolean.class,
            int.class
    );
    Method SPY$clientInterceptor = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "clientInterceptor",
            Object.class
    );
    Method SPY$serverInterceptor = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "serverInterceptor",
            Object.class
    );
    Method SPY$startGrpcCall = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "startGrpcCall"
    );
    Method SPY$closeGrpcCall = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "closeGrpcCall"
    );
    Method SPY$blockingUnaryCall = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "blockingUnaryCall",
            Object.class,
            Object.class
    );
    Method SPY$sendMessage = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "sendMessage",
            Object.class
    );
    Method SPY$toStringUtf8 = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "toStringUtf8",
            Object.class
    );
    Method SPY$reportService = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "reportService",
            String.class,
            String.class,
            String.class,
            String.class,
            String.class
    );
    Method SPY$isReplayRequest = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isReplayRequest"
    );
    Method SPY$isNotReplayRequest = InnerHelper.getAsmMethod(
            SpyDispatcher.class,
            "isNotReplayRequest"
    );
}
