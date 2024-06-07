package cn.huspace.temperature.temp.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class DeviceInnerDataVO {
    private Double recordedData;
    private Date recordedDate;
}
