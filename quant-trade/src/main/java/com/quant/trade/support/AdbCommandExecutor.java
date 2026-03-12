package com.quant.trade.support;

import com.quant.trade.config.AndroidTradeProperties;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ADB 命令执行器。
 */
@Component
public class AdbCommandExecutor {

    private static final String REMOTE_UI_DUMP_PATH = "/sdcard/quant_uix.xml";

    @Resource
    private AndroidTradeProperties androidTradeProperties;

    public void launchApp(String packageName, String launchActivity) {
        if (StringUtils.isNotBlank(launchActivity)) {
            runCommand(List.of("shell", "am", "start", "-n", packageName + "/" + launchActivity));
            return;
        }
        runCommand(List.of("shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1"));
    }

    public void tap(int x, int y) {
        runCommand(List.of("shell", "input", "tap", String.valueOf(x), String.valueOf(y)));
    }

    public void inputText(String value) {
        String sanitized = value.replace(" ", "%s");
        runCommand(List.of("shell", "input", "text", sanitized));
    }

    public void pressDelete(int times) {
        for (int i = 0; i < times; i++) {
            runCommand(List.of("shell", "input", "keyevent", "67"));
        }
    }

    public String dumpCurrentScreenXml() {
        runCommand(List.of("shell", "uiautomator", "dump", REMOTE_UI_DUMP_PATH));
        return runCommand(List.of("shell", "cat", REMOTE_UI_DUMP_PATH));
    }

    public String runCommand(List<String> adbArgs) {
        List<String> command = new ArrayList<>();
        command.add(androidTradeProperties.getAdbPath());
        if (StringUtils.isNotBlank(androidTradeProperties.getDeviceSerial())) {
            command.add("-s");
            command.add(androidTradeProperties.getDeviceSerial());
        }
        command.addAll(adbArgs);
        try {
            Process process = new ProcessBuilder(command).start();
            boolean finished = process.waitFor(androidTradeProperties.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("adb command timeout: " + command);
            }
            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            if (process.exitValue() != 0) {
                throw new IllegalStateException("adb command failed: " + command + ", stderr=" + stderr);
            }
            return stdout;
        } catch (Exception e) {
            throw new IllegalStateException("adb command error: " + command, e);
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
