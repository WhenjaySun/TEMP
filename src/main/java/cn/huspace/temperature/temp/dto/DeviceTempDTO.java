package cn.huspace.temperature.temp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class DeviceTempDTO {
    private String id;
    private String deviceId;
    private Double deviceTemp;
    private Date createTime;
}

