package cn.huspace.temperature.temp.vo;

import cn.huspace.temperature.temp.enums.RecordedTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeviceDataVO {
    private String deviceName;
    private RecordedTypeEnum recordedType;
    private List<DeviceInnerDataVO> deviceInnerDataVOList;
}
