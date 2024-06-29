package cn.huspace.temperature.temp.utils;

import cn.huspace.temperature.temp.dto.DeviceDTO;
import cn.huspace.temperature.temp.dto.DeviceMemoryDTO;
import cn.huspace.temperature.temp.dto.DeviceTempDTO;
import cn.huspace.temperature.temp.mapper.DeviceMapper;
import cn.huspace.temperature.temp.mapper.DeviceMemoryMapper;
import cn.huspace.temperature.temp.mapper.DeviceTempMapper;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SshUtil {
    private final DeviceMapper deviceMapper;
    private final DeviceTempMapper deviceTempMapper;
    private final DeviceMemoryMapper deviceMemoryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public SshUtil(DeviceMapper deviceMapper, DeviceTempMapper deviceTempMapper,
                   DeviceMemoryMapper deviceMemoryMapper, StringRedisTemplate stringRedisTemplate) {
        this.deviceMapper = deviceMapper;
        this.deviceTempMapper = deviceTempMapper;
        this.deviceMemoryMapper = deviceMemoryMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String openwrtDeviceName = "zn-m2";
    private static final String openwrtHost = "10.98.0.1";
    private static final int openwrtPort = 55022;
    private static final String openwrtUser = "root";
    private static final String openwrtPassword = "tuji2langji";
    private static final String raspberryDeviceName = "raspberry";
    private static final String raspberryHost = "10.98.99.99";
    private static final int raspberryPort = 22;
    private static final String raspberryUser = "root";
    private static final String raspberryPassword = "tuji2langji";
    private static final String tempCommand = "cat /sys/class/thermal/thermal_zone0/temp";
    private static final String memoryCommand = "free | grep Mem | awk '{print $3/$2 * 100.0}'";

    public String recordAllDevicesTemp() {
        CompletableFuture<Map> openwrtInfoMapFuture = getOpenwrtInfoMap();
        CompletableFuture<Map> raspberryInfoMapFuture = getRaspberryInfoMap();
        CompletableFuture.allOf(openwrtInfoMapFuture, raspberryInfoMapFuture).join();
        double openwrtTemp;
        double raspberryTemp;
        double openwrtMemory;
        double raspberryMemory;
        try {
            openwrtTemp = Double.parseDouble((String) openwrtInfoMapFuture.get().get("temperature"));
            openwrtMemory = Double.parseDouble((String) openwrtInfoMapFuture.get().get("memory"));
            raspberryTemp = Double.parseDouble((String) raspberryInfoMapFuture.get().get("temperature")) / 1000.0;
            raspberryMemory = Double.parseDouble((String) raspberryInfoMapFuture.get().get("memory"));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> map = getDeviceFromRedisData();
        DeviceTempDTO openwrtTempDTO = new DeviceTempDTO(UUID.randomUUID().toString(), map.get(openwrtDeviceName), openwrtTemp, new Date());
        DeviceTempDTO raspberryTempDTO = new DeviceTempDTO(UUID.randomUUID().toString(), map.get(raspberryDeviceName), raspberryTemp, new Date());
        deviceTempMapper.insertAll(List.of(openwrtTempDTO, raspberryTempDTO));
        DeviceMemoryDTO openwrtMemoryDTO = new DeviceMemoryDTO(UUID.randomUUID().toString(), map.get(openwrtDeviceName), openwrtMemory, new Date());
        DeviceMemoryDTO raspberryMemoryDTO = new DeviceMemoryDTO(UUID.randomUUID().toString(), map.get(raspberryDeviceName), raspberryMemory, new Date());
        deviceMemoryMapper.insertAll(List.of(openwrtMemoryDTO, raspberryMemoryDTO));
        return "success";
    }

    // 获取远程ssh执行结果
    public Map<String, String> getSshResult(String host, int port, String user, String password) {
        JSch jsch = new JSch();
        Session session;
        Map<String, String> tempRes = new HashMap<>();

        try {
            session = jsch.getSession(user, host, port);
            session.setConfig("server_host_key",
                    "ssh-rsa,ssh-ed25519,ecdsa-sha2-nistp256," +
                            "ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256");
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            tempRes.put("temperature", sessionRes(session, tempCommand));
            tempRes.put("memory", sessionRes(session, memoryCommand));
            session.disconnect();
        } catch (JSchException | IOException e) {
            throw new RuntimeException(e);
        }
        return tempRes;
    }

    // EU获取远程ssh执行结果
    public Map<String, String> getEuSshResult(String host, int port, String user, String password) {
        JSch jsch = new JSch();
        Session session;
        Map<String, String> tempRes = new HashMap<>();

        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);

            // Set preferred authentication methods
            session.setConfig("PreferredAuthentications", "keyboard-interactive,password");

            // Set UserInfo to handle keyboard-interactive authentication
            session.setUserInfo(new MyUserInfo(password));

            // Optional: Disable host key checking if you are sure about the server's identity
            session.setConfig("StrictHostKeyChecking", "no");

            // Connect to the server
            session.connect();

            tempRes.put("status", sessionRes(session, "cd scripts && bash check_pm2.sh "));
            session.disconnect();
        } catch (JSchException | IOException e) {
            throw new RuntimeException(e);
        }
        return tempRes;
    }

    private static String sessionRes(Session session, String cmd) throws JSchException, IOException {
        String tempRes;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(cmd);

        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        tempRes = reader.readLine();

        channel.disconnect();
        return tempRes;
    }

    @Async
    public CompletableFuture<Map> getOpenwrtInfoMap() {
        return CompletableFuture.completedFuture(getSshResult(openwrtHost, openwrtPort, openwrtUser, openwrtPassword));
    }

    @Async
    public CompletableFuture<Map> getRaspberryInfoMap() {
        return CompletableFuture.completedFuture(getSshResult(raspberryHost, raspberryPort, raspberryUser, raspberryPassword));
    }

    /**
     * 获取redis数据
     *
     * @return Map 结果
     */
    public Map<String, String> getDeviceFromRedisData() {
        String openwrtDeviceId = stringRedisTemplate.opsForValue().get(openwrtDeviceName);
        String raspberryDeviceId = stringRedisTemplate.opsForValue().get(raspberryDeviceName);
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isNotBlank(openwrtDeviceId) && StringUtils.isNotBlank(raspberryDeviceId)) {
            result.put(openwrtDeviceName, openwrtDeviceId);
            result.put(raspberryDeviceName, raspberryDeviceId);
            return result;
        }
        List<DeviceDTO> deviceDTOS = deviceMapper.selectAll();
        for (DeviceDTO deviceDTO : deviceDTOS) {
            String id = deviceDTO.getId();
            String deviceName = deviceDTO.getDeviceName();
            stringRedisTemplate.opsForValue().set(deviceName, id, 1000, TimeUnit.SECONDS);
        }
        openwrtDeviceId = stringRedisTemplate.opsForValue().get(openwrtDeviceName);
        raspberryDeviceId = stringRedisTemplate.opsForValue().get(raspberryDeviceName);
        result.put(openwrtDeviceName, openwrtDeviceId);
        result.put(raspberryDeviceName, raspberryDeviceId);
        return result;
    }
}
