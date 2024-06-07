package cn.huspace.temperature.temp.api;

import cn.huspace.temperature.temp.dto.DeviceDTO;
import cn.huspace.temperature.temp.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceApi {
    private final DeviceMapper deviceMapper;

    @Autowired
    public DeviceApi(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @GetMapping(path = "/getAllDevices")
    public List<DeviceDTO> getAllDevices() {
        return deviceMapper.selectAll();
    }
}
