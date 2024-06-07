package cn.huspace.temperature.temp.api;

import cn.huspace.temperature.temp.enums.RecordedTypeEnum;
import cn.huspace.temperature.temp.dto.DeviceMemoryDTO;
import cn.huspace.temperature.temp.dto.DeviceTempDTO;
import cn.huspace.temperature.temp.mapper.DeviceMemoryMapper;
import cn.huspace.temperature.temp.mapper.DeviceTempMapper;
import cn.huspace.temperature.temp.utils.SshUtil;
import cn.huspace.temperature.temp.vo.DeviceDataVO;
import cn.huspace.temperature.temp.vo.DeviceInnerDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/temp")
@Slf4j
public class TempApi {
    private final SshUtil sshUtil;
    private final DeviceMemoryMapper deviceMemoryMapper;
    private final DeviceTempMapper deviceTempMapper;

    @Autowired
    public TempApi(SshUtil sshUtil, DeviceMemoryMapper deviceMemoryMapper, DeviceTempMapper deviceTempMapper) {
        this.sshUtil = sshUtil;
        this.deviceMemoryMapper = deviceMemoryMapper;
        this.deviceTempMapper = deviceTempMapper;
    }

    @GetMapping(path = "/recordAllDevicesTemp")
    public String recordAllDevicesTemp() {
        return sshUtil.recordAllDevicesTemp();
    }

    @GetMapping(path = "/getAllDeviceData/{limit}")
    public List<DeviceDataVO> getAllDeviceData(@PathVariable int limit) {
        Map<String, String> deviceInfo = sshUtil.getDeviceFromRedisData();
        String openwrtDeviceId = deviceInfo.get("zn-m2");
        String raspberryDeviceId = deviceInfo.get("raspberry");
        List<DeviceMemoryDTO> deviceMemoryDTOs = deviceMemoryMapper.selectAllLimit(limit * 2);
        List<DeviceTempDTO> deviceTempDTOs = deviceTempMapper.selectAllLimit(limit * 2);
        List<DeviceDataVO> results = new ArrayList<>();
        List<DeviceInnerDataVO> openwrtTempLeaf = new ArrayList<>();
        results.add(new DeviceDataVO("zn-m2", RecordedTypeEnum.TEMPERATURE, openwrtTempLeaf));
        List<DeviceInnerDataVO> openwrtMemoryLeaf = new ArrayList<>();
        results.add(new DeviceDataVO("zn-m2", RecordedTypeEnum.MEMORY, openwrtMemoryLeaf));
        List<DeviceInnerDataVO> raspberryTempLeaf = new ArrayList<>();
        results.add(new DeviceDataVO("raspberry", RecordedTypeEnum.TEMPERATURE, raspberryTempLeaf));
        List<DeviceInnerDataVO> raspberryMemoryLeaf = new ArrayList<>();
        results.add(new DeviceDataVO("raspberry", RecordedTypeEnum.MEMORY, raspberryMemoryLeaf));
        deviceTempDTOs.forEach(deviceTempDTO -> {
            String deviceId = deviceTempDTO.getDeviceId();
            Double deviceTemp = deviceTempDTO.getDeviceTemp();
            Date createTime = deviceTempDTO.getCreateTime();
            if (deviceId.equals(openwrtDeviceId)) {
                openwrtTempLeaf.add(new DeviceInnerDataVO(deviceTemp, createTime));
            }
            if (deviceId.equals(raspberryDeviceId)) {
                raspberryTempLeaf.add(new DeviceInnerDataVO(deviceTemp, createTime));
            }
        });
        deviceMemoryDTOs.forEach(deviceMemoryDTO -> {
            String deviceId = deviceMemoryDTO.getDeviceId();
            Double deviceMemory = deviceMemoryDTO.getDeviceMemory();
            Date createTime = deviceMemoryDTO.getCreateTime();
            if (deviceId.equals(openwrtDeviceId)) {
                openwrtMemoryLeaf.add(new DeviceInnerDataVO(deviceMemory, createTime));
            }
            if (deviceId.equals(raspberryDeviceId)) {
                raspberryMemoryLeaf.add(new DeviceInnerDataVO(deviceMemory, createTime));
            }
        });
        openwrtTempLeaf.sort(Comparator.comparing(DeviceInnerDataVO::getRecordedDate));
        raspberryTempLeaf.sort(Comparator.comparing(DeviceInnerDataVO::getRecordedDate));
        openwrtMemoryLeaf.sort(Comparator.comparing(DeviceInnerDataVO::getRecordedDate));
        raspberryMemoryLeaf.sort(Comparator.comparing(DeviceInnerDataVO::getRecordedDate));
        return results;
    }
}
